package com.mshernandez.vertconomy.core.account;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

import com.mshernandez.vertconomy.core.deposit.Deposit;
/**
 * A class to represent a general account,
 * compatible with Hibernate for persistence in a
 * relational database.
 */
@Entity
@Table(name = "ACCOUNT")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Account
{
    @Id
    @Column(name = "ID")
    private UUID id;

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
    public Account(UUID id)
    {
        this.id = id;
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
        return id;
    }

    /**
     * Associate a deposit with this account.
     * 
     * @param deposit The deposit to associate with this account.
     */
    public void associateDeposit(Deposit deposit)
    {
        balances.add(deposit);
    }

    /**
     * Remove a deposit from this account.
     * 
     * @param deposit The deposit to remove from this account.
     */
    public void removeDeposit(Deposit deposit)
    {
        balances.remove(deposit);
    }

    /**
     * Returns a reference to the set of deposits
     * this account is associated with.
     * 
     * @return The deposits this account is associated with.
     */
    public Set<Deposit> getDeposits()
    {
        return balances;
    }

    /**
     * Calculate the usable account balance.
     * Usable balances can be transferred
     * between accounts but in some situations
     * may not be withdrawn for a period of time.
     * 
     * @return The total balance of this account.
     */
    public long calculateBalance()
    {
        long balance = 0L;
        for (Deposit deposit : balances)
        {
            balance += deposit.getShare(this);
        }
        return balance;
    }

    /**
     * Calculate the withdrawable account balance.
     * 
     * @return The total balance of this account.
     */
    public long calculateWithdrawableBalance()
    {
        long balance = 0L;
        for (Deposit deposit : balances)
        {
            if (!deposit.hasWithdrawLock())
            {
                balance += deposit.getShare(this);
            }
        }
        return balance;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof Account))
        {
            return false;
        }
        return id.equals(((Account) other).id);
    }

    @Override
    public String toString()
    {
        return String.format("Account: %s, Balance: %d", id.toString(), calculateBalance());
    }
}