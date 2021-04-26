package com.mshernandez.vertconomy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import com.mshernandez.vertconomy.wallet_interface.RPCWalletConnection;
import com.mshernandez.vertconomy.wallet_interface.WalletRequestException;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

public class Vertconomy implements Listener
{
    private static final String DATABASE_PATH = "./plugins/Vertconomy/vertconomydb";

    // Plugin For Reference
    private Plugin plugin;
    // Database For Persistence
    private Connection db;
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
        // Register Bukkit Events
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event)
    {
        // Attempt To Connect To Database
        try
        {
            Class.forName("org.h2.Driver");
            db = DriverManager.getConnection("jdbc:h2:" + DATABASE_PATH, "sa", "");
        }
        catch (SQLException | ClassNotFoundException e)
        {
            plugin.getLogger().warning("ERROR Connecting To DB: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event)
    {
        try
        {
            db.close();
        }
        catch (SQLException e)
        {
            plugin.getLogger().warning("ERROR Closing DB: " + e.getMessage());
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

    public double getServerBalance()
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
        return 0.0;
    }
}
