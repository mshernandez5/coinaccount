package com.mshernandez.coinaccount.dao;

import java.util.Collection;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import com.mshernandez.coinaccount.entity.Account;

import org.jboss.logging.Logger;

@ApplicationScoped
@Transactional
public class JPAAccountDao implements AccountDao
{
    @Inject
    Logger logger;

    @Inject
    EntityManager entityManager;

    @Override
    public Account find(UUID accountUUID)
    {
        return entityManager.find(Account.class, accountUUID);
    }

    @Override
    public Account findOrCreate(UUID accountUUID)
    {
        Account account = entityManager.find(Account.class, accountUUID);
        if (account == null)
        {
            logger.info("Creating New Account For User: " + accountUUID);
            account = new Account(accountUUID);
            entityManager.persist(account);
        }
        return account;
    }

    @Override
    public Collection<Account> findAll()
    {
        return entityManager.createQuery("SELECT a FROM Account a", Account.class)
            .getResultList();
    }

    @Override
    public Account update(Account account)
    {
        return entityManager.merge(account);
    }

    @Override
    public void refresh(Account account)
    {
        entityManager.refresh(account);
    }

    @Override
    public void remove(Account account)
    {
        entityManager.remove(entityManager.merge(account));
    }
}