package com.mshernandez.vertconomy.core.response;

/**
 * Carries response information for user requests to
 * withdraw balances.
 */
public abstract class WithdrawRequestResponse
{
    /**
     * Get the response type, indicating either success
     * or some type of failure.
     * 
     * @return The response type.
     */
    public abstract WithdrawRequestResponseType getResponseType();

    /**
     * If the response type is successful, returns
     * the TXID of the withdraw transaction.
     * <p>
     * Undefined for failure responses.
     * 
     * @return The TXID of the withdraw transaction.
     */
    public abstract String getTxid();

    /**
     * If the response was successful, returns
     * the amount to be withdrawn, excluding fees.
     * <p>
     * Undefined for failure responses.
     * 
     * @return The amount to be withdrawn, excluding fees.
     */
    public abstract long getWithdrawAmount();

    /**
     * If the response was successful, returns
     * the additional amount to be paid in fees.
     * <p>
     * Undefined for failure responses.
     * 
     * @return The amount to be paid in fees.
     */
    public abstract long getFeeAmount();

    /**
     * If the response was successful, returns
     * the total cost to the user to make this withdrawal.
     * <p>
     * Undefined for failure responses.
     * 
     * @return The total cost to make the withdrawal.
     */
    public abstract long getTotalCost();
}