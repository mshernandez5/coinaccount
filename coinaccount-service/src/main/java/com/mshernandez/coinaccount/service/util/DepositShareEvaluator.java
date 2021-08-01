package com.mshernandez.coinaccount.service.util;

import com.mshernandez.coinaccount.entity.Account;
import com.mshernandez.coinaccount.entity.Deposit;

public class DepositShareEvaluator implements CoinEvaluator<Deposit>
{
    private Account account;
    private boolean useLockedDeposits;

    private long inputFee;

    /**
     * Create a deposit share evaluator.
     * 
     * @param account The account owning portions of the deposits.
     * @param inputFee A constant fee to use for each input. (Will be removed in future.)
     */
    public DepositShareEvaluator(Account account, long inputFee)
    {
        this.account = account;
        useLockedDeposits = false;
        this.inputFee = inputFee;
    }

    /**
     * Create a deposit share evaluator.
     * 
     * @param account The account owning portions of the deposits.
     * @param useLockedDeposits Whether to consider locked deposits valid.
     * @param inputFee A constant fee to use for each input. (Will be removed in future.)
     */
    public DepositShareEvaluator(Account account, boolean useLockedDeposits, long inputFee)
    {
        this.account = account;
        this.useLockedDeposits = useLockedDeposits;
        this.inputFee = inputFee;
    }

    @Override
    public boolean isValid(Deposit deposit)
    {
        return useLockedDeposits || !deposit.hasWithdrawLock();
    }

    @Override
    public long cost(Deposit obj)
    {
        return inputFee;
    }

    @Override
    public long evaluate(Deposit deposit)
    {
        return deposit.getShare(account);
    }
}