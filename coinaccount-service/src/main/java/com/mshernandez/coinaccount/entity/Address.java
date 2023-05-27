package com.mshernandez.coinaccount.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.mshernandez.coinaccount.service.wallet_rpc.parameter.DepositType;

/**
 * Represents a deposit address.
 */
@Entity
@Table(name = "ADDRESS")
public class Address
{
    @Id
    @Column(name = "ADDRESS")
    private String address;

    @Column(name = "TYPE")
    private DepositType type;

    @Column(name = "USED")
    private boolean used;

    @ManyToOne
    @JoinColumn(name = "OWNER", referencedColumnName = "ID")
    private Account owner;

    /**
     * Create a new unused address.
     * 
     * @param address The address string.
     * @param type The address type.
     * @param owner The address owner.
     */
    public Address(String address, DepositType type, Account owner)
    {
        this.address = address;
        this.type = type;
        this.owner = owner;
        used = false;
    }

    /**
     * Required for Hibernate to instantiate
     * class instances.
     */
    Address()
    {
        // Required For Hibernate
    }

    /**
     * Returns the address string.
     * 
     * @return The address string.
     */
    public String getAddress()
    {
        return address;
    }

    /**
     * Get the deposit type this address accepts.
     * 
     * @return The corresponding deposit type.
     */
    public DepositType getType()
    {
        return type;
    }

    /**
     * Mark the address as used.
     */
    public void setUsed()
    {
        used = true;
    }

    /**
     * Whether this address has been used.
     * 
     * @return True if this address has already been used.
     */
    public boolean isUsed()
    {
        return used;
    }

    @Override
    public String toString()
    {
        return address;
    }

    @Override
    public int hashCode()
    {
        return address.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof Address))
        {
            return false;
        }
        return address.equals(((Address) obj).address);
    }
}
