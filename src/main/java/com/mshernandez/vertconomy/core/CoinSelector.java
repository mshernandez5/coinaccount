package com.mshernandez.vertconomy.core;

import java.util.Set;

public interface CoinSelector<T>
{
    /**
     * Given a set of inputs - each with a discrete value - attempt to select
     * a subset of the provided inputs that together reach the desired target
     * value. A cost may be provided to select inputs in such a way that the
     * resulting set can both cover the target value in addition to the added
     * costs of each selected input.
     * 
     * @param evaluator An evaluator to compare inputs and determine their values.
     * @param inputs The inputs to select from to meet the target amount.
     * @param cost The added cost of selecting an additional input.
     * @param target The target amount to select, excluding the cost of selecting additional inputs.
     * @return A set of inputs that can meet the target amount in addition to the cost of their selection. Returns null if no such set could be formed.
     */
    Set<T> selectInputs(Evaluator<T> evaluator, Set<T> inputs, long cost, long target);
}