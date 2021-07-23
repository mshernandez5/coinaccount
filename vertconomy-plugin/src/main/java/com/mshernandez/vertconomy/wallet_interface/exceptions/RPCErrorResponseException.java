package com.mshernandez.vertconomy.wallet_interface.exceptions;

import com.mshernandez.vertconomy.wallet_interface.ResponseError;

/**
 * A <code>WalletRequestException</code> thrown when the RPC response was
 * received but indicated an error code.
 */
public class RPCErrorResponseException extends WalletRequestException
{
    private ResponseError error;

    public RPCErrorResponseException(ResponseError error)
    {
        super("The wallet response returned error code "
              + error.code() + ": "
              + error.name());
        this.error = error;
    }

    public ResponseError getError()
    {
        return error;
    }
}