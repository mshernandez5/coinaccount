package com.mshernandez.vertconomy;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;

import com.mshernandez.vertconomy.commands.CommandBalance;
import com.mshernandez.vertconomy.commands.CommandDeposit;
import com.mshernandez.vertconomy.commands.CommandVertconomy;
import com.mshernandez.vertconomy.database.HibernateUtil;
import com.mshernandez.vertconomy.wallet_interface.RPCWalletConnection;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.h2.tools.Server;

import net.milkbowl.vault.economy.Economy;

/**
 * Where the plugin begins.
 */
public class App extends JavaPlugin
{
    @Override
    public void onEnable()
    {
        // Load Configuration, Save Default If None Found
        saveDefaultConfig();
        FileConfiguration configuration = getConfig();

        // Grab Wallet Connection Information
        String walletUriString = configuration.getString("uri", "http://127.0.0.1:5888");
        String user = configuration.getString("user", "vertuser");
        String pass = configuration.getString("pass", "vertpass");

        // Form Wallet Connection
        URI walletUri;
        try
        {
            walletUri = new URI(walletUriString);
        }
        catch (URISyntaxException e)
        {
            getLogger().warning("Invalid wallet URI given in configuration!");
            return;
        }
        RPCWalletConnection wallet = new RPCWalletConnection(walletUri, user, pass);

        // Grab Wallet Management Settings
        int minConfirmations = configuration.getInt("min-confirmations", 10);
        int targetBlockTime = configuration.getInt("target-block-time", 2);

        // Grab Currency Information
        String symbol = configuration.getString("symbol", "VTC");
        String baseUnit = configuration.getString("base-unit", "sat");
        CoinScale scale;
        switch (configuration.getString("scale", "base"))
        {
            case "full":
                scale = CoinScale.FULL;
                break;
            case "micro":
                scale = CoinScale.MICRO;
                break;
            case "milli":
                scale = CoinScale.MILLI;
                break;
            default:
                scale = CoinScale.BASE;
        }

        // If Essentials Economy Exists, Configure Economy Commands
        Plugin essentials = getServer().getPluginManager().getPlugin("Essentials");
        if (configuration.getBoolean("configure-essentials") && essentials != null)
        {
            getLogger().info("Found Essentials, customizing currency formatting.");
            FileConfiguration essentialsConfiguration = essentials.getConfig();
            essentialsConfiguration.options().copyHeader(true);
            essentialsConfiguration.set("currency-symbol", scale.CHAR_PREFIX);
            essentialsConfiguration.set("currency-symbol-prefix", false);
            essentialsConfiguration.set("currency-symbol-suffix", (scale == CoinScale.FULL) ? false : true);
            essentialsConfiguration.set("min-money", 0);
            essentialsConfiguration.set("minimum-pay-amount", (1.0 / scale.SAT_SCALE));
            try
            {
                essentialsConfiguration.save(new File("plugins/Essentials/config.yml"));
            }
            catch (IOException e)
            {
                getLogger().warning("Failed to configure Essentials economy commands!");
            }
        }

        // Configure HibernateUtil SessionFactory For Database
        try
        {
            HibernateUtil.configure();
        }
        catch (RuntimeException e)
        {
            getLogger().warning(e.getMessage());
            return;
        }

        // Start H2 Web Console If Enabled; Option May Be Removed In Future
        if (configuration.getBoolean("enable-h2-console", false))
        {
            try
            {
                Server webServer = Server.createWebServer("-webPort", "8082");
                webServer.start();
                getLogger().warning("Security Risk: H2 Console Enabled, Be Careful");
            }
            catch (SQLException e)
            {
                getLogger().warning("Failed To Start H2 Console");
            }
        }

        // Create Vertconomy Instance With Loaded Values
        Vertconomy vertconomy = new Vertconomy(this, wallet, minConfirmations,
            targetBlockTime, symbol, baseUnit, scale);

        // Register Vertconomy Wrapper With Vault
        Plugin vault = getServer().getPluginManager().getPlugin("Vault");
        if (vault == null)
        {
            getLogger().warning("ERROR: The Vault plugin cannot be found!");
            return;
        }
        getLogger().info("Vault found, attempting to register economy...");
        getServer().getServicesManager().register(Economy.class, new VertconomyVaultSupport(vertconomy),
            vault, ServicePriority.Normal);

        // Register Commands
        getCommand("balance").setExecutor(new CommandBalance(vertconomy));
        getCommand("deposit").setExecutor(new CommandDeposit(vertconomy));
        getCommand("vertconomy").setExecutor(new CommandVertconomy(vertconomy));
    }

    @Override
    public void onDisable()
    {
        getLogger().info("Stopping Vertconomy...");
        // Configure HibernateUtil SessionFactory
        try
        {
            HibernateUtil.reset();
        }
        catch (RuntimeException e)
        {
            getLogger().warning(e.getMessage());
        }
    }
}