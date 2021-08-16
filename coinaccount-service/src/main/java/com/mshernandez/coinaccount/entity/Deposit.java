package com.mshernandez.coinaccount.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Saves details of a deposit transaction
 * and distributes ownership of the received
 * coins among accounts.
 * <p>
 * Basically represents a UTXO that can be split
 * up among many accounts.
 * <p>
 * For example, if account #1 deposits 1000 sats
 * and transfers 250 to account #2, then the
 * deposit holding the 1000 sats will allocate
 * 750 sats to account #1 and 250 sats to account #2.
 */
@Entity
@IdClass(DepositKey.class)
@Table(name = "DEPOSIT")
public class Deposit
{
    @Id
    @Column(name = "TXID")
    private String TXID;
    
    @Id
    @Column(name = "VOUT")
    private int vout;
    
    /**
     * The total value of this deposit
     * ignoring share distribution.
     */
    @Column(name = "TOTAL")
    private long total;

    /**
     * The type of UTXO backing this deposit.
     */
    @Column(name = "TYPE")
    private DepositType type;

    /**
     * All shares of this deposit.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable
    (
        name = "SHARES",
        joinColumns =
        {
            @JoinColumn(name = "TXID"),
            @JoinColumn(name = "VOUT")
        }
    )
    @MapKeyJoinColumn(name = "OWNER", referencedColumnName = "ID")
    @Column(name = "AMOUNT")
    private Map<Account, Long> shares;

    /**
     * Identifies any lock on the deposit for
     * pending withdrawal, null if no lock exists.
     * <p>
     * Locks prevent multiple users from trying to
     * spend the same UTXO when withdrawing.
     */
    @ManyToOne
    @JoinColumn(name = "WITHDRAW_LOCK", referencedColumnName = "TXID")
    private WithdrawRequest withdrawLock;

    /**
     * Used for optimistic locking to prevent concurrent
     * modification of the same deposit.
     */
    @Version
    @Column(name = "VERSION")
    private long version;

    /**
     * Create a new deposit.
     * 
     * @param TXID The deposit transaction ID.
     * @param vout The vout index for this output.
     * @param size The vsize that this UTXO would take to use as an input.
     * @param total The total number of sats received in the output.
     */
    public Deposit(String TXID, int vout, DepositType type, long total)
    {
        this.TXID = TXID;
        this.vout = vout;
        this.type = type;
        this.total = total;
        this.shares = new HashMap<>();
        withdrawLock = null;
        version = 0L;
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
     * Get the TXID of the transaction which
     * created this deposit.
     * 
     * @return The deposit transaction ID.
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
     * Get the type of UTXO backing this deposit.
     * 
     * @return The type of UTXO backing this deposit.
     */
    public DepositType getType()
    {
        return type;
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
        return shares.keySet();
    }

    /**
     * Return the share an account has over this deposit.
     * 
     * @param account The account owning a share.
     * @return This account's share in the deposit.
     */
    public long getShare(Account account)
    {
        return shares.getOrDefault(account, 0L);
    }

    /**
     * Set the share an account has over this deposit.
     * <p>
     * The account will be removed from this deposit
     * if the share is <= 0.
     * 
     * @param deposit The account to associate with the share.
     * @param amount How much of this deposit is owned by the account.
     */
    public void setShare(Account account, long amount)
    {
        if (amount > 0L)
        {
            shares.put(account, amount);
            account.associateDeposit(this);
        }
        else
        {
            shares.remove(account);
            account.removeDeposit(this);
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
