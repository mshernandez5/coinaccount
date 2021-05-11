package com.mshernandez.vertconomy.wallet_interface.requests;

public class RawTransactionInput
{
    /**
     * The transaction id of the UTXO.
     */
    public String txid;

    /**
     * The output number corresponding to the UTXO.
     */
    public int vout;
}