package com.mshernandez.vertconomy.wallet_interface;

import com.google.gson.annotations.SerializedName;

/**
 * Used to deserialize estimatesmartfee
 * responses.
 */
public class SmartFeeResponse extends WalletResponse<SmartFeeResponse.Result>
{
    public class Result
    {
        @SerializedName("feerate")
        SatAmount feeRate;

        int blocks;
    }
}