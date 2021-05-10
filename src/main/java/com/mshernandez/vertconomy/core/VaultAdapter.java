package com.mshernandez.vertconomy.core;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;

/**
 * A wrapper following the Vault API interface
 * translating calls into Vertconomy-compatible
 * requests.
 */
public class VaultAdapter implements Economy
{
    // Vertconomy Instance
    Vertconomy vertconomy;

    public VaultAdapter(Vertconomy vertconomy)
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
    public double getBalance(OfflinePlayer player)
    {
        return vertconomy.getPlayerBalance(player);
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
        return vertconomy.getPlayerBalance(player) >= amount;
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
        if (vertconomy.moveToTransferFund(player, amount))
        {
            return new EconomyResponse(amount,
                vertconomy.getPlayerBalance(player),
                EconomyResponse.ResponseType.SUCCESS,
                null);
        }
        return new EconomyResponse(0.0,
            vertconomy.getPlayerBalance(player),
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
        if (vertconomy.takeFromTransferFund(player, amount))
        {
            return new EconomyResponse(amount,
                vertconomy.getPlayerBalance(player),
                EconomyResponse.ResponseType.SUCCESS,
                null);
        }
        return new EconomyResponse(0.0,
            vertconomy.getPlayerBalance(player),
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

    // Not Supported

    @Override
    public boolean createPlayerAccount(OfflinePlayer player)
    {
        return false;
    }

    @Override
    public boolean createPlayerAccount(String playerName)
    {
        return false;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String world)
    {
        return false;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String world)
    {
        return false;
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