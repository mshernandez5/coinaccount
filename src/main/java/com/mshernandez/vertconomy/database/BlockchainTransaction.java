package com.mshernandez.vertconomy.database;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MapKeyJoinColumn;

/**
 * Saves details of a deposit transaction
 * and distributes ownership of the received
 * coins among server players.
 * 
 * For example, if player #1 deposits 1000 sats
 * and gives 250 to player #2 in game, then the
 * transaction holding the 1000 sats will allocate
 * 750 sats to player #1 and 250 sats to player #2. 
 */
@Entity
public class BlockchainTransaction
{
    @Id
    private String TXID;
    private long total;

    @ElementCollection
    @MapKeyJoinColumn(name = "ACCOUNT")
    private Map<Account, Long> distribution;

    /**
     * Save transaction details for player deposits.
     * 
     * @param TXID The blockchain transaction ID.
     * @param total The total number of sats received in the transaction.
     * @param ownership How the received sats should be distributed across accounts.
     */
    public BlockchainTransaction(String TXID, long total, Map<Account, Long> ownership)
    {
        this.TXID = TXID;
        this.total = total;
        this.distribution = new HashMap<>(ownership);
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
     * Get the total sats that the server received
     * as part of this transaction.
     * 
     * @return
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
    public Map<Account, Long> getDistribution()
    {
        return new HashMap<>(distribution);
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
}
