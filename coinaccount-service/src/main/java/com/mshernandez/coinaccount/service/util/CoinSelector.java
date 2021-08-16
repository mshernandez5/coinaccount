package com.mshernandez.coinaccount.service.util;

import java.util.Set;

public interface CoinSelector<T>
{
    /**
     * Given a set of inputs - each with a discrete value and cost - attempt
     * to select a subset of the provided inputs such that the combined value
     * of the subset both reaches the desired target and covers the additional
     * cost of each selection.
     * 
     * @param evaluator An evaluator to compare inputs and determine their values/costs.
     * @param inputs The inputs to select from to meet the target amount.
     * @param target The target amount to reach, excluding the cost of selection itself.
     * @return A coin selection result including the selected set and costs of selection.
     */
    CoinSelectionResult<T> selectInputs(CoinEvaluator<T> evaluator, Set<T> inputs, long target);
}