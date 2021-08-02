package com.mshernandez.coinaccount.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.mshernandez.coinaccount.dao.AccountDao;
import com.mshernandez.coinaccount.dao.DepositDao;
import com.mshernandez.coinaccount.dao.WithdrawRequestDao;
import com.mshernandez.coinaccount.entity.Account;
import com.mshernandez.coinaccount.entity.Deposit;
import com.mshernandez.coinaccount.entity.WithdrawRequest;
import com.mshernandez.coinaccount.service.exception.CannotAffordFeesException;
import com.mshernandez.coinaccount.service.exception.InvalidAddressException;
import com.mshernandez.coinaccount.service.exception.NotEnoughWithdrawableFundsException;
import com.mshernandez.coinaccount.service.exception.WithdrawRequestAlreadyExistsException;
import com.mshernandez.coinaccount.service.exception.WithdrawRequestNotFoundException;
import com.mshernandez.coinaccount.service.result.WithdrawRequestResult;
import com.mshernandez.coinaccount.service.util.BinarySearchCoinSelector;
import com.mshernandez.coinaccount.service.util.CoinSelector;
import com.mshernandez.coinaccount.service.util.DepositShareEvaluator;
import com.mshernandez.coinaccount.service.util.MaxAmountCoinSelector;
import com.mshernandez.coinaccount.service.wallet_rpc.WalletService;
import com.mshernandez.coinaccount.service.wallet_rpc.exception.WalletResponseException;
import com.mshernandez.coinaccount.service.wallet_rpc.parameter.CreateRawTransactionInput;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Initiates, cancels, and completes withdraw requests.
 */
@ApplicationScoped
public class WithdrawService
{
    // Match sus Address Patterns
    private static final Pattern SUS_PATTERN = Pattern.compile(".*[^a-zA-Z0-9].*");

    // Base Size + 2 Outputs (Destination & Change)
    private static final int BASE_WITHDRAW_TX_SIZE = 10 + (34 * 2);

    // Additional Size For Each Input (Including +1 Uncertainty Assuming Worst Case)
    private static final int P2PKH_INPUT_VSIZE = 149;

    @ConfigProperty(name = "coinaccount.internal.account")
    UUID internalAccountId;

    @ConfigProperty(name = "coinaccount.withdraw.target")
    int blockConfirmationTarget;

    @ConfigProperty(name = "coinaccount.withdraw.expire")
    long withdrawExpireTime;

    @Inject
    WalletService walletService;

    @Inject
    AccountDao accountDao;

    @Inject
    DepositDao depositDao;

    @Inject
    WithdrawRequestDao withdrawRequestDao;

