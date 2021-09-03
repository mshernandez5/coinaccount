package com.mshernandez.coinaccount.dao;

import java.util.Collection;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

import com.mshernandez.coinaccount.entity.Account;
import com.mshernandez.coinaccount.entity.Deposit;
import com.mshernandez.coinaccount.service.wallet_rpc.WalletService;
import com.mshernandez.coinaccount.service.wallet_rpc.exception.WalletRequestException;

import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;

@ApplicationScoped
@Transactional
public class JPAAccountDao implements AccountDao
{
    @Inject
    Logger logger;

    @Inject
    WalletService walletService;

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
            String depositAddress = null;
            try
            {
                depositAddress = walletService.getNewAddress(accountUUID.toString());
            }
            catch (WalletRequestException e)
            {
                logger.log(Level.WARN, "Failed To Get/Create Account: " + e.getMessage());
                return null;
            }
            account = new Account(accountUUID, depositAddress);
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
    public Collection<Account> findAllWithDeposit(Deposit deposit)
    {
        return entityManager.createQuery("SELECT account FROM Account account JOIN account.shares entry ON KEY(entry) = :deposit", Account.class)
            .setParameter("deposit", deposit)
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