package com.mshernandez.vertconomy.core.withdraw;

public enum WithdrawRequestResponseType
{
    SUCCESS,
    NOT_ENOUGH_WITHDRAWABLE_FUNDS,
    CANNOT_AFFORD_FEES,
    REQUEST_ALREADY_EXISTS,
    UNKNOWN_FAILURE
}