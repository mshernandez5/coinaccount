package com.mshernandez.vertconomy;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;

/**
 * A vertconomy wrapper following the
 * Vault API interface.
 */
public class VertconomyVaultSupport implements Economy
{
    // Vertconomy Instance
    Vertconomy vertconomy;

    public VertconomyVaultSupport(Vertconomy vertconomy)
    {
        this.vertconomy = vertconomy;
    }

    // Plugin Information
    @Override
    public String getName()
    {
        return "vertconomy";
    }

    @Override
    public boolean isEnabled()
    {
        return true;
    }

    // Currency Information
    @Override
    public String currencyNamePlural()
    {
        return vertconomy.getSymbol();
    }

    @Override
    public String currencyNameSingular()
    {
        return vertconomy.getSymbol();
    }

    @Override
    public int fractionalDigits()
    {
        return vertconomy.fractionalDigits();
    }

    @Override
    public String format(double amount)
    {
        return vertconomy.format(amount);
    }

    // Player Economy Operations
    @Override
    public boolean createPlayerAccount(OfflinePlayer player)
    {
        return vertconomy.getOrCreateAccount(player.getUniqueId()) == null;
    }

    @Override
    public boolean createPlayerAccount(String playerName)
    {
        return createPlayerAccount(Bukkit.getPlayer(playerName));
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String world)
    {
        // No World-Specific Accounts For Now
        return createPlayerAccount(player);
    }

    @Override
    public boolean createPlayerAccount(String playerName, String world)
    {
        // No World-Specific Accounts For Now
        return createPlayerAccount(Bukkit.getPlayer(playerName), world);
    }

    @Override
    public double getBalance(OfflinePlayer player)
    {
        return vertconomy.getBalance(player.getUniqueId());
    }

    @Override
    public double getBalance(String playerName)
    {
        return getBalance(Bukkit.getPlayer(playerName));
    }

    @Override
    public double getBalance(OfflinePlayer player, String world)
    {
        // No World-Specific Accounts For Now
        return getBalance(player);
    }

    @Override
    public double getBalance(String playerName, String world)
    {
        // No World-Specific Accounts For Now
        return getBalance(Bukkit.getPlayer(playerName), world);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount)
    {
        return vertconomy.getBalance(player.getUniqueId()) >= amount;
    }

    @Override
    public boolean has(String playerName, double amount)
    {
        return has(Bukkit.getPlayer(playerName), amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String world, double amount)
    {
        // No World-Specific Accounts For Now
        return has(player, amount);
    }

    @Override
    public boolean has(String playerName, String world, double amount)
    {
        // No World-Specific Accounts For Now
        return has(Bukkit.getPlayer(playerName), world, amount);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player)
    {
        // Accounts Are Created Automatically When Needed, Can Assume True
        return true;
    }

    @Override
    public boolean hasAccount(String playerName)
    {
        // Accounts Are Created Automatically When Needed, Can Assume True
        return true;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String world)
    {
        // Accounts Are Created Automatically When Needed, Can Assume True
        return true;
    }

    @Override
    public boolean hasAccount(String playerName, String world)
    {
        // Accounts Are Created Automatically When Needed, Can Assume True
        return true;
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount)
    {
        Bukkit.getLogger().info("Attempting To Withdraw: " + amount);
        if (vertconomy.moveToTransferFund(player.getUniqueId(), amount))
        {
            return new EconomyResponse(amount,
                vertconomy.getBalance(player.getUniqueId()),
                EconomyResponse.ResponseType.SUCCESS,
                null);
        }
        return new EconomyResponse(0.0,
            vertconomy.getBalance(player.getUniqueId()),
            EconomyResponse.ResponseType.FAILURE,
            "Failed To Move " + vertconomy.format(amount));
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount)
    {
        return withdrawPlayer(Bukkit.getPlayer(playerName), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String world, double amount)
    {
        // No World-Specific Accounts For Now
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String world, double amount)
    {
        // No World-Specific Accounts For Now
        return withdrawPlayer(Bukkit.getPlayer(playerName), world, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount)
    {
        Bukkit.getLogger().info("Attempting To Deposit: " + amount);
        if (vertconomy.takeFromTransferFund(player.getUniqueId(), amount))
        {
            return new EconomyResponse(amount,
                vertconomy.getBalance(player.getUniqueId()),
                EconomyResponse.ResponseType.SUCCESS,
                null);
        }
        return new EconomyResponse(0.0,
            vertconomy.getBalance(player.getUniqueId()),
            EconomyResponse.ResponseType.FAILURE,
            "Failed To Claim " + vertconomy.format(amount));
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount)
    {
        return depositPlayer(Bukkit.getPlayer(playerName), amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String world, double amount)
    {
        // No World-Specific Accounts For Now
        return depositPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String world, double amount)
    {
        // No World-Specific Accounts For Now
        return depositPlayer(Bukkit.getPlayer(playerName), world, amount);
    }

    // Plugin Does Not Support Banks
    private static final String NO_BANK_SUPPORT_MESSAGE = "Vertconomy does not support banks.";

    @Override
    public boolean hasBankSupport()
    {
        return false;
    }

    @Override
    public List<String> getBanks()
    {
        throw new UnsupportedOperationException(NO_BANK_SUPPORT_MESSAGE);
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player)
    {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, NO_BANK_SUPPORT_MESSAGE);
    }

    @Override
    public EconomyResponse createBank(String name, String playerName)
    {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, NO_BANK_SUPPORT_MESSAGE);
    }

    @Override
    public EconomyResponse deleteBank(String name)
    {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, NO_BANK_SUPPORT_MESSAGE);
    }

    @Override
    public EconomyResponse bankBalance(String name)
    {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, NO_BANK_SUPPORT_MESSAGE);
    }

    @Override
    public EconomyResponse bankHas(String name, double amount)
    {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, NO_BANK_SUPPORT_MESSAGE);
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount)
    {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, NO_BANK_SUPPORT_MESSAGE);
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount)
    {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, NO_BANK_SUPPORT_MESSAGE);
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player)
    {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, NO_BANK_SUPPORT_MESSAGE);
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player)
    {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, NO_BANK_SUPPORT_MESSAGE);
    }
    @Override
    public EconomyResponse isBankMember(String name, String playerName)
    {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, NO_BANK_SUPPORT_MESSAGE);
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName)
    {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, NO_BANK_SUPPORT_MESSAGE);
    }
}
