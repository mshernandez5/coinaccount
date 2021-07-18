package com.mshernandez.vertconomy.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This coin selector uses a binary search algorithm
 * to find inputs closest to the target amount, which
 * is dynamically updated as inputs are selected.
 * <p>
 * If the next selected input is larger than the last,
 * selection will rewind using the larger input in place
 * of those before it since a better solution is possible.
 * <p>
 * This selector does not always return the optimal solution,
 * but attempts to get close to it without attempting all possibilities.
 * For example, given inputs (1,4,6) with target 5
 * then (1,4) will be selected which is best.
 * However, given inputs (2,3,6) with target 5
 * the algorithm will give the unoptimal solution (6).
 * <p>
 * The returned set's iterator will follow the order of selection.
 */
public class BinarySearchCoinSelector<T> implements CoinSelector<T>
{
    /**
     * How far back the algorithm is willing to rewind
     * its results if a better solution is possible.
     * <p>
     * Note that once a valid solution is found it will
     * always be selected even if better solutions are
     * available. Rewinding only affects
     * incomplete solutions.
     * <p>
     * The complexity of the selection depends on this
     * factor. With n inputs the worst-case complexity
     * is O(m*n*log(n)), where m is determined by this
     * parameter.
     * <p>
     * -1 (default) indicates the algorithm will dynamically
     * select this parameter based on input size.
     * <p>
     * -2 indicates the algorithm will always rewind results
     * despite increased cost. Using -1, m = n. The worst-case
     * complexity will be n^2*log(n) which is quite bad when
     * a large number of inputs are used.
     * <p>
     * 0 indicates the algorithm will never rewind results
     * even if a better solution is possible.
     * In this case, the worst-case complexity is O(n*log(n)).
     */
    private int maxRewindSetting;

    /**
     * Create a binary search coin selector
     * with default settings.
     */
    public BinarySearchCoinSelector()
    {
        maxRewindSetting = -1;
    }

    /**
     * Create a binary search coin selector
     * with customized parameters.
     * <p>
     * Read class documentation for more parameter
     * details.
     * 
     * @param maxRewindSetting Max result rewind if better solution if found.
     */
    public BinarySearchCoinSelector(int maxStackDepthSetting)
    {
        this.maxRewindSetting = maxStackDepthSetting;
    }

    @Override
    public Set<T> selectInputs(Evaluator<T> evaluator, Set<T> inputs, long cost, long target)
    {
        // Get Actual Max Stack Depth Based On Setting
        int maxRewind;
        if (maxRewindSetting >= 0)
        {
            // Manually Set Parameter
            maxRewind = maxRewindSetting;
        }
        else if (maxRewindSetting == -1)
        {
            // Dynamically Choose Parameter
            if (inputs.size() < 15)
            {
                maxRewind = inputs.size();
            }
            else if (inputs.size() < 30)
            {
                maxRewind = 25;
            }
            else
            {
                maxRewind = 35;
            }
        }
        else
        {
            // Enable Max Rewind Despite Performance Cost
            maxRewind = inputs.size();
        }
        // Get List Of Sorted Inputs That Can Be Used
        List<T> sorted = inputs.stream()
            .filter(o -> evaluator.isValid(o))
            .sorted(evaluator)
            .collect(Collectors.toCollection(ArrayList::new));
        // Store Selected Inputs & Where They Were Found
        Deque<Pair<T, Integer>> selectedInputs = new ArrayDeque<>(sorted.size());
        // Keep Selecting Inputs Until Target Value Is Met
        while (target > 0L && !sorted.isEmpty())
        {
            // Every Selected Input Adds Fees To The Target Amount
            target += cost;
            // Binary Search For Next Input Closest To Current Target Amount
            int first = 0,
                last = sorted.size() - 1,
                mid;
            while (first <= last)
            {
                mid = (first + last) / 2;
                long value = evaluator.evaluate(sorted.get(mid));
                double difference = absDiff(value, target);
                // Check If Any Smaller Deposits Closer Or Equally Close To Target Amount
                if (mid - 1 >= first
                    && absDiff(evaluator.evaluate(sorted.get(mid - 1)), target) <= difference)
                {
                    last = mid - 1;
                }
                // Check If Any Larger Deposits Closer To Target Amount
                else if (mid + 1 <= last
                    && absDiff(evaluator.evaluate(sorted.get(mid + 1)), target) < difference)
                {
                    first = mid + 1;
                }
                // This Deposit Is Closest To The Target Amount
                else
                {
                    T selected = sorted.get(mid);
                    Pair<T, Integer> lastSelected;
                    // If Selected Input Larger Than Last Selected, Try To Only Use Larger Input
                    int numRewinds = 0;
                    while (numRewinds < maxRewind
                        && (lastSelected = selectedInputs.peek()) != null
                        && value > evaluator.evaluate(lastSelected.getKey()))
                    {
                        numRewinds++;
                        sorted.add(lastSelected.getVal(), selectedInputs.pop().getKey());
                        target += evaluator.evaluate(lastSelected.getKey());
                        target -= cost;
                    }
                    selectedInputs.push(new Pair<>(selected, sorted.indexOf(selected)));
                    sorted.remove(selected);
                    target -= value;
                    break;
                }
            }
        }
        // Return null If Target Value Couldn't Be Fulfilled
        if (target > 0L)
        {
            return null;
        }
        // Set Iterator Will Follow Order Of Selection
        Set<T> result = new LinkedHashSet<>(selectedInputs.size());
        while (!selectedInputs.isEmpty())
        {
            result.add(selectedInputs.removeLast().getKey());
        }
        return result;
    }

    /**
     * Calculate the absolute difference between the two values.
     * 
     * @param a The first value.
     * @param b The second value.
     * @return The absolute difference.
     */
    private double absDiff(long a, long b)
    {
        return Math.abs(a - b);
    }
}