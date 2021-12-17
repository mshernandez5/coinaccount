package com.mshernandez.coinaccount.dao;

import java.util.Collection;

import com.mshernandez.coinaccount.entity.Account;
import com.mshernandez.coinaccount.entity.Address;
import com.mshernandez.coinaccount.service.wallet_rpc.parameter.DepositType;

public interface AddressDao
{
    /**
     * Gets address information.
     * 
     * @param address The address string.
     * @return An address object or null if none was found.
     */
    Address find(String address);

    /**
     * Finds all addresses.
     * 
     * @return A collection of all addresses.
     */
    Collection<Address> findAll();

    /**
     * Finds or creates an address matching the specified criteria.
     * 
     * @param account The owning account.
     * @param type The type of address to find.
     * @param requireUnused Whether the address must be unused.
     * @return An address matching the criteria.
     */
    Address findOrCreate(Account account, DepositType type, boolean requireUnused);

    /**
     * Merges any changes made to the address information.
     * 
     * @param address The address object.
     * @return An address object reference.
     */
    Address update(Address address);

    /**
     * Update the state of the address object to match the
     * database, overwriting any changes.
     * 
     * @param address The address object to refresh.
     */
    void refresh(Address address);

    /**
     * Remove the address.
     * 
     * @param address The address object to remove.
     */
    void remove(Address address);
}
