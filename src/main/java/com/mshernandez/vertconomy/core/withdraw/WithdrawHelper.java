package com.mshernandez.vertconomy.core.withdraw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;

import com.mshernandez.vertconomy.core.BinarySearchCoinSelector;
import com.mshernandez.vertconomy.core.CoinSelector;
import com.mshernandez.vertconomy.core.MaxAmountCoinSelector;
import com.mshernandez.vertconomy.core.DepositShareEvaluator;
import com.mshernandez.vertconomy.core.account.AccountRepository;
import com.mshernandez.vertconomy.core.account.DepositAccount;
import com.mshernandez.vertconomy.core.deposit.Deposit;
import com.mshernandez.vertconomy.wallet_interface.RPCWalletConnection;
import com.mshernandez.vertconomy.wallet_interface.ResponseError;
import com.mshernandez.vertconomy.wallet_interface.exceptions.RPCErrorResponseException;
import com.mshernandez.vertconomy.wallet_interface.exceptions.WalletRequestException;
import com.mshernandez.vertconomy.wallet_interface.requests.RawTransactionInput;

/**
 * Helps initiate, cancel, and complete withdraw requests.
 */
public class WithdrawHelper
{
    // Logger
    private Logger logger;

    // Wallet Access
    RPCWalletConnection wallet;

    // Persistence
    private EntityManager entityManager;

    // Account Repository
    private AccountRepository accountRepository;

    // Withdraw Account
    private UUID withdrawAccountUUID;

    // Settings
    private int targetBlockTime;

    // Base Size + 2 Outputs (Destination & Change)
    private static final int BASE_WITHDRAW_TX_SIZE = 10 + (34 * 2);
    // Additional Size For Each Input (Including +1 Uncertainty Assuming Worst Case)
    private static final int P2PKH_INPUT_VSIZE = 149;

    /**
     * Create a new withdraw helper instance.
     * 
     * @param logger A logger to use.
     * @param wallet A connection to the wallet.
     * @param entityManager An entity manager for persistence.
     * @param withdrawAccount The server withdraw account.
     * @param targetBlockTime The target block time to confirm withdrawals.
     */
    public WithdrawHelper(Logger logger, RPCWalletConnection wallet,
                          EntityManager entityManager, AccountRepository accountRepository,
                          UUID withdrawAccountUUID, int targetBlockTime)
    {
        this.logger = logger;
        this.wallet = wallet;
        this.entityManager = entityManager;
        this.accountRepository = accountRepository;
        this.withdrawAccountUUID = withdrawAccountUUID;
        this.targetBlockTime = targetBlockTime;
    }

    // Match sus Address Patterns
    private static final Pattern susPattern = Pattern.compile(".*[^a-zA-Z0-9].*");

