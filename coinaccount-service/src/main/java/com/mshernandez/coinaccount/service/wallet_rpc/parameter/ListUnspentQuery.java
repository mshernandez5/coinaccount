package com.mshernandez.coinaccount.service.wallet_rpc.parameter;

import com.mshernandez.coinaccount.service.wallet_rpc.SatAmount;

import lombok.Getter;
import lombok.Setter;

@Getter
public class ListUnspentQuery
{
    // Standard Request Parameters
    private int minConfirmations;
    private int maxConfirmations;
    private String[] addresses;
    private boolean includeUnsafe;

    // Query Options
    private QueryOptions queryOptions;
    
    /**
     * Create a list unspent query with default options.
     */
    public ListUnspentQuery()
    {
        minConfirmations = 1;
        maxConfirmations = 9999999;
        addresses = new String[] {};
        includeUnsafe = true;
        queryOptions = null;
    }

    /**
     * Set the minimum number of confirmations.
     * 
     * @param minConfirmations The minimum number of confirmations.
     * @return A reference to this query object for chaining.
     */
    public ListUnspentQuery setMinConfirmations(int minConfirmations)
    {
        this.minConfirmations = minConfirmations;
        return this;
    }

    /**
     * Set the maximum number of confirmations.
     * 
     * @param minConfirmations The maximum number of confirmations.
     * @return A reference to this query object for chaining.
     */
    public ListUnspentQuery setMaxConfirmations(int maxConfirmations)
    {
        this.maxConfirmations = maxConfirmations;
        return this;
    }

    /**
     * Set the addresses to filter UTXOs.
     * 
     * @param addresses The addresses.
     * @return A reference to this query object for chaining.
     */
    public ListUnspentQuery setAddresses(String... addresses)
    {
        this.addresses = addresses;
        return this;
    }

    /**
     * Set the minimum value of each UTXO.
     * 
     * @param minAmount The minimum value of each UTXO.
     * @return A reference to this query object for chaining.
     */
    public ListUnspentQuery setMinimumAmount(long minAmount)
    {
        if (queryOptions == null)
        {
            queryOptions = new QueryOptions();
        }
        queryOptions.setMinimumAmount(new SatAmount(minAmount));
        return this;
    }

    /**
     * Set the maximum value of each UTXO.
     * 
     * @param maxAmount The maximum value of each UTXO.
     * @return A reference to this query object for chaining.
     */
    public ListUnspentQuery setMaximumAmount(long maxAmount)
    {
        if (queryOptions == null)
        {
            queryOptions = new QueryOptions();
        }
        queryOptions.setMaximumAmount(new SatAmount(maxAmount));
        return this;
    }

    /**
     * Set the maximum number of UTXOs.
     * 
     * @param maxCount The maximum number of UTXOs.
     * @return A reference to this query object for chaining.
     */
    public ListUnspentQuery setMaximumCount(int maxCount)
    {
        if (queryOptions == null)
        {
            queryOptions = new QueryOptions();
        }
        queryOptions.setMaximumCount(maxCount);
        return this;
    }

    /**
     * Set the minimum sum value of all UTXOs.
     * 
     * @param minSumAmount The minimum sum value of all UTXOs.
     * @return A reference to this query object for chaining.
     */
    public ListUnspentQuery setMinimumSumAmount(long minSumAmount)
    {
        if (queryOptions == null)
        {
            queryOptions = new QueryOptions();
        }
        queryOptions.setMinimumAmount(new SatAmount(minSumAmount));
        return this;
    }

    @Setter
    @Getter
    public class QueryOptions
    {
        private SatAmount minimumAmount;
        private SatAmount maximumAmount;
        private int maximumCount;
        private SatAmount minimumSumAmount;

        public QueryOptions()
        {
            minimumAmount = null; // no value, exclude from serialization by default
            maximumAmount = null; // no value, exclude from serialization by default
            maximumCount = Integer.MAX_VALUE;
            minimumSumAmount = null; // no value, exclude from serialization by default
        }
    }
}