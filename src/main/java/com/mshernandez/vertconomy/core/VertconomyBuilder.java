package com.mshernandez.vertconomy.core;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.persist.jpa.JpaPersistModule;
import com.mshernandez.vertconomy.core.util.CoinScale;
import com.mshernandez.vertconomy.wallet_interface.RPCWalletConnection;

/**
 * Builds new instances of Vertconomy.
 */
public class VertconomyBuilder
{
    // The instance being created.
    Vertconomy instance;

    // The properties needed to create the instance.
    private RPCWalletConnection wallet;
    private int minDepositConfirmations;
    private int minChangeConfirmations;
    private int targetBlockTime;

    private String symbol;
    private String baseUnitSymbol;
    private CoinScale scale;

    private long withdrawRequestExpireTime;

    /**
     * Begin creating a new Vertconomy instance.
     */
    public VertconomyBuilder()
    {
        // Default Values (Invalid)
        wallet = null;
        minDepositConfirmations = -1;
        minChangeConfirmations = -1;
        targetBlockTime = -1;
        symbol = null;
        baseUnitSymbol = null;
        scale = null;
        withdrawRequestExpireTime = -1L;
    }

    /**
     * Attempts to build a Vertconomy instance.
     * 
     * @return The instance if successful, or null otherwise.
     */
    public Vertconomy build()
    {
        if (!validate())
        {
            return null;
        }
        // Create Configuration Object
        VertconomyConfiguration config = new VertconomyConfiguration(minDepositConfirmations,
                                                                     minChangeConfirmations,
                                                                     targetBlockTime,
                                                                     symbol,
                                                                     baseUnitSymbol, scale,
                                                                     withdrawRequestExpireTime);
        // Setup Dependency Injection
        Module jpaModule = new JpaPersistModule(VertconomyConfiguration.JPA_UNIT_NAME);
        Module vertconomyModule = new VertconomyInjectorModule(wallet, config);
        Injector injector = Guice.createInjector(jpaModule, vertconomyModule);
        // Configure Class Loader To Make Embedded JPA Configuration Available
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        // Get Vertconomy Instance
        return injector.getInstance(Vertconomy.class);
    }

    /**
     * Validates the information needed to create the
     * instance being built.
     * 
     * @return True if the instance can be created, false otherwise.
     */
    private boolean validate()
    {
        return wallet != null
            && minDepositConfirmations >= 0
            && minChangeConfirmations >= 0
            && targetBlockTime > 0
            && symbol != null
            && baseUnitSymbol != null
            && scale != null
            && withdrawRequestExpireTime > 0L;
    }

    /**
     * Set the wallet connection to use for this Vertconomy instance.
     * 
     * @param wallet The wallet connection to use for this vertconomy instance.
     * @return A reference to this builder for chaining methods.
     */
    public VertconomyBuilder setWallet(RPCWalletConnection wallet)
    {
        this.wallet = wallet;
        return this;
    }

    /**
     * Set the minimum number of confirmations to
     * consider received deposits valid.
     * 
     * @param minChangeConfirmations The minimum number of confirmations to consider deposits valid.
     * @return A reference to this builder for chaining methods.
     */
    public VertconomyBuilder setMinDepositConfirmations(int minDepositConfirmations)
    {
        this.minDepositConfirmations = minDepositConfirmations;
        return this;
    }

    /**
     * Set the minimum number of confirmations to
     * consider received change transactions valid.
     * 
     * @param minChangeConfirmations The minimum number of confirmations to consider change transactions valid.
     * @return A reference to this builder for chaining methods.
     */
    public VertconomyBuilder setMinChangeConfirmations(int minChangeConfirmations)
    {
        this.minChangeConfirmations = minChangeConfirmations;
        return this;
    }

    /**
     * Set the target number of blocks to confirm a withdrawal.
     * 
     * @param targetBlockTime The coin symbol.
     * @return A reference to this builder for chaining methods.
     */
    public VertconomyBuilder setTargetBlockTime(int targetBlockTime)
    {
        this.targetBlockTime = targetBlockTime;
        return this;
    }

    /**
     * Set the coin symbol, ex. VTC.
     * 
     * @param symbol The coin symbol.
     * @return A reference to this builder for chaining methods.
     */
    public VertconomyBuilder setSymbol(String symbol)
    {
        this.symbol = symbol;
        return this;
    }

    /**
     * Set base coin unit name, ex. sat.
     * 
     * @param baseUnit The base unit.
     * @return A reference to this builder for chaining methods.
     */
    public VertconomyBuilder setBaseUnitSymbol(String baseUnitSymbol)
    {
        this.baseUnitSymbol = baseUnitSymbol;
        return this;
    }

    /**
     * Set the scale to represent coin values with.
     * 
     * @param scale The scale to use.
     * @return A reference to this builder for chaining methods.
     */
    public VertconomyBuilder setScale(CoinScale scale)
    {
        this.scale = scale;
        return this;
    }

    /**
     * Set the time, in milliseconds, for a withdraw request
     * to expire.
     * 
     * @param withdrawRequestExpireTime The time, in milliseconds, for a withdraw request to expire.
     * @return A reference to this builder for chaining methods.
     */
    public VertconomyBuilder setWithdrawRequestExpireTime(long withdrawRequestExpireTime)
    {
        this.withdrawRequestExpireTime = withdrawRequestExpireTime;
        return this;
    }
}
