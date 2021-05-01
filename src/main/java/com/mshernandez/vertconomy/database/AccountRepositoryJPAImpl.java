package com.mshernandez.vertconomy.database;

import java.util.UUID;

import javax.persistence.EntityManager;

public class AccountRepositoryJPAImpl implements AccountRepository<Account>
{
    private EntityManager entityManager;

    public AccountRepositoryJPAImpl()
    {
        entityManager = JPAUtil.getEntityManager();
    }

    @Override
    public Account getAccount(UUID accountUUID)
    {
        return entityManager.find(Account.class, accountUUID);
    }

    @Override
    public Account save(Account account)
    {
        if (entityManager.contains(account))
        {
            account = entityManager.merge(account);
        }
        else
        {
            entityManager.persist(account);
        }
        return account;
    }

    @Override
    public void delete(Account account)
    {
        if (entityManager.contains(account))
        {
            entityManager.remove(account);
        }
    }
    
}
