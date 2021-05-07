package com.mshernandez.vertconomy.database;

import java.io.Serializable;
import java.util.Objects;

class DepositKey implements Serializable
{
    private String TXID;
    private int vectorOutIndex;

    DepositKey(String TXID, int vout)
    {
        this.TXID = TXID;
        this.vectorOutIndex = vout;
    }

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
        return vectorOutIndex;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(TXID, vectorOutIndex);
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
        return TXID.equals(other.TXID) && vectorOutIndex == other.vectorOutIndex;
    }
}
