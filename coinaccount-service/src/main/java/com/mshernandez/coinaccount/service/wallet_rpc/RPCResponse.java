package com.mshernandez.coinaccount.service.wallet_rpc;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RPCResponse<T>
{
    /**
     * The JSON-RPC version being used.
     */
    @JsonProperty("jsonrpc")
    private String jsonRpcVersion;

    /**
     * The result of the method invocation,
     * only required to exist on success.
     */
    private T result;

    /**
     * An error object, which must only exist
     * if an error occured during invocation.
     */
    private RPCError error;

    /**
     * The client-provided identifier
     * given in the method invocation request.
     */
    private String id;
}