package com.mshernandez.coinaccount.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.MapKeyJoinColumns;
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
     * The set of deposit shares actively
     * contributing to the account balance.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "SHARES")
    @MapKeyJoinColumns({@MapKeyJoinColumn(name = "TXID"), @MapKeyJoinColumn(name = "VOUT")})
    @Column(name = "AMOUNT")
    private Map<Deposit, Long> shares;

    /**
     * The current balance of this account.
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
     * A wallet address assigned to this account
     * to receive user deposits.
     */
    @Column(name = "DEPOSIT_ADDRESS")
    private String depositAddress;

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
     * Remembers transactions that have been applied to
     * the account, regardless of whether their balances
     * are still available to this account or not.
     */
    @ElementCollection
    @CollectionTable
    (
        name = "PROCESSED_DEPOSITS",
        joinColumns = @JoinColumn(name = "DEPOSITOR", referencedColumnName = "ID")
    )
    @Column(name = "TXID")
    private Set<String> processedDepositIDs;

    /**
     * Used for optimistic locking to prevent concurrent
     * modification of the same account.
     */
    @Version
    @Column(name = "VERSION")
    private long version;

    /**
     * Creates a new account.
     * There should only be one account per user.
     * 
     * @param id The UUID to associate with the account.
     * @param depositAddress A wallet deposit address for this account.
     */
    public Account(UUID id, String depositAddress)
    {
        this.id = id;
        shares = new HashMap<>();
        balance = 0L;
        pendingBalance = 0L;
        this.depositAddress = depositAddress;
        returnAddress = "";
        processedDepositIDs = new HashSet<>();
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
     * Return the share this account has over a deposit.
     * 
     * @param deposit The deposit.
     * @return This account's share in the deposit.
     */
    public long getShare(Deposit deposit)
    {
        return shares.getOrDefault(deposit, 0L);
    }

    /**
     * Set the share this account has over a deposit.
     * 
     * @param deposit The deposit to associate with the share.
     * @param amount How much of the deposit is owned by this account.
     */
    public void setShare(Deposit deposit, long amount)
    {
        long previousShare = shares.getOrDefault(deposit, 0L);
        if (amount > 0L)
        {
            shares.put(deposit, amount);
        }
        else
        {
            shares.remove(deposit);
        }
        long updatedShare = shares.getOrDefault(deposit, 0L);
        balance += updatedShare - previousShare;
    }

    /**
     * Returns a reference to the set of deposits
     * this account is associated with.
     * 
     * @return The deposits this account is associated with.
     */
    public Set<Deposit> getDeposits()
    {
        return shares.keySet();
    }

    /**
     * Calculate the usable account balance.
     * Usable balances can be transferred
     * between accounts but in some situations
     * may not be withdrawn for a period of time.
     * 
     * @return The total balance of this account.
     */
    public long getBalance()
    {
        return balance;
    }

    /**
     * Calculate the withdrawable account balance.
     * 
     * @return The withdrawable balance of this account.
     */
    public long calculateWithdrawableBalance()
    {
        long withdrawableBalance = balance;
        for (Deposit deposit : shares.keySet())
        {
            if (deposit.hasWithdrawLock())
            {
                withdrawableBalance -= shares.get(deposit);
            }
        }
        return withdrawableBalance;
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

    /**
     * Get a set of transaction IDs corresponding
     * to account deposits which
     * have already been processed.
     * 
     * @return Set of previously processed deposit IDs.
     */
    public Set<String> getProcessedDepositIDs()
    {
        return new HashSet<>(processedDepositIDs);
    }

    /**
     * Define the set of transaction IDs which should
     * be remembered as already processed.
     * 
     * @param newIds Set of previously processed deposit IDs.
     */
    public void setProcessedDepositIDs(Set<String> newIds)
    {
        processedDepositIDs = new HashSet<>(newIds);
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
        return String.format("Account: %s, Balance: %d", id.toString(), balance);
    }
}