    /**
     * Initiates a withdrawal that will not be sent to the network
     * until player confirmation is received.
     * 
     * @param initiatorId The ID of the account initiating the withdrawal.
     * @param destAddress The wallet address the account is attempting to withdraw to.
     * @param withdrawAll If true, ignores the amount and withdraws all balances greater than the fees required to withdraw them.
     * @param amount The amount the account is attempting to withdraw, excluding fees.
     * @return An object holding withdraw details including determined fees.
     * @throws InvalidAddressException If an invalid address is given.
     * @throws NotEnoughWithdrawableFundsException If the account does not have enough funds to complete the withdrawal.
     * @throws CannotAffordFeesException If the account cannot afford the fees to complete the withdrawal.
     */
    @Transactional
    public WithdrawRequestResult initiateWithdrawRequest(UUID initiatorId, String destAddress, boolean withdrawAll, long amount)
    {
        Account initiator = accountDao.findOrCreate(initiatorId);
        // Prevent Possibility Of JSON-RPC Injection, Just In Case
        if (SUS_PATTERN.matcher(destAddress).matches())
        {
            throw new InvalidAddressException();
        }
        // Make Sure No Existing Request Already Exists
        if (initiator.getWithdrawRequest() != null)
        {
            throw new WithdrawRequestAlreadyExistsException();
        }
        // Begin Withdraw
        if (!withdrawAll && initiator.calculateWithdrawableBalance() < amount)
        {
            throw new NotEnoughWithdrawableFundsException();
        }
        Account internalAccount = accountDao.findOrCreate(internalAccountId);
        // Select Input Deposits
        long feeRateSatKb = walletService.estimateSmartFee(blockConfirmationTarget).getFeeRate().getSatAmount();
        long inputFee = (long) Math.ceil(P2PKH_INPUT_VSIZE * feeRateSatKb / 1000.0);
        long totalFees = (long) Math.ceil(BASE_WITHDRAW_TX_SIZE * feeRateSatKb / 1000.0);
        CoinSelector<Deposit> coinSelector;
        if (withdrawAll)
        {
            coinSelector = new MaxAmountCoinSelector<>();
        }
        else
        {
            coinSelector = new BinarySearchCoinSelector<>();
        }
        Set<Deposit> inputDeposits = coinSelector.selectInputs(new DepositShareEvaluator(initiator, inputFee), initiator.getDeposits(), amount);
        if (inputDeposits == null)
        {
            throw new CannotAffordFeesException();
        }
        // Get Corresponding Input UTXOs
        long totalInputValue = 0L;
        long totalOwnedValue = 0L;
        Set<CreateRawTransactionInput> txInputs = new HashSet<>();
        for (Deposit inputDeposit : inputDeposits)
        {
            txInputs.add(new CreateRawTransactionInput(inputDeposit.getTXID(), inputDeposit.getVout()));
            totalInputValue += inputDeposit.getTotal();
            totalOwnedValue += inputDeposit.getShare(initiator);
        }
        // Form TX Outputs
        long withdrawAmount = withdrawAll ? (totalOwnedValue - totalFees) : amount;
        long changeAmount = totalInputValue - (withdrawAmount + totalFees);
        // Make Sure That Account Can Afford Combined Fees
        if (withdrawAmount <= 0L)
        {
            throw new CannotAffordFeesException();
        }
        Map<String, Long> txOutputs = new HashMap<>();
        txOutputs.put(destAddress, withdrawAmount);
        if (changeAmount > 0L)
        {
            txOutputs.put(internalAccount.getDepositAddress(), changeAmount);
        }
        // Build Withdraw TX
        String unsignedTxHex;
        try
        {
            unsignedTxHex = walletService.createRawTransaction(txInputs, txOutputs);
        }
        catch (WalletResponseException e)
        {
            throw new InvalidAddressException();
        }
        String signedTxHex = walletService.signRawTransactionWithWallet(unsignedTxHex).getHex();
        String withdrawTxid = walletService.decodeRawTransaction(signedTxHex).getTxid();
        // Save Records
        long timestamp = System.currentTimeMillis();
        WithdrawRequest request = new WithdrawRequest(withdrawTxid, initiator, inputDeposits, withdrawAmount, totalFees, signedTxHex, timestamp);
        withdrawRequestDao.persist(request);
        // Lock Input Deposits
        long remainingHoldAmount = request.getWithdrawAmount() + request.getFeeAmount();
        for (Deposit inputDeposit : request.getInputs())
        {
            if (remainingHoldAmount != 0)
            {
                long depositValue = inputDeposit.getShare(initiator);
                if (depositValue <= remainingHoldAmount)
                {
                    inputDeposit.setShare(initiator, 0L);
                    inputDeposit.setShare(internalAccount, depositValue);
                    remainingHoldAmount -= depositValue;
                }
                else
                {
                    inputDeposit.setShare(initiator, depositValue - remainingHoldAmount);
                    inputDeposit.setShare(internalAccount, remainingHoldAmount);
                    remainingHoldAmount = 0L;
                }
            }
            inputDeposit.setWithdrawLock(request);
            depositDao.update(inputDeposit);
        }
        initiator.setWithdrawRequest(request);
        accountDao.update(initiator);
        accountDao.update(internalAccount);
        return new WithdrawRequestResult()
            .setTxid(withdrawTxid)
            .setWithdrawAmount(withdrawAmount)
            .setFeeAmount(totalFees)
            .setTotalCost(withdrawAmount + totalFees);
    }

    /**
     * Cancel the given withdraw request, restoring
     * reserved funds to the owner and unlocking the
     * deposits involved for future withdrawals.
     * <p>
     * SHOULD NOT BE USED ON A COMPLETED WITHDRAW REQUEST!
     * 
     * @param withdrawTxid The TXID of the withdraw request.
     * @throws WithdrawRequestNotFoundException If the request was not found.
     */
    @Transactional
    public void cancelWithdraw(String withdrawTxid)
    {
        WithdrawRequest withdrawRequest = withdrawRequestDao.find(withdrawTxid);
        if (withdrawRequest == null || withdrawRequest.isComplete())
        {
            throw new WithdrawRequestNotFoundException();
        }
        Account initiatorAccount = withdrawRequest.getAccount();
        Account internalAccount = accountDao.findOrCreate(internalAccountId);
        Set<Deposit> lockedDeposits = withdrawRequest.getInputs();
        for (Deposit lockedDeposit : lockedDeposits)
        {
            long lockedAmount = lockedDeposit.getShare(internalAccount);
            long updatedAmount = lockedDeposit.getShare(internalAccount) + lockedAmount;
            lockedDeposit.setShare(initiatorAccount, updatedAmount);
            lockedDeposit.setShare(internalAccount, 0L);
            lockedDeposit.setWithdrawLock(null);
            depositDao.update(lockedDeposit);
        }
        initiatorAccount.setWithdrawRequest(null);
        accountDao.update(initiatorAccount);
        accountDao.update(internalAccount);
        withdrawRequestDao.remove(withdrawRequest);
    }

