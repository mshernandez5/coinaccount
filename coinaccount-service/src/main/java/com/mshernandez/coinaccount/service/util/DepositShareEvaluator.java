package com.mshernandez.coinaccount.service.util;

import com.mshernandez.coinaccount.entity.Account;
import com.mshernandez.coinaccount.entity.Deposit;

public class DepositShareEvaluator implements CoinEvaluator<Deposit>
{
    private Account account;
    private boolean useLockedDeposits;

    private double feeRate;

    /**
     * Create a deposit share evaluator.
     * 
     * @param account The account owning portions of the deposits.
     * @param feeRate The current fee rate in sat/byte.
     */
    public DepositShareEvaluator(Account account, double feeRate)
    {
        this.account = account;
        useLockedDeposits = false;
        this.feeRate = feeRate;
    }

    /**
     * Create a deposit share evaluator.
     * 
     * @param account The account owning portions of the deposits.
     * @param useLockedDeposits Whether to consider locked deposits valid.
     * @param feeRate The current fee rate in sat/byte.
     */
    public DepositShareEvaluator(Account account, boolean useLockedDeposits, double feeRate)
    {
        this.account = account;
        this.useLockedDeposits = useLockedDeposits;
        this.feeRate = feeRate;
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

    @Override
    public double cost(Deposit deposit)
    {
        return TXFeeUtilities.getInputSize(deposit.getType()) + TXFeeUtilities.TX_INPUT_WITNESS_ITEM_COUNTER_VSIZE;
    }

    @Override
    public double nthInputCost(long index)
    {
        if (index == 0L)
        {
            // [1, 252]: 1 Byte
            return 1.0;
        }
        else if (index == 252L)
        {
            // [253, 65535]: 1 Byte Prefix + 2 Bytes (2 Bytes Additional)
            return 2.0;
        }
        else if (index == 65535L)
        {
            // [65536, 4294967295]: 1 Byte Prefix + 4 Bytes (2 Bytes Additional)
            return 2.0;
        }
        else if (index == 4294967295L)
        {
            // [4294967296, How did you get all these UTXOs?]: 1 Byte Prefix + 8 Bytes (4 Bytes Additional)
            return 4.0;
        }
        // Somewhere in between the ranges, no additional costs yet.
        return 0.0;
    }

    @Override
    public long costImpactOnTarget(double cost)
    {
        return (long) Math.ceil(cost * feeRate);
    }
}