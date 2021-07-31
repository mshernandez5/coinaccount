package com.mshernandez.coinaccount.service.wallet_rpc.exception;

public class WalletRequestException extends RuntimeException
{
    /**
     * Create a generic wallet request exception.
     */
    public WalletRequestException()
    {
        super("Failed to make a wallet request!");
    }

    /**
     * Customize the exception with a message.
     * 
     * @param customMessage An exception message.
     */
    public WalletRequestException(String customMessage)
    {
        super(customMessage);
    }
}