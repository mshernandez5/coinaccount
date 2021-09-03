package com.mshernandez.coinaccount.entity;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Saves details of a deposit UTXO.
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
    @Column(name = "AMOUNT")
    private long amount;

    /**
     * The type of UTXO backing this deposit.
     */
    @Column(name = "TYPE")
    private DepositType type;

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
     * @param type The deposit type: P2PKH, P2WPKH, etc.
     * @param amount The total number of sats received in the output.
     */
    public Deposit(String TXID, int vout, DepositType type, long amount)
    {
        this.TXID = TXID;
        this.vout = vout;
        this.type = type;
        this.amount = amount;
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
     * Get the amount held by this deposit.
     * 
     * @return The amount, in sats, held by this deposit.
     */
    public long getAmount()
    {
        return amount;
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
        return String.format("TXID: %s, vout: %d, Amount: %d", TXID, vout, amount);
    }
}
