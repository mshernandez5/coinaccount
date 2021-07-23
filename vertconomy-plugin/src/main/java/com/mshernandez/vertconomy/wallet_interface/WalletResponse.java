package com.mshernandez.vertconomy.wallet_interface;

/**
 * Used to deserialize RPC call responses.
 * 
 * @param <T> The expected result type.
 */
public class WalletResponse<T>
{
    public T result;
    public ResponseError error;
    public String id;
}