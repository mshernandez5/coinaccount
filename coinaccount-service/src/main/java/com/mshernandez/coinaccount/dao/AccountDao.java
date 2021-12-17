package com.mshernandez.coinaccount.dao;

import java.util.Collection;
import java.util.UUID;

import com.mshernandez.coinaccount.entity.Account;

public interface AccountDao
{
    /**
     * Gets an account.
     * 
     * @param id The account UUID.
     * @return An account reference or null if no account was found.
     */
    Account find(UUID id);

    /**
     * Gets an account or creates a new one
     * if one does not already exist.
     * 
     * @param id The account UUID.
     * @return An account reference.
     */
    Account findOrCreate(UUID id);

    /**
     * Finds all accounts.
     * 
     * @return A collection of all accounts.
     */
    Collection<Account> findAll();

    /**
     * Merges any changes made to the account.
     * 
     * @param account The account with changes.
     * @return A reference to the updated account.
     */
    Account update(Account account);

    /**
     * Update the state of the account to match the
     * database, overwriting any changes.
     * 
     * @param account The account to refresh.
     */
    void refresh(Account account);

    /**
     * Remove the account.
     * 
     * @param account The account to remove.
     */
    void remove(Account account);
}