package com.mshernandez.coinaccount.service.wallet_rpc.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mshernandez.coinaccount.service.wallet_rpc.SatAmount;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EstimateSmartFeeResult
{
    /**
     * Estimate fee rate in sats/kB.
     */
    @JsonProperty("feerate")
    private SatAmount feeRate;

    /**
     * Block number where the estimate was found.
     */
    private int blocks;
}
