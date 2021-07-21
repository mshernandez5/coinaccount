package com.mshernandez.vertconomy.core;

import java.util.HashSet;
import java.util.Set;

/**
 * Select all valid input deposits that have values greater than
 * the costs required to withdraw them. Target values are ignored.
 */
public class MaxAmountCoinSelector<T> implements CoinSelector<T>
{
    @Override
    public Set<T> selectInputs(CoinEvaluator<T> evaluator, Set<T> inputs, long target)
    {
        Set<T> selectedInputs = new HashSet<>();
        for (T input : inputs)
        {
            if (evaluator.isValid(input) && evaluator.costAdjustedValue(input) > 0L)
            {
                selectedInputs.add(input);
            }
        }
        return selectedInputs.isEmpty() ? null : selectedInputs;
    }
}