package com.mshernandez.coinaccount.service;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.mshernandez.coinaccount.dao.AccountDao;
import com.mshernandez.coinaccount.dao.DepositDao;
import com.mshernandez.coinaccount.entity.Account;
import com.mshernandez.coinaccount.entity.Deposit;
import com.mshernandez.coinaccount.service.exception.InsufficientFundsException;
import com.mshernandez.coinaccount.service.exception.UnaccountedFundsException;
import com.mshernandez.coinaccount.service.util.InternalTransferPreselector;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;

/**
 * Transfers balances internally between accounts.
 */
@ApplicationScoped
public class TransferService
{
    @ConfigProperty(name = "coinaccount.internal.account")
    UUID internalAccountId;

    @Inject
    Logger logger;

    @Inject
    AccountDao accountDao;

    @Inject
    DepositDao depositDao;

    /**
     * Transfer balances from one account to
     * another, internally redistributing
     * deposit shares.
     * 
     * @param senderId The sending account ID.
     * @param receiverId The receiving account ID.
     * @param transferAll If true, ignores the amount and transfers all possible balances.
     * @param amount The amount to transfer, in sats.
     * @throws InsufficientFundsException If the sender cannot afford the transfer.
     */
    @Transactional
    public void transferBalance(UUID senderId, UUID receiverId, boolean transferAll, long amount)
    {
        Account sender = accountDao.find(senderId);
        if (sender == null || (!transferAll && (amount <= 0L || sender.getBalance() < amount)))
        {
            throw new InsufficientFundsException();
        }
        Account receiver = accountDao.findOrCreate(receiverId);
        // Select Deposits To Source Funds From
        Set<Deposit> selected;
        if (transferAll)
        {
            selected = sender.getDeposits();
        }
        else
        {
            selected = new InternalTransferPreselector(sender, receiver).selectInputs(amount);
        }
        if (selected == null)
        {
            throw new InsufficientFundsException();
        }
        // Conduct The Transfer Using The Selected Deposits
        transferSelected(sender, receiver, selected, transferAll, amount);
    }

    /**
     * Only for internal service usage.
     * <p>
     * Transfers balances between accounts drawing
     * only from the selected set of deposits.
     * 
     * @param sender The sending account.
     * @param receiver The receiving account.
     * @param selected The set of selected deposits.
     * @param transferAll If true, ignores the amount and transfers all possible balances.
     * @param amount The amount to transfer, in sats.
     * @throws InsufficientFundsException If the sender cannot afford the transfer.
     */
    @Transactional
    private void transferSelected(Account sender, Account receiver, Set<Deposit> selected, boolean transferAll, long amount)
    {
        Iterator<Deposit> it = selected.iterator();
        long remainingOwed = amount;
        while (it.hasNext() && (transferAll || remainingOwed > 0L))
        {
            Deposit deposit = it.next();
            long senderShare = sender.getShare(deposit);
            long takenAmount;
            if (senderShare <= remainingOwed)
            {
                sender.setShare(deposit, 0L);
                receiver.setShare(deposit, receiver.getShare(deposit) + senderShare);
                takenAmount = senderShare;
            }
            else
            {
                sender.setShare(deposit, senderShare - remainingOwed);
                receiver.setShare(deposit, receiver.getShare(deposit) + remainingOwed);
                takenAmount = remainingOwed;
            }
            remainingOwed -= takenAmount;
        }
        if (!transferAll && remainingOwed > 0L)
        {
            logger.log(Level.WARN, "Transfer attempted with impossible deposit set! The selection algorithm may not be returning valid results.");
            throw new InsufficientFundsException();
        }
        accountDao.update(sender);
        accountDao.update(receiver);
    }

    /**
     * Specify multiple account balance changes
     * to make as one transaction, where the entire
     * batch will fail if any one change fails.
     * <p>
     * The sum of all changes must be exactly 0
     * so that all funds are accounted for.
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
     *       <td>-1L</td>
     *    </tr>
     *    <tr>
     *       <td>#3</td>
     *       <td>3L</td>
     *    </tr>
     *    <tr>
     *       <td>#4</td>
     *       <td>3L</td>
     *    </tr>
     * </table>
     * Then account #1 and account #2 will together be sending
     * (with different contributions) 6 sats split between
     * accounts #3 and #4. If either sending account does not have
     * enough funds the transfer will fail as a whole.
     * 
     * @param changes A map of account UUIDs to balance changes.
     * @throws UnaccountedFundsException If the changes would leave funds unaccounted for.
     */
    @Transactional
    public void batchTransfer(Map<UUID, Long> changes)
    {
        if (changes.values().stream().mapToLong(v -> v).sum() != 0L)
        {
            throw new UnaccountedFundsException();
        }
        // Must Sort Changes To Take Funds Before Giving Them
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
                transferBalance(id, internalAccountId, false, Math.abs(change));
            }
            else
            {
                transferBalance(internalAccountId, id, false, change);
            }
        }
    }
}