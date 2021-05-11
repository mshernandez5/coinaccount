package com.mshernandez.vertconomy.wallet_interface.responses;

import java.util.List;

import com.mshernandez.vertconomy.wallet_interface.WalletResponse;

/**
 * Used to get general wallet status information.
 */
public class SignRawTransactionResponse extends WalletResponse<SignRawTransactionResponse.Result>
{
    public class Result
    {
        /**
         * The hex-encoded raw transaction with signatures.
         */
        public String hex;

        /**
         * Whether the transaction has a complete set
         * of signatures.
         */
        public boolean complete;

        /**
         * Script verification errors, if any.
         */
        public List<Error> errors;
    }

    public class Error
    {
        /**
         * The hash of the referenced, previous transaction.
         */
        public String txid;

        /**
         * The index of the output to spend and use as an input.
         */
        public int vout;

        /**
         * The hex-encoded signature script.
         */
        public String scriptSig;

        /**
         * Script sequence number.
         */
        public int sequence;

        /**
         * Verification or signing error related to the input.
         */
        public String error;
    }
}
