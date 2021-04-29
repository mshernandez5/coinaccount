package com.mshernandez.vertconomy.wallet_interface;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Used to get general wallet status information.
 */
public class ListTransactionResponse
{
    List<Transaction> result;
    String error;
    String id;
    
    public class Transaction
    {
        /**
         * Only returns true if imported addresses were
         * involved in transaction.
         */
        @SerializedName("involvesWatchonly")
        boolean involvesWatchOnly;

        /**
         * The address of the transaction.
         */
        String address;

        /**
         * The transaction category.
         * 
         * - send
         * - receive
         * - generate
         * - immature
         * - orphan
         */
        String category;

        /**
         * The amount in VTC. This is negative
         * for the 'send' category, and is positive
         * for all other categories.
         */
        double amount;

        /**
         * A comment for the address/transaction, if any.
         */
        String label;

        /**
         * The vout value.
         */
        int vout;

        /**
         * The amount of the fee in VTC.
         * This is negative and only available
         * for the 'send' category of transactions.
         */
        double fee;

        /**
         * The number of confirmations for the transaction.
         * Negative confirmations means the transaction
         * conflicted that many blocks ago.
         */
        int confirmations;

        /**
         * Only present if transaction only input
         * is a coinbase one.
         */
        boolean generated;

        /**
         * Only present if we consider transaction
         * to be trusted and so safe to spend from.
         */
        boolean trusted;

        /**
         * The block hash containing the transaction.
         */
        @SerializedName("blockhash")
        String blockHash;

        /**
         * The block height containing the transaction.
         */
        @SerializedName("blockheight")
        int blockHeight;

        /**
         * The index of the transaction in the block that includes it.
         */
        @SerializedName("blockindex")
        int blockIndex;

        /**
         * The block time expressed in UNIX epoch time.
         */
        @SerializedName("blocktime")
        long blockTime;

        /**
         * The transaction id.
         */
        String txid;

        /**
         * Conflicting transaction ids.
         */
        @SerializedName("walletconflicts")
        List<String> walletConflicts;

        /**
         * The transaction time expressed in UNIX epoch time.
         */
        long time;

        /**
         * The time received expressed in UNIX epoch time.
         */
        @SerializedName("timereceived")
        long timeReceived;

        /**
         * If a comment is associated with the transaction,
         * only present if not empty.
         */
        String comment;

        /**
         * Whether this transaction could be replaced
         * due to BIP125 (replace-by-fee);
         * may be unknown for unconfirmed
         * transactions not in the mempool
         */
        @SerializedName("bip125-replaceable")
        String bip125Replaceable;

        /**
         * 'true' if the transaction has been
         * abandoned (inputs are respendable).
         * Only available for the
         * 'send' category of transactions.
         */
        boolean abandoned;
    }
}
