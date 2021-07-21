package com.mshernandez.vertconomy.core.entity;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;

import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

@Singleton
@Transactional
public class JPAWithdrawRequestDao implements WithdrawRequestDao
{
    private final Provider<EntityManager> emProvider;

    /**
     * Create a new JPA withdraw request DAO.
     * 
     * @param emProvider An entity manager provider.
     */
    @Inject
    public JPAWithdrawRequestDao(Provider<EntityManager> emProvider)
    {
        this.emProvider = emProvider;
    }

    @Override
    public WithdrawRequest find(String txid)
    {
        EntityManager entityManager = emProvider.get();
        return entityManager.find(WithdrawRequest.class, txid);
    }

    @Override
    public void persist(WithdrawRequest withdrawRequest)
    {
        EntityManager entityManager = emProvider.get();
        entityManager.persist(withdrawRequest);
    }

    @Override
    public WithdrawRequest update(WithdrawRequest withdrawRequest)
    {
        EntityManager entityManager = emProvider.get();
        return entityManager.merge(withdrawRequest);
    }

    @Override
    public void refresh(WithdrawRequest withdrawRequest)
    {
        EntityManager entityManager = emProvider.get();
        entityManager.refresh(withdrawRequest);
    }

    @Override
    public void remove(WithdrawRequest withdrawRequest)
    {
        EntityManager entityManager = emProvider.get();
        entityManager.remove(withdrawRequest);
    }
}
