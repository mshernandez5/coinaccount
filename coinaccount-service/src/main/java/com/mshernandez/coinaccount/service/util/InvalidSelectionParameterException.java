package com.mshernandez.coinaccount.service.util;

/**
 * An exception thrown by CoinSelectionBuilder when
 * necessary selection parameters are missing or invalid.
 */
public class InvalidSelectionParameterException extends RuntimeException
{
    protected InvalidSelectionParameterException()
    {
        super();
    }

    protected InvalidSelectionParameterException(String message)
    {
        super(message);
    }
}
