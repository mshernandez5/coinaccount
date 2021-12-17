package com.mshernandez.coinaccount.service.wallet_rpc.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mshernandez.coinaccount.service.wallet_rpc.SatAmount;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GetWalletInfoResult
{
    /**
     * The wallet name.
     */
    @JsonProperty("walletname")
    private String walletName;

    /**
     * The wallet version.
     */
    @JsonProperty("walletversion")
    private String walletVersion;

    /**
     * The database format (bdb or sqlite).
     */
    private String format;

    /**
     * DEPRECATED. Identical to getbalances().mine.trusted
     */
    private SatAmount balance;

    /**
     * DEPRECATED. Identical to getbalances().mine.untrusted_pending
     */
    @JsonProperty("unconfirmed_balance")
    private SatAmount unconfirmedBalance;

    /**
     * DEPRECATED. Identical to getbalances().mine.immature
     */
    @JsonProperty("immature_balance")
    private SatAmount immatureBalance;
    
    /**
     * The total number of transactions in the wallet.
     */
    @JsonProperty("txcount")
    private long txCount;

    /**
     * The UNIX epoch time of the oldest pre-generated
     * key in the key pool. Legacy wallets only.
     */
    @JsonProperty("keypoololdest")
    private long keyPoolOldest;

    /**
     * How many new keys are pre-generated
     * (only counts external keys).
     */
    @JsonProperty("keypoolsize")
    private long keyPoolSize;

    /**
     * How many new keys are pre-generated for
     * internal use (used for change outputs,
     * only appears if the wallet is using this
     * feature, otherwise external keys are used).
     */
    @JsonProperty("keypoolsize_hd_internal")
    private long keyPoolSizeHdInternal;

    /**
     * UNIX epoch time until which the
     * wallet is unlocked for transfers,
     * or 0 if the wallet is locked
     * (only present for passphrase-encrypted wallets).
     */
    @JsonProperty("unlocked_until")
    private long unlockedUntil;

    /**
     * The transaction fee configuration, set in
     * VTC/kvB.
     */
    @JsonProperty("paytxfee")
    private long payTxFee;

    /**
     * The Hash160 of the HD seed
     * (only present when HD is enabled).
     */
    @JsonProperty("hdseedid")
    private String hdSeedId;

    /**
     * False if privatekeys are disabled for
     * this wallet (enforced watch-only wallet).
     */
    @JsonProperty("private_keys_enabled")
    private boolean privateKeysEnabled;

    /**
     * Whether this wallet tracks clean/dirty
     * coins in terms of reuse.
     */
    @JsonProperty("avoid_reuse")
    private boolean avoidReuse;

    /**
     * Whether this wallet uses descriptors for
     * scriptPubKey management.
     */
    private boolean descriptors;
}
