package com.mshernandez.vertconomy.wallet_interface.responses;

import com.google.gson.annotations.SerializedName;
import com.mshernandez.vertconomy.wallet_interface.SatAmount;
import com.mshernandez.vertconomy.wallet_interface.WalletResponse;

/**
 * Used to deserialize estimatesmartfee
 * responses.
 */
public class SmartFeeResponse extends WalletResponse<SmartFeeResponse.Result>
{
    public class Result
    {
        @SerializedName("feerate")
        public SatAmount feeRate;

        public int blocks;
    }
}