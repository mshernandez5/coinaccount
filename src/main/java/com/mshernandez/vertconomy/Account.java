package com.mshernandez.vertconomy;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Id;

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
    private String returnAddress;
    private String depositAddress;
    private long balance;

    /**
     * Creates a new account.
     * There should only be one account per player!
     * 
     * @param playerUUID The player UUID to associate with the account.
     * @param returnAddress A wallet refund address, required.
     */
    protected Account(UUID playerUUID, String returnAddress)
    {
        this.playerUUID = playerUUID;
        this.returnAddress = returnAddress;
        balance = 0L;
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
     * Get the account balance.
     * @return
     */
    public long getBalance()
    {
        return balance;
    }
}