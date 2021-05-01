package com.mshernandez.vertconomy.database;

import java.util.UUID;

import javax.persistence.EntityManager;

public class UserAccountRepositoryJPAImpl implements AccountRepository<UserAccount>
{
    private EntityManager entityManager;

    public UserAccountRepositoryJPAImpl()
    {
        entityManager = JPAUtil.getEntityManager();
    }

    @Override
    public UserAccount getAccount(UUID accountUUID)
    {
        return entityManager.find(UserAccount.class, accountUUID);
    }

    @Override
    public UserAccount save(UserAccount account)
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
    public void delete(UserAccount account)
    {
        if (entityManager.contains(account))
        {
            entityManager.remove(account);
        }
    }
    
}
