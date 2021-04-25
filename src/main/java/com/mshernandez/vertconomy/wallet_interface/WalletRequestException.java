package com.mshernandez.vertconomy.wallet_interface;

public class WalletRequestException extends Exception
{
    /**
     * Create a generic wallet request exception.
     */
    public WalletRequestException()
    {
        super("Failed To Make A Wallet Request");
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