    /**
     * Cancel the given withdraw request, restoring
     * reserved funds to the owner and unlocking the
     * deposits involved for future withdrawals.
     * 
     * @param initiatorId The UUID of the initiating account.
     * @return True if the request was found and canceled.
     * @throws WithdrawRequestNotFoundException If the request was not found.
     */
    @Transactional
    public void cancelWithdraw(UUID initiatorId)
    {
        Account initiator = accountDao.find(initiatorId);
        if (initiator == null || initiator.getWithdrawRequest() == null)
        {
            throw new WithdrawRequestNotFoundException();
        }
        cancelWithdraw(initiator.getWithdrawRequest().getTxid());
    }

    /**
     * Sends a pending withdraw transaction out to the network.
     * 
     * @param withdrawTxid The TXID of the withdraw request.
     * @return The TXID of the sent transaction.
     * @throws WithdrawRequestNotFoundException If the request was not found.
     */
    @Transactional
    public String completeWithdraw(String withdrawTxid)
    {
        WithdrawRequest withdrawRequest = withdrawRequestDao.find(withdrawTxid);
        if (withdrawRequest == null)
        {
            throw new WithdrawRequestNotFoundException();
        }
        Account internalAccount = accountDao.findOrCreate(internalAccountId);
        // Broadcast Transaction
        String txid = walletService.sendRawTransaction(withdrawRequest.getTxHex());
        // Clear Request From Initiator Account
        Account initiator = withdrawRequest.getAccount();
        initiator.setWithdrawRequest(null);
        // If No Change Will Be Received From TX, Request Can Be Removed Immediately
        boolean change = false;
        Set<Deposit> inputs = withdrawRequest.getInputs();
        for (Deposit input : inputs)
        {
            // Only Remember Deposits Contributing To Change
            if (input.getShare(internalAccount) == input.getTotal())
            {
                input.setShare(internalAccount, 0L);
                withdrawRequest.forgetInput(input);
                depositDao.remove(input);
            }
            else
            {
                change = true;
            }
        }
        if (!change)
        {
            withdrawRequestDao.remove(withdrawRequest);
        }
        else
        {
            withdrawRequest.setComplete();
            withdrawRequestDao.update(withdrawRequest);
        }
        accountDao.update(internalAccount);
        accountDao.update(initiator);
        return txid;
    }

    /**
     * Sends a pending withdraw transaction out to the network.
     * 
     * @param initiatorId The UUID of the initiating account.
     * @return The TXID of the sent transaction.
     * @throws WithdrawRequestNotFoundException If the request was not found.
     */
    @Transactional
    public String completeWithdraw(UUID initiatorId)
    {
        Account initiator = accountDao.find(initiatorId);
        if (initiator == null || initiator.getWithdrawRequest() == null)
        {
            throw new WithdrawRequestNotFoundException();
        }
        return completeWithdraw(initiator.getWithdrawRequest().getTxid());
    }

    /**
     * Get any active withdraw request for the
     * given account.
     * 
     * @param initiatorId The UUID of the initiating account.
     * @return An object containing information about the request or null if no request exists.
     */
    @Transactional
    public WithdrawRequestResult getWithdrawRequest(UUID initiatorId)
    {
        Account initiator = accountDao.find(initiatorId);
        if (initiator == null)
        {
            return null;
        }
        WithdrawRequest request = initiator.getWithdrawRequest();
        if (request == null)
        {
            return null;
        }
        return new WithdrawRequestResult()
            .setTxid(request.getTxid())
            .setWithdrawAmount(request.getWithdrawAmount())
            .setFeeAmount(request.getFeeAmount())
            .setTotalCost(request.getTotalCost());
    }

    /**
     * Cancels all expired withdraw requests.
     * 
     * @return A set of UUIDs corresponding to accounts with requests that were canceled.
     */
    @Transactional
    public Set<UUID> cancelExpiredRequests()
    {
        Collection<WithdrawRequest> requests = withdrawRequestDao.findAllIncomplete();
        Set<UUID> expiredRequestInitiatingAccounts = new HashSet<>();
        for (WithdrawRequest request : requests)
        {
            long timeSinceRequestInitiated = System.currentTimeMillis() - request.getTimestamp();
            if (timeSinceRequestInitiated < 0L || timeSinceRequestInitiated > withdrawExpireTime)
            {
                expiredRequestInitiatingAccounts.add(request.getAccount().getAccountUUID());
                cancelWithdraw(request.getTxid());
            }
        }
        return expiredRequestInitiatingAccounts;
    }
}