package com.mshernandez.vertconomy.core.entity;

import java.util.UUID;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;

import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import com.mshernandez.vertconomy.wallet_interface.RPCWalletConnection;
import com.mshernandez.vertconomy.wallet_interface.exceptions.WalletRequestException;

@Singleton
@Transactional
public class JPAAccountDao implements AccountDao
{
    private final Logger logger;

    private final RPCWalletConnection wallet;

    private final Provider<EntityManager> emProvider;

    /**
     * Creates a new JPA account DAO.
     * 
     * @param logger A logger to use.
     * @param wallet A wallet connection.
     * @param emProvider An entity manager provider.
     */
    @Inject
    public JPAAccountDao(Logger logger, RPCWalletConnection wallet, Provider<EntityManager> emProvider)
    {
        this.logger = logger;
        this.wallet = wallet;
        this.emProvider = emProvider;
    }

    @Override
    public Account findOrCreate(UUID accountUUID)
    {
        EntityManager entityManager = emProvider.get();
        Account account = entityManager.find(Account.class, accountUUID);
        if (account == null)
        {
            logger.info("Creating New Account For User: " + accountUUID);
            String depositAddress = null;
            try
            {
                depositAddress = wallet.getNewAddress(accountUUID.toString());
            }
            catch (WalletRequestException e)
            {
                logger.warning("Failed To Get/Create Account: " + e.getMessage());
                return null;
            }
            account = new Account(accountUUID, depositAddress);
            entityManager.persist(account);
        }
        return account;
    }

    @Override
    public Account update(Account account)
    {
        EntityManager entityManager = emProvider.get();
        return entityManager.merge(account);
    }

    @Override
    public void refresh(Account account)
    {
        EntityManager entityManager = emProvider.get();
        entityManager.refresh(account);
    }

    @Override
    public void remove(Account account)
    {
        EntityManager entityManager = emProvider.get();
        entityManager.remove(entityManager.merge(account));
    }
}