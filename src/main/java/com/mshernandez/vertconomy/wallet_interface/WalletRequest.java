package com.mshernandez.vertconomy.wallet_interface;

import com.google.gson.JsonArray;
import com.google.gson.annotations.SerializedName;

/**
 * A JSON-serializable object to build a request
 * to send to the wallet.
 * 
 * Available methods and their respective parameters
 * can be found by looking through bitcoind
 * RPC API documentation.
 */
class WalletRequest
{
    @SerializedName("jsonrpc")
    static final int JSON_RPC = 1;

    String id;

    String method;

    JsonArray params;

    /**
     * Set the ID for this request.
     * 
     * @param id An ID for this request.
     * @return A reference to this request for chaining.
     */
    WalletRequest setId(String id)
    {
        this.id = id;
        return this;
    }

    /**
     * Set the method to request from the wallet.
     * 
     * @param method The method to request.
     * @return A reference to this request for chaining.
     */
    WalletRequest setMethod(String method)
    {
        this.method = method;
        return this;
    }

    /**
     * Set method parameters, as required by
     * each individual method.
     * 
     * @param params Parameters for the requested method.
     * @return A reference to this request for chaining.
     */
    WalletRequest setParams(JsonArray params)
    {
        this.params = params;
        return this;
    }
}
