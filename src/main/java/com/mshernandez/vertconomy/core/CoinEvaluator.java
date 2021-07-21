package com.mshernandez.vertconomy.core;

import java.util.Comparator;

/**
 * An extension of Comparator that can also
 * evaluate objects into discrete values.
 */
public interface CoinEvaluator<T> extends Comparator<T>
{
    /**
     * Whether an object is valid for evaluation
     * or should be ignored.
     * 
     * @param obj The object.
     * @return Whether it is valid for evaluation.
     */
    boolean isValid(T obj);

    /**
     * Determine the cost of selecting this particular input.
     * 
     * @param obj The object.
     * @return The cost for selecting the object.
     */
    long cost(T obj);

    /**
     * Evaluate an object into a discrete value
     * suitable for comparisons.
     * 
     * @param obj The object to evaluate.
     * @return A value for the object suitable for comparison.
     */
    long evaluate(T obj);

    /**
     * Get the net value of the object considering
     * costs.
     * 
     * @param obj The object to evaluate.
     * @return The adjusted value.
     */
    default long costAdjustedValue(T obj)
    {
        return evaluate(obj) - cost(obj);
    }
}