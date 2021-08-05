package com.mshernandez.coinaccount.service.util;

import java.util.HashSet;
import java.util.Set;

/**
 * Selects all valid input deposits that have values greater than
 * the costs required to withdraw them.
 * <p>
 * The target is used as a minimum value for the selected inputs to
 * reach; if the inputs cannot reach the minimum target then
 * null is returned.
 */
public class MaxAmountCoinSelector<T> implements CoinSelector<T>
{
    @Override
    public Set<T> selectInputs(CoinEvaluator<T> evaluator, Set<T> inputs, long minTarget)
    {
        Set<T> selectedInputs = new HashSet<>();
        long totalSelectedValue = 0L;
        for (T input : inputs)
        {
            long costAdjustedValue = evaluator.costAdjustedValue(input);
            if (evaluator.isValid(input) && costAdjustedValue > 0L)
            {
                selectedInputs.add(input);
                totalSelectedValue += costAdjustedValue;
            }
        }
        return selectedInputs.isEmpty() || totalSelectedValue < minTarget ? null : selectedInputs;
    }
}