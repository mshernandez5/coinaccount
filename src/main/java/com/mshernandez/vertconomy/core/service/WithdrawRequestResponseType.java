package com.mshernandez.vertconomy.core.service;

public enum WithdrawRequestResponseType
{
    SUCCESS,
    NOT_ENOUGH_WITHDRAWABLE_FUNDS,
    CANNOT_AFFORD_FEES,
    REQUEST_ALREADY_EXISTS,
    INVALID_ADDRESS,
    UNKNOWN_FAILURE
}