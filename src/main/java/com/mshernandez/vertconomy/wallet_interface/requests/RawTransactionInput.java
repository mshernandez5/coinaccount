package com.mshernandez.vertconomy.wallet_interface.requests;

public class RawTransactionInput
{
    public RawTransactionInput(String txid, int vout)
    {
        this.txid = txid;
        this.vout = vout;
    }

    public RawTransactionInput()
    {
        // For Serialization
    }

    /**
     * The transaction id of the UTXO.
     */
    public String txid;

    /**
     * The output number corresponding to the UTXO.
     */
    public int vout;
}