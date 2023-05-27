package com.mshernandez.coinaccount.task;

import java.util.Collection;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.mshernandez.coinaccount.dao.AccountDao;
import com.mshernandez.coinaccount.entity.Account;
import com.mshernandez.coinaccount.service.DepositService;
import com.mshernandez.coinaccount.service.wallet_rpc.exception.WalletRequestException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;

import io.quarkus.scheduler.Scheduled;
import io.vertx.core.eventbus.EventBus;

/**
 * Periodically registers new deposits.
 */
@ApplicationScoped
public class DepositTask
{
    @ConfigProperty(name = "coinaccount.account.change")
    UUID changeAccountId;

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
        try
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
                }
            }
        }
        catch (WalletRequestException e)
        {
            logger.log(Level.ERROR, "Failed To Check For Deposits: " + e.getMessage());
        }
    }
}