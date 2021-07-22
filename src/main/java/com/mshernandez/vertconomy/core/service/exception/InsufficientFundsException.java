package com.mshernandez.vertconomy.core.service.exception;

public class InsufficientFundsException extends Exception
{
    public InsufficientFundsException()
    {
        super();
    }

    public InsufficientFundsException(String message)
    {
        super(message);
    }
}
