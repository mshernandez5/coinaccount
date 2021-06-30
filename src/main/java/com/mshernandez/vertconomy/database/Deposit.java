package com.mshernandez.vertconomy.database;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;

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
     * Save transaction details for player deposits.
     * 
     * @param TXID The blockchain transaction ID.
     * @param total The total number of sats received in the transaction.
     * @param ownership How the received sats should be distributed across accounts.
     */
    public Deposit(String TXID, int vectorOutIndex, long total, boolean spendable, Map<Account, Long> ownership)
    {
        this.TXID = TXID;
        this.vout = vectorOutIndex;
        this.total = total;
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
     * @return A vout index.
     */
    public int getVectorOutIndex()
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
     * Get a mapping from Account to the
     * number of sats they own from this transaction.
     * 
     * @return How many sats each player owns.
     */
    public Map<Account, Long> getOwnershipDistribution()
    {
        return distribution;
    }

    /**
     * Returns the number of sats in this transaction
     * allocated to the given account.
     * 
     * @param account The account to lookup.
     * @return The sats belonging to this account.
     */
    public long getDistribution(Account account)
    {
        return distribution.getOrDefault(account, 0L);
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
