package com.mshernandez.vertconomy.database;

import java.util.UUID;

/**
 * A repository to store and retrieve accounts.
 * 
 * @param <T> The specific type of account.
 */
public interface AccountRepository<T extends Account>
{
    /**
     * Get an account from the repository.
     * 
     * @param accountUUID The account UUID.
     * @return An account, or null if none was found.
     */
    T getAccount(UUID accountUUID);

    /**
     * Save an account to the repository,
     * updating existing accounts.
     * 
     * @param account The account to save.
     */
    T save(T account);

    /**
     * Delete an account from the repository.
     * 
     * @param account The account to delete.
     */
    void delete(T account);
}