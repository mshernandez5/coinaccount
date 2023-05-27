package com.mshernandez.coinaccount.dao;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

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
    public List<Deposit> findAllWithdrawable()
    {
        String jpql = "SELECT d FROM Deposit d WHERE d.withdrawLock IS NULL ORDER BY d.amount ASC";
        return entityManager.createQuery(jpql, Deposit.class).getResultList();
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

    @Override
    public long getTotalBalance()
    {
        String jpql = "SELECT COALESCE(SUM(d.amount), 0) FROM Deposit d";
        return ((Number) entityManager.createQuery(jpql).getSingleResult()).longValue();
    }

    @Override
    public long getWithdrawableBalance()
    {
        String jpql = "SELECT COALESCE(SUM(d.amount), 0) FROM Deposit d WHERE d.withdrawLock IS NULL";
        return ((Number) entityManager.createQuery(jpql).getSingleResult()).longValue();
    }
}
