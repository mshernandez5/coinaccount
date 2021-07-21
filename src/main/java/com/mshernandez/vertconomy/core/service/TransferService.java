package com.mshernandez.vertconomy.core.service;

import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.persist.Transactional;
import com.mshernandez.vertconomy.core.BinarySearchCoinSelector;
import com.mshernandez.vertconomy.core.CoinSelector;
import com.mshernandez.vertconomy.core.DepositShareEvaluator;
import com.mshernandez.vertconomy.core.Evaluator;
import com.mshernandez.vertconomy.core.entity.Account;
import com.mshernandez.vertconomy.core.entity.AccountDao;
import com.mshernandez.vertconomy.core.entity.Deposit;
import com.mshernandez.vertconomy.core.entity.DepositDao;

/**
 * Helps transfer balances internally between accounts.
 */
@Singleton
public class TransferService
{
    private final Logger logger;

    private final AccountDao accountDao;

    private final DepositDao depositDao;

    // Coin Selection For Transfers
    private final CoinSelector<Deposit> coinSelector;

    // Rewind Parameter, See BinarySearchCoinSelector Documentation
    private static final int COIN_SELECTOR_MAX_REWIND = 5;

    /**
     * Create a new transfer service instance.
     * 
     * @param logger A logger for this service to use.
     * @param accountDao An account DAO.
     * @param depositDao A deposit DAO.
     */
    @Inject
    public TransferService(Logger logger, AccountDao accountDao, DepositDao depositDao)
    {
        this.logger = logger;
        this.accountDao = accountDao;
        this.depositDao = depositDao;
        coinSelector = new BinarySearchCoinSelector<>(COIN_SELECTOR_MAX_REWIND);
    }

    /**
     * Transfer an amount from one account to another,
     * internally redistributing ownership of the
     * underlying deposits.
     * 
     * @param sender The sending account ID.
     * @param receiver The receiving account ID.
     * @param amount The amount to transfer, in sats.
     * @return True if the transfer was successful.
     */
    @Transactional
    public boolean transferBalance(UUID senderId, UUID receiverId, long amount)
    {
        Account sender = accountDao.findOrCreate(senderId);
        Account receiver = accountDao.findOrCreate(receiverId);
        if (sender.calculateBalance() < amount)
        {
            logger.info(sender + " can't send " + amount + " to " + receiver);
            return false;
        }
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
            depositDao.update(deposit);
        }
        accountDao.update(sender);
        accountDao.update(receiver);
        logger.info(sender + " successfully sent " + amount + " to " + receiver);
        return true;
    }
}
