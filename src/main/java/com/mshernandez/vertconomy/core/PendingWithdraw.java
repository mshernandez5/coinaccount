package com.mshernandez.vertconomy.core;

/**
 * Stores information about a pending withdraw
 * initiated by a user, waiting to be confirmed.
 */
public class PendingWithdraw
{
    private long totalAmount;
    private long txFee;

    private String txHex;

    PendingWithdraw(long withdrawAmount, long txFee, String txHex)
    {
        this.totalAmount = withdrawAmount;
        this.txFee = txFee;
        this.txHex = txHex;
    }

    /**
     * Get the total withdrawal cost, including TX fee.
     * 
     * @return The total withdrawal cost, including TX fee.
     */
    public long getTotalCost()
    {
        return totalAmount;
    }

    /**
     * Get the amount of cost contributed by the TX fee.
     * 
     * @return The TX fee to be payed.
     */
    public long getTxFee()
    {
        return txFee;
    }

    /**
     * Get the amount to be received by the user.
     * 
     * @return The actual amount to be received.
     */
    public long getReceivedAmount()
    {
        return totalAmount - txFee;
    }

    /**
     * Get the unsigned hex-encoded transaction
     * waiting to be signed and sent to the network.
     * 
     * @return The unsigned hex-encoded transaction.
     */
    public String getTxHex()
    {
        return txHex;
    }
}