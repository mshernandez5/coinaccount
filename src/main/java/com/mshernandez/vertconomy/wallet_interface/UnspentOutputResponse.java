package com.mshernandez.vertconomy.wallet_interface;

import java.util.List;

/**
 * Used to get general wallet status information.
 */
public class UnspentOutputResponse extends WalletResponse<List<UnspentOutputResponse.UnspentOutput>>
{  
    public class UnspentOutput
    {
        /**
         * Transaction ID
         */
        public String txid;

        /**
         * Vector Out Index For This Output
         */
        public int vout;

        /**
         * Address Transaction Sent To
         */
        public String address;

        /**
         * Label Corresponding To Address
         */
        public String label;

        /**
         * The script key.
         */
        public String scriptPubKey;

        /**
         * Output Amount, Converted To Sats
         */
        public SatAmount amount;

        /**
         * Number Of Confirmations
         */
        public int confirmations;

        /**
         * The redeemScript if scriptPubKey is P2SH.
         */
        public String redeemScript;

        /**
         * witnessScript if the scriptPubKey is P2WSH or P2SH-P2WSH.
         */
        public String witnessScript;

        /**
         * Whether we have the private keys to spend this output.
         */
        public boolean spendable;

        /**
         * Whether we know how to spend this output, ignoring the lack of keys.
         */
        public boolean solvable;

        /**
         * (only present if avoid_reuse is set) Whether this output is reused/dirty
         * (sent to an address that was previously spent from)
         */
        public boolean reused;

        /**
         * (only when solvable) A descriptor for spending this output.
         */
        public String desc;

        /**
         * Whether this output is considered safe to spend.
         * Unconfirmed transactions from outside keys and
         * unconfirmed replacement transactions are considered
         * unsafe and are not eligible for spending by
         * fundrawtransaction and sendtoaddress.
         */
        public boolean safe;
    }
}
