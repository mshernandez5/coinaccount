package com.mshernandez.vertconomy.wallet_interface.responses;

import java.util.List;

import com.google.gson.annotations.SerializedName;
import com.mshernandez.vertconomy.wallet_interface.WalletResponse;

/**
 * Used to get raw transaction information.
 */
public class RawTransactionResponse extends WalletResponse<RawTransactionResponse.Result>
{
    public class Result
    {
        /**
         * Whether specified block is in the active chain or not
         * (only present with explicit "blockhash" argument)
         */
        @SerializedName("in_active_chain")
        public boolean inActiveChain;

        /**
         * The serialized, hex-encoded data for 'txid'
         */
        public String hex;

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
         * The block hash.
         */
        @SerializedName("blockhash")
        public String blockHash;

        /**
         * The confirmations.
         */
        public int confirmations;

        /**
         * The block time expressed in UNIX epoch time.
         */
        @SerializedName("blocktime")
        public long blockTime;

        /**
         * Same as "blocktime".
         */
        public long time;
    }
}