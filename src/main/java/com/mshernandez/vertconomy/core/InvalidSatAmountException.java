package com.mshernandez.vertconomy.core;

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