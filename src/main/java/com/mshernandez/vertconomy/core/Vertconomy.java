package com.mshernandez.vertconomy.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import com.mshernandez.vertconomy.database.Account;
import com.mshernandez.vertconomy.database.Deposit;
import com.mshernandez.vertconomy.database.JPAUtil;
import com.mshernandez.vertconomy.database.WithdrawRequest;
import com.mshernandez.vertconomy.database.DepositAccount;
import com.mshernandez.vertconomy.wallet_interface.RPCWalletConnection;
import com.mshernandez.vertconomy.wallet_interface.ResponseError;
import com.mshernandez.vertconomy.wallet_interface.WalletRequestException;
import com.mshernandez.vertconomy.wallet_interface.requests.RawTransactionInput;
import com.mshernandez.vertconomy.wallet_interface.responses.UnspentOutputResponse;
import com.mshernandez.vertconomy.wallet_interface.responses.UnspentOutputResponse.UnspentOutput;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * The core of the plugin, the duct tape
 * bonding Minecraft and Vertcoin together.
 */
public class Vertconomy
{
    // Account For Balances Owned By The Server Operators
    private static final UUID SERVER_ACCOUNT_UUID = UUID.fromString("a8a73687-8f8b-4199-8078-36e676f32d8f");

    // Account Allowing Intermediate Transfers For Vault Compatibility
    private static final UUID TRANSFER_ACCOUNT_UUID = UUID.fromString("ced87bc1-4730-41e1-955b-c4c45b4e9ccf");

    // Account To Hold Funds For Pending Withdrawals & Receive Change Transactions
    private static final UUID WITHDRAW_ACCOUNT_UUID = UUID.fromString("884b2231-6c7a-4db5-b022-1cc5aeb949a8");

    // How Often To Check For New Deposits, In Ticks
    private static final long DEPOSIT_CHECK_INTERVAL = 200L; // Approximately 10 Seconds

    // Plugin For Reference
    private Plugin plugin;
    
    // RPC Wallet API
    private RPCWalletConnection wallet;
    private int minDepositConfirmations;
    private int minChangeConfirmations;
    private int targetBlockTime;

    // Currency Information
    private String symbol;
    private String baseUnitSymbol;
    private CoinScale scale;

    // Sat Amount Formatter
    private SatAmountFormat formatter;

    // Database Persistence
    EntityManager entityManager;

    // Periodically Check For New Deposits
    BukkitTask depositCheckTask;

    /**
     * Create an uninitialized Vertconomy instance.
     * Must run initialize() after setting all fields to
     * non-default values.
     */
    Vertconomy()
    {
        // For Builder, Not For Direct Usage
    }

    /**
     * Initialize this instance, must be done before
     * using Vertconomy.
     */
    void initialize()
    {
        // Create Formatter
        formatter = new SatAmountFormat(scale, symbol, baseUnitSymbol);
        // Get Entity Manager For Persistence
        entityManager = JPAUtil.getEntityManager();
        // Register Task To Check For New Deposits
        depositCheckTask = Bukkit.getScheduler()
            .runTaskTimer(plugin, new CheckDepositTask(this), DEPOSIT_CHECK_INTERVAL, DEPOSIT_CHECK_INTERVAL);
    }

    /**
     * How many fractional digits should be displayed
     * based on the coin scale being used.
     * 
     * @return The proper number of fractional digits.
     */
    public int fractionalDigits()
    {
        return scale.NUM_VALID_FRACTION_DIGITS;
    }

    /**
     * Returns any wallet error, or null
     * if there is no error.
     * 
     * @return Any wallet error, or null if none.
     */
    public ResponseError checkWalletConnection()
    {
        try
        {
            return wallet.getWalletInfo();
        }
        catch (WalletRequestException e)
        {
            ResponseError error = new ResponseError();
            error.code = -1;
            error.message = e.getMessage();
            return error;
        }
    }

    /**
     * Gets a deposit account or creates a new one
     * if one does not already exist.
     * 
     * @param accountUUID The account UUID.
     * @return A deposit account reference.
     */
    DepositAccount getOrCreateUserAccount(UUID accountUUID)
    {
        DepositAccount account = null;
        try
        {
            entityManager.getTransaction().begin();
            account = entityManager.find(DepositAccount.class, accountUUID);
            if (account == null)
            {
                plugin.getLogger().info("Creating New Account For User: " + accountUUID);
                String depositAddress = wallet.getNewAddress(accountUUID.toString());
                account = new DepositAccount(accountUUID, depositAddress);
                entityManager.persist(account);
            }
            entityManager.getTransaction().commit();
        }
        catch (Exception e)
        {
            plugin.getLogger().warning("Failed To Get/Create Account: " + e.getMessage());
            entityManager.getTransaction().rollback();
        }
        return account;
    }

