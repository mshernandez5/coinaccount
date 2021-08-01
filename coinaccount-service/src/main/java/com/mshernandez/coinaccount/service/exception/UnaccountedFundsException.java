package com.mshernandez.coinaccount.service.exception;

public class UnaccountedFundsException extends RuntimeException
{
    public UnaccountedFundsException()
    {
        super();
    }

    public UnaccountedFundsException(String message)
    {
        super(message);
    }
}
