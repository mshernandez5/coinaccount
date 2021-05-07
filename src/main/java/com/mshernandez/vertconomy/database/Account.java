package com.mshernandez.vertconomy.database;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToMany;

/**
 * A class to represent a general account,
 * compatible with Hibernate for persistence in a
 * relational database.
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Account
{
    @Id
    private UUID accountUUID;

    /**
     * The set of deposits actively contributing
     * to the account balance.
     * 
     * These will always be used when fetching an
     * account so they are automatically fetched.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Deposit> balances;

    /**
     * Creates a new account capable of holding
     * in-game funds but cannot deposit from or
     * withdraw to an external wallet.
     * 
     * @param playerUUID The player UUID to associate with the account.
     * @param returnAddress A wallet refund address, required.
     */
    public Account(UUID accountUUID)
    {
        this.accountUUID = accountUUID;
        balances = new HashSet<>();
    }

    /**
     * Required for Hibernate to instantiate
     * class instances.
     */
    Account()
    {
        // Required For Hibernate
    }

    /**
     * The UUID associated with this account.
     * 
     * @return The UUID associated with this account.
     */
    public UUID getAccountUUID()
    {
        return accountUUID;
    }

    /**
     * Returns a reference to the set of transactions
     * this account is associated with.
     * 
     * @return The transactions this account is associated with.
     */
    public Set<Deposit> getTransactions()
    {
        return balances;
    }

    /**
     * Calculate the account balance.
     * 
     * @return The total balance of this account.
     */
    public long calculateBalance()
    {
        long balance = 0L;
        for (Deposit t : balances)
        {
            balance += t.getDistribution(this);
        }
        return balance;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(accountUUID);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof Account))
        {
            return false;
        }
        return accountUUID.equals(((Account) other).accountUUID);
    }

    @Override
    public String toString()
    {
        return String.format("Account: %s, Balance: %d", accountUUID.toString(), calculateBalance());
    }
}