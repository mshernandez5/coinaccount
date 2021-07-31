package com.mshernandez.coinaccount.dao;

import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import com.mshernandez.coinaccount.entity.WithdrawRequest;

@ApplicationScoped
@Transactional
public class JPAWithdrawRequestDao implements WithdrawRequestDao
{
    @Inject
    EntityManager entityManager;

    @Override
    public WithdrawRequest find(String txid)
    {
        return entityManager.find(WithdrawRequest.class, txid);
    }

    @Override
    public Collection<WithdrawRequest> findAll()
    {
        return entityManager.createQuery("SELECT w FROM WithdrawRequest w", WithdrawRequest.class)
            .getResultList();
    }

    @Override
    public Collection<WithdrawRequest> findAllIncomplete()
    {
        return entityManager.createQuery("SELECT w FROM WithdrawRequest w WHERE w.complete = FALSE", WithdrawRequest.class)
            .getResultList();
    }

    @Override
    public void persist(WithdrawRequest withdrawRequest)
    {
        entityManager.persist(withdrawRequest);
    }

    @Override
    public WithdrawRequest update(WithdrawRequest withdrawRequest)
    {
        return entityManager.merge(withdrawRequest);
    }

    @Override
    public void refresh(WithdrawRequest withdrawRequest)
    {
        entityManager.refresh(withdrawRequest);
    }

    @Override
    public void remove(WithdrawRequest withdrawRequest)
    {
        entityManager.remove(withdrawRequest);
    }
}
