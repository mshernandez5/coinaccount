package com.mshernandez.coinaccount.service.exception;

public class NotEnoughWithdrawableFundsException extends RuntimeException
{
    public NotEnoughWithdrawableFundsException()
    {
        super();
    }

    public NotEnoughWithdrawableFundsException(String message)
    {
        super(message);
    }
}
