package com.mshernandez.vertconomy.core.service;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.persist.Transactional;
import com.mshernandez.vertconomy.core.VertconomyConfiguration;
import com.mshernandez.vertconomy.core.entity.Account;
import com.mshernandez.vertconomy.core.entity.AccountDao;
import com.mshernandez.vertconomy.core.entity.Deposit;
import com.mshernandez.vertconomy.core.entity.DepositDao;
import com.mshernandez.vertconomy.core.service.exception.InsufficientFundsException;
import com.mshernandez.vertconomy.core.util.BinarySearchCoinSelector;
import com.mshernandez.vertconomy.core.util.CoinEvaluator;
import com.mshernandez.vertconomy.core.util.CoinSelector;
import com.mshernandez.vertconomy.core.util.DepositShareEvaluator;
import com.mshernandez.vertconomy.core.util.InternalTransferPreselector;

/**
 * Helps transfer balances internally between accounts.
 */
@Singleton
public class TransferService
{
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
    public TransferService(AccountDao accountDao, DepositDao depositDao)
    {
        this.accountDao = accountDao;
        this.depositDao = depositDao;
        coinSelector = new BinarySearchCoinSelector<>(COIN_SELECTOR_MAX_REWIND);
    }

    /**
     * Transfer an amount from one account to another,
     * internally redistributing ownership of the
     * underlying deposits.
     * 
     * @param senderId The sending account ID.
     * @param receiverId The receiving account ID.
     * @param amount The amount to transfer, in sats.
     * @throws InsufficientFundsException If the sender cannot afford the transfer.
     * @return True if the transfer was successful.
     */
    @Transactional(rollbackOn = InsufficientFundsException.class)
    public void transferBalance(UUID senderId, UUID receiverId, long amount) throws InsufficientFundsException
    {
        Account sender = accountDao.findOrCreate(senderId);
        Account receiver = accountDao.findOrCreate(receiverId);
        if (amount <= 0 || sender.calculateBalance() < amount)
        {
            throw new InsufficientFundsException();
        }
        Set<Deposit> selected = new InternalTransferPreselector(coinSelector, sender, receiver).selectInputs(amount);
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
    }

    /**
     * Specify multiple account balance changes
     * to make as one transaction, where the entire
     * batch will fail if any one change fails.
     * <p>
     * If the total of all balance changes is not
     * zero, then funds will be given or taken
     * from the server account to attempt to complete
     * the batch.
     * <p>
     * For example, using the following map:
     * <table>
     *    <tr>
     *       <td>Account</td>
     *       <td>Change</td>
     *    </tr>
     *    <tr>
     *       <td>#1</td>
     *       <td>-5L</td>
     *    </tr>
     *    <tr>
     *       <td>#2</td>
     *       <td>10L</td>
     *    </tr>
     * </table>
     * Then account #2 will receive 5 sats from account #1 as well
     * as an additional 5 sats from the server account, assuming
     * account #1 and the server account have the balances to
     * complete this operation.
     * 
     * @param changes A map of account UUIDs to balance changes.
     * @return True if the changes were successfully executed.
     */
    @Transactional(rollbackOn = InsufficientFundsException.class)
    public void batchTransfer(Map<UUID, Long> changes) throws InsufficientFundsException
    {
        // Must Execute Changes Adding To Server Account First
        Comparator<UUID> changeComparator = new Comparator<UUID>()
        {
            @Override
            public int compare(UUID a, UUID b)
            {
                return (int) (changes.get(a) - changes.get(b));
            }
        };
        SortedSet<UUID> accountChangeOrder = new TreeSet<>(changeComparator);
        accountChangeOrder.addAll(changes.keySet());
        // Attempt To Make Changes
        for (UUID id : accountChangeOrder)
        {
            long change = changes.get(id);
            if (change < 0L)
            {
                transferBalance(id, VertconomyConfiguration.SERVER_ACCOUNT_UUID, Math.abs(change));
            }
            else
            {
                transferBalance(VertconomyConfiguration.SERVER_ACCOUNT_UUID, id, change);
            }
        }
    }
}
