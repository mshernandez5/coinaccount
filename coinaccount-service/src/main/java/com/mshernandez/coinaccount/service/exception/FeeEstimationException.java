package com.mshernandez.coinaccount.service.exception;

public class FeeEstimationException extends RuntimeException
{
    public FeeEstimationException()
    {
        super();
    }

    public FeeEstimationException(String message)
    {
        super(message);
    }
}
