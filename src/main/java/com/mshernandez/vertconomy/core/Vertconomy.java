package com.mshernandez.vertconomy.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManager;

import com.mshernandez.vertconomy.database.Account;
import com.mshernandez.vertconomy.database.Deposit;
import com.mshernandez.vertconomy.database.JPAUtil;
import com.mshernandez.vertconomy.database.DepositAccount;
import com.mshernandez.vertconomy.wallet_interface.UnspentOutputResponse;
import com.mshernandez.vertconomy.wallet_interface.RPCWalletConnection;
import com.mshernandez.vertconomy.wallet_interface.ResponseError;
import com.mshernandez.vertconomy.wallet_interface.WalletRequestException;
import com.mshernandez.vertconomy.wallet_interface.UnspentOutputResponse.UnspentOutput;

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

    // Account To Receive Withdrawal Change Transactions
    private static final UUID CHANGE_ACCOUNT_UUID = UUID.fromString("884b2231-6c7a-4db5-b022-1cc5aeb949a8");

    // How Often To Check For New Deposits, In Ticks
    private static final long DEPOSIT_CHECK_INTERVAL = 200L; // Approximately 10 Seconds

    // Plugin For Reference
    private Plugin plugin;
    
    // RPC Wallet API
    private RPCWalletConnection wallet;
    private int minConfirmations;
    private int targetBlockTime;

    // Currency Information
    private String symbol;
    private String baseUnit;
    private CoinScale scale;

    // Database Persistence
    EntityManager entityManager = JPAUtil.getEntityManager();

    // Periodically Check For New Deposits
    BukkitTask depositCheckTask;

    public Vertconomy(Plugin plugin, RPCWalletConnection wallet, int minConfirmations,
        int targetBlockTime, String symbol, String baseUnit, CoinScale scale)
    {
        // Save References / Values
        this.plugin = plugin;
        this.wallet = wallet;
        this.minConfirmations = minConfirmations;
        this.targetBlockTime = targetBlockTime;
        this.symbol = symbol;
        this.baseUnit = baseUnit;
        this.scale = scale;
        // Register Deposit Checking Task
        depositCheckTask = Bukkit.getScheduler()
            .runTaskTimer(plugin, new CheckDepositTask(this), DEPOSIT_CHECK_INTERVAL, DEPOSIT_CHECK_INTERVAL);
    }

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
     * Format a sat amount into a readable String according
     * to the current currency settings.
     * 
     * @param amount The unformatted amount, in sats.
     * @return A formatted string representing the amount.
     */
    public String format(long amount)
    {
        return format((double) amount / scale.SAT_SCALE);
    }

    /**
     * Format a double into a readable amount according
     * to the current currency settings.
     * 
     * @param amount The unformatted amount, using the current scale.
     * @return A formatted string representing the amount.
     */
    public String format(double amount)
    {
        return String.format("%." + scale.NUM_VALID_FRACTION_DIGITS + "f "
            + ((scale == CoinScale.BASE) ? baseUnit : (scale.PREFIX + symbol)), amount);
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
     * Get the minimum number of confirmations required
     * for Vertconomy to consider a transaction valid.
     * 
     * @return The minimum number of transactions to consider a transaction valid.
     */
    public int getMinimumConfirmations()
    {
        return minConfirmations;
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
            return wallet.getWalletError();
        }
        catch (WalletRequestException e)
        {
            ResponseError error = new ResponseError();
            error.code = 0;
            error.message = e.getMessage();
            return error;
        }
    }

    /**
     * Gets a player account or creates a new one
     * if one does not already exist.
     * 
     * @param accountUUID The account UUID.
     * @return A user account reference.
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
     * Register new deposits for the given user account.
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
            Set<String> newlyProcessedTXIDs = new HashSet<>();
            // Check For New Unspent Outputs Deposited To Account
            for (UnspentOutput output : unspentOutputs)
            {
                if (output.confirmations >= minConfirmations
                    && output.spendable && output.safe && output.solvable)
                {
                    if (!oldTXIDs.contains(output.txid))
                    {
                        long depositAmount = output.amount.satAmount;
                        // New Deposit Transaction Initially 100% Owned By Depositing Account
                        Map<Account, Long> distribution = new HashMap<>();
                        distribution.put(account, depositAmount);
                        Deposit bt = new Deposit(output.txid, output.vout ,depositAmount, distribution);
                        entityManager.persist(bt);
                        // Associate With Account
                        account.getTransactions().add(bt);
                        newlyProcessedTXIDs.add(output.txid);
                        addedBalance += depositAmount;
                    }
                }
                else
                {
                    unconfirmedBalance += output.amount.satAmount;
                }
            }
            account.getProcessedDepositIDs().addAll(newlyProcessedTXIDs);
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
     * Transfer an amount from one account to another,
     * internally redistributing ownership of the
     * underlying blockchain transactions.
     * 
     * @param sendingAccount The sending account UUID.
     * @param receivingAccount The receiving account UUID.
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
            Iterator<Deposit> it = sender.getTransactions().iterator();
            while (it.hasNext() && remainingOwed > 0L)
            {
                Deposit deposit = it.next();
                Map<Account, Long> distribution = deposit.getOwnershipDistribution();
                long senderShare = distribution.get(sender);
                long takenAmount;
                if (senderShare <= remainingOwed)
                {
                    distribution.remove(sender);
                    distribution.put(receiver, distribution.getOrDefault(receiver, 0L) + senderShare);
                    deposit = entityManager.merge(deposit);
                    it.remove();
                    receiver.getTransactions().add(deposit);
                    takenAmount = senderShare;
                }
                else
                {
                    distribution.put(sender, senderShare - remainingOwed);
                    distribution.put(receiver, distribution.getOrDefault(receiver, 0L) + remainingOwed);
                    deposit = entityManager.merge(deposit);
                    receiver.getTransactions().add(deposit);
                    takenAmount = remainingOwed;
                }
                remainingOwed -= takenAmount;
            }
            entityManager.getTransaction().commit();
            plugin.getLogger().info(sender + " successfully sent " + amount + " to " + receiver);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            plugin.getLogger().info(sender + " failed to send " + amount + " to " + receiver);
            entityManager.getTransaction().rollback();
        }
        return true;
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
     * @param playerUUID The account UUID.
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
     * @param playerUUID The account UUID.
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
}