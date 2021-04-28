package com.mshernandez.vertconomy.database;

import java.util.HashSet;
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
}