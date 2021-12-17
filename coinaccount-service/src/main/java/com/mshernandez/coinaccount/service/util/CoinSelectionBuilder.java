package com.mshernandez.coinaccount.service.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

public class CoinSelectionBuilder<T>
{
    LinkedList<SelectionStep> steps;
    CoinEvaluator<T> evaluator;
    long target;

    /**
     * Create a new unconfigured coin selection builder.
     */
    public CoinSelectionBuilder()
    {
        steps = new LinkedList<>();
        evaluator = null;
        target = -1L;
    }

    /**
     * Adds a step in the coin selection process using
     * the provided selector and input set.
     * 
     * @param selector The selector to use in this step.
     * @param inputs The inputs to select from in this step.
     * @return A reference to this builder for method chaining.
     */
    public CoinSelectionBuilder<T> step(CoinSelector<T> selector, Set<T> inputs)
    {
        steps.add(new SelectionStep(selector, inputs));
        return this;
    }

    /**
     * Set the evaluator for selectors to compare inputs
     * and determine their values.
     * 
     * @param evaluator The evaluator for the selectors to use.
     * @return A reference to this builder for method chaining.
     */
    public CoinSelectionBuilder<T> evaluator(CoinEvaluator<T> evaluator)
    {
        this.evaluator = evaluator;
        return this;
    }

    /**
     * Set the target selection value.
     * 
     * @param target The target selection value.
     * @return A reference to this builder for method chaining.
     */
    public CoinSelectionBuilder<T> target(long target)
    {
        this.target = target;
        return this;
    }

    /**
     * Completes the coin selection based on
     * the builder configuration.
     * 
     * @return An object holding the coin selection results.
     * @throws InvalidSelectionParameterException If the evaluator or target are missing/invalid.
     */
    public CoinSelectionState<T> select()
    {
        CoinSelectionState<T> result = new CoinSelectionState<>(target);
        if (target < 0L)
        {
            throw new InvalidSelectionParameterException("Invalid selection target!");
        }
        if (evaluator == null)
        {
            throw new InvalidSelectionParameterException("Invalid or missing evaluator!");
        }
        Iterator<SelectionStep> iterator = steps.iterator();
        while (!result.isComplete() && iterator.hasNext())
        {
            SelectionStep nextStep = iterator.next();
            nextStep.selector.selectInputs(result, nextStep.inputs, evaluator);
        }
        return result;
    }

    /**
     * Remembers which selector and corresponding input set
     * to use in a step of the coin selection process.
     */
    private class SelectionStep
    {
        CoinSelector<T> selector;
        Set<T> inputs;

        SelectionStep(CoinSelector<T> selector, Set<T> inputs)
        {
            this.selector = selector;
            this.inputs = inputs;
        }
    }
}