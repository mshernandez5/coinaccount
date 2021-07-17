package com.mshernandez.vertconomy.core.transfer;

import java.util.Iterator;
import java.util.logging.Logger;

import javax.persistence.EntityManager;

import com.mshernandez.vertconomy.core.account.Account;
import com.mshernandez.vertconomy.core.deposit.Deposit;

/**
 * Helps transfer balances internally between accounts.
 */
public class TransferHelper
{
    // Logger
    private Logger logger;

    // Persistence
    private EntityManager entityManager;

    /**
     * Create a new transfer manager instance.
     * 
     * @param logger A logger to use.
     * @param entityManager An entity manager for persistence.
     */
    public TransferHelper(Logger logger, EntityManager entityManager)
    {
        this.logger = logger;
        this.entityManager = entityManager;
    }

    /**
     * Transfer an amount from one account to another,
     * internally redistributing ownership of the
     * underlying deposits.
     * 
     * @param sender The sending account.
     * @param receiver The receiving account.
     * @param amount The amount to transfer, in sats.
     * @return True if the transfer was successful.
     */
    public boolean transferBalance(Account sender, Account receiver, long amount)
    {
        if (sender.calculateBalance() < amount)
        {
            logger.info(sender + " can't send " + amount + " to " + receiver);
            return false;
        }
        try
        {
            entityManager.getTransaction().begin();
            long remainingOwed = amount;
            Iterator<Deposit> it = sender.getDeposits().iterator();
            while (it.hasNext() && remainingOwed > 0L)
            {
                Deposit deposit = it.next();
                long senderShare = deposit.getShare(sender);
                long takenAmount;
                if (senderShare <= remainingOwed)
                {
                    deposit.setShare(sender, 0L);
                    deposit.setShare(receiver, deposit.getShare(receiver) + senderShare);
                    takenAmount = senderShare;
                }
                else
                {
                    deposit.setShare(sender, senderShare - remainingOwed);
                    deposit.setShare(receiver, deposit.getShare(receiver) + remainingOwed);
                    takenAmount = remainingOwed;
                }
                remainingOwed -= takenAmount;
            }
            entityManager.getTransaction().commit();
            logger.info(sender + " successfully sent " + amount + " to " + receiver);
        }
        catch (Exception e)
        {
            logger.info(sender + " failed to send " + amount + " to " + receiver);
            entityManager.getTransaction().rollback();
        }
        return true;
    }
}
