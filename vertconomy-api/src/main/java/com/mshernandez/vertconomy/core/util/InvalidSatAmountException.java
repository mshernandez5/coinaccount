package com.mshernandez.vertconomy.core.util;

/**
 * An exception thrown when a <code>SatAmountFormatter</code>
 * attempts to parse an invalid String into an amount.
 */
public class InvalidSatAmountException extends Exception
{
    public InvalidSatAmountException()
    {
        super();
    }

    public InvalidSatAmountException(String message)
    {
        super(message);
    }
}