package com.mshernandez.vertconomy.core;

import com.mshernandez.vertconomy.core.account.Account;
import com.mshernandez.vertconomy.core.deposit.Deposit;

public class DepositShareEvaluator implements Evaluator<Deposit>
{
    private Account account;
    private boolean useLockedDeposits;

    /**
     * Create a deposit share evaluator.
     * 
     * @param account The account owning portions of the deposits.
     */
    public DepositShareEvaluator(Account account)
    {
        this.account = account;
        useLockedDeposits = false;
    }

    /**
     * Create a deposit share evaluator.
     * 
     * @param account The account owning portions of the deposits.
     * @param useLockedDeposits Whether to consider locked deposits valid.
     */
    public DepositShareEvaluator(Account account, boolean useLockedDeposits)
    {
        this.account = account;
        this.useLockedDeposits = useLockedDeposits;
    }

    @Override
    public int compare(Deposit a, Deposit b)
    {
        return (int) (a.getShare(account) - b.getShare(account));
    }

    @Override
    public boolean isValid(Deposit deposit)
    {
        return useLockedDeposits || !deposit.hasWithdrawLock();
    }

    @Override
    public long evaluate(Deposit deposit)
    {
        return deposit.getShare(account);
    }
}