package com.mshernandez.coinaccount.service.util;

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
     * Evaluate an object into a discrete value
     * suitable for comparisons.
     * 
     * @param obj The object to evaluate.
     * @return A value for the object suitable for comparison.
     */
    long evaluate(T obj);

    /**
     * Determine the cost of selecting this particular input.
     * The cost may use a different scale/unit than values.
     * <p>
     * In practice, this is used to determine the added vsize
     * requirement from selecting this input, excluding
     * input counter size increases.
     * 
     * @param obj The object.
     * @return The cost for selecting the object.
     */
    double cost(T obj);

    /**
     * Determine any additional cost of selecting the nth input,
     * independent of the standard value or cost of the input.
     * <p>
     * This may be used to add additional costs based solely on the
     * number of inputs selected.
     * <p>
     * In practice, this is used to determine any added vsize
     * requirement from having to expand the input counter varint size.
     * 
     * @param index The zero-based index representing the position of the next input.
     * @return The cost of selecting the input at the given index.
     */
    double nthInputCost(long index);

    /**
     * Determine how much the given cost raises the selection target.
     * 
     * @param cost The cost.
     * @return The impact of the cost on the selection target, in units of the target value.
     */
    long costImpactOnTarget(double cost);

    /**
     * Get the net value of the object considering
     * its selection cost.
     * <p>
     * This should not include costs based on the
     * number of inputs selected, as that would apply
     * equally to any of the next selected inputs.
     * 
     * @param obj The object to evaluate.
     * @return The adjusted net value.
     */
    default long effectiveValue(T obj)
    {
        return evaluate(obj) - costImpactOnTarget(cost(obj));
    }

    @Override
    default int compare(T a, T b)
    {
        return (int) (effectiveValue(a) - effectiveValue(b));
    }
}