package com.mshernandez.vertconomy.core.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.persist.Transactional;
import com.mshernandez.vertconomy.core.VertconomyConfiguration;
import com.mshernandez.vertconomy.core.entity.Account;
import com.mshernandez.vertconomy.core.entity.AccountDao;
import com.mshernandez.vertconomy.core.entity.Deposit;
import com.mshernandez.vertconomy.core.entity.DepositDao;
import com.mshernandez.vertconomy.core.entity.WithdrawRequest;
import com.mshernandez.vertconomy.core.entity.WithdrawRequestDao;
import com.mshernandez.vertconomy.core.util.BinarySearchCoinSelector;
import com.mshernandez.vertconomy.core.util.CoinSelector;
import com.mshernandez.vertconomy.core.util.DepositShareEvaluator;
import com.mshernandez.vertconomy.core.util.MaxAmountCoinSelector;
import com.mshernandez.vertconomy.wallet_interface.RPCWalletConnection;
import com.mshernandez.vertconomy.wallet_interface.ResponseError;
import com.mshernandez.vertconomy.wallet_interface.exceptions.RPCErrorResponseException;
import com.mshernandez.vertconomy.wallet_interface.exceptions.WalletRequestException;
import com.mshernandez.vertconomy.wallet_interface.requests.RawTransactionInput;

/**
 * Helps initiate, cancel, and complete withdraw requests.
 */
@Singleton
public class WithdrawService
{
    private final Logger logger;

    private final RPCWalletConnection wallet;

    private final AccountDao accountDao;

    private final DepositDao depositDao;

    private final WithdrawRequestDao withdrawRequestDao;

    private final VertconomyConfiguration config;

    // Base Size + 2 Outputs (Destination & Change)
    private static final int BASE_WITHDRAW_TX_SIZE = 10 + (34 * 2);

    // Additional Size For Each Input (Including +1 Uncertainty Assuming Worst Case)
    private static final int P2PKH_INPUT_VSIZE = 149;

    /**
     * Create a new withdraw service instance.
     * 
     * @param logger A logger for this server to use.
     * @param wallet A wallet connection.
     * @param accountDao An account DAO.
     * @param depositDao A deposit DAO.
     * @param withdrawRequestDao A withdraw request DAO.
     * @param config A Vertconomy configuration object.
     */
    @Inject
    public WithdrawService(Logger logger, RPCWalletConnection wallet, AccountDao accountDao,
                           DepositDao depositDao, WithdrawRequestDao withdrawRequestDao,
                           VertconomyConfiguration config)
    {
        this.logger = logger;
        this.wallet = wallet;
        this.accountDao = accountDao;
        this.depositDao = depositDao;
        this.withdrawRequestDao = withdrawRequestDao;
        this.config = config;
    }

    // Match sus Address Patterns
    private static final Pattern SUS_PATTERN = Pattern.compile(".*[^a-zA-Z0-9].*");

