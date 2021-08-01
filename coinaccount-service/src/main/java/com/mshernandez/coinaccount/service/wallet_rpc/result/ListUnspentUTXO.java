package com.mshernandez.coinaccount.service.wallet_rpc.result;

import com.mshernandez.coinaccount.service.wallet_rpc.SatAmount;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ListUnspentUTXO
{
    /**
     * The transaction id.
     */
    private String txid;

    /**
     * The index of this output.
     */
    private int vout;

    /**
     * The address the output was sent to.
     */
    private String address;

    /**
     * The label corresponding to the address.
     */
    private String label;

    /**
     * The script key.
     */
    private String scriptPubKey;

    /**
     * The output amount.
     */
    private SatAmount amount;

    /**
     * The number of confirmations.
     */
    private int confirmations;

    /**
     * The redeem script if scriptPubKey is P2SH.
     */
    private String redeemScript;

    /**
     * The witness script if scriptPubKey is P2WSH.
     */
    private String witnessScript;

    /**
     * Whether we have the private keys to spend
     * this output.
     */
    private boolean spendable;

    /**
     * Whether we know how to spend this output,
     * ignoring the lack of keys.
     */
    private boolean solvable;

    /**
     * (only present if avoid_reuse is set)
     * Whether this output is reused/dirty
     * (sent to an address that was previously spent from)
     */
    private boolean reused;

    /**
     * (only when solvable)
     * A descriptor for spending this output.
     */
    private String desc;

    /**
     * Whether this output is considered safe to spend.
     * Unconfirmed transactions from outside keys and
     * unconfirmed replacement transactions are considered
     * unsafe and are not eligible for spending by
     * fundrawtransaction and sendtoaddress.
     */
    private boolean safe;
}