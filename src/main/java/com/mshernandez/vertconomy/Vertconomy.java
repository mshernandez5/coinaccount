package com.mshernandez.vertconomy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mshernandez.vertconomy.database.Account;
import com.mshernandez.vertconomy.database.BlockchainTransaction;
import com.mshernandez.vertconomy.database.HibernateUtil;
import com.mshernandez.vertconomy.wallet_interface.ListTransactionResponse;
import com.mshernandez.vertconomy.wallet_interface.RPCWalletConnection;
import com.mshernandez.vertconomy.wallet_interface.WalletInfoResponse;
import com.mshernandez.vertconomy.wallet_interface.WalletRequestException;

import org.bukkit.plugin.Plugin;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class Vertconomy
{
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

    // Internally Used Accounts
    private static final UUID SERVER_ACCOUNT_UUID = UUID.fromString("a8a73687-8f8b-4199-8078-36e676f32d8f");
    private static final UUID TRANSFER_FUND_ACCOUNT_UUID = UUID.fromString("ced87bc1-4730-41e1-955b-c4c45b4e9ccf");
    private static final UUID CHANGE_ACCOUNT_UUID = UUID.fromString("884b2231-6c7a-4db5-b022-1cc5aeb949a8");

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
     * Gets an Account AND initializes its fields.
     * Creates a new account identified by the
     * given UUID if one does not already exist.
     * 
     * @param accountUUID The account UUID.
     * @return An initialized Account object or null if account creation failed.
     */
    public Account getOrCreateAccount(UUID accountUUID)
    {
        Account account;
        try (Session session = HibernateUtil.getSessionFactory().openSession())
        {
            account = session.get(Account.class, accountUUID);
            // Return If Account Already Exists
            if (account != null)
            {
                return account;
            }
            // Create New Wallet Deposit Address For Account
            String depositAddress = wallet.getNewAddress(accountUUID.toString());
            // Create & Save New Account
            Transaction dbtx = session.beginTransaction();
            account = new Account(accountUUID, depositAddress);
            session.save(account);
            dbtx.commit();
        }
        catch (Exception e)
        {
            plugin.getLogger().warning("Failed To Create New Account: " + e.getMessage());
            return null;
        }
        return account;
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
     * Get the total balances owned by an account
     * belonging to the given player UUID.
     * 
     * @param accountUUID The account UUID.
     * @return A pair where the key is confirmed balances, the value is unconfirmed balances.
     */
    public Pair<Long, Long> getBalances(UUID accountUUID)
    {
        Account account = getOrCreateAccount(accountUUID);
        if (account == null)
        {
            return new Pair<Long, Long>(0L, 0L);
        }
        long unconfirmedBalance = 0L;
        try (Session session = HibernateUtil.getSessionFactory().openSession())
        {
            account = (Account) session.merge(account);
            Hibernate.initialize(account.getProcessedTransactionIDs());
            Set<String> oldTransactionIDs = account.getProcessedTransactionIDs();
            List<ListTransactionResponse.Transaction> walletTransactions = wallet.getTransactions(accountUUID.toString());
            for (ListTransactionResponse.Transaction t : walletTransactions)
            {
                if (t.trusted && t.confirmations >= minConfirmations)
                {
                    if (!oldTransactionIDs.contains(t.txid))
                    {
                        Transaction dbtx = session.beginTransaction();
                        // TODO: will clean up with custom deserializer
                        long depositAmount = (long) (t.amount * CoinScale.BASE.SAT_SCALE);
                        // Account Initially Owns 100% Of TX Amount
                        Map<Account, Long> distribution = new HashMap<>();
                        distribution.put(account, depositAmount);
                        // Add Deposit To Account
                        BlockchainTransaction bt = new BlockchainTransaction(t.txid, depositAmount, distribution);
                        account.associateTransaction(bt);
                        dbtx.commit();
                    }
                }
                else
                {
                    // TODO: will clean up with custom deserializer
                    unconfirmedBalance += (long) (t.amount * CoinScale.BASE.SAT_SCALE);
                }
            }
        }
        catch (Exception e)
        {
            plugin.getLogger().warning("Failed To Check For New Transactions: " + e.getMessage());
        }
        return new Pair<Long, Long>(account.calculateBalance(), unconfirmedBalance);
    }

    /**
     * Get the total confirmed balances owned
     * by an account belonging to the given player UUID.
     * 
     * @param accountUUID The account UUID.
     * @return The total balance owned by the account.
     */
    public long getBalance(UUID accountUUID)
    {
        return getBalances(accountUUID).getKey();
    }

    /**
     * Get a wallet address for the specified
     * player to deposit coins into.
     * 
     * @param accountUUID The account UUID.
     * @return The corresponding deposit address.
     */
    public String getDepositAddress(UUID accountUUID)
    {
        Account account = getOrCreateAccount(accountUUID);
        if (account == null)
        {
            return "ERROR RETRIEVING ACCOUNT";
        }
        return account.getDepositAddress();
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
    public boolean transferFrom(UUID sendingAccount, UUID receivingAccount, long amount)
    {
        Account sender = getOrCreateAccount(sendingAccount);
        if (sender.calculateBalance() < amount)
        {
            return false;
        }
        Account receiver = getOrCreateAccount(receivingAccount);
        try (Session session = HibernateUtil.getSessionFactory().openSession())
        {
            long remaining = amount;
            Transaction dbtx = session.beginTransaction();
            for (BlockchainTransaction bt : sender.getTransactions())
            {
                if (remaining == 0L)
                {
                    break;
                }
                Map<Account, Long> distribution = bt.getDistribution();
                long senderShare = distribution.getOrDefault(sender, 0L);
                long takenAmount;
                if (senderShare <= remaining)
                {
                    distribution.remove(sender);
                    distribution.put(receiver, senderShare);
                    sender.detatchTransaction(bt);
                    receiver.associateTransaction(bt);
                    takenAmount = senderShare;
                }
                else
                {
                    distribution.put(sender, senderShare - remaining);
                    distribution.put(receiver, distribution.getOrDefault(receiver, 0L) + remaining);
                    receiver.associateTransaction(bt);
                    takenAmount = remaining;
                }
                remaining -= takenAmount;
            }
            sender.getTransactions();
            dbtx.commit();
        }
        catch (Exception e)
        {
            plugin.getLogger().warning("Failed To Make Transfer: " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Takes a part of a player's balance and moves it
     * into a general transfer fund where it can be claimed
     * by another account as part of a transfer.
     * 
     * As limitation of Vault API and existing plugins
     * designed to create and destroy money out of thin
     * air, transfers between players can get complicated.
     * 
     * The transfer fund captures magically withdrawn
     * balances and saves them for limited time so that
     * plugins trying to magically create money for
     * the other end of the transfer can instead
     * check and claim that balance on the transfer fund.
     * 
     * Balances remaining on the transfer fund for extended
     * times are assumed to be payed to the server itself.
     * 
     * @param playerUUID The account UUID.
     * @param amount The amount to send to the transfer fund.
     * @return True if the transfer was successful.
     */
    public boolean moveToTransferFund(UUID playerUUID, double amount)
    {
        long satAmount = (long) (amount * scale.SAT_SCALE);
        return transferFrom(playerUUID, TRANSFER_FUND_ACCOUNT_UUID, satAmount);
    }

    /**
     * Moves a certain amount from the general transfer
     * fund into a certain player's account balance.
     * 
     * As limitation of Vault API and existing plugins
     * designed to create and destroy money out of thin
     * air, transfers between players can get complicated.
     * 
     * The transfer fund captures magically withdrawn
     * balances and saves them for limited time so that
     * plugins trying to magically create money for
     * the other end of the transfer can instead
     * check and claim that balance on the transfer fund.
     * 
     * Balances remaining on the transfer fund for extended
     * times are assumed to be payed to the server itself.
     * 
     * @param playerUUID The account UUID.
     * @param amount The amount to send to the transfer fund.
     * @return True if the transfer was successful.
     */
    public boolean takeFromTransferFund(UUID playerUUID, double amount)
    {
        long satAmount = (long) (amount * scale.SAT_SCALE);
        return transferFrom(TRANSFER_FUND_ACCOUNT_UUID, playerUUID, satAmount);
    }
}
