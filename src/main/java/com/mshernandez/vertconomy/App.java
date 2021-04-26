package com.mshernandez.vertconomy;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.mshernandez.vertconomy.commands.CommandBalance;
import com.mshernandez.vertconomy.wallet_interface.RPCWalletConnection;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

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
        String walletUriString = configuration.getString("uri");
        String user = configuration.getString("user");
        String pass = configuration.getString("pass");

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

        // Grab Currency Information
        String symbol = configuration.getString("symbol");
        String baseUnit = configuration.getString("base-unit");
        CoinScale scale;
        switch (configuration.getString("scale"))
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

        // Create Vertconomy Instance With Loaded Values
        Vertconomy vertconomy = new Vertconomy(this, wallet, symbol, baseUnit, scale);

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
    }

    @Override
    public void onDisable()
    {
        getLogger().info("Stopping Vertconomy...");
    }
}