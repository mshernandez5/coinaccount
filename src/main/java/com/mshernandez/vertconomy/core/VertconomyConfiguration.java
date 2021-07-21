package com.mshernandez.vertconomy.core;

import java.util.UUID;

import com.mshernandez.vertconomy.core.util.CoinScale;

public class VertconomyConfiguration
{
    // JPA Unit Name
    public static final String JPA_UNIT_NAME = "vertconomy";

    // Account For Balances Owned By The Server Operators
    public static final UUID SERVER_ACCOUNT_UUID = UUID.fromString("a8a73687-8f8b-4199-8078-36e676f32d8f");

    // Account Allowing Intermediate Transfers For Vault Compatibility
    public static final UUID TRANSFER_ACCOUNT_UUID = UUID.fromString("ced87bc1-4730-41e1-955b-c4c45b4e9ccf");

    // Account To Hold Funds For Pending Withdrawals & Receive Change Transactions
    public static final UUID WITHDRAW_ACCOUNT_UUID = UUID.fromString("884b2231-6c7a-4db5-b022-1cc5aeb949a8");

    // Wallet Related Settings
    private final int minDepositConfirmations;
    private final int minChangeConfirmations;
    private final int targetBlockTime;

    // Currency Related Settings
    private final String symbol;
    private final String baseUnitSymbol;
    private final CoinScale scale;

    public VertconomyConfiguration(int minDepositConfirmations,
                                   int minChangeConfirmations,
                                   int targetBlockTime,
                                   String symbol,
                                   String baseUnitSymbol,
                                   CoinScale scale)
    {
        this.minDepositConfirmations = minDepositConfirmations;
        this.minChangeConfirmations = minChangeConfirmations;
        this.targetBlockTime = targetBlockTime;
        this.symbol = symbol;
        this.baseUnitSymbol = baseUnitSymbol;
        this.scale = scale;
    }

    public int getMinDepositConfirmations()
    {
        return minDepositConfirmations;
    }

    public int getMinChangeConfirmations()
    {
        return minChangeConfirmations;
    }

    public int getTargetBlockTime()
    {
        return targetBlockTime;
    }

    public String getSymbol()
    {
        return symbol;
    }

    public String getBaseUnitSymbol()
    {
        return baseUnitSymbol;
    }

    public CoinScale getScale()
    {
        return scale;
    }
}
