package com.mshernandez.coinaccount.service.util;

/**
 * Evaluate wrapped Long values for testing purposes.
 * Allows the use of Long values instead of actual
 * Deposit objects in testing.
 * <p>
 * Assumes zero-fee to select any value.
 */
public class LongEvaluator implements CoinEvaluator<Long>
{
    private long fee;

    /**
     * Create an evaluator that assigns no fees
     * to each input.
     */
    public LongEvaluator()
    {
        fee = 0L;
    }

    /**
     * Create an evaluator that assigns a constant
     * fee to each input.
     * 
     * @param fee The fee to use for each input.
     */
    public LongEvaluator(long fee)
    {
        this.fee = fee;
    }

    @Override
    public boolean isValid(Long l)
    {
        return l > 0L;
    }

    @Override
    public long cost(Long l)
    {
        return fee;
    }

    @Override
    public long evaluate(Long l)
    {
        return l.longValue();
    }
}