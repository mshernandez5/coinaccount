package com.mshernandez.coinaccount.service.wallet_rpc.result;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DecodeRawTransactionInput
{
    /**
     * The transaction id.
     */
    private String txid;

    /**
     * The output number.
     */
    private int vout;

    /**
     * The unlocking script.
     */
    private ScriptSignature scriptSig;

    @Getter
    @Setter
    @NoArgsConstructor
    public class ScriptSignature
    {
        /**
         * The script assembly.
         */
        private String asm;

        /**
         * The script hex.
         */
        private String hex;
    }
}