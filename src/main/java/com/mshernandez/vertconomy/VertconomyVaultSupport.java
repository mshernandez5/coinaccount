package com.mshernandez.vertconomy;

import java.util.List;

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
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName)
    {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String world)
    {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String world)
    {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public double getBalance(OfflinePlayer player)
    {
        // TODO Auto-generated method stub
        // temporary return entire wallet balance for test
        return 3;
    }

    @Override
    public double getBalance(String playerName)
    {
        // TODO Auto-generated method stub
        // temporary return entire wallet balance for test
        return 300000.555;
    }

    @Override
    public double getBalance(OfflinePlayer player, String world)
    {
        // TODO Auto-generated method stub
        return 4;
    }

    @Override
    public double getBalance(String playerName, String world)
    {
        // TODO Auto-generated method stub
        return 4;
    }

    @Override
    public boolean has(OfflinePlayer player, double amount)
    {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean has(String playerName, double amount)
    {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean has(OfflinePlayer player, String world, double amount)
    {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean has(String playerName, String world, double amount)
    {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player)
    {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean hasAccount(String playerName)
    {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String world)
    {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean hasAccount(String playerName, String world)
    {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String world, double amount)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String world, double amount)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String world, double amount)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String world, double amount)
    {
        // TODO Auto-generated method stub
        return null;
    }

    // Plugin Does Not Support Banks
    private static final String NO_BANK_SUPPORT_MESSAGE = "vertconomy does not support banks.";

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
