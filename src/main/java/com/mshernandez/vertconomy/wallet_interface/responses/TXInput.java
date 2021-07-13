package com.mshernandez.vertconomy.wallet_interface.responses;

/**
 * Stores information about a decoded
 * transaction input.
 */
public class TXInput
{
    public String txid;
    
    public int vout;

    public ScriptSignature scriptSig;

    public class ScriptSignature
    {
        public String asm;

        public String hex;
    }
}