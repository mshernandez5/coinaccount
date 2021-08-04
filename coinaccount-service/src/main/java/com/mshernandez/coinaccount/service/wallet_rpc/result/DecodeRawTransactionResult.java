package com.mshernandez.coinaccount.service.wallet_rpc.result;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DecodeRawTransactionResult
{
    /**
     * The transaction id.
     */
    private String txid;

    /**
     * The transaction hash
     * (differs from txid for witness transactions).
     */
    private String hash;

    /**
     * The transaction size.
     */
    private int size;

    /**
     * The virtual transaction size
     * (differs from size for witness transactions).
     */
    private int vsize;

    /**
     * The transaction weight
     * (between vsize*4 - 3 and vsize*4).
     */
    private int weight;

    /**
     * The version.
     */
    private String version;

    /**
     * The lock time.
     */
    @JsonProperty("locktime")
    private long lockTime;

    /**
     * Transaction inputs.
     */
    @JsonProperty("vin")
    private List<DecodeRawTransactionInput> inputs;

    /**
     * Transaction outputs.
     */
    @JsonProperty("vout")
    private List<DecodeRawTransactionOutput> outputs;

    /**
     * Hex-encoded witness data.
     */
    @JsonProperty("txinwitness")
    private List<String> witnessData;

    /**
     * The script sequence number.
     */
    private int sequence;
}
