package com.mshernandez.vertconomy.wallet_interface.responses;

import java.util.List;

import com.google.gson.annotations.SerializedName;
import com.mshernandez.vertconomy.wallet_interface.SatAmount;
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

    /**
     * Stores information about a decoded
     * transaction input.
     */
    public class TXInput
    {
        public String txid;
        
        public int vout;

        public ScriptSignature scriptSig;
    }

    public class ScriptSignature
    {
        public String asm;

        public String hex;
    }

    /**
     * Stores information about a decoded
     * transaction output.
     */
    public class TXOutput
    {
        public SatAmount value;

        @SerializedName("n")
        public int index;

        public ScriptPubKey scriptPubKey;
    }

    public class ScriptPubKey
    {
        public String asm;

        public String hex;

        public int reqSigs;

        public String type;

        List<String> addresses;
    }
}
