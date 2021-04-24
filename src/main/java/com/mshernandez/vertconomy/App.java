package com.mshernandez.vertconomy;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * The main part of the plugin.
 */
public class App extends JavaPlugin
{
    @Override
    public void onEnable()
    {
        getLogger().info("Starting Vertconomy...");
    }

    @Override
    public void onDisable()
    {
        getLogger().info("Stopping Vertconomy...");
    }
}