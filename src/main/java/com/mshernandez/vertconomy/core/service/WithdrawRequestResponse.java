package com.mshernandez.vertconomy.core.service;

import com.mshernandez.vertconomy.core.entity.WithdrawRequest;

/**
 * Carries response information for user requests to
 * withdraw balances.
 */
public class WithdrawRequestResponse
{
    // Response Type
    private WithdrawRequestResponseType responseType;

    // Transaction TXID
    private String txid;

    // Amount to Withdraw Excluding Fees
    private long withdrawAmount;

    // Amount Paid in Fees
    private long feeAmount;

    // Combined Amount, Total Cost To User
    private long totalCost;

    /**
     * Create a successful response using information
     * from the withdraw request.
     * 
     * @param withdrawRequest
     * @param responseType
     */
    public WithdrawRequestResponse(WithdrawRequest withdrawRequest)
    {
        responseType = WithdrawRequestResponseType.SUCCESS;
        txid = withdrawRequest.getTxid();
        withdrawAmount = withdrawRequest.getWithdrawAmount();
        feeAmount = withdrawRequest.getFeeAmount();
        totalCost = withdrawRequest.getTotalCost();
    }

    /**
     * Create a failure response, specifying the type
     * of failure with the response type.
     * 
     * @param responseType The type of failure.
     */
    public WithdrawRequestResponse(WithdrawRequestResponseType responseType)
    {
        this.responseType = responseType;
        txid = "ERROR";
        withdrawAmount = 0L;
        feeAmount = 0L;
        totalCost = 0L;
    }

    /**
     * Get the response type, indicating wither success
     * or some type of failure.
     * 
     * @return The response type.
     */
    public WithdrawRequestResponseType getResponseType()
    {
        return responseType;
    }

    /**
     * If the response type is successful, returns
     * the TXID of the withdraw transaction.
     * 
     * @return The TXID of the withdraw transaction.
     */
    public String getTxid()
    {
        return txid;
    }

    /**
     * If the response was successful, returns
     * the amount to be withdrawn, excluding fees.
     * 
     * @return The amount to be withdrawn, excluding fees.
     */
    public long getWithdrawAmount()
    {
        return withdrawAmount;
    }

    /**
     * If the response was successful, returns
     * the additional amount to be paid in fees.
     * 
     * @return The amount to be paid in fees.
     */
    public long getFeeAmount()
    {
        return feeAmount;
    }

    /**
     * If the response was successful, returns
     * the total cost to the user to make this withdrawal.
     * 
     * @return The total cost to make the withdrawal.
     */
    public long getTotalCost()
    {
        return totalCost;
    }
}