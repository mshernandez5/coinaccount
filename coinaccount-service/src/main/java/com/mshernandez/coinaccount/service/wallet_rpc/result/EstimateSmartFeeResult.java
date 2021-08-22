package com.mshernandez.coinaccount.service.wallet_rpc.result;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mshernandez.coinaccount.service.wallet_rpc.SatAmount;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EstimateSmartFeeResult
{
    /**
     * Estimate fee rate in sats/kB.
     */
    @JsonProperty("feerate")
    private SatAmount feeRate;

    private List<String> errors;

    /**
     * Block number where the estimate was found.
     */
    private int blocks;
}
