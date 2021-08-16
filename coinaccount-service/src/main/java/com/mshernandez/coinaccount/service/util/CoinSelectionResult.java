package com.mshernandez.coinaccount.service.util;

import java.util.Set;

/**
 * Stores the results from coin selection.
 * @see CoinSelector
 */
public class CoinSelectionResult<T>
{
    private Set<T> selected;
    private double selectionCost;

    /**
     * Create a new coin selection result.
     * 
     * @param selected A valid set of selected inputs or null if selection failed.
     * @param selectionCost The total cost of selecting the inputs.
     */
    CoinSelectionResult(Set<T> selected, double selectionCost)
    {
        this.selected = selected;
        this.selectionCost = selectionCost;
    }

    /**
     * Indicates whether selection was successful
     * and the selected set is valid.
     * 
     * @return True if selection was successful.
     */
    public boolean isValid()
    {
        return selected != null;
    }

    /**
     * Returns the set of selected inputs or
     * null if selection failed.
     * 
     * @return The set of selected inputs or null if selection failed.
     */
    public Set<T> getSelection()
    {
        return selected;
    }

    /**
     * Get the total cost to select the given inputs.
     * <p>
     * For deposits, this corresponds to the additional
     * TX size incurred by the selected UTXO set.
     * 
     * @return The total cost to select the given inputs.
     */
    public double getSelectionCost()
    {
        return selectionCost;
    }
}