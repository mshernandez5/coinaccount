package com.mshernandez.vertconomy.wallet_interface.responses;

import com.google.gson.annotations.SerializedName;
import com.mshernandez.vertconomy.wallet_interface.SatAmount;
import com.mshernandez.vertconomy.wallet_interface.WalletResponse;

/**
 * Used to get funded transaction information.
 */
public class FundRawTransactionResponse extends WalletResponse<FundRawTransactionResponse.Result>
{
    public class Result
    {
        /**
         * The resulting raw transaction (hex-encoded string).
         */
        public String hex;

        /**
         * Fee the resulting transaction pays.
         */
        public SatAmount fee;

        /**
         * The position of the added change output, or -1.
         */
        @SerializedName("changepos")
        public int changePos;
    }
}