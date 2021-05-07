package com.mshernandez.vertconomy.database;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;

/**
 * A class to represent an account capable of
 * interacting with external wallets in addition
 * to in-game funds.
 */
@Entity
public class DepositAccount extends Account
{
    private String depositAddress;
    private String returnAddress;

    private long pendingBalance;

    /**
     * Remembers transactions that have been applied to
     * the account, regardless of whether their balances
     * are still available or not.
     */
    @ElementCollection
    private Set<String> processedDepositIDs;

    /**
     * Creates a new user account.
     * There should only be one account per user.
     * 
     * @param accountUUID The player UUID to associate with the account.
     * @param returnAddress A wallet refund address, required.
     */
    public DepositAccount(UUID accountUUID, String depositAddress)
    {
        super(accountUUID);
        this.depositAddress = depositAddress;
        returnAddress = "";
        processedDepositIDs = new HashSet<>();
    }

    /**
     * Required for Hibernate to instantiate
     * class instances.
     */
    DepositAccount()
    {
        super();
    }

    /**
     * Get an address that can be used to deposit
     * coins to this account.
     * 
     * @return A public wallet address for deposits.
     */
    public String getDepositAddress()
    {
        return depositAddress;
    }

    /**
     * Get an address that can be used to refund
     * account balances in the event of a server
     * shutdown or user ban.
     * 
     * This property is optional and not guaranteed
     * to be set.
     * 
     * @return A wallet refund address.
     */
    public String getReturnAddress()
    {
        return returnAddress;
    }

    /**
     * Set an address that can be used to refund
     * account balances in the event of a server
     * shutdown or user ban.
     * 
     * @param returnAddress The address to refund to.
     */
    public void setReturnAddress(String returnAddress)
    {
        this.returnAddress = returnAddress;
    }

    /**
     * Gets the total balances pending on
     * deposit confirmations for this account.
     * 
     * @return The total pending balance, in sats.
     */
    public long getPendingBalance()
    {
        return pendingBalance;
    }

    /**
     * Update the total pending balance of
     * this account.
     * 
     * @param pendingBalance The total pending balance, in sats.
     */
    public void setPendingBalance(long pendingBalance)
    {
        this.pendingBalance = pendingBalance;
    }

    /**
     * Get a set of transaction IDs corresponding
     * to blockchain deposits to this account which
     * have already been processed.
     * 
     * @return Set of previously processed deposit IDs.
     */
    public Set<String> getProcessedDepositIDs()
    {
        return processedDepositIDs;
    }
}