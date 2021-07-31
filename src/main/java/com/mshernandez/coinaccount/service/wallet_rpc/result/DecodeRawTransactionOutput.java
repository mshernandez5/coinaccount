package com.mshernandez.coinaccount.service.wallet_rpc.result;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mshernandez.coinaccount.service.wallet_rpc.SatAmount;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DecodeRawTransactionOutput
{
    /**
     * The output value.
     */
    private SatAmount value;

    /**
     * Output index.
     */
    @JsonProperty("n")
    private int index;

    /**
     * The locking script.
     */
    private ScriptPubKey scriptPubKey;

    @Getter
    @Setter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public class ScriptPubKey
    {
        /**
         * The script assembly.
         */
        private String asm;

        /**
         * The script hex.
         */
        private String hex;

        /**
         * The required signatures.
         */
        private int reqSigs;

        /**
         * The type, ex. "pubkeyhash"
         */
        private String type;

        /**
         * The addresses.
         */
        private List<String> addresses;
    }
}