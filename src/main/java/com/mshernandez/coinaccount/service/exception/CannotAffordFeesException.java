package com.mshernandez.coinaccount.service.exception;

public class CannotAffordFeesException extends RuntimeException
{
    public CannotAffordFeesException()
    {
        super();
    }

    public CannotAffordFeesException(String message)
    {
        super(message);
    }
}
