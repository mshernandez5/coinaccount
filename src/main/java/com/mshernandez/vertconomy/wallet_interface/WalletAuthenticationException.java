package com.mshernandez.vertconomy.wallet_interface;

public class WalletAuthenticationException extends WalletRequestException
{
    /**
     * Create an authentication exception.
     */
    public WalletAuthenticationException()
    {
        super("Failed To Authenticate");
    }

    /**
     * Create an exception specifying the user
     * that the program attempted to authenticate.
     * 
     * @param user The user used for authentication.
     */
    public WalletAuthenticationException(String user)
    {
        super("Failed To Authenticate With User: " + user);
    }
}
