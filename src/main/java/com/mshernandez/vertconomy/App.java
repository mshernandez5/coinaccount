package com.mshernandez.vertconomy;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;

import com.mshernandez.vertconomy.commands.CommandBalance;
import com.mshernandez.vertconomy.commands.CommandDeposit;
import com.mshernandez.vertconomy.commands.CommandVertconomy;
import com.mshernandez.vertconomy.commands.CommandWithdraw;
import com.mshernandez.vertconomy.core.Vertconomy;
import com.mshernandez.vertconomy.core.VertconomyBuilder;
import com.mshernandez.vertconomy.core.util.CoinScale;
import com.mshernandez.vertconomy.tasks.CancelExpiredRequestsTask;
import com.mshernandez.vertconomy.tasks.CheckDepositTask;
import com.mshernandez.vertconomy.vault.VaultAdapter;
import com.mshernandez.vertconomy.vault.VaultRequestExecutor;
import com.mshernandez.vertconomy.wallet_interface.RPCWalletConnection;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.h2.tools.Server;

import net.milkbowl.vault.economy.Economy;

/**
 * Where the plugin begins.
 */
public class App extends JavaPlugin
{
    // How Often To Check For New Deposits, In Ticks
    private static final long DEPOSIT_CHECK_INTERVAL = 200L; // Approximately 10 Seconds

    // How Often To Check For Expired Tasks, In Ticks
    private static final long EXPIRED_REQUEST_CHECK_INTERVAL = 200L; // Approximately 10 Seconds

    // Periodically Check For New Deposits
    private BukkitTask depositCheckTask;

    // Check For & Cancel Expired Withdraw Requests
    private BukkitTask cancelExpireRequesTask;

    // Group & Execute Vault Requests
    private BukkitTask vaultRequestTask;

    // Save References To Close On Plugin Disable
    private static Server webServer = null;

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

        // Grab Transaction Settings
        int minDepositConfirmations = configuration.getInt("min-deposit-confirmations", 6);
        int minChangeConfirmations = configuration.getInt("min-change-confirmations", 1);
        int targetBlockTime = configuration.getInt("target-block-time", 2);

        // Grab Currency Information
        String symbol = configuration.getString("symbol", "VTC");
        String baseUnitSymbol = configuration.getString("base-unit", "sat");
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

        // Grab General Behavior Settings
        long withdrawRequestExpireTime = configuration.getLong("withdraw-request-expire-time", 60000L);

        // If Essentials Economy Exists, Configure Economy Commands
        Plugin essentials = getServer().getPluginManager().getPlugin("Essentials");
        if (configuration.getBoolean("configure-essentials", false) && essentials != null)
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

        // Start H2 Web Console If Enabled; Option Will Likely Be Removed In Future
        if (configuration.getBoolean("enable-h2-console", false))
        {
            try
            {
                webServer = Server.createWebServer("-webPort", "8082");
                webServer.start();
                getLogger().warning("Security Risk: H2 Console Enabled, Be Careful");
            }
            catch (SQLException e)
            {
                getLogger().warning("Failed To Start H2 Console");
            }
        }

        // Create Vertconomy Instance With Loaded Values
        Vertconomy vertconomy = new VertconomyBuilder()
            .setWallet(wallet)
            .setMinDepositConfirmations(minDepositConfirmations)
            .setMinChangeConfirmations(minChangeConfirmations)
            .setTargetBlockTime(targetBlockTime)
            .setSymbol(symbol)
            .setBaseUnitSymbol(baseUnitSymbol)
            .setScale(scale)
            .setWithdrawRequestExpireTime(withdrawRequestExpireTime)
            .build();

        // Register Vertconomy Wrapper With Vault
        if (configuration.getBoolean("vault-integration", false))
        {
            VaultRequestExecutor vaultRequestExecutor = new VaultRequestExecutor(vertconomy);
            vaultRequestTask = Bukkit.getScheduler().runTaskTimer(this, vaultRequestExecutor, 0L, 0L);
            Plugin vault = getServer().getPluginManager().getPlugin("Vault");
            if (vault == null)
            {
                getLogger().warning("ERROR: The Vault plugin cannot be found!");
                return;
            }
            getLogger().info("Vault found, attempting to register economy...");
            getServer().getServicesManager().register(Economy.class, new VaultAdapter(vertconomy, vaultRequestExecutor),
                this, ServicePriority.Normal);
        }

        // Register Commands
        getCommand("balance").setExecutor(new CommandBalance(vertconomy));
        getCommand("deposit").setExecutor(new CommandDeposit(vertconomy));
        getCommand("withdraw").setExecutor(new CommandWithdraw(vertconomy));
        getCommand("vertconomy").setExecutor(new CommandVertconomy(vertconomy));

        // Register Core Tasks
        depositCheckTask = Bukkit.getScheduler()
            .runTaskTimer(this, new CheckDepositTask(vertconomy), DEPOSIT_CHECK_INTERVAL, DEPOSIT_CHECK_INTERVAL);
        cancelExpireRequesTask = Bukkit.getScheduler()
            .runTaskTimer(this, new CancelExpiredRequestsTask(vertconomy), EXPIRED_REQUEST_CHECK_INTERVAL, EXPIRED_REQUEST_CHECK_INTERVAL);
        
        // Register Vertconomy API For External Use
        getServer().getServicesManager().register(Vertconomy.class, vertconomy, this, ServicePriority.Highest);
    }

    @Override
    public void onDisable()
    {
        getLogger().info("Stopping Vertconomy...");
        // End Running Tasks
        if (vaultRequestTask != null)
        {
            vaultRequestTask.cancel();
        }
        depositCheckTask.cancel();
        cancelExpireRequesTask.cancel();
        // Close H2 Web Console
        if (webServer != null)
        {
            try
            {
                webServer.stop();
                webServer = null;
            }
            catch (RuntimeException e)
            {
                getLogger().warning(e.getMessage());
            }
        }
    }
}