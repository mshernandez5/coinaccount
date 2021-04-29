package com.mshernandez.vertconomy.database;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

/**
 * A class to represent a player's account,
 * compatible with JPA for persistence in a
 * relational database.
 */
@Entity
public class Account
{
    @Id
    private UUID playerUUID;
    private String depositAddress;
    private String returnAddress;

    /**
     * The set of transactions actively contributing
     * to the account balance.
     * 
     * Transactions will always be used when fetching an
     * account so they are automatically fetched when
     * fetching an account.
     */
    @ManyToMany(fetch = FetchType.EAGER)
    private Set<BlockchainTransaction> transactions;

    /**
     * Remembers transactions that have been applied to
     * the account, regardless of whether their balances
     * are still available or not.
     * 
     * Lazy fetching, only used to check for new deposits.
     */
    @ElementCollection
    private Set<String> processedTransactionIDs;

    /**
     * Creates a new account.
     * There should only be one account per player!
     * 
     * @param playerUUID The player UUID to associate with the account.
     * @param returnAddress A wallet refund address, required.
     */
    public Account(UUID playerUUID, String depositAddress)
    {
        this.playerUUID = playerUUID;
        this.depositAddress = depositAddress;
        returnAddress = "";
        transactions = new HashSet<>();
        processedTransactionIDs = new HashSet<>();
    }

    public Account()
    {
        // Required For Hibernate
    }

    /**
     * The Minecraft player UUID associated
     * with this account.
     * 
     * @return Player UUID associated with this account.
     */
    public UUID getPlayerUUID()
    {
        return playerUUID;
    }

    /**
     * Associate a transaction with this account,
     * assuming the player owns part of the transaction.
     * 
     * @param transaction The transaction to associate.
     * @param isDeposit True if the transaction was a direct deposit by the player.
     */
    public void associateTransaction(BlockchainTransaction transaction)
    {
        if (transaction.getDistribution(this) == 0L)
        {
            return;
        }
        else
        {
            transactions.add(transaction);
        }
        processedTransactionIDs.add(transaction.getTXID());
    }

    /**
     * Dissociate a transaction from the account,
     * used when the transaction has been used by
     * this player or another with a stake in its funds.
     * 
     * If the player still had a valid portion of the
     * transaction after it was used, these funds will
     * be reassociated with the account through a new
     * change transaction.
     * 
     * @param transaction The transaction to detatch from the account.
     */
    public void detatchTransaction(BlockchainTransaction transaction)
    {
        transactions.remove(transaction);
    }

    /**
     * A set of transaction IDs that have been applied to
     * the account, regardless of whether their balances
     * are still available or not.
     * 
     * @return A set of transaction IDs that have been applied to the account.
     */
    public Set<String> getProcessedTransactionIDs()
    {
        return processedTransactionIDs;
    }

    /**
     * Set a wallet address where the user's balance
     * may be refunded to in the event of a server
     * shutdown, user ban, or other situation.
     * 
     * @param returnAddress A wallet refund address to associate with this account.
     */
    public void setReturnAddress(String returnAddress)
    {
        this.returnAddress = returnAddress;
    }

    /**
     * A wallet address where the user's balance
     * may be refunded to in the event of a server
     * shutdown, user ban, or other situation.
     * 
     * @return A wallet refund address associated with this account.
     */
    public String getReturnAddress()
    {
        return returnAddress;
    }

    /**
     * A wallet address designated for receiving
     * deposits from this user.
     * 
     * @return A deposit address associated with this account.
     */
    public String getDepositAddress()
    {
        return depositAddress;
    }

    /**
     * Calculate the account balance.
     * 
     * @return The total balance of this account.
     */
    public long calculateBalance()
    {
        long balance = 0L;
        for (BlockchainTransaction t : transactions)
        {
            balance += t.getDistribution(this);
        }
        return balance;
    }

    /**
     * Returns a reference to the set of transactions
     * this account is associated with.
     * 
     * @return The transactions this account is associated with.
     */
    public Set<BlockchainTransaction> getTransactions()
    {
        return transactions;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(playerUUID);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof Account))
        {
            return false;
        }
        return playerUUID.equals(((Account) other).playerUUID);
    }
}