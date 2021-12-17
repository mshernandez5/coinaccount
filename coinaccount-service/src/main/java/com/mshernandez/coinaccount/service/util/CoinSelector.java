package com.mshernandez.coinaccount.service.util;

import java.util.Set;

public interface CoinSelector<T>
{
    /**
     * Given an existing (incomplete) coin selection state and a new set
     * of inputs - each with a discrete value and cost - attempt to join
     * a subset of the provided inputs to the existing selection such
     * that the combined value of all selected inputs reaches the
     * desired target and covers the additional costs incurred
     * by each selection. This method should update the state of
     * the coin selection to reflect any changes.
     * <p>
     * The existing input selection may not be mutually exclusive
     * from the provided set of inputs. In this case, the coin selector
     * must not attempt to select the same input twice as each may only
     * be used once.
     * 
     * @param state The existing state of the selection.
     * @param inputs A set of inputs to select from to meet the target amount.
     * @param evaluator An evaluator to compare and evaluate inputs.
     */
    public abstract void selectInputs(CoinSelectionState<T> state, Set<T> inputs, CoinEvaluator<T> evaluator);
}