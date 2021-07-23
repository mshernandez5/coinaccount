package com.mshernandez.vertconomy.wallet_interface.requests;

import java.util.List;

import com.google.gson.annotations.SerializedName;
import com.mshernandez.vertconomy.wallet_interface.SatAmount;

/**
 * Defines options to provide to fundRawTransaction().
 */
public class FundRawTransactionOptions
{
    /**
     * For a transaction with existing inputs,
     * whether to automatically include more
     * if they are not enough.
     */
    @SerializedName("add_inputs")
    public boolean addInputs;

    /**
     * The address to receive the change.
     */
    public String changeAddress;

    /**
     * The index of the change output.
     */
    public int changePosition;

    @SerializedName("change_type")
    public String changeType;

    /**
     * Also select inputs which are watch only.
     * Only solvable inputs can be used.
     * Watch-only destinations are solvable if
     * the public key and/or output script was imported,
     * e.g. with 'importpubkey' or 'importmulti' with the
     * 'pubkeys' or 'desc' field.
     */
    public boolean includeWatching;

    /**
     * Lock selected unspent outputs.
     */
    public boolean lockUnspents;

    /**
     * Set a fee rate in sat/kvB.
     */
    @SerializedName("fee_rate")
    public SatAmount satFeeRate;

    /**
     * Set a fee rate in VTC/kvB.
     */
    public double feeRate;

    /**
     * vout list, which outputs to subtract from.
     * Fees are distributed equally among selected outputs.
     */
    List<Integer> subtractFeeFromOutputs;
}
