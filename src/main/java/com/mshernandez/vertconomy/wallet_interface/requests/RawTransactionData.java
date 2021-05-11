package com.mshernandez.vertconomy.wallet_interface.requests;

import java.util.List;
import java.util.Map;

import com.mshernandez.vertconomy.wallet_interface.SatAmount;

/**
 * Used to create new raw transactions.
 */
public class RawTransactionData
{
    /**
     * The UTXO inputs for the transaction.
     */
    public List<RawTransactionInput> inputs;

    /**
     * Key-value pairs. The key (string) is the
     * bitcoin address, the value is the amount.
     */
    public Map<String, SatAmount> outputs;
}
