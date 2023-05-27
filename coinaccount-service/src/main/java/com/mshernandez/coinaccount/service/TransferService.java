package com.mshernandez.coinaccount.service;

import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.mshernandez.coinaccount.dao.AccountDao;
import com.mshernandez.coinaccount.dao.DepositDao;
import com.mshernandez.coinaccount.entity.Account;
import com.mshernandez.coinaccount.service.exception.InsufficientFundsException;
import com.mshernandez.coinaccount.service.exception.UnaccountedFundsException;

import org.jboss.logging.Logger;

/**
 * Transfers balances internally between accounts.
 */
@ApplicationScoped
public class TransferService
{
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
        // Transfer Balances
        long senderBalance = sender.getBalance();
        if (transferAll)
        {
            receiver.setBalance(receiver.getBalance() + senderBalance);
            senderBalance = 0L;
        }
        else
        {
            receiver.setBalance(receiver.getBalance() + amount);
            senderBalance -= amount;
        }
        sender.setBalance(senderBalance);
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
     * @throws InsufficientFundsException If the changes would leave an account balance negative.
     */
    @Transactional
    public void batchTransfer(Map<UUID, Long> changes)
    {
        // Ensure Net Zero Change, All Balances Accounted For
        if (changes.values().stream().mapToLong(v -> v).sum() != 0L)
        {
            throw new UnaccountedFundsException();
        }
        // Attempt To Make Changes
        for (Entry<UUID, Long> change : changes.entrySet())
        {
            UUID accountId = change.getKey();
            long delta = change.getValue();
            // Find Account, Must Already Exist For Negative Amounts
            Account account;
            if (delta < 0L)
            {
                account = accountDao.find(accountId);
            }
            else
            {
                account = accountDao.findOrCreate(accountId);
            }
            // Ensure Account Can Afford Changes
            long updatedBalance = account.getBalance() + delta;
            if (updatedBalance < 0L)
            {
                throw new InsufficientFundsException();
            }
            // Make Change
            account.setBalance(updatedBalance);
            accountDao.update(account);
        }
    }
}