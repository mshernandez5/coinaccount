package com.mshernandez.vertconomy;

/**
 * An exception thrown when players provide
 * an invalid wallet address.
 */
public class InvalidAddressException extends Exception
{
    /**
     * Create the exception with a default message.
     */
    public InvalidAddressException()
    {
        super("The specified wallet address is invalid!");
    }

    /**
     * Create a customized exception including the
     * address at fault as part of the message.
     * 
     * @param address The invalid wallet address.
     */
    public InvalidAddressException(String address)
    {
        super("The address \"" + address + "\" is not valid.");
    }
}