    /**
     * Gets a holding account or creates a new one
     * if one does not already exist.
     * 
     * @param accountUUID The account UUID.
     * @return A holding account reference.
     */
    Account getOrCreateHoldingAccount(UUID accountUUID)
    {
        Account account = null;
        try
        {
            entityManager.getTransaction().begin();
            account = entityManager.find(Account.class, accountUUID);
            if (account == null)
            {
                plugin.getLogger().info("Initializing Holding Account: " + accountUUID);
                account = new Account(accountUUID);
                entityManager.persist(account);
            }
            entityManager.getTransaction().commit();
        }
        catch (Exception e)
        {
            plugin.getLogger().warning("Failed To Get/Create Account: " + e.getMessage());
            entityManager.getTransaction().rollback();
        }
        return account;
    }

    /**
     * Return the useable balance held by the player's
     * account.
     * 
     * @param player The player associated with the account.
     * @return The balance associated with the account.
     */
    public long getPlayerBalance(OfflinePlayer player)
    {
        DepositAccount playerAccount = getOrCreateUserAccount(player.getUniqueId());
        return playerAccount == null ? 0L : playerAccount.calculateBalance();
    }

    /**
     * Return the total unconfirmed balances associated
     * with the player's account.
     * 
     * @param player The player associated with the account.
     * @return Unconfirmed deposit balances for the account.
     */
    public long getPlayerUnconfirmedBalance(OfflinePlayer player)
    {
        DepositAccount playerAccount = getOrCreateUserAccount(player.getUniqueId());
        return playerAccount == null ? 0L : playerAccount.getPendingBalance();
    }

    /**
     * Return both the usable and unconfirmed balances
     * associated with a player's account.
     * 
     * @param player The player associated with the account.
     * @return The usable and unconfirmed balances.
     */
    public Pair<Long, Long> getPlayerBalances(OfflinePlayer player)
    {
        DepositAccount playerAccount = getOrCreateUserAccount(player.getUniqueId());
        return playerAccount == null ? new Pair<Long,Long>(0L, 0L)
            : new Pair<Long, Long>(playerAccount.calculateBalance(), playerAccount.getPendingBalance());
    }

    /**
     * Get the public wallet address allowing the player to
     * deposit funds into their account.
     * 
     * @param player The player associated with the account.
     * @return The deposit address associated with the account.
     */
    public String getPlayerDepositAddress(OfflinePlayer player)
    {
        DepositAccount playerAccount = getOrCreateUserAccount(player.getUniqueId());
        return playerAccount == null ? "ERROR" : playerAccount.getDepositAddress();
    }

    /**
     * Return any active withdraw request initiated by the user.
     * 
     * @param player The player that initiated the request.
     * @return Any active withdraw request initiated by the user.
     */
    public WithdrawRequest getPlayerWithdrawRequest(OfflinePlayer player)
    {
        DepositAccount playerAccount = getOrCreateUserAccount(player.getUniqueId());
        return playerAccount == null ? null : playerAccount.getWithdrawRequest();
    }

