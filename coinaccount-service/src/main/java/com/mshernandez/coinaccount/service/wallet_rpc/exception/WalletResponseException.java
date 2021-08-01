package com.mshernandez.coinaccount.service.wallet_rpc.exception;

/**
 * A <code>WalletRequestException</code> thrown when the RPC response was
 * received but indicated an error code.
 */
public class WalletResponseException extends WalletRequestException
{
    private WalletResponseError error;

    public WalletResponseException(WalletResponseError error)
    {
        super("The wallet response returned error code "
              + error.code() + ": "
              + error.name());
        this.error = error;
    }

    public WalletResponseError getError()
    {
        return error;
    }
}