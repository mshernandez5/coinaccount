package com.mshernandez.coinaccount.service.util;

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
 * but attempts to get close to it without attempting all
 * possibilities.
 * <p>
 * For example, given inputs (1,4,6) with target 5
 * then (1,4) will be selected which is best.
 * However, given inputs (2,3,6) with target 5
 * the algorithm will give the unoptimal solution (6).
 * <p>
 * The returned set's iterator will follow the order of selection.
 * 
 * @author Markus Hernandez
 */
public class BinarySearchCoinSelector<T> implements CoinSelector<T>
{
    /**
     * How far back the algorithm is willing to rewind
     * the selection if a better solution is possible.
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
     * The dynamically selected parameter will grow logarithmically
     * with input size, meaning this will technically result in
     * an overall worst-case complexity of O(n*log(n)^2).
     * <p>
     * -2 indicates the algorithm will always rewind results
     * despite increased cost. Using -2, m = n. The worst-case
     * complexity will be O(n^2*log(n)) which is quite bad when
     * a large number of inputs are used.
     * <p>
     * 0 indicates the algorithm will never rewind results
     * even if a better solution is possible.
     * In this case, the worst-case complexity is O(n*log(n)).
     */
    private int maxRewindSetting;

    /**
     * Whether the selection should account
     * for fees introduced by the selection
     * itself (cover input fees).
     */
    private boolean coverFees;

    /**
     * Create a binary search coin selector
     * with default settings.
     */
    public BinarySearchCoinSelector()
    {
        maxRewindSetting = -1;
        coverFees = true;
    }

    /**
     * Create a binary search coin selector
     * with customized parameters.
     * <p>
     * Read class documentation for more parameter
     * details.
     * 
     * @param maxRewindSetting Max result rewind if better solution if found.
     * @param coverFees Whether the selection should account for fees introduced by the selection itself.
     */
    public BinarySearchCoinSelector(int maxStackDepthSetting, boolean coverFees)
    {
        this.maxRewindSetting = maxStackDepthSetting;
        this.coverFees = coverFees;
    }

    @Override
    public void selectInputs(CoinSelectionState<T> state, Set<T> inputs, CoinEvaluator<T> evaluator)
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
            if (inputs.size() < 25)
            {
                maxRewind = inputs.size();
            }
            else
            {
                maxRewind = 25 + (int) Math.log(inputs.size());
            }
        }
        else
        {
            // Enable Max Rewind Despite Performance Cost
            maxRewind = inputs.size();
        }
        // Get List Of Sorted Inputs That Can Be Used
        Set<T> previousSelection = state.getSelection();
        List<T> sorted = inputs.stream()
            .filter(o -> evaluator.isValid(o) && !previousSelection.contains(o))
            .sorted(evaluator)
            .collect(Collectors.toCollection(ArrayList::new));
        // Store Selected Inputs & Where They Were Found
        Deque<SelectionEntry> selectedInputs = new ArrayDeque<>(sorted.size());
        // Keep Selecting Inputs Until Target Value Is Met
        long selectionValue = state.getValue(); // The total value of the selected inputs not considering their costs.
        double selectionCost = state.getCost(); // The total costs of the selected inputs.
        long amountNeeded = state.getTarget() - selectionValue; // The remaining amount needed to reach the target.
        if (coverFees)
        {
            amountNeeded += evaluator.costImpactOnTarget(selectionCost);
        }
        while (amountNeeded > 0L && !sorted.isEmpty())
        {
            // Keep Track Of Costs To Select Next Input
            double costDelta = evaluator.nthInputCost(previousSelection.size() + selectedInputs.size());
            selectionCost += costDelta;
            // Update New Selection Target
            amountNeeded = state.getTarget() - selectionValue;
            if (coverFees)
            {
                amountNeeded += evaluator.costImpactOnTarget(selectionCost);
            }
            // Binary Search For Next Input Closest To Current Target Amount
            int first = 0,
                last = sorted.size() - 1,
                mid;
            while (first <= last)
            {
                mid = (first + last) / 2;
                long value = getValue(sorted.get(mid), evaluator);
                double difference = absDiff(value, amountNeeded);
                // Check If Any Smaller Deposits Closer Or Equally Close To Target Amount
                if (mid - 1 >= first
                    && absDiff(getValue(sorted.get(mid - 1), evaluator), amountNeeded) <= difference)
                {
                    last = mid - 1;
                }
                // Check If Any Larger Deposits Closer To Target Amount
                else if (mid + 1 <= last
                    && absDiff(getValue(sorted.get(mid + 1), evaluator), amountNeeded) < difference)
                {
                    first = mid + 1;
                }
                // This Deposit Is Closest To The Target Amount
                else
                {
                    T selected = sorted.get(mid);
                    SelectionEntry lastSelected;
                    // If Selected Input Larger Than Last Selected, Try To Only Use Larger Input
                    int numRewinds = 0;
                    while (numRewinds < maxRewind
                        && (lastSelected = selectedInputs.peek()) != null
                        && value > lastSelected.getValue())
                    {
                        numRewinds++;
                        selectionValue -= lastSelected.getValue();
                        selectionCost -= lastSelected.getTotalSelectionCosts();
                        sorted.add(lastSelected.getIndex(), selectedInputs.pop().getInput());
                    }
                    double inputCost = evaluator.cost(selected);
                    costDelta += inputCost;
                    selectedInputs.push(new SelectionEntry(selected, sorted.indexOf(selected), value, costDelta));
                    sorted.remove(selected);
                    selectionValue += value;
                    selectionCost += inputCost;
                    break;
                }
            }
            // Update New Selection Target
            amountNeeded = state.getTarget() - selectionValue;
            if (coverFees)
            {
                amountNeeded += evaluator.costImpactOnTarget(selectionCost);
            }
        }
        // Return Without Changes If Target Value Couldn't Be Fulfilled
        if (amountNeeded > 0L)
        {
            return;
        }
        // Set Iterator Will Follow Order Of Selection
        Set<T> result = new LinkedHashSet<>(selectedInputs.size());
        while (!selectedInputs.isEmpty())
        {
            result.add(selectedInputs.removeLast().getInput());
        }
        state.updateSelection(result, selectionValue, selectionCost, true);
    }

    private long getValue(T input, CoinEvaluator<T> evaluator)
    {
        return coverFees ? evaluator.effectiveValue(input) : evaluator.evaluate(input);
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

    /**
     * Used to store intermediate selection
     * results.
     */
    private class SelectionEntry
    {
        private T input;
        private int index;
        private long value;
        private double costDelta;

        SelectionEntry(T input, int index, long value, double costDelta)
        {
            this.input = input;
            this.index = index;
            this.value = value;
            this.costDelta = costDelta;
        }

        /**
         * Get the selected input.
         * 
         * @return The selected input for this entry.
         */
        T getInput()
        {
            return input;
        }

        /**
         * Get the index this input was taken from.
         * 
         * @return The index in the collection this input was taken from.
         */
        int getIndex()
        {
            return index;
        }

        /**
         * Get the value of this input.
         * 
         * @return The value of this input.
         */
        long getValue()
        {
            return value;
        }

        /**
         * Get the net change in total costs after
         * selecting this input, including costs
         * due to the number of inputs selected.
         * 
         * @return The net change in total selection costs.
         */
        double getTotalSelectionCosts()
        {
            return costDelta;
        }
    }
}