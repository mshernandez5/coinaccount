package com.mshernandez.vertconomy.wallet_interface.responses;

import java.util.List;

import com.google.gson.annotations.SerializedName;
import com.mshernandez.vertconomy.wallet_interface.WalletResponse;

/**
 * Used to store decoded transaction information.
 */
public class DecodeTransactionResponse extends WalletResponse<DecodeTransactionResponse.Result>
{
    /**
     * Decoded transaction information.
     */
    public class Result
    {
        /**
         * The transaction id.
         */
        public String txid;

        /**
         * The transaction hash
         * (differs from txid for witness transactions).
         */
        public String hash;

        /**
         * The transaction size.
         */
        public int size;

        /**
         * The virtual transaction size
         * (differs from size for witness transactions).
         */
        public int vsize;

        /**
         * The transaction's weight
         * (between vsize*4 - 3 and vsize*4).
         */
        public int weight;

        /**
         * The version.
         */
        public int version;

        /**
         * The lock time.
         */
        @SerializedName("locktime")
        public long lockTime;

        /**
         * Transaction inputs.
         */
        public List<TXInput> vin;

        /**
         * Transaction outputs.
         */
        public List<TXOutput> vout;

        /**
         * Hex-encoded witness data.
         */
        @SerializedName("txinwitness")
        public List<String> witnessData;

        /**
         * The script sequence number.
         */
        public int sequence;
    }
}
