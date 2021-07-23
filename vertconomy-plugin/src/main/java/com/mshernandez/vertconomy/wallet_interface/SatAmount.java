package com.mshernandez.vertconomy.wallet_interface;

/**
 * Stores an amount of VTC in sats.
 * Used for custom deserialization.
 */
public class SatAmount
{
    public SatAmount(long satAmount)
    {
        this.satAmount = satAmount;
    }

    public SatAmount()
    {
        // For Better GSON Compatibility
    }

    /**
     * An amount, in sats.
     */
    public long satAmount;
}
