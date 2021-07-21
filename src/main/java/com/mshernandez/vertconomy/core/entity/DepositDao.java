package com.mshernandez.vertconomy.core.entity;

public interface DepositDao
{
    /**
     * Finds a deposit based on its TXID & vout.
     * 
     * @param txid The deposit TXID.
     * @param vout The deposit vout.
     * @return A deposit reference, or null if none was found.
     */
    Deposit find(String txid, int vout);

    /**
     * Persist a newly created deposit.
     * 
     * @param deposit The deposit object.
     */
    void persist(Deposit deposit);

    /**
     * Merges any changes made to the deposit.
     * 
     * @param deposit The deposit with changes.
     * @return A reference to the updated deposit.
     */
    Deposit update(Deposit deposit);

    /**
     * Update the state of the deposit to match the
     * database, overwriting any changes.
     * 
     * @param deposit The deposit to refresh.
     */
    void refresh(Deposit deposit);

    /**
     * Remove the deposit.
     * 
     * @param deposit The deposit to remove.
     */
    void remove(Deposit deposit);
}
