package com.mshernandez.vertconomy.wallet_interface.responses;

import com.google.gson.annotations.SerializedName;
import com.mshernandez.vertconomy.wallet_interface.WalletResponse;

/**
 * Used to get general wallet status information.
 */
public class WalletInfoResponse extends WalletResponse<WalletInfoResponse.Result>
{
    public class Result
    {
        /**
         * The wallet name.
         */
        @SerializedName("walletname")
        public String walletName;

        /**
         * The wallet version.
         */
        @SerializedName("walletversion")
        public double walletVersion;

        /**
         * The database format (bdb or sqlite).
         */
        public String format;

        /**
         * DEPRECATED. Identical to getbalances().mine.trusted
         */
        public double balance;
        
        /**
         * DEPRECATED. Identical to getbalances().mine.untrusted_pending
         */
        @SerializedName("unconfirmed_balance")
        public double unconfirmedBalance;

        /**
         * DEPRECATED. Identical to getbalances().mine.immature
         */
        @SerializedName("immature_balance")
        public double immatureBalance;

        /**
         * The total number of transactions in the wallet.
         */
        @SerializedName("txcount")
        public long txCount;

        /**
         * The UNIX epoch time of the oldest pre-generated
         * key in the key pool. Legacy wallets only.
         */
        @SerializedName("keypoololdest")
        public long keyPoolOldest;

        /**
         * How many new keys are pre-generated
         * (only counts external keys).
         */
        @SerializedName("keypoolsize")
        public long keyPoolSize;

        /**
         * How many new keys are pre-generated for
         * internal use (used for change outputs,
         * only appears if the wallet is using this
         * feature, otherwise external keys are used).
         */
        @SerializedName("keypoolsize_hd_internal")
        public long keyPoolSizeHdInternal;

        /**
         * UNIX epoch time until which the
         * wallet is unlocked for transfers,
         * or 0 if the wallet is locked
         * (only present for passphrase-encrypted wallets).
         */
        @SerializedName("unlocked_until")
        public long unlockedUntil;

        /**
         * The transaction fee configuration, set in
         * VTC/kvB.
         */
        @SerializedName("paytxfee")
        public long payTxFee;

        /**
         * The Hash160 of the HD seed
         * (only present when HD is enabled).
         */
        @SerializedName("hdseedid")
        public String hdSeedId;

        /**
         * False if privatekeys are disabled for
         * this wallet (enforced watch-only wallet).
         */
        @SerializedName("privatekeysenabled")
        public boolean privateKeysEnabled;

        /**
         * Whether this wallet tracks clean/dirty
         * coins in terms of reuse.
         */
        @SerializedName("avoid_reuse")
        public boolean avoidReuse;

        /**
         * Current scanning details,
         * or null if no scan is in progress
         */
        public ScanningDetails scanning;

        /**
         * Whether this wallet uses descriptors for
         * scriptPubKey management.
         */
        public boolean descriptors;
    }

    /**
     * Holds scanning progress details if the wallet
     * is busy scanning.
     */
    public class ScanningDetails
    {
        /**
         * Elapsed seconds since scan start.
         */
        public long duration;

        /**
         * Scanning progress percentage [0.0, 1.0]
         */
        public double progress;
    }
}
