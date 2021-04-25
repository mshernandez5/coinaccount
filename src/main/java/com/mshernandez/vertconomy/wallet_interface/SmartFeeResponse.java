package com.mshernandez.vertconomy.wallet_interface;

import com.google.gson.annotations.SerializedName;

public class SmartFeeResponse
{
    Result result;
    String error;
    String id;
    
    public class Result
    {
        @SerializedName("feerate")
        String feeRate;

        int blocks;
    }
}