package com.mshernandez.coinaccount.service.exception;

public class WithdrawRequestAlreadyExistsException extends RuntimeException
{
    public WithdrawRequestAlreadyExistsException()
    {
        super();
    }

    public WithdrawRequestAlreadyExistsException(String message)
    {
        super(message);
    }
}
