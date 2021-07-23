package com.mshernandez.vertconomy.core.response;

/**
 * All possible response types when attempting
 * to initiate a withdraw request.
 */
public enum WithdrawRequestResponseType
{
    /**
     * Indicates the withdraw request was successful.
     */
    SUCCESS,
    
    /**
     * Indicates the user does not have enough withdrawable
     * funds to complete the request. Some account funds may
     * be temporarily unavailable for withdraw.
     */
    NOT_ENOUGH_WITHDRAWABLE_FUNDS,

    /**
     * Indicates the account cannot afford the necessary
     * fees to make the withdrawal.
     */
    CANNOT_AFFORD_FEES,

    /**
     * Indicates the account already initiated a withdraw
     * request and cannot make another one until it
     * is confirmed or canceled.
     */
    REQUEST_ALREADY_EXISTS,

    /**
     * Indicates that an invalid withdraw address was provided.
     */
    INVALID_ADDRESS,

    /**
     * Indicates an unknown failure.
     */
    UNKNOWN_FAILURE
}