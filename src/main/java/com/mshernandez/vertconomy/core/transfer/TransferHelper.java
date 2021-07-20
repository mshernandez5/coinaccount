package com.mshernandez.vertconomy.core.transfer;

import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import javax.persistence.EntityManager;

import com.mshernandez.vertconomy.core.BinarySearchCoinSelector;
import com.mshernandez.vertconomy.core.CoinSelector;
import com.mshernandez.vertconomy.core.DepositShareEvaluator;
import com.mshernandez.vertconomy.core.Evaluator;
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

    // Coin Selection For Transfers
    private CoinSelector<Deposit> coinSelector;

    /**
     * Create a new transfer helper instance.
     * 
     * @param logger A logger to use.
     * @param entityManager An entity manager for persistence.
     */
    public TransferHelper(Logger logger, EntityManager entityManager)
    {
        this.logger = logger;
        this.entityManager = entityManager;
        coinSelector = new BinarySearchCoinSelector<>(5);
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
        entityManager.getTransaction().begin();
        Evaluator<Deposit> evaluator = new DepositShareEvaluator(sender);
        Set<Deposit> selected = coinSelector.selectInputs(evaluator, sender.getDeposits(), 0L, amount);
        long remainingOwed = amount;
        Iterator<Deposit> it = selected.iterator();
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
        return true;
    }
}
