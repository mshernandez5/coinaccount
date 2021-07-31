package com.mshernandez.coinaccount.service.wallet_rpc;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ArrayNode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RPCRequest
{
    /**
     * The JSON-RPC version being used.
     */
    @JsonProperty("jsonrpc")
    private String jsonRpcVersion;

    /**
     * The name of the method to be invoked.
     */
    private String method;

    /**
     * The method parameters.
     */
    private ArrayNode params;

    /**
     * A client-provided identifier.
     */
    private String id;
}