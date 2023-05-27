package com.mshernandez.coinaccount.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.mshernandez.coinaccount.dao.AccountDao;
import com.mshernandez.coinaccount.dao.AddressDao;
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
import com.mshernandez.coinaccount.service.util.CoinSelectionState;
import com.mshernandez.coinaccount.service.util.CoinSelectionBuilder;
import com.mshernandez.coinaccount.service.util.DepositShareEvaluator;
import com.mshernandez.coinaccount.service.wallet_rpc.WalletService;
import com.mshernandez.coinaccount.service.wallet_rpc.exception.WalletResponseException;
import com.mshernandez.coinaccount.service.wallet_rpc.parameter.CreateRawTransactionInput;
import com.mshernandez.coinaccount.service.wallet_rpc.parameter.DepositType;
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

    @ConfigProperty(name = "coinaccount.account.change")
    UUID changeAccountId;

    @ConfigProperty(name = "coinaccount.withdraw.target")
    int blockConfirmationTarget;

    @ConfigProperty(name = "coinaccount.withdraw.expire")
    long withdrawExpireTime;

    @ConfigProperty(name = "coinaccount.address.type")
    DepositType defaultAddressType;

    @ConfigProperty(name = "coinaccount.address.change.reuse")
    boolean reuseChangeAddresses;

    @Inject
    Logger logger;

    @Inject
    WalletService walletService;

    @Inject
    AccountDao accountDao;

    @Inject
    AddressDao addressDao;

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
        // Prevent Possibility Of JSON-RPC Injection, Just In Case
        if (SUS_PATTERN.matcher(destAddress).matches())
        {
            String logMsg = String.format("User %s provided a potentially malicious address string! \"%s\"", initiatorId, destAddress);
            logger.log(Level.WARN, logMsg);
            throw new InvalidAddressException();
        }
        // Validate Address
        if (!walletService.validateAddress(destAddress).isValid())
        {
            throw new InvalidAddressException();
        }
        // Account Must Already Exist (Otherwise Zero Balance)
        Account initiator = accountDao.find(initiatorId);
        if (initiator == null)
        {
            throw new NotEnoughWithdrawableFundsException();
        }
        // Make Sure No Existing Request Already Exists
        if (initiator.getWithdrawRequest() != null)
        {
            throw new WithdrawRequestAlreadyExistsException();
        }
        // Initial Check That Initiator Has Enough Funds (Ignoring Fees For Now)
        long withdrawableBalance = Math.min(initiator.getBalance(), depositDao.getWithdrawableBalance());
        if ((!withdrawAll && withdrawableBalance < amount) || withdrawableBalance == 0)
        {
            throw new NotEnoughWithdrawableFundsException();
        }
        // Begin Creating New Withdraw Request
        Account changeAccount = accountDao.findOrCreate(changeAccountId);
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
        String changeAddress = addressDao.findOrCreate(changeAccount, defaultAddressType, !reuseChangeAddresses).getAddress();
        vsize += getOutputSize(walletService.getAddressInfo(changeAddress).getScriptPubKey());
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
        DepositShareEvaluator evaluator = new DepositShareEvaluator(feeRateByte);
        CoinSelectionState<Deposit> selectionResult;
        if (withdrawAll)
        {
            selectionResult = new CoinSelectionBuilder<Deposit>()
                .step(new BinarySearchCoinSelector<>(-1, false), new LinkedHashSet<>(depositDao.findAllWithdrawable()))
                .evaluator(evaluator)
                .target(withdrawableBalance)
                .select();
        }
        else
        {
            selectionResult = new CoinSelectionBuilder<Deposit>()
                .step(new BinarySearchCoinSelector<>(), new LinkedHashSet<>(depositDao.findAllWithdrawable()))
                .evaluator(evaluator)
                .target(amount)
                .select();
        }
        if (!selectionResult.isComplete())
        {
            throw new CannotAffordFeesException();
        }
        Set<Deposit> inputDeposits = selectionResult.getSelection();
        // Calculate Final TX Size Based On Selected Inputs
        vsize += selectionResult.getCost();
        totalFees = (long) Math.ceil(vsize * feeRateByte);
        // Begin Building TX, Specify Selected Transaction Inputs
        long totalValue = 0L;
        Set<CreateRawTransactionInput> txInputs = new HashSet<>();
        for (Deposit inputDeposit : inputDeposits)
        {
            txInputs.add(new CreateRawTransactionInput(inputDeposit.getTXID(), inputDeposit.getVout()));
            totalValue += inputDeposit.getAmount();
        }
        // Specify Recipient TX Output
        Map<String, Long> txOutputs = new HashMap<>();
        long recipientAmount = withdrawAll ? (withdrawableBalance - totalFees) : amount;
        txOutputs.put(destAddress, recipientAmount);
        // Final Check Whether Total Cost Exceeds Balance
        long totalCost = recipientAmount + totalFees;
        if (totalCost > withdrawableBalance)
        {
            throw new CannotAffordFeesException();
        }
        initiator.changeBalance(-totalCost);
        // Specify Change TX Output
        long changeAmount = totalValue - (recipientAmount + totalFees);
        if (changeAmount > 0L)
        {
            txOutputs.put(changeAddress, changeAmount);
        }
        // Build Withdraw TX
        String unsignedTxHex;
        try
        {
            unsignedTxHex = walletService.createRawTransaction(txInputs, txOutputs);
        }
        catch (WalletResponseException e)
        {
            logger.error("Failed to create withdraw transaction! Error: " + e.getMessage());
            throw e;
        }
        String signedTxHex = walletService.signRawTransactionWithWallet(unsignedTxHex).getHex();
        String withdrawTxid = walletService.decodeRawTransaction(signedTxHex).getTxid();
        // Persist Withdraw Request For Future Confirmation With Timestamp
        long timestamp = System.currentTimeMillis();
        WithdrawRequest request = new WithdrawRequest(withdrawTxid, initiator, inputDeposits, recipientAmount, totalFees, signedTxHex, timestamp);
        withdrawRequestDao.persist(request);
        // Lock Input Deposits, Prevent Attempts To Spend Same UTXOs
        for (Deposit inputDeposit : request.getInputs())
        {
            inputDeposit.setWithdrawLock(request);
            depositDao.update(inputDeposit);
        }
        initiator.setWithdrawRequest(request);
        accountDao.update(initiator);
        accountDao.update(changeAccount);
        logger.log(Level.INFO, String.format("Withdraw Request Created: Account %s, vsize: %.2f, Fees: %d", initiatorId, vsize, totalFees));
        return new WithdrawRequestResult()
            .setTxid(withdrawTxid)
            .setWithdrawAmount(recipientAmount)
            .setFeeAmount(totalFees)
            .setTotalCost(totalCost);
    }

    /**
     * Cancel the given withdraw request, unlocking the
     * deposits involved for future withdrawals.
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
        // Unlock UTXOs For Future Use
        Set<Deposit> lockedDeposits = withdrawRequest.getInputs();
        for (Deposit lockedDeposit : lockedDeposits)
        {
            lockedDeposit.setWithdrawLock(null);
            depositDao.update(lockedDeposit);
        }
        // Restore Funds
        initiatorAccount.changeBalance(withdrawRequest.getTotalCost());
        // Remove Withdraw Request
        initiatorAccount.setWithdrawRequest(null);
        accountDao.update(initiatorAccount);
        withdrawRequestDao.remove(withdrawRequest);
        logger.log(Level.INFO, String.format("Withdraw Request Canceled: Account %s", initiatorAccount.getAccountUUID()));
    }

    /**
     * Cancel the given withdraw request, unlocking the
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
        // Broadcast Transaction
        String txid = walletService.sendRawTransaction(withdrawRequest.getTxHex());
        // Clear Request From Initiator Account
        Account initiator = withdrawRequest.getAccount();
        initiator.setWithdrawRequest(null);
        // Remove Withdraw Request & Spent TX Output Records
        Set<Deposit> inputs = withdrawRequest.getInputs();
        for (Deposit input : inputs)
        {
            depositDao.remove(input);
        }
        withdrawRequestDao.remove(withdrawRequest);
        accountDao.update(initiator);
        // Log Broadcasted Withdraw
        logger.info(String.format("Withdraw Request Completed: Account: %s, TXID: %s", initiator.getAccountUUID(), txid));
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