package com.mshernandez.vertconomy;

import java.sql.SQLException;
import java.util.UUID;

import com.mshernandez.vertconomy.database.Account;
import com.mshernandez.vertconomy.database.HibernateUtil;
import com.mshernandez.vertconomy.wallet_interface.RPCWalletConnection;
import com.mshernandez.vertconomy.wallet_interface.WalletRequestException;

import org.bukkit.plugin.Plugin;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class Vertconomy
{
    // Plugin For Reference
    private Plugin plugin;
    
    // RPC Wallet API
    private RPCWalletConnection wallet;

    // Currency Information
    private String symbol;
    private String baseUnit;
    private CoinScale scale;

    public Vertconomy(Plugin plugin, RPCWalletConnection wallet, String symbol, String baseUnit, CoinScale scale)
    {
        // Save References
        this.plugin = plugin;
        this.wallet = wallet;
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
     * Format a double into a readable amount according
     * to the current currency settings.
     * 
     * @param amount The unformatted amount.
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
     * Creates a new account identified by the
     * given UUID if one does not already exist.
     * 
     * @param accountUUID The account UUID.
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

    public double getCombinedWalletBalance()
    {
        try
        {
            return wallet.getBalance(6);
        }
        catch (WalletRequestException e)
        {
            plugin.getLogger().warning("Failed To Get Server Balance: " + e.getMessage());
        }
        return 0.0;
    }

    public double getBalance(UUID accountUUID)
    {
        Account account = getOrCreateAccount(accountUUID);
        try (Session session = HibernateUtil.getSessionFactory().openSession())
        {
            // check for new transactions at address
            // create, save, and add any new transactions to user
            // save transaction in db
            // update account in db
        }
        catch (Exception e)
        {
            plugin.getLogger().warning("Failed To Check For New Transactions: " + e.getMessage());
        }
        if (account == null)
        {
            return 0.0;
        }
        return account.calculateBalance();
    }

    public String getDepositAddress(UUID accountUUID)
    {
        Account account = getOrCreateAccount(accountUUID);
        if (account == null)
        {
            return "ERROR RETRIEVING ACCOUNT";
        }
        return account.getDepositAddress();
    }
}
