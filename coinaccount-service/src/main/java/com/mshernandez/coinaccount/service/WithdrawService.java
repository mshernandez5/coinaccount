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
import com.mshernandez.coinaccount.service.exception.FeeEstimationException;
import com.mshernandez.coinaccount.service.exception.InvalidAddressException;
import com.mshernandez.coinaccount.service.exception.NotEnoughWithdrawableFundsException;
import com.mshernandez.coinaccount.service.exception.WithdrawRequestAlreadyExistsException;
import com.mshernandez.coinaccount.service.exception.WithdrawRequestNotFoundException;
import com.mshernandez.coinaccount.service.result.WithdrawRequestResult;
import com.mshernandez.coinaccount.service.util.BinarySearchCoinSelector;
import com.mshernandez.coinaccount.service.util.CoinSelectionResult;
import com.mshernandez.coinaccount.service.util.CoinSelector;
import com.mshernandez.coinaccount.service.util.DepositShareEvaluator;
import com.mshernandez.coinaccount.service.util.MaxAmountCoinSelector;
import com.mshernandez.coinaccount.service.wallet_rpc.WalletService;
import com.mshernandez.coinaccount.service.wallet_rpc.exception.WalletResponseException;
import com.mshernandez.coinaccount.service.wallet_rpc.parameter.CreateRawTransactionInput;
import com.mshernandez.coinaccount.service.wallet_rpc.result.EstimateSmartFeeResult;

import static com.mshernandez.coinaccount.service.util.TXFeeUtilities.*;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;

/**
 * Initiates, cancels, and completes withdraw requests.
 */
@ApplicationScoped
public class WithdrawService
{
    // Match sus Address Patterns
    private static final Pattern SUS_PATTERN = Pattern.compile(".*[^a-zA-Z0-9].*");

    @ConfigProperty(name = "coinaccount.internal.account")
    UUID internalAccountId;

    @ConfigProperty(name = "coinaccount.withdraw.target")
    int blockConfirmationTarget;

    @ConfigProperty(name = "coinaccount.withdraw.expire")
    long withdrawExpireTime;

    @Inject
    Logger logger;

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
     * until further confirmation is received.
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
        // Calculate Base TX Size Excluding Input Counter & Inputs (Unknown At This Point)
        double vsize = TX_VERSION_VSIZE + TX_LOCKTIME_VSIZE + TX_SEGWIT_MARKER_VSIZE + getCounterByteSize(2);
        // Calculate Size Of Output To External Address
        try
        {
            vsize += getOutputSize(walletService.getAddressInfo(destAddress).getScriptPubKey());
        }
        catch (WalletResponseException e)
        {
            throw new InvalidAddressException();
        }
        // Calculate Size Of Change Output
        vsize += getOutputSize(walletService.getAddressInfo(internalAccount.getDepositAddress()).getScriptPubKey());
        // Get Current Fee Rate Estimate
        EstimateSmartFeeResult estimateSmartFeeResult = walletService.estimateSmartFee(blockConfirmationTarget);
        if (estimateSmartFeeResult.getErrors() != null)
        {
            for (String errorMessage : estimateSmartFeeResult.getErrors())
            {
                logger.log(Level.WARN, "Error estimating fees for withdrawal!"
                    + " Your node may not have been running long enough to properly estimate fees."
                    + " Error Message: " + errorMessage);
            }
            throw new FeeEstimationException();
        }
        long feeRateKb = estimateSmartFeeResult.getFeeRate().getSatAmount();
        double feeRateByte = feeRateKb / 1000.0;
        long totalFees = (long) Math.ceil(vsize * feeRateByte);
        // Select Input Deposits Considering Fees
        DepositShareEvaluator evaluator = new DepositShareEvaluator(initiator, feeRateByte);
        CoinSelectionResult<Deposit> selectionResult;
        if (withdrawAll)
        {
            CoinSelector<Deposit> coinSelector = new MaxAmountCoinSelector<>();
            selectionResult = coinSelector.selectInputs(evaluator, initiator.getDeposits(), totalFees);
        }
        else
        {
            CoinSelector<Deposit> coinSelector = new BinarySearchCoinSelector<>();
            selectionResult = coinSelector.selectInputs(evaluator, initiator.getDeposits(), amount);
        }
        if (!selectionResult.isValid())
        {
            throw new CannotAffordFeesException();
        }
        Set<Deposit> inputDeposits = selectionResult.getSelection();
        // Calculate Final TX Size Based On Selected Inputs
        vsize += selectionResult.getSelectionCost();
        totalFees = (long) Math.ceil(vsize * feeRateByte);
        logger.log(Level.INFO, String.format("A withdraw request is being created with vsize %.2f costing %d.", vsize, totalFees));
        // Begin Creating Transaction By Specifying UTXOs Corresponding To Selected Deposits
        long totalInputValue = 0L;
        long totalOwnedValue = 0L;
        Set<CreateRawTransactionInput> txInputs = new HashSet<>();
        for (Deposit inputDeposit : inputDeposits)
        {
            txInputs.add(new CreateRawTransactionInput(inputDeposit.getTXID(), inputDeposit.getVout()));
            totalInputValue += inputDeposit.getTotal();
            totalOwnedValue += inputDeposit.getShare(initiator);
        }
        // Specify TX Output Addresses & Amounts
        long withdrawAmount = withdrawAll ? (totalOwnedValue - totalFees) : amount;
        long changeAmount = totalInputValue - (withdrawAmount + totalFees);
        // Should Never Happen, But Should Not Be Ignored If It Did
        if (withdrawAmount < 0L || changeAmount < 0L)
        {
            logger.log(Level.ERROR, "Severe Input Selection / Fee Calculation Error");
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
        // Persist Withdraw Request For Future Confirmation With Timestamp
        long timestamp = System.currentTimeMillis();
        WithdrawRequest request = new WithdrawRequest(withdrawTxid, initiator, inputDeposits, withdrawAmount, totalFees, signedTxHex, timestamp);
        withdrawRequestDao.persist(request);
        // Lock Input Deposits, Prevent Attempts By Other Accounts To Withdraw Same UTXOs
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
            long updatedAmount = lockedDeposit.getShare(initiatorAccount) + lockedAmount;
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