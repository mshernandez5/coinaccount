package com.mshernandez.vertconomy.core.withdraw;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.mshernandez.vertconomy.core.account.DepositAccount;
import com.mshernandez.vertconomy.core.deposit.Deposit;

/**
 * Stores information about a pending withdraw
 * initiated by a user but still waiting for
 * confirmation before execution.
 */
@Entity
@Table(name = "WITHDRAW_REQUEST")
public class WithdrawRequest
{
    @Id
    @Column(name = "TXID")
    private String txid;

    @OneToOne(mappedBy = "withdrawRequest")
    private DepositAccount account;

    @OneToMany(mappedBy = "withdrawLock")
    private Set<Deposit> inputs;

    @Column(name = "WITHDRAW_AMOUNT")
    private long withdrawAmount;

    @Column(name = "FEE_AMOUNT")
    private long feeAmount;

    @Lob
    @Column(name = "TX_HEX")
    private String txHex;

    @Column(name = "TIMESTAMP")
    private long timestamp;

    public WithdrawRequest(String txid, DepositAccount account, Set<Deposit> inputs,
                           long withdrawAmount, long fees, String txHex, long timestamp)
    {
        this.txid = txid;
        this.account = account;
        this.inputs = new HashSet<>(inputs);
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
        return new HashSet<>(inputs);
    }

    /**
     * Don't save the provided input, use
     * only if the input provides no value
     * left after withdrawal.
     * 
     * @param input The input to forget.
     */
    public void forgetInput(Deposit input)
    {
        inputs.remove(input);
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