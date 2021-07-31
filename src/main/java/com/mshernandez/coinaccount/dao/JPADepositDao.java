package com.mshernandez.coinaccount.dao;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import com.mshernandez.coinaccount.entity.Deposit;
import com.mshernandez.coinaccount.entity.DepositKey;

@ApplicationScoped
@Transactional
public class JPADepositDao implements DepositDao
{
    @Inject
    EntityManager entityManager;

    @Override
    public Deposit find(String txid, int vout)
    {
        DepositKey key = new DepositKey(txid, vout);
        return entityManager.find(Deposit.class, key);
    }

    @Override
    public void persist(Deposit deposit)
    {
        entityManager.persist(deposit);
    }

    @Override
    public Deposit update(Deposit deposit)
    {
        return entityManager.merge(deposit);
    }

    @Override
    public void refresh(Deposit deposit)
    {
        entityManager.refresh(deposit);
    }

    @Override
    public void remove(Deposit deposit)
    {
        entityManager.remove(deposit);
    }
}
