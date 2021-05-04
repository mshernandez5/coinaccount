package com.mshernandez.vertconomy.wallet_interface;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Used to get general wallet status information.
 */
public class TransactionListResponse extends WalletResponse<List<TransactionListResponse.Transaction>>
{  
    public class Transaction
    {
        /**
         * Only returns true if imported addresses were
         * involved in transaction.
         */
        @SerializedName("involvesWatchonly")
        public boolean involvesWatchOnly;

        /**
         * The address of the transaction.
         */
        public String address;

        /**
         * The transaction category.
         * 
         * - send
         * - receive
         * - generate
         * - immature
         * - orphan
         */
        public String category;

        /**
         * The amount in VTC. This is negative
         * for the 'send' category, and is positive
         * for all other categories.
         */
        public SatAmount amount;

        /**
         * A comment for the address/transaction, if any.
         */
        String label;

        /**
         * The vout value.
         */
        public int vout;

        /**
         * The amount of the fee in VTC.
         * This is negative and only available
         * for the 'send' category of transactions.
         */
        public SatAmount fee;

        /**
         * The number of confirmations for the transaction.
         * Negative confirmations means the transaction
         * conflicted that many blocks ago.
         */
        public int confirmations;

        /**
         * Only present if transaction only input
         * is a coinbase one.
         */
        public boolean generated;

        /**
         * Only present if we consider transaction
         * to be trusted and so safe to spend from.
         */
        public boolean trusted;

        /**
         * The block hash containing the transaction.
         */
        @SerializedName("blockhash")
        public String blockHash;

        /**
         * The block height containing the transaction.
         */
        @SerializedName("blockheight")
        public int blockHeight;

        /**
         * The index of the transaction in the block that includes it.
         */
        @SerializedName("blockindex")
        public int blockIndex;

        /**
         * The block time expressed in UNIX epoch time.
         */
        @SerializedName("blocktime")
        public long blockTime;

        /**
         * The transaction id.
         */
        public String txid;

        /**
         * Conflicting transaction ids.
         */
        @SerializedName("walletconflicts")
        public List<String> walletConflicts;

        /**
         * The transaction time expressed in UNIX epoch time.
         */
        public long time;

        /**
         * The time received expressed in UNIX epoch time.
         */
        @SerializedName("timereceived")
        public long timeReceived;

        /**
         * If a comment is associated with the transaction,
         * only present if not empty.
         */
        public String comment;

        /**
         * Whether this transaction could be replaced
         * due to BIP125 (replace-by-fee);
         * may be unknown for unconfirmed
         * transactions not in the mempool
         */
        @SerializedName("bip125-replaceable")
        public String bip125Replaceable;

        /**
         * 'true' if the transaction has been
         * abandoned (inputs are respendable).
         * Only available for the
         * 'send' category of transactions.
         */
        public boolean abandoned;
    }
}
