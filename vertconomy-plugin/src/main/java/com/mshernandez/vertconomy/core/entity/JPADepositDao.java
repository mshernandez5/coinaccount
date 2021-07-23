package com.mshernandez.vertconomy.core.entity;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;

import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

@Singleton
@Transactional
public class JPADepositDao implements DepositDao
{
    private final Provider<EntityManager> emProvider;

    /**
     * Creates a new JPA deposit DAO.
     * 
     * @param emProvider An entity manager provider.
     */
    @Inject
    public JPADepositDao(Provider<EntityManager> emProvider)
    {
        this.emProvider = emProvider;
    }

    @Override
    public Deposit find(String txid, int vout)
    {
        EntityManager entityManager = emProvider.get();
        DepositKey key = new DepositKey(txid, vout);
        return entityManager.find(Deposit.class, key);
    }

    @Override
    public void persist(Deposit deposit)
    {
        EntityManager entityManager = emProvider.get();
        entityManager.persist(deposit);
    }

    @Override
    public Deposit update(Deposit deposit)
    {
        EntityManager entityManager = emProvider.get();
        return entityManager.merge(deposit);
    }

    @Override
    public void refresh(Deposit deposit)
    {
        EntityManager entityManager = emProvider.get();
        entityManager.refresh(deposit);
    }

    @Override
    public void remove(Deposit deposit)
    {
        EntityManager entityManager = emProvider.get();
        entityManager.remove(deposit);
    }
}
