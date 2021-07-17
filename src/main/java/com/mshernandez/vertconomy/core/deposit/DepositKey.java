package com.mshernandez.vertconomy.core.deposit;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for <code>Deposit</code> entities,
 * allows the <code>TXID</code> and <code>vout</code> to
 * form the primary key together (as necessary) instead
 * of using only a single attribute as the primary key.
 */
class DepositKey implements Serializable
{
    private String TXID;
    private int vout;

    DepositKey(String TXID, int vout)
    {
        this.TXID = TXID;
        this.vout = vout;
    }

    /**
     * Needed for Hibernate to instantiate the class,
     * not for manual use.
     */
    DepositKey()
    {
        // Required For Hibernate
    }

    public String getTXID()
    {
        return TXID;
    }

    public int getVectorOutIndex()
    {
        return vout;
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
        if (!(obj instanceof DepositKey))
        {
            return false;
        }
        DepositKey other = (DepositKey) obj;
        return TXID.equals(other.TXID) && vout == other.vout;
    }
}
