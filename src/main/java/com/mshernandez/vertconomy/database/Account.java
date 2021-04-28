package com.mshernandez.vertconomy.database;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Entity;
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

    @ManyToMany
    private Set<BlockchainTransaction> transactions;
    private BlockchainTransaction lastDeposit;

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
        lastDeposit = null;
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
    public void associateTransaction(BlockchainTransaction transaction, boolean isDeposit)
    {
        if (transaction.getDistribution(this) == 0L)
        {
            return;
        }
        else
        {
            transactions.add(transaction);
        }
        if (isDeposit)
        {
            lastDeposit = transaction;
        }
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
     * Get the last deposit transaction,
     * or null if none exists.
     * 
     * @return The last deposit transaction, or null.
     */
    public BlockchainTransaction getLastDeposit()
    {
        return lastDeposit;
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