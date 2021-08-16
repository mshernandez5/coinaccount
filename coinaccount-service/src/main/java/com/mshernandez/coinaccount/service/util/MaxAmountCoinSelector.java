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
    public CoinSelectionResult<T> selectInputs(CoinEvaluator<T> evaluator, Set<T> inputs, long minTarget)
    {
        Set<T> selected = new HashSet<>();
        long totalNetValue = 0L;
        double totalCost = 0.0;
        for (T input : inputs)
        {
            long netValue = evaluator.netValue(input);
            if (evaluator.isValid(input) && netValue > 0L)
            {
                totalNetValue += netValue;
                totalCost += evaluator.cost(input) + evaluator.nthInputCost(selected.size());
                selected.add(input);
            }
        }
        minTarget += evaluator.costImpactOnTarget(totalCost);
        return selected.isEmpty() || totalNetValue < minTarget ? new CoinSelectionResult<>(null, 0L) : new CoinSelectionResult<>(selected, totalCost);
    }
}