    /**
     * Initiates a withdrawal that will not be sent to the network
     * until player confirmation is received.
     * 
     * @param player The player initiating the withdrawal.
     * @param destAddress The wallet address the player is attempting to withdraw to.
     * @param amount The amount the player is attempting to withdraw, excluding fees. Use < 0 for all funds.
     * @return An object holding withdraw details including determined fees, or null if the withdraw request failed.
     */
    public WithdrawRequestResponse initiateWithdraw(DepositAccount account, String destAddress, long amount)
    {
        // Reject Any Address With Suspicious Characters, Prevent Possibility Of Request Injection Just In Case
        if (susPattern.matcher(destAddress).matches())
        {
            logger.warning(account.getAccountUUID() + " attempted to withdraw to invalid address: " + destAddress);
            return new WithdrawRequestResponse(WithdrawRequestResponseType.INVALID_ADDRESS);
        }
        // Make Sure No Existing Request Already Exists
        if (account.getWithdrawRequest() != null)
        {
            return new WithdrawRequestResponse(WithdrawRequestResponseType.REQUEST_ALREADY_EXISTS);
        }
        DepositAccount withdrawAccount = accountRepository.getOrCreateUserAccount(withdrawAccountUUID);
        boolean withdrawAll = amount < 0L;
        long playerBalance = account.calculateWithdrawableBalance();
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
            double feeRate = wallet.estimateSmartFee(targetBlockTime);
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
            Set<Deposit> inputDeposits = coinSelector.selectInputs(new DepositShareEvaluator(account), account.getDeposits(), inputFee, amount);
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
                totalOwnedValue += inputDeposit.getShare(account);
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
            request = new WithdrawRequest(withdrawTxid, account, inputDeposits, withdrawAmount, fees, txHex, timestamp);
        }
        catch (RPCErrorResponseException e)
        {
            if (e.getError() == ResponseError.RPC_INVALID_ADDRESS_OR_KEY)
            {
                return new WithdrawRequestResponse(WithdrawRequestResponseType.INVALID_ADDRESS);
            }
            logger.warning("Error Response (" + e.getError().code()
                           + ") Creating Withdraw TX For: " + account.getAccountUUID());
            return new WithdrawRequestResponse(WithdrawRequestResponseType.UNKNOWN_FAILURE);
        }
        catch (WalletRequestException e)
        {
            logger.warning("Error Initiating Withdraw For Account: " + account.getAccountUUID());
            e.printStackTrace();
            return new WithdrawRequestResponse(WithdrawRequestResponseType.UNKNOWN_FAILURE);
        }
        // Persist Request & Lock Input Deposits
        entityManager.getTransaction().begin();
        entityManager.persist(request);
        long remainingHoldAmount = request.getWithdrawAmount() + request.getFeeAmount();
        for (Deposit inputDeposit : request.getInputs())
        {
            if (remainingHoldAmount != 0)
            {
                long depositValue = inputDeposit.getShare(account);
                if (depositValue <= remainingHoldAmount)
                {
                    inputDeposit.setShare(account, 0L);
                    inputDeposit.setShare(withdrawAccount, depositValue);
                    remainingHoldAmount -= depositValue;
                }
                else
                {
                    inputDeposit.setShare(account, depositValue - remainingHoldAmount);
                    inputDeposit.setShare(withdrawAccount, remainingHoldAmount);
                    remainingHoldAmount = 0L;
                }
            }
            inputDeposit.setWithdrawLock(request);
            entityManager.merge(inputDeposit);
        }
        account.setWithdrawRequest(request);
        entityManager.merge(account);
        entityManager.merge(withdrawAccount);
        entityManager.getTransaction().commit();
        return new WithdrawRequestResponse(request);
    }

    /**
     * Cancel the given withdraw request, restoring
     * reserved funds to the owner and unlocking the
     * deposits involved for future withdrawals.
     * <p>
     * SHOULD NOT BE USED ON A COMPLETED WITHDRAW REQUEST!
     * 
     * @param withdrawRequest The withdraw request to cancel.
     */
    public void cancelWithdraw(WithdrawRequest withdrawRequest)
    {
        DepositAccount initiatorAccount = withdrawRequest.getAccount();
        DepositAccount withdrawAccount = accountRepository.getOrCreateUserAccount(withdrawAccountUUID);
        entityManager.getTransaction().begin();
        Set<Deposit> lockedDeposits = withdrawRequest.getInputs();
        for (Deposit lockedDeposit : lockedDeposits)
        {
            long lockedAmount = lockedDeposit.getShare(withdrawAccount);
            long updatedAmount = lockedDeposit.getShare(initiatorAccount) + lockedAmount;
            lockedDeposit.setShare(initiatorAccount, updatedAmount);
            lockedDeposit.setShare(withdrawAccount, 0L);
            lockedDeposit.setWithdrawLock(null);
            lockedDeposit = entityManager.merge(lockedDeposit);
        }
        initiatorAccount.setWithdrawRequest(null);
        entityManager.merge(initiatorAccount);
        entityManager.merge(withdrawAccount);
        entityManager.remove(withdrawRequest);
        entityManager.getTransaction().commit();
    }

    /**
     * Signs & sends a pending withdraw transaction out to the network.
     * 
     * @param withdrawRequest The withdraw request made by the user.
     * @return The TXID of the sent transaction, or null if there was an issue.
     */
    public String completeWithdraw(WithdrawRequest withdrawRequest)
    {
        DepositAccount withdrawAccount = accountRepository.getOrCreateUserAccount(withdrawAccountUUID);
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
        withdrawRequest.getAccount().setWithdrawRequest(null);
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
                entityManager.remove(input);
            }
            else
            {
                change = true;
            }
        }
        if (!change)
        {
            withdrawAccount.setWithdrawRequest(null);
            entityManager.remove(withdrawRequest);
        }
        else
        {
            entityManager.merge(withdrawAccount);
        }
        return txid;
    }
}