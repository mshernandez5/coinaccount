package com.mshernandez.vertconomy;

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
        // Register Vertconomy Implementation With Vault
        Plugin vault = getServer().getPluginManager().getPlugin("Vault");
        if (vault == null)
        {
            getLogger().warning("ERROR: The Vault plugin cannot be found!");
            return;
        }
        getLogger().info("Vault found, attempting to register economy...");
        getServer().getServicesManager().register(Economy.class, new VertconomyVaultSupport(),
            vault, ServicePriority.Normal);
    }

    @Override
    public void onDisable()
    {
        getLogger().info("Stopping Vertconomy...");
    }
}