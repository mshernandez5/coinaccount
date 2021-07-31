package com.mshernandez.coinaccount.task;

import java.util.UUID;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * An event sent out when an account-initiated
 * withdraw request expires.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class WithdrawRequestExpiredEvent
{
    /**
     * The UUID of the account that no longer
     * has an active withdraw request.
     */
    private UUID accountId;
}