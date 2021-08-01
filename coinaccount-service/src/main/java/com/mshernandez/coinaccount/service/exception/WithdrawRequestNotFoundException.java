package com.mshernandez.coinaccount.service.exception;

public class WithdrawRequestNotFoundException extends RuntimeException
{
    public WithdrawRequestNotFoundException()
    {
        super();
    }

    public WithdrawRequestNotFoundException(String message)
    {
        super(message);
    }
}