    /**
     * Initiates a withdrawal that will not be sent to the network
     * until player confirmation is received.
     * 
     * @param player The player initiating the withdrawal.
     * @param destAddress The wallet address the player is attempting to withdraw to.
     * @param amount The amount the player is attempting to withdraw, excluding fees. Use < 0 for all funds.
     * @return An object holding withdraw details including determined fees, or null if the withdraw request failed.
     */
    @Transactional
    public WithdrawRequestResponse initiateWithdraw(UUID initiatorId, String destAddress, long amount)
    {
        Account initiator = accountDao.findOrCreate(initiatorId);
        // Reject Any Address With Suspicious Characters, Prevent Possibility Of Request Injection Just In Case
        if (SUS_PATTERN.matcher(destAddress).matches())
        {
            logger.warning(initiator.getAccountUUID() + " attempted to withdraw to invalid address: " + destAddress);
            return new WithdrawRequestResponse(WithdrawRequestResponseType.INVALID_ADDRESS);
        }
        // Make Sure No Existing Request Already Exists
        if (initiator.getWithdrawRequest() != null)
        {
            return new WithdrawRequestResponse(WithdrawRequestResponseType.REQUEST_ALREADY_EXISTS);
        }
        // Begin Withdraw
        Account withdrawAccount = accountDao.findOrCreate(VertconomyConfiguration.WITHDRAW_ACCOUNT_UUID);
        boolean withdrawAll = amount < 0L;
        long playerBalance = initiator.calculateWithdrawableBalance();
        // Can't Withdraw If Player Does Not Have At Least Withdraw Amount
        if (!withdrawAll && playerBalance < amount)
        {
            return new WithdrawRequestResponse(WithdrawRequestResponseType.NOT_ENOUGH_WITHDRAWABLE_FUNDS);
        }
        // Form Withdraw Request
        WithdrawRequest request = null;
        try
        {
            // Account For Fees
            double feeRate = wallet.estimateSmartFee(config.getTargetBlockTime());
            long inputFee = (long) Math.ceil(P2PKH_INPUT_VSIZE * feeRate);
            // Attempt To Grab Inputs For Transaction
            long fees = (long) Math.ceil(BASE_WITHDRAW_TX_SIZE * feeRate);
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
                return new WithdrawRequestResponse(WithdrawRequestResponseType.CANNOT_AFFORD_FEES);
            }
            fees += inputDeposits.size() * inputFee;
            // TX Inputs
            long totalInputValue = 0L;
            long totalOwnedValue = 0L;
            List<RawTransactionInput> txInputs = new ArrayList<>();
            for (Deposit inputDeposit : inputDeposits)
            {
                txInputs.add(new RawTransactionInput(inputDeposit.getTXID(), inputDeposit.getVout()));
                totalInputValue += inputDeposit.getTotal();
                totalOwnedValue += inputDeposit.getShare(initiator);
            }
            // TX Outputs
            long withdrawAmount = withdrawAll ? (totalOwnedValue - fees) : amount;
            long changeAmount = totalInputValue - (withdrawAmount + fees);
            Map<String, Long> txOutputs = new HashMap<>();
            txOutputs.put(destAddress, withdrawAmount);
            if (changeAmount > 0L)
            {
                txOutputs.put(withdrawAccount.getDepositAddress(), changeAmount);
            }
            // Build TX
            String txHex = wallet.createRawTransaction(txInputs, txOutputs);
            txHex = wallet.signRawTransactionWithWallet(txHex).hex;
            String withdrawTxid = wallet.decodeRawTransaction(txHex).txid;
            // Save Records
            long timestamp = System.currentTimeMillis();
            request = new WithdrawRequest(withdrawTxid, initiator, inputDeposits, withdrawAmount, fees, txHex, timestamp);
        }
        catch (RPCErrorResponseException e)
        {
            if (e.getError() == ResponseError.RPC_INVALID_ADDRESS_OR_KEY)
            {
                return new WithdrawRequestResponse(WithdrawRequestResponseType.INVALID_ADDRESS);
            }
            logger.warning("Error Response (" + e.getError().code()
                           + ") Creating Withdraw TX For: " + initiator.getAccountUUID());
            return new WithdrawRequestResponse(WithdrawRequestResponseType.UNKNOWN_FAILURE);
        }
        catch (WalletRequestException e)
        {
            logger.warning("Error Initiating Withdraw For Account: " + initiator.getAccountUUID());
            e.printStackTrace();
            return new WithdrawRequestResponse(WithdrawRequestResponseType.UNKNOWN_FAILURE);
        }
        // Persist Request & Lock Input Deposits
        withdrawRequestDao.persist(request);
        long remainingHoldAmount = request.getWithdrawAmount() + request.getFeeAmount();
        for (Deposit inputDeposit : request.getInputs())
        {
            if (remainingHoldAmount != 0)
            {
                long depositValue = inputDeposit.getShare(initiator);
                if (depositValue <= remainingHoldAmount)
                {
                    inputDeposit.setShare(initiator, 0L);
                    inputDeposit.setShare(withdrawAccount, depositValue);
                    remainingHoldAmount -= depositValue;
                }
                else
                {
                    inputDeposit.setShare(initiator, depositValue - remainingHoldAmount);
                    inputDeposit.setShare(withdrawAccount, remainingHoldAmount);
                    remainingHoldAmount = 0L;
                }
            }
            inputDeposit.setWithdrawLock(request);
            depositDao.update(inputDeposit);
        }
        initiator.setWithdrawRequest(request);
        accountDao.update(initiator);
        accountDao.update(withdrawAccount);
        return new WithdrawRequestResponse(request);
    }

    /**
     * Cancel the given withdraw request, restoring
     * reserved funds to the owner and unlocking the
     * deposits involved for future withdrawals.
     * <p>
     * SHOULD NOT BE USED ON A COMPLETED WITHDRAW REQUEST!
     * 
     * @param withdrawTxid The TXID of the withdraw request.
     * @return True if the request was found and canceled.
     */
    @Transactional
    public boolean cancelWithdraw(String withdrawTxid)
    {
        WithdrawRequest withdrawRequest = withdrawRequestDao.find(withdrawTxid);
        if (withdrawRequest == null || withdrawRequest.isComplete())
        {
            return false;
        }
        Account initiatorAccount = withdrawRequest.getAccount();
        Account withdrawAccount = accountDao.findOrCreate(VertconomyConfiguration.WITHDRAW_ACCOUNT_UUID);
        Set<Deposit> lockedDeposits = withdrawRequest.getInputs();
        for (Deposit lockedDeposit : lockedDeposits)
        {
            long lockedAmount = lockedDeposit.getShare(withdrawAccount);
            long updatedAmount = lockedDeposit.getShare(initiatorAccount) + lockedAmount;
            lockedDeposit.setShare(initiatorAccount, updatedAmount);
            lockedDeposit.setShare(withdrawAccount, 0L);
            lockedDeposit.setWithdrawLock(null);
            depositDao.update(lockedDeposit);
        }
        initiatorAccount.setWithdrawRequest(null);
        accountDao.update(initiatorAccount);
        accountDao.update(withdrawAccount);
        withdrawRequestDao.remove(withdrawRequest);
        return true;
    }

    /**
     * Cancel the given withdraw request, restoring
     * reserved funds to the owner and unlocking the
     * deposits involved for future withdrawals.
     * 
     * @param initiatorId The UUID of the initiating account.
     * @return True if the request was found and canceled.
     */
    public boolean cancelWithdraw(UUID initiatorId)
    {
        Account initiator = accountDao.findOrCreate(initiatorId);
        if (initiator == null || initiator.getWithdrawRequest() == null)
        {
            return false;
        }
        return cancelWithdraw(initiator.getWithdrawRequest().getTxid());
    }

    /**
     * Signs & sends a pending withdraw transaction out to the network.
     * 
     * @param withdrawTxid The TXID of the withdraw request.
     * @return The TXID of the sent transaction, or null if there was an issue.
     */
    @Transactional
    public String completeWithdraw(String withdrawTxid)
    {
        WithdrawRequest withdrawRequest = withdrawRequestDao.find(withdrawTxid);
        Account withdrawAccount = accountDao.findOrCreate(VertconomyConfiguration.WITHDRAW_ACCOUNT_UUID);
        String txid = null;
        try
        {
            // Send Transaction
            txid = wallet.sendRawTransaction(withdrawRequest.getTxHex());
        }
        catch (WalletRequestException e)
        {
            logger.warning("Error While Completing Withdraw For Account: "
                           + withdrawRequest.getAccount().getAccountUUID());
            e.printStackTrace();
            if (txid == null)
            {
                txid = "ERROR";
            }
            return txid;
        }
        // Clear Completed Request From Account
        Account initiator = withdrawRequest.getAccount();
        initiator.setWithdrawRequest(null);
        // If No Change Will Be Received From TX, Request Can Be Removed Immediately
        boolean change = false;
        Set<Deposit> inputs = withdrawRequest.getInputs();
        for (Deposit input : inputs)
        {
            // Only Remember Deposits Contributing To Change
            if (input.getShare(withdrawAccount) == input.getTotal())
            {
                input.setShare(withdrawAccount, 0L);
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
        accountDao.update(withdrawAccount);
        accountDao.update(initiator);
        return txid;
    }

    /**
     * Signs & sends a pending withdraw transaction out to the network.
     * 
     * @param initiatorId The UUID of the initiating account.
     * @return The TXID of the sent transaction, or null if there was an issue.
     */
    public String completeWithdraw(UUID initiatorId)
    {
        Account initiator = accountDao.findOrCreate(initiatorId);
        if (initiator == null || initiator.getWithdrawRequest() == null)
        {
            return null;
        }
        return completeWithdraw(initiator.getWithdrawRequest().getTxid());
    }
}