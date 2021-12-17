package com.mshernandez.coinaccount.entity;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * A class to represent an account,
 * compatible with Hibernate for persistence in a
 * relational database.
 */
@Entity
@Table(name = "ACCOUNT")
public class Account
{
    @Id
    @Column(name = "ID", length = 16)
    private UUID id;

    /**
     * Balances backed by UTXO
     * shares held as part of a common pool.
     */
    @Column(name = "BALANCE")
    private long balance;

    /**
     * Stores the last known pending balance from
     * deposits the user has made but have not yet
     * met the minimum number of confirmations.
     */
    @Column(name = "PENDING_BALANCE")
    private long pendingBalance;

    /**
     * All wallet addresses created to receive
     * user deposits for this account.
     */
    @OneToMany(mappedBy = "owner")
    private Set<Address> addresses;

    /**
     * An address that can be used to refund
     * account balances in the event of a server
     * shutdown or user ban.
     */
    @Column(name = "RETURN_ADDRESS")
    private String returnAddress;

    /**
     * Save any active withdraw request the user has
     * created but not yet confirmed.
     */
    @OneToOne
    @JoinColumn(name = "WITHDRAW_REQUEST", referencedColumnName = "TXID")
    private WithdrawRequest withdrawRequest;

    /**
     * Used for optimistic locking to prevent concurrent
     * modification of the same account.
     */
    @Version
    @Column(name = "VERSION")
    private long version;

    /**
     * Creates a new account.
     * <p>
     * There should only be one account per user.
     * 
     * @param id The UUID to associate with the account.
     * @param depositAddress A wallet deposit address for this account.
     */
    public Account(UUID id)
    {
        this.id = id;
        balance = 0L;
        pendingBalance = 0L;
        addresses = new HashSet<>();
        version = 0L;
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
     * Returns the transferable account balance.
     * <p>
     * These balances may always be transferred
     * but portions may be temporarily
     * unavailable for withdrawal.
     * 
     * @return The transferable balance of this account.
     */
    public long getBalance()
    {
        return balance;
    }

    /**
     * Updates the account balance.
     * 
     * @param pooledBalance The updated account balance.
     */
    public void setBalance(long balance)
    {
        this.balance = balance;
    }

    /**
     * Updates the account balance.
     * 
     * @param delta The change to be applied.
     */
    public void changeBalance(long delta)
    {
        balance += delta;
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
     * Associate a newly created deposit
     * address with this account.
     * 
     * @param address The address object.
     * 
     * @return True if the address was added, or false if it is a duplicate.
     */
    public boolean addNewAddress(Address address)
    {
        return addresses.add(address);
    }

    /**
     * Return a set of every address associated
     * with this account.
     * 
     * @return Addresses associated with this account.
     */
    public Set<Address> getAddresses()
    {
        return new HashSet<>(addresses);
    }

    /**
     * Get an address that can be used to refund
     * account balances in the event of a server
     * shutdown or user ban.
     * <p>
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
     * Get the active withdraw request for this account,
     * or null if there is no active withdraw request.
     * 
     * @return The active withdraw request, or null if none exists.
     */
    public WithdrawRequest getWithdrawRequest()
    {
        return withdrawRequest;
    }

    /**
     * Sets an active withdraw request for this account.
     * 
     * @param withdrawRequest The withdraw request to associate with this account.
     */
    public void setWithdrawRequest(WithdrawRequest withdrawRequest)
    {
        this.withdrawRequest = withdrawRequest;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof Account))
        {
            return false;
        }
        return id.equals(((Account) obj).id);
    }

    @Override
    public String toString()
    {
        return String.format("Account: %s, Balance: %d", id.toString(), getBalance());
    }
}