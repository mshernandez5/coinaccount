package com.mshernandez.coinaccount.dao;

import java.util.Collection;

import com.mshernandez.coinaccount.entity.WithdrawRequest;

public interface WithdrawRequestDao
{
    /**
     * Finds a withdraw request based on the TXID
     * of its pending transaction.
     * 
     * @param txid The TXID corresponding to the withdraw transaction.
     * @return A withdraw request object.
     */
    WithdrawRequest find(String txid);

    /**
     * Finds all withdraw requests.
     * 
     * @return A collection of all withdraw requests.
     */
    Collection<WithdrawRequest> findAll();

    /**
     * Finds all incomplete withdraw requests.
     * 
     * @return A collection of all incomplete withdraw requests.
     */
    Collection<WithdrawRequest> findAllIncomplete();

    /**
     * Persist a newly created withdraw request.
     * 
     * @param withdrawRequest The withdraw request.
     */
    void persist(WithdrawRequest withdrawRequest);

    /**
     * Merges any changes made to the withdraw request.
     * 
     * @param withdrawRequest The withdraw request.
     * @return A reference to the updated withdraw request.
     */
    WithdrawRequest update(WithdrawRequest withdrawRequest);

    /**
     * Update the state of the deposit to match the
     * database, overwriting any changes.
     * 
     * @param withdrawRequest The withdraw request to refresh.
     */
    void refresh(WithdrawRequest withdrawRequest);
    
    /**
     * Remove the withdraw request.
     * 
     * @param withdrawRequest The withdraw request to remove.
     */
    void remove(WithdrawRequest withdrawRequest);
}
