package com.mshernandez.coinaccount.service.exception;

public class InvalidAddressException extends RuntimeException
{
    public InvalidAddressException()
    {
        super();
    }

    public InvalidAddressException(String message)
    {
        super(message);
    }
}
