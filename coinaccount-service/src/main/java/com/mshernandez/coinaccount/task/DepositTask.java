package com.mshernandez.coinaccount.task;

import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.mshernandez.coinaccount.dao.AccountDao;
import com.mshernandez.coinaccount.entity.Account;
import com.mshernandez.coinaccount.service.DepositService;

import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;
import io.vertx.core.eventbus.EventBus;

/**
 * Periodically registers new deposits.
 */
@ApplicationScoped
public class DepositTask
{
    @Inject
    Logger logger;

    @Inject
    EventBus eventBus;

    @Inject
    AccountDao accountDao;

    @Inject
    DepositService depositService;

    /**
     * Periodically checks for and registers new deposits.
     * <p>
     * Publishes events for newly confirmed account deposits.
     */
    @Scheduled(every = "{coinaccount.deposit.check}")
    public void registerNewDeposits()
    {
        // Check For New Deposits For All Accounts
        Collection<Account> accounts = accountDao.findAll();
        for (Account account : accounts)
        {
            // If Account Has New Confirmed Balances, Publish Event
            long newlyConfirmed = depositService.registerDeposits(account.getAccountUUID());
            if (newlyConfirmed > 0L)
            {
                eventBus.publish("deposit-confirmed", new DepositConfirmedEvent(account.getAccountUUID(), newlyConfirmed));
                logger.info("New Deposit Registered For Account: " + account.getAccountUUID() + ", Value: " + newlyConfirmed);
            }
        }
        // Register Change Deposits
        depositService.registerChangeDeposits();
    }
}