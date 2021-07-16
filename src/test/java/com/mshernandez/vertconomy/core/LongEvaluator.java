package com.mshernandez.vertconomy.core;

/**
 * Evaluate wrapped Long values for testing purposes.
 * Allows the use of Long values instead of actual
 * Deposit objects in testing.
 */
public class LongEvaluator implements Evaluator<Long>
{
    @Override
    public int compare(Long a, Long b)
    {
        return a.compareTo(b);
    }

    @Override
    public boolean isValid(Long l)
    {
        return l > 0L;
    }

    @Override
    public long evaluate(Long l)
    {
        return l.longValue();
    }
}