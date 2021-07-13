package com.mshernandez.vertconomy.core;

import java.util.Comparator;

import com.mshernandez.vertconomy.database.Account;
import com.mshernandez.vertconomy.database.Deposit;

/**
 * Compares deposits by the funds a specific account
 * holds in them (regardless of their total values).
 */
public class DepositShareComparator implements Comparator<Deposit>
{
    private Account account;

    /**
     * Create a deposit share comparator.
     * 
     * @param account The account owning portions of the deposits.
     */
    public DepositShareComparator(Account account)
    {
        this.account = account;
    }

    @Override
    public int compare(Deposit a, Deposit b)
    {
        return (int) (a.getDistribution(account) - b.getDistribution(account));
    }
}
