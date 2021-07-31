package com.mshernandez.coinaccount.task;

import java.util.UUID;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * An event sent out when an account receives
 * newly confirmed balances that are ready for use.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class DepositConfirmedEvent
{
    /**
     * The UUID of the account that received newly
     * confirmed balances.
     */
    private UUID accountId;

    /**
     * The sum of all newly confirmed balances.
     */
    private long amount;
}