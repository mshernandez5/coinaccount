package com.mshernandez.vertconomy.core.account;

import java.util.UUID;
import java.util.logging.Logger;

import javax.persistence.EntityManager;

import com.mshernandez.vertconomy.wallet_interface.RPCWalletConnection;
import com.mshernandez.vertconomy.wallet_interface.exceptions.WalletRequestException;

public class AccountRepository
{
    // Logger
    private Logger logger;

    // Wallet Access
    RPCWalletConnection wallet;

    // Persistence
    private EntityManager entityManager;

    /**
     * Creates a new account repository.
     * 
     * @param logger A logger to use.
     * @param wallet A wallet connection.
     * @param entityManager An entity manager for persistence.
     */
    public AccountRepository(Logger logger, RPCWalletConnection wallet, EntityManager entityManager)
    {
        this.logger = logger;
        this.wallet = wallet;
        this.entityManager = entityManager;
    }

    /**
     * Gets a deposit account or creates a new one
     * if one does not already exist.
     * 
     * @param accountUUID The account UUID.
     * @return A deposit account reference.
     */
    public DepositAccount getOrCreateUserAccount(UUID accountUUID)
    {
        DepositAccount account = entityManager.find(DepositAccount.class, accountUUID);
        if (account == null)
        {
            logger.info("Creating New Account For User: " + accountUUID);
            try
            {
                String depositAddress = wallet.getNewAddress(accountUUID.toString());
                account = new DepositAccount(accountUUID, depositAddress);
                entityManager.getTransaction().begin();
                entityManager.persist(account);
                entityManager.getTransaction().commit();
            }
            catch (WalletRequestException e)
            {
                logger.warning("Failed To Get/Create Account: " + e.getMessage());
                return null;
            }
        }
        return account;
    }

    /**
     * Gets a holding account or creates a new one
     * if one does not already exist.
     * 
     * @param accountUUID The account UUID.
     * @return A holding account reference.
     */
    public Account getOrCreateHoldingAccount(UUID accountUUID)
    {
        Account account = entityManager.find(Account.class, accountUUID);
        if (account == null)
        {
            logger.info("Initializing Holding Account: " + accountUUID);
            account = new Account(accountUUID);
            entityManager.getTransaction().begin();
            entityManager.persist(account);
            entityManager.getTransaction().commit();
        }
        return account;
    }
}