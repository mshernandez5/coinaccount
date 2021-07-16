package com.mshernandez.vertconomy.core;

import com.mshernandez.vertconomy.database.Account;
import com.mshernandez.vertconomy.database.Deposit;

public class WithdrawDepositShareEvaluator implements Evaluator<Deposit>
{
    private Account account;

    /**
     * Create a deposit share evaluator.
     * 
     * @param account The account owning portions of the deposits.
     */
    public WithdrawDepositShareEvaluator(Account account)
    {
        this.account = account;
    }

    @Override
    public int compare(Deposit a, Deposit b)
    {
        return (int) (a.getDistribution(account) - b.getDistribution(account));
    }

    @Override
    public boolean isValid(Deposit deposit)
    {
        return !deposit.hasWithdrawLock();
    }

    @Override
    public long evaluate(Deposit deposit)
    {
        return deposit.getDistribution(account);
    }
}