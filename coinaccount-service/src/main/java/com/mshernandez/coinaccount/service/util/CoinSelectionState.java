package com.mshernandez.coinaccount.service.util;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Stores the results from coin selection,
 * whether partial or complete.
 * 
 * @see CoinSelector
 */
public class CoinSelectionState<T>
{
    // Selection Target Value
    private long target;

    // Selection State
    private Set<T> selected;
    private long value;
    private double cost;
    private boolean complete;

    /**
     * Create a incomplete coin selection state
     * with no selected inputs.
     * 
     * @param target The target selection value.
     */
    protected CoinSelectionState(long target)
    {
        this.target = target;
        selected = new LinkedHashSet<>();
        value = 0L;
        cost = 0.0;
        complete = false;
    }

    /**
     * Update the selection result to reflect newly
     * chosen inputs.
     * 
     * @param selected The updated set of selected inputs.
     * @param effectiveValue The total value of the updated selection.
     * @param cost The total cost of the updated selection.
     * @param complete Whether the selection has been completed.
     */
    protected void updateSelection(Set<T> selected, long value, double cost, boolean complete)
    {
        this.selected = selected;
        this.value = value;
        this.cost = cost;
        this.complete = complete;
    }

    /**
     * Get the selection target value.
     * 
     * @return The selection target value.
     */
    public long getTarget()
    {
        return target;
    }

    /**
     * Get the effective value of the current selection.
     * 
     * @return The effective value of the current selection.
     */
    public long getValue()
    {
        return value;
    }

    /**
     * Get the selection cost.
     * 
     * @return The selection cost.
     */
    public double getCost()
    {
        return cost;
    }

    /**
     * Returns the set of selected inputs.
     * <p>
     * Check <code>isComplete()</code> to determine 
     * whether the selection result is valid.
     * 
     * @return The set of selected inputs.
     */
    public Set<T> getSelection()
    {
        return new LinkedHashSet<>(selected);
    }

    /**
     * Indicates whether the selection fulfills
     * the target amount.
     * 
     * @return True if the selection is complete.
     */
    public boolean isComplete()
    {
        return complete;
    }
}