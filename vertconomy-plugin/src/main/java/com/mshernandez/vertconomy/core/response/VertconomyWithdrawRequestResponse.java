package com.mshernandez.vertconomy.core.response;

import com.mshernandez.vertconomy.core.entity.WithdrawRequest;

/**
 * Carries response information for user requests to
 * withdraw balances.
 */
public class VertconomyWithdrawRequestResponse extends WithdrawRequestResponse
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
    public VertconomyWithdrawRequestResponse(WithdrawRequest withdrawRequest)
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
    public VertconomyWithdrawRequestResponse(WithdrawRequestResponseType responseType)
    {
        this.responseType = responseType;
        txid = "ERROR";
        withdrawAmount = 0L;
        feeAmount = 0L;
        totalCost = 0L;
    }

    @Override
    public WithdrawRequestResponseType getResponseType()
    {
        return responseType;
    }

    @Override
    public String getTxid()
    {
        return txid;
    }

    @Override
    public long getWithdrawAmount()
    {
        return withdrawAmount;
    }

    @Override
    public long getFeeAmount()
    {
        return feeAmount;
    }

    @Override
    public long getTotalCost()
    {
        return totalCost;
    }
}