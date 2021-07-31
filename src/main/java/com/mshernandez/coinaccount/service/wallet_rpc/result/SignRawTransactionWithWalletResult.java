package com.mshernandez.coinaccount.service.wallet_rpc.result;

import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SignRawTransactionWithWalletResult
{
    /**
     * The hex-encoded raw transaction with signatures.
     */
    private String hex;

    /**
     * Whether the transaction has a complete set
     * of signatures.
     */
    private boolean complete;

    /**
     * Script verification errors, if any.
     */
    private List<ScriptVerificationError> errors;

    @Getter
    @Setter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public class ScriptVerificationError
    {
        /**
         * The has of the referenced, previous transaction.
         */
        private String txid;

        /**
         * The index of the output to spend and use as an input.
         */
        private int vout;

        /**
         * The hex-encoded signature script.
         */
        private String scriptSig;

        /**
         * The script sequence number.
         */
        private int sequence;

        /**
         * The verification or signing error related to the input.
         */
        private String error;
    }
}