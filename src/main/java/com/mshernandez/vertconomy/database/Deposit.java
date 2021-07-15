package com.mshernandez.vertconomy.database;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;

/**
 * Saves details of a deposit transaction
 * and distributes ownership of the received
 * coins among server players.
 * <p>
 * Basically represents a UTXO that can be split
 * up among many players.
 * <p>
 * For example, if player #1 deposits 1000 sats
 * and gives 250 to player #2 in game, then the
 * deposit holding the 1000 sats will allocate
 * 750 sats to player #1 and 250 sats to player #2.
 */
@Entity
@IdClass(DepositKey.class)
public class Deposit
{
    @Id
    private String TXID;
    @Id
    private int vout;
    
    private long total;

    /**
     * Distributes the balance held by this transaction
     * across multiple accounts.
     * 
     * Eagerly fetched since this will always be accessed
     * when looking up a transaction.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    private Map<Account, Long> distribution;

    /**
     * Identifies any lock on the deposit for
     * pending withdrawal, null if no lock exists.
     * <p>
     * Locks prevent multiple users from trying to
     * spend the same UTXO when withdrawing.
     */
    @ManyToOne
    private WithdrawRequest withdrawLock;

    /**
     * Create a new deposit initially owned solely
     * by a single owner.
     * 
     * @param TXID The blockchain transaction ID.
     * @param vout The transaction output index corresponding to this UTXO.
     * @param total The total number of sats received in the transaction.
     * @param owner The sole owner of this deposit.
     */
    public Deposit(String TXID, int vout, long total, Account owner)
    {
        this.TXID = TXID;
        this.vout = vout;
        this.total = total;
        this.distribution = new HashMap<>();
        withdrawLock = null;
        distribution.put(owner, total);
    }

    /**
     * Save transaction details for player deposits.
     * 
     * @param TXID The blockchain transaction ID.
     * @param total The total number of sats received in the transaction.
     * @param ownership How the received sats should be distributed across accounts.
     */
    public Deposit(String TXID, int vout, long total, Map<Account, Long> ownership)
    {
        this.TXID = TXID;
        this.vout = vout;
        this.total = total;
        withdrawLock = null;
        this.distribution = new HashMap<>(ownership);
    }

    /**
     * Needed for Hibernate to instantiate the class,
     * not for manual use.
     */
    Deposit()
    {
        // Required For Hibernate
    }

    /**
     * Get the unique ID for this transaction.
     * 
     * @return A blockchain transaction ID.
     */
    public String getTXID()
    {
        return TXID;
    }

    /**
     * Get the vout corresponding to this deposit.
     * 
     * @return The vout index.
     */
    public int getVout()
    {
        return vout;
    }

    /**
     * Get the total sats that the server received
     * as part of this deposit, regardless of ownership.
     * 
     * @return The total number of sats in this deposit.
     */
    public long getTotal()
    {
        return total;
    }

    /**
     * Get the set of accounts owning a share
     * of this deposit.
     * 
     * @return The set of accounts with a share in this deposit.
     */
    public Set<Account> getOwners()
    {
        return distribution.keySet();
    }

    /**
     * Returns the number of sats in this deposit
     * allocated to the given account.
     * 
     * @param account The account to lookup.
     * @return The sats belonging to this account.
     */
    public long getDistribution(Account account)
    {
        return distribution.getOrDefault(account, 0L);
    }

    /**
     * Sets the number of sats in this deposit
     * allocated to the given account.
     * <p>
     * If an amount <= 0 is provided then the
     * account will be removed from this deposit's records.
     * 
     * @param account The account associated with the new amount.
     * @param amount The sats belonging to this account.
     */
    public void setDistribution(Account account, long amount)
    {
        if (amount > 0L)
        {
            distribution.put(account, amount);
        }
        else
        {
            distribution.remove(account);
        }
    }

    /**
     * Set a withdraw lock on this transaction by
     * the given withdraw request.
     * 
     * @param withdrawRequest The withdraw request locking this deposit.
     */
    public void setWithdrawLock(WithdrawRequest withdrawRequest)
    {
        this.withdrawLock = withdrawRequest;
    }

    /**
     * Determines whether this UTXO has a
     * withdraw lock or not.
     * <p>
     * Locks prevent multiple users from trying to
     * spend the same UTXO when withdrawing.
     * 
     * @return True if this deposit has an active withdraw lock.
     */
    public boolean hasWithdrawLock()
    {
        return withdrawLock != null;
    }

    /**
     * Return any withdraw lock on this deposit,
     * or null if there is none.
     * 
     * @return The active withdraw lock, or null if none exists.
     */
    public WithdrawRequest getWithdrawLock()
    {
        return withdrawLock;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(TXID, vout);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof Deposit))
        {
            return false;
        }
        Deposit other = (Deposit) obj;
        return TXID.equals(other.TXID) && vout == other.vout;
    }

    @Override
    public String toString()
    {
        return String.format("TXID: %s, vout: %d, Total Sats: %d", TXID, vout, total);
    }
}
