package com.mshernandez.vertconomy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManager;

import com.mshernandez.vertconomy.database.Account;
import com.mshernandez.vertconomy.database.BlockchainTransaction;
import com.mshernandez.vertconomy.database.JPAUtil;
import com.mshernandez.vertconomy.database.UserAccount;
import com.mshernandez.vertconomy.wallet_interface.ListTransactionResponse;
import com.mshernandez.vertconomy.wallet_interface.RPCWalletConnection;
import com.mshernandez.vertconomy.wallet_interface.WalletInfoResponse;
import com.mshernandez.vertconomy.wallet_interface.WalletRequestException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

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

    // Entity Manager For Persistence
    EntityManager entityManager;

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
        // Get Entity Manager
        entityManager = JPAUtil.getEntityManager();
        // Register Deposit Checking Task
        depositCheckTask = Bukkit.getScheduler()
            .runTaskTimer(plugin, new DepositCheckTask(), DEPOSIT_CHECK_INTERVAL, DEPOSIT_CHECK_INTERVAL);
    }

    /**
     * A task run periodically to check for new player
     * deposits.
     */
    private class DepositCheckTask implements Runnable
    {
        @Override
        public void run()
        {
            // Only Check Deposits For Online Players
            for (Player p : Bukkit.getOnlinePlayers())
            {
                UserAccount account = getOrCreateUserAccount(p.getUniqueId());
                Pair<Long, Long> changes = registerNewDeposits(account);
                if (changes.getKey() != 0L)
                {
                    StringBuilder message = new StringBuilder();
                    message.append(ChatColor.BLUE);
                    message.append("[Vertconomy] Processed Deposits: ");
                    message.append(ChatColor.GREEN);
                    message.append(format(changes.getKey()));
                    p.sendMessage(message.toString());
                }
            }
        }
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
     * Returns wallet status information or null
     * if the wallet cannot be reached.
     * 
     * @return Wallet status information, or null if the wallet is unreachable.
     */
    public WalletInfoResponse.Result getWalletStatus()
    {
        try
        {
            return wallet.getWalletStatus();
        }
        catch (WalletRequestException e)
        {
            return null;
        }
    }

    /**
     * Get the balance of the entire server wallet,
     * including all player balances combined.
     * 
     * @return The total balance of the server wallet.
     */
    public double getCombinedWalletBalance()
    {
        try
        {
            return wallet.getBalance(minConfirmations);
        }
        catch (WalletRequestException e)
        {
            plugin.getLogger().warning("Failed To Get Server Balance: " + e.getMessage());
        }
        return 0.0;
    }

    /**
     * Gets a player account or creates a new one
     * if one does not already exist.
     * 
     * @param accountUUID The account UUID.
     * @return A user account reference.
     */
    private UserAccount getOrCreateUserAccount(UUID accountUUID)
    {
        UserAccount account = entityManager.find(UserAccount.class, accountUUID);
        if (account != null)
        {
            return account;
        }
        try
        {
            entityManager.getTransaction().begin();
            plugin.getLogger().info("Creating New Account For User: " + accountUUID);
            String depositAddress = wallet.getNewAddress(accountUUID.toString());
            account = new UserAccount(accountUUID, depositAddress);
            entityManager.persist(account);
            entityManager.getTransaction().commit();
        }
        catch (WalletRequestException e)
        {
            plugin.getLogger().warning("Failed To Create New Account: " + e.getMessage());
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
    private Account getOrCreateHoldingAccount(UUID accountUUID)
    {
        Account account = entityManager.find(Account.class, accountUUID);
        if (account != null)
        {
            return account;
        }
        plugin.getLogger().info("Initializing Holding Account: " + accountUUID);
        account = new Account(accountUUID);
        entityManager.persist(account);
        return account;
    }

    /**
     * Register new deposits for the given user account.
     * 
     * @param account The account to check for new deposits.
     * @return Balance gained from newly registered deposits, and unconfirmed deposit balances.
     */
    private Pair<Long, Long> registerNewDeposits(UserAccount account)
    {
        try
        {
            // Get Wallet Transactions For Addresses Associated With Account
            List<ListTransactionResponse.Transaction> walletTransactions = wallet.getTransactions(account.getAccountUUID().toString());
            // Remember Which Transactions Have Already Been Accounted For
            Set<String> oldTransactionIDs = account.getProcessedDepositIDs();
            // Keep Track Of New Balances & Pending Unconfirmed Balances
            long addedBalance = 0L;
            long unconfirmedBalance = 0L;
            // Associate New Deposits To Account
            entityManager.getTransaction().begin();
            for (ListTransactionResponse.Transaction t : walletTransactions)
            {
                if (t.confirmations >= minConfirmations)
                {
                    if (!oldTransactionIDs.contains(t.txid))
                    {
                        // TODO: will clean up with custom deserializer
                        long depositAmount = (long) (t.amount * CoinScale.FULL.SAT_SCALE);
                        // New Deposit Transaction Initially 100% Owned By Depositing Account
                        Map<Account, Long> distribution = new HashMap<>();
                        distribution.put(account, depositAmount);
                        BlockchainTransaction bt = new BlockchainTransaction(t.txid, depositAmount, distribution);
                        entityManager.persist(bt);
                        // Associate With Account
                        account.getTransactions().add(bt);
                        account.getProcessedDepositIDs().add(t.txid);
                        addedBalance += depositAmount;
                    }
                }
                else
                {
                    // TODO: will clean up with custom deserializer
                    unconfirmedBalance += (long) (t.amount * CoinScale.FULL.SAT_SCALE);
                }
            }
            account.setPendingBalance(unconfirmedBalance);
            entityManager.merge(account);
            entityManager.getTransaction().commit();
            return new Pair<Long, Long>(addedBalance, unconfirmedBalance);
        }
        catch (WalletRequestException e)
        {
            plugin.getLogger().warning("Failed To Get Transactions: " + e.getMessage());
            return new Pair<Long, Long>(0L, 0L);
        }
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
    private boolean transferBalance(Account sender, Account receiver, long amount)
    {
        if (sender.calculateBalance() < amount)
        {
            plugin.getLogger().info(sender + " can't send " + amount + " to " + receiver);
            return false;
        }
        long remainingOwed = amount;
        entityManager.getTransaction().begin();
        for (BlockchainTransaction bt : sender.getTransactions())
        {
            if (remainingOwed == 0L)
            {
                break;
            }
            Map<Account, Long> distribution = bt.getDistribution();
            long senderShare = distribution.get(sender);
            long takenAmount;
            if (senderShare <= remainingOwed)
            {
                distribution.remove(sender);
                distribution.put(receiver, distribution.getOrDefault(receiver, 0L) + senderShare);
                bt = entityManager.merge(bt);
                sender.getTransactions().remove(bt);
                receiver.getTransactions().add(bt);
                takenAmount = senderShare;
            }
            else
            {
                distribution.put(sender, senderShare - remainingOwed);
                distribution.put(receiver, distribution.getOrDefault(receiver, 0L) + remainingOwed);
                bt = entityManager.merge(bt);
                receiver.getTransactions().add(bt);
                takenAmount = remainingOwed;
            }
            remainingOwed -= takenAmount;
        }
        entityManager.merge(sender);
        entityManager.merge(receiver);
        entityManager.getTransaction().commit();
        plugin.getLogger().info(sender + " successfully sent " + amount + " to " + receiver);
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
        UserAccount playerAccount = getOrCreateUserAccount(player.getUniqueId());
        return playerAccount.calculateBalance();
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
        UserAccount playerAccount = getOrCreateUserAccount(player.getUniqueId());
        return playerAccount.getPendingBalance();
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
        UserAccount playerAccount = getOrCreateUserAccount(player.getUniqueId());
        return new Pair<Long, Long>(playerAccount.calculateBalance(), playerAccount.getPendingBalance());
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
        UserAccount playerAccount = getOrCreateUserAccount(player.getUniqueId());
        return playerAccount.getDepositAddress();
    }

    /**
     * Vault API Compatibility Use ONLY
     * <p>
     * Moves portions of a player's balance into
     * a temporary transfer fund where it will be
     * pending a move into the server account fund.
     * <p>
     * The temporary transfer fund gives time for
     * the funds to be reclaimed, which is required
     * for plugins which intend to conduct transfers
     * by burning and minting currency.
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
        UserAccount sender = getOrCreateUserAccount(player.getUniqueId());
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
        UserAccount receiver = getOrCreateUserAccount(player.getUniqueId());
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