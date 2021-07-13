package com.mshernandez.vertconomy.database;

import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

/**
 * Stores information about a pending withdraw
 * initiated by a user but still waiting for
 * confirmation before execution.
 */
@Entity
public class WithdrawRequest
{
    @Id
    private String txid;

    @OneToOne
    private DepositAccount account;

    @OneToMany(fetch = FetchType.EAGER)
    private Set<Deposit> inputs;

    private long withdrawAmount;
    private long feeAmount;

    @Lob
    private String txHex;

    private long timestamp;

    public WithdrawRequest(String txid, DepositAccount account, Set<Deposit> inputs,
                           long withdrawAmount, long fees, String txHex, long timestamp)
    {
        this.txid = txid;
        this.account = account;
        this.inputs = inputs;
        this.withdrawAmount = withdrawAmount;
        this.feeAmount = fees;
        this.txHex = txHex;
        this.timestamp = timestamp;
    }

    /**
     * Needed for Hibernate to instantiate the class,
     * not for manual use.
     */
    WithdrawRequest()
    {
        // Required for Hibernate
    }

    /**
     * Get the TXID of the unsent withdraw transaction.
     * 
     * @return The TXID of the unsent withdraw transaction.
     */
    public String getTxid()
    {
        return txid;
    }

    /**
     * Get the account associated with this withdraw
     * request.
     * 
     * @return The account associated with the withdrawal request.
     */
    public DepositAccount getAccount()
    {
        return account;
    }

    /**
     * Get all the deposits that would be used
     * to fund the withdrawal.
     * 
     * @return The deposits funding the withdraw.
     */
    public Set<Deposit> getInputs()
    {
        return inputs;
    }

    /**
     * Get the total withdrawal cost, including TX fee.
     * 
     * @return The total withdrawal cost, including TX fee.
     */
    public long getTotalCost()
    {
        return withdrawAmount + feeAmount;
    }

    /**
     * Get the amount of cost contributed by the TX fee.
     * 
     * @return The TX fee to be payed.
     */
    public long getFeeAmount()
    {
        return feeAmount;
    }

    /**
     * Get the amount to be received by the user.
     * 
     * @return The actual amount to be received.
     */
    public long getWithdrawAmount()
    {
        return withdrawAmount;
    }

    /**
     * Get the unsigned hex-encoded transaction
     * waiting to be signed and sent to the network.
     * 
     * @return The unsigned hex-encoded transaction.
     */
    public String getTxHex()
    {
        return txHex;
    }

    /**
     * Get the system time (millis) when this withdraw
     * was initiated.
     * 
     * @return A timestamp of when this withdraw was initiated.
     */
    public long getTimestamp()
    {
        return timestamp;
    }
}