    /**
     * Register new deposits for the given user account.
     * <p>
     * Returns newly confirmed balances as well as pending
     * deposits.
     * 
     * @param account The account to check for new deposits.
     * @return Balance gained from newly registered deposits, and unconfirmed deposit balances.
     */
    Pair<Long, Long> registerNewDeposits(DepositAccount account)
    {
        // Keep Track Of New Balances & Pending Unconfirmed Balances
        long addedBalance = 0L;
        long unconfirmedBalance = 0L;
        try
        {
            entityManager.getTransaction().begin();
            // Get Wallet Transactions For Addresses Associated With Account
            List<UnspentOutputResponse.UnspentOutput> unspentOutputs = wallet.getUnspentOutputs(account.getDepositAddress());
            // Remember Which Transactions Have Already Been Accounted For
            Set<String> oldTXIDs = account.getProcessedDepositIDs();
            Set<String> unspentTXIDs = new HashSet<>();
            // Check For New Unspent Outputs Deposited To Account
            for (UnspentOutput output : unspentOutputs)
            {
                if (output.confirmations >= minDepositConfirmations
                    && output.spendable && output.safe && output.solvable)
                {
                    if (!oldTXIDs.contains(output.txid))
                    {
                        long depositAmount = output.amount.satAmount;
                        // New Deposit Transaction Initially 100% Owned By Depositing Account
                        Deposit deposit = new Deposit(output.txid, output.vout, depositAmount, account);
                        entityManager.persist(deposit);
                        // Associate With Account
                        account.associateDeposit(deposit);
                        addedBalance += depositAmount;
                    }
                    unspentTXIDs.add(output.txid);
                }
                else
                {
                    unconfirmedBalance += output.amount.satAmount;
                }
            }
            account.getProcessedDepositIDs().clear();
            account.getProcessedDepositIDs().addAll(unspentTXIDs);
            account.setPendingBalance(unconfirmedBalance);
            entityManager.merge(account);
            entityManager.getTransaction().commit();
        }
        catch (Exception e)
        {
            plugin.getLogger().warning("Failed To Get Transactions: " + e.getMessage());
            entityManager.getTransaction().rollback();
            return new Pair<Long, Long>(0L, 0L);
        }
        return new Pair<Long, Long>(addedBalance, unconfirmedBalance);
    }

    /**
     * Register change transactions from recent withdrawals.
     * <p>
     * Associate and distribute change among the proper owners.
     */
    void registerChangeDeposits()
    {
        DepositAccount account = getOrCreateUserAccount(WITHDRAW_ACCOUNT_UUID);
        try
        {
            entityManager.getTransaction().begin();
            // Get Wallet Transactions For Addresses Associated With Account
            List<UnspentOutputResponse.UnspentOutput> unspentOutputs = wallet.getUnspentOutputs(account.getDepositAddress());
            // Remember Which Transactions Have Already Been Accounted For
            Set<String> oldTXIDs = account.getProcessedDepositIDs();
            Set<String> unspentTXIDs = new HashSet<>();
            // Check For New Unspent Outputs Deposited To Account
            for (UnspentOutput output : unspentOutputs)
            {
                if (output.confirmations >= minChangeConfirmations
                    && output.spendable && output.safe && output.solvable)
                {
                    if (!oldTXIDs.contains(output.txid))
                    {
                        // Look For Withdraw Request The Transaction Was Created From
                        WithdrawRequest withdrawRequest = entityManager.find(WithdrawRequest.class, output.txid);
                        if (withdrawRequest != null)
                        {
                            Map<Account, Long> changeDistribution = new HashMap<>();
                            Set<Deposit> inputs = withdrawRequest.getInputs();
                            for (Deposit d : inputs)
                            {
                                for (Account a : d.getOwners())
                                {
                                    if (!a.equals(account))
                                    {
                                        changeDistribution.put(a, changeDistribution.getOrDefault(a, 0L) + d.getDistribution(a));
                                    }
                                    a.removeDeposit(d);
                                }
                                entityManager.remove(d);
                            }
                            Deposit deposit = new Deposit(output.txid, output.vout, output.amount.satAmount, changeDistribution);
                            entityManager.persist(deposit);
                            for (Account a : deposit.getOwners())
                            {
                                a.associateDeposit(deposit);
                                entityManager.merge(a);
                            }
                            entityManager.remove(withdrawRequest);
                        }
                    }
                    unspentTXIDs.add(output.txid);
                }
            }
            account.getProcessedDepositIDs().clear();
            account.getProcessedDepositIDs().addAll(unspentTXIDs);
            entityManager.merge(account);
            entityManager.getTransaction().commit();
        }
        catch (Exception e)
        {
            plugin.getLogger().warning("Failed To Get Transactions: " + e.getMessage());
            entityManager.getTransaction().rollback();
        }
    }

    // Base Size + 2 Outputs (Destination & Change)
    private static final int BASE_WITHDRAW_TX_SIZE = 10 + (34 * 2);
    // Additional Size For Each Input (Including +1 Uncertainty Assuming Worst Case)
    private static final int P2PKH_INPUT_VSIZE = 149;

