package com.mshernandez.vertconomy.wallet_interface.exceptions;

public class WalletRequestException extends Exception
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