    /**
     * Initiates a withdrawal that will not be sent to the network
     * until player confirmation is received.
     * 
     * @param player The player initiating the withdrawal.
     * @param destAddress The wallet address the player is attempting to withdraw to.
     * @param amount The amount the player is attempting to withdraw, excluding fees. Use < 0 for all funds.
     * @return An object holding withdraw details including determined fees, or null if the withdraw request failed.
     */
    public WithdrawRequest initiateWithdraw(OfflinePlayer player, String destAddress, long amount)
    {
        boolean withdrawAll = amount < 0L;
        DepositAccount account = getOrCreateUserAccount(player.getUniqueId());
        DepositAccount withdrawAccount = getOrCreateUserAccount(WITHDRAW_ACCOUNT_UUID);
        long playerBalance = account.calculateBalance();
        // Can't Withdraw If Player Does Not Have At Least Withdraw Amount
        if (!withdrawAll && playerBalance < amount)
        {
            return null;
        }
        try
        {
            // Account For Fees
            double feeRate = wallet.estimateSmartFee(targetBlockTime);
            long inputFee = (long) Math.ceil(P2PKH_INPUT_VSIZE * feeRate);
            // Attempt To Grab Inputs For Transaction
            long fees = (long) Math.ceil(BASE_WITHDRAW_TX_SIZE * feeRate);
            Set<Deposit> inputDeposits;
            if (withdrawAll)
            {
                inputDeposits = selectInputDeposits(account, inputFee);
            }
            else
            {
                inputDeposits = selectInputDeposits(account, amount, inputFee);
            }
            if (inputDeposits == null)
            {
                return null;
            }
            fees += inputDeposits.size() * inputFee;
            // TX Inputs
            long totalInputValue = 0L;
            long totalOwnedValue = 0L;
            List<RawTransactionInput> txInputs = new ArrayList<>();
            for (Deposit d : inputDeposits)
            {
                txInputs.add(new RawTransactionInput(d.getTXID(), d.getVout()));
                totalInputValue += d.getTotal();
                totalOwnedValue += d.getDistribution(account);
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
            WithdrawRequest request = new WithdrawRequest(withdrawTxid, account, inputDeposits, withdrawAmount, fees, txHex, timestamp);
            // Save Request & Lock Input Deposits
            entityManager.getTransaction().begin();
            entityManager.persist(request);
            long remainingHoldAmount = withdrawAmount + fees;
            for (Deposit d : inputDeposits)
            {
                if (remainingHoldAmount != 0)
                {
                    long depositValue = d.getDistribution(account);
                    if (depositValue <= remainingHoldAmount)
                    {
                        d.setDistribution(account, 0L);
                        d.setDistribution(withdrawAccount, depositValue);
                        account.removeDeposit(d);
                        withdrawAccount.associateDeposit(d);
                        remainingHoldAmount -= depositValue;
                    }
                    else
                    {
                        d.setDistribution(account, depositValue - remainingHoldAmount);
                        d.setDistribution(withdrawAccount, remainingHoldAmount);
                        remainingHoldAmount = 0L;
                    }
                }
                d.setWithdrawLock(request);
                entityManager.merge(d);
            }
            account.setWithdrawRequest(request);
            entityManager.merge(account);
            entityManager.merge(withdrawAccount);
            entityManager.getTransaction().commit();
            return request;
        }
        catch (Exception e)
        {
            entityManager.getTransaction().rollback();
            return null;
        }
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
        DepositAccount withdrawAccount = getOrCreateUserAccount(WITHDRAW_ACCOUNT_UUID);
        DepositAccount initiatorAccount = withdrawRequest.getAccount();
        try
        {
            entityManager.getTransaction().begin();
            Set<Deposit> lockedDeposits = withdrawRequest.getInputs();
            for (Deposit deposit : lockedDeposits)
            {
                long lockedAmount = deposit.getDistribution(withdrawAccount);
                long updatedAmount = deposit.getDistribution(initiatorAccount) + lockedAmount;
                deposit.setDistribution(withdrawAccount, 0L);
                deposit.setDistribution(initiatorAccount, updatedAmount);
                deposit.setWithdrawLock(null);
                deposit = entityManager.merge(deposit);
                initiatorAccount.associateDeposit(deposit);
                withdrawAccount.removeDeposit(deposit);
            }
            initiatorAccount.setWithdrawRequest(null);
            entityManager.merge(initiatorAccount);
            entityManager.merge(withdrawAccount);
            entityManager.remove(withdrawRequest);
            entityManager.getTransaction().commit();
        }
        catch (Exception e)
        {
            plugin.getLogger().info("Failed to cancel withdraw request!");
            entityManager.getTransaction().rollback();
        }
    }

    /**
     * Signs & sends a pending withdraw transaction out to the network.
     * 
     * @param withdrawRequest The withdraw request made by the user.
     * @return The TXID of the sent transaction, or null if there was an issue.
     */
    public String completeWithdraw(WithdrawRequest withdrawRequest)
    {
        String txid = null;
        try
        {
            // Send Transaction
            txid = wallet.sendRawTransaction(withdrawRequest.getTxHex());
            // Clear Completed Request From Account
            withdrawRequest.getAccount().setWithdrawRequest(null);
            // If No Change Will Be Received From TX, Request Can Be Removed Immediately
            DepositAccount withdrawAccount = getOrCreateUserAccount(WITHDRAW_ACCOUNT_UUID);
            boolean change = false;
            Set<Deposit> inputs = withdrawRequest.getInputs();
            for (Deposit d : inputs)
            {
                // Only Remember Deposits Contributing To Change
                if (d.getDistribution(withdrawAccount) == d.getTotal())
                {
                    withdrawAccount.removeDeposit(d);
                    inputs.remove(d);
                    entityManager.remove(d);
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
        }
        catch (Exception e)
        {
            if (txid == null)
            {
                txid = "ERROR";
            }
        }
        return txid;
    }

    /**
     * Select Deposit inputs to use for transferring or withdrawing
     * the specified amount.
     * <p>
     * Uses a binary search algorithm to find inputs closest to the
     * target amount, which is dynamically updated as inputs are selected.
     * If the next selected input is larger than the last, the previous
     * input is unselected and selection continues attempting to use only
     * the larger input.
     * 
     * @param account The account associated with the Deposit objects.
     * @param inputFee The expected fee for adding a single input.
     * @param selectionTarget The amount needed from the resulting inputs excluding input fees.
     * @return A set of deposits that can make up the desired amount or null if the target cannot be reached.
     */
    private Set<Deposit> selectInputDeposits(Account account, long inputFee, long selectionTarget)
    {
        Deque<Deposit> selectedInputs = new ArrayDeque<>();
        // Get List Of Transactions That Can Be Used
        List<Deposit> inputs = account.getDeposits().stream()
            .filter(d -> !d.hasWithdrawLock())
            .sorted(new DepositShareComparator(account))
            .collect(Collectors.toCollection(ArrayList::new));
        // Remember Index Of Last Selected Deposit
        int lastSelectedIndex = -1;
        // Keep Selecting Inputs Until Target Value Is Met
        while (selectionTarget > 0L && !inputs.isEmpty())
        {
            // Every Selected Input Adds Fees To The Target Amount
            selectionTarget += inputFee;
            // Binary Search For Next Input Closest To Current Target Amount
            int first = 0,
                last = inputs.size() - 1,
                mid;
            while (first <= last)
            {
                mid = (first + last) / 2;
                long value = inputs.get(mid).getDistribution(account);
                double difference = difference(value, selectionTarget);
                // Check If Any Smaller Deposits Closer To Target Amount
                if (mid - 1 >= first
                    && difference(inputs.get(mid - 1).getDistribution(account), selectionTarget) < difference)
                {
                    last = mid - 1;
                }
                // Check If Any Larger Deposits Closer To Target Amount
                else if (mid + 1 <= last
                    && difference(inputs.get(mid + 1).getDistribution(account), selectionTarget) < difference)
                {
                    first = mid + 1;
                }
                // This Deposit Is Closest To The Target Amount
                else
                {
                    Deposit selected = inputs.get(mid);
                    Deposit lastSelected = selectedInputs.peek();
                    // If Selected Input Larger Than Last Selected, Try To Only Use Larger Input
                    if (lastSelected != null && value > lastSelected.getDistribution(account))
                    {
                        inputs.add(lastSelectedIndex, selectedInputs.pop());
                        selectionTarget += lastSelected.getDistribution(account);
                        selectionTarget -= inputFee;
                    }
                    lastSelectedIndex = inputs.indexOf(selected);
                    selectedInputs.push(selected);
                    inputs.remove(selected);
                    selectionTarget -= value;
                    break;
                }
            }
        }
        // Return null If Target Value Couldn't Be Fulfilled
        if (selectionTarget > 0L)
        {
            return null;
        }
        return new HashSet<>(selectedInputs);
    }

    /**
     * Select all valid input deposits that have values greater than
     * the fees required to withdraw them.
     * 
     * @param account The account associated with the Deposit objects.
     * @param inputFee The expected fee for adding a single input.
     * @return A set of deposits that can be withdrawn or null if none meet the criteria.
     */
    private Set<Deposit> selectInputDeposits(Account account, long inputFee)
    {
        Set<Deposit> selectedInputs = new HashSet<>();
        for (Deposit deposit : account.getDeposits())
        {
            if (!deposit.hasWithdrawLock() && deposit.getDistribution(account) > inputFee)
            {
                selectedInputs.add(deposit);
            }
        }
        return selectedInputs.isEmpty() ? null : selectedInputs;
    }

    /**
     * Calculate the percent difference between the two values.
     * 
     * @param a The first value.
     * @param b The second value.
     * @return The percent difference.
     */
    private double difference(long a, long b)
    {
        return Math.abs(b - a) / ((a + b) / 2.0);
    }

    /**
     * Transfer an amount from one account to another,
     * internally redistributing ownership of the
     * underlying deposits.
     * 
     * @param sender The sending account.
     * @param receiver The receiving account.
     * @param amount The amount to transfer, in sats.
     * @return True if the transfer was successful.
     */
    boolean transferBalance(Account sender, Account receiver, long amount)
    {
        if (sender.calculateBalance() < amount)
        {
            plugin.getLogger().info(sender + " can't send " + amount + " to " + receiver);
            return false;
        }
        try
        {
            entityManager.getTransaction().begin();
            long remainingOwed = amount;
            Iterator<Deposit> it = sender.getDeposits().iterator();
            while (it.hasNext() && remainingOwed > 0L)
            {
                Deposit deposit = it.next();
                long senderShare = deposit.getDistribution(sender);
                long takenAmount;
                if (senderShare <= remainingOwed)
                {
                    deposit.setDistribution(sender, 0L);
                    deposit.setDistribution(receiver, deposit.getDistribution(receiver) + senderShare);
                    deposit = entityManager.merge(deposit);
                    it.remove();
                    sender.removeDeposit(deposit);
                    receiver.associateDeposit(deposit);
                    takenAmount = senderShare;
                }
                else
                {
                    deposit.setDistribution(sender, senderShare - remainingOwed);
                    deposit.setDistribution(receiver, deposit.getDistribution(receiver) + remainingOwed);
                    deposit = entityManager.merge(deposit);
                    receiver.associateDeposit(deposit);
                    takenAmount = remainingOwed;
                }
                remainingOwed -= takenAmount;
            }
            entityManager.getTransaction().commit();
            plugin.getLogger().info(sender + " successfully sent " + amount + " to " + receiver);
        }
        catch (Exception e)
        {
            plugin.getLogger().info(sender + " failed to send " + amount + " to " + receiver);
            entityManager.getTransaction().rollback();
        }
        return true;
    }

    /**
     * Vault API Compatibility Use ONLY
     * <p>
     * Moves portions of a player's balance into
     * a temporary transfer fund where it will be
     * pending a move to another player's account
     * or into the server account fund if unclaimed.
     * <p>
     * The temporary transfer fund gives a set amount
     * of time for the funds to be reclaimed, which is
     * required for plugins which intend to conduct transfers
     * by burning sender balances and minting new currency
     * for the receiver.
     * 
     * @param player The player to take funds from.
     * @param amount The amount to send to the transfer fund.
     * @return True if the transfer was successful.
     */
    public boolean moveToTransferFund(OfflinePlayer player, double amount)
    {
        if (!Bukkit.isPrimaryThread())
        {
            plugin.getLogger().warning("Cannot Support Asynchronous Vault API Requests");
            return false;
        }
        DepositAccount sender = getOrCreateUserAccount(player.getUniqueId());
        Account receiver = getOrCreateHoldingAccount(TRANSFER_ACCOUNT_UUID);
        // Can't Have Fractions Of Satoshi, Celing Function To Next Satoshi
        long satAmount = (long) (Math.ceil(amount * scale.SAT_SCALE));
        if (satAmount < 0L)
        {
            return false;
        }
        return transferBalance(sender, receiver, satAmount);
    }

    /**
     * Vault API Compatibility Use ONLY
     * <p>
     * Reclaim a balance from the temporary transfer
     * fund.
     * 
     * @param player The player to give the taken funds to.
     * @param amount The amount to take from the transfer fund.
     * @return True if the transfer was successful.
     */
    public boolean takeFromTransferFund(OfflinePlayer player, double amount)
    {
        if (!Bukkit.isPrimaryThread())
        {
            plugin.getLogger().warning("Cannot Support Asynchronous Vault API Requests");
            return false;
        }
        DepositAccount receiver = getOrCreateUserAccount(player.getUniqueId());
        Account sender = getOrCreateHoldingAccount(TRANSFER_ACCOUNT_UUID);
        // Can't Have Fractions Of Satoshi, Celing Function To Next Satoshi
        long satAmount = (long) (Math.ceil(amount * scale.SAT_SCALE));
        if (satAmount < 0L)
        {
            return false;
        }
        satAmount = Math.min(satAmount, sender.calculateBalance()); // TODO: temporary
        return transferBalance(sender, receiver, satAmount);
    }

    // Setters For Building An Instance (Not For Regular Use):

    /**
     * Set the plugin associated with this instance.
     * 
     * @param plugin The plugin associated with this instance.
     */
    void setPlugin(Plugin plugin)
    {
        this.plugin = plugin;
    }

    /**
     * Set the wallet connection to use for this Vertconomy instance.
     * 
     * @param wallet The wallet connection to use for this vertconomy instance.
     */
    void setWallet(RPCWalletConnection wallet)
    {
        this.wallet = wallet;
    }

    /**
     * Set the minimum number of confirmations to
     * consider received deposits valid.
     * 
     * @param minChangeConfirmations The minimum number of confirmations to consider deposits valid.
     */
    void setMinDepositConfirmations(int minDepositConfirmations)
    {
        this.minDepositConfirmations = minDepositConfirmations;
    }

    /**
     * Set the minimum number of confirmations to
     * consider received change transactions valid.
     * 
     * @param minChangeConfirmations The minimum number of confirmations to consider change transactions valid.
     */
    void setMinChangeConfirmations(int minChangeConfirmations)
    {
        this.minChangeConfirmations = minChangeConfirmations;
    }

    /**
     * Set the target number of blocks to confirm a withdrawal.
     * 
     * @param targetBlockTime The coin symbol.
     */
    void setTargetBlockTime(int targetBlockTime)
    {
        this.targetBlockTime = targetBlockTime;
    }

    /**
     * Set the coin symbol, ex. VTC.
     * 
     * @param symbol The coin symbol.
     */
    void setSymbol(String symbol)
    {
        this.symbol = symbol;
    }

    /**
     * Set base coin unit name, ex. sat.
     * 
     * @param baseUnit The base unit.
     */
    void setBaseUnitSymbol(String baseUnitSymbol)
    {
        this.baseUnitSymbol = baseUnitSymbol;
    }

    /**
     * Set the scale to represent coin values with.
     * 
     * @param scale The scale to use.
     */
    void setScale(CoinScale scale)
    {
        this.scale = scale;
    }

    // Getters For Outside Usage:

    /**
     * Get the plugin associated with this instance.
     * 
     * @return A plugin reference.
     */
    public Plugin getPlugin()
    {
        return plugin;
    }

    /**
     * Get the coin symbol, ex. VTC.
     * 
     * @return The coin symbol.
     */
    public String getSymbol()
    {
        return symbol;
    }

    /**
     * Get a configured formatter to format and parse sat amounts.
     * 
     * @return A formatter for this Vertconomy instance.
     */
    public SatAmountFormat getFormatter()
    {
        return formatter;
    }

    /**
     * Get the minimum number of confirmations required
     * for Vertconomy to consider a deposit valid.
     * 
     * @return The minimum number of confirmations to consider a deposit valid.
     */
    public int getMinDepositConfirmations()
    {
        return minDepositConfirmations;
    }

    /**
     * Get the minimum number of confirmations required
     * for Vertconomy to consider change UTXOs valid.
     * 
     * @return The minimum number of confirmations to use change.
     */
    public int getMinChangeConfirmations()
    {
        return minChangeConfirmations;
    }

    /**
     * Get the target block time to process a
     * withdrawal.
     * 
     * @return The target block time.
     */
    public int getTargetBlockTime()
    {
        return targetBlockTime;
    }
}