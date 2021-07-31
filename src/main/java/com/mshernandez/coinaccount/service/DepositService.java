package com.mshernandez.coinaccount.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.mshernandez.coinaccount.dao.AccountDao;
import com.mshernandez.coinaccount.dao.DepositDao;
import com.mshernandez.coinaccount.dao.WithdrawRequestDao;
import com.mshernandez.coinaccount.entity.Account;
import com.mshernandez.coinaccount.entity.Deposit;
import com.mshernandez.coinaccount.entity.WithdrawRequest;
import com.mshernandez.coinaccount.service.wallet_rpc.WalletService;
import com.mshernandez.coinaccount.service.wallet_rpc.exception.WalletRequestException;
import com.mshernandez.coinaccount.service.wallet_rpc.result.ListUnspentUTXO;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Processes UTXOs into deposits and allocates the funds
 * accordingly.
 * <p>
 * This includes both deposits made by users and
 * change UTXOs resulting from withdraw transactions.
 */
@ApplicationScoped
public class DepositService
{
    @ConfigProperty(name = "coinaccount.internal.account")
    UUID internalAccountId;

    @ConfigProperty(name = "coinaccount.deposit.confirmations")
    int minDepositConfirmations;

    @ConfigProperty(name = "coinaccount.change.confirmations")
    int minChangeConfirmations;

    @Inject
    WalletService walletService;

    @Inject
    AccountDao accountDao;

    @Inject
    DepositDao depositDao;

    @Inject
    WithdrawRequestDao withdrawRequestDao;

    /**
     * Registers new deposits and updates pending balances
     * for the given account.
     * 
     * @param accountId The account ID to check for new deposits.
     * @return A response object indicating success/failure and the amount added to the account balance.
     * @throws WalletRequestException If an error occured contacting the wallet.
     */
    @Transactional
    public long registerDeposits(UUID accountId)
    {
        Account account = accountDao.find(accountId);
        if (account == null)
        {
            return 0L;
        }
        long addedBalance = 0L;
        long unconfirmedBalance = 0L;
        // Get UTXOs For Account
        List<ListUnspentUTXO> utxos = walletService.listUnspent(account.getDepositAddress());
        // Avoid Reprocessing Previous Transactions
        Set<String> oldTXIDs = account.getProcessedDepositIDs();
        Set<String> unspentTXIDs = new HashSet<>();
        // Process UTXOs
        for (ListUnspentUTXO utxo : utxos)
        {
            if (utxo.getConfirmations() >= minDepositConfirmations
                && utxo.isSpendable() && utxo.isSafe() && utxo.isSolvable())
            {
                if (!oldTXIDs.contains(utxo.getTxid()))
                {
                    long depositAmount = utxo.getAmount().getSatAmount();
                    Deposit deposit = new Deposit(utxo.getTxid(), utxo.getVout(), depositAmount);
                    depositDao.persist(deposit);
                    deposit.setShare(account, depositAmount);
                    addedBalance += depositAmount;
                }
                unspentTXIDs.add(utxo.getTxid());
            }
            else
            {
                unconfirmedBalance += utxo.getAmount().getSatAmount();
            }
        }
        // Only Need To Remember Outputs Which Remain Unspent
        account.setProcessedDepositIDs(unspentTXIDs);
        account.setPendingBalance(unconfirmedBalance);
        accountDao.update(account);
        return addedBalance;
    }

    @Transactional
    public void registerChangeDeposits()
    {
        Account internalAccount = accountDao.findOrCreate(internalAccountId);
        // Get Change UTXOs
        List<ListUnspentUTXO> utxos = walletService.listUnspent(internalAccount.getDepositAddress());
        // Avoid Reprocessing Previous Transactions
        Set<String> oldTXIDs = internalAccount.getProcessedDepositIDs();
        Set<String> unspentTXIDs = new HashSet<>();
        // Associate Change UTXOs Back To Original Owners
        for (ListUnspentUTXO utxo : utxos)
        {
            if (utxo.getConfirmations() >= minChangeConfirmations
                && utxo.isSpendable() && utxo.isSafe() && utxo.isSolvable())
            {
                if (!oldTXIDs.contains(utxo.getTxid()))
                {
                    // Look For Withdraw Request The Transaction Was Created From
                    WithdrawRequest withdrawRequest = withdrawRequestDao.find(utxo.getTxid());
                    if (withdrawRequest != null)
                    {
                        // Create Change Deposit
                        Deposit deposit = new Deposit(utxo.getTxid(), utxo.getVout(), utxo.getAmount().getSatAmount());
                        // Lookup Inputs To Change Deposit, Distribute Unused Balances
                        Set<Deposit> inputs = withdrawRequest.getInputs();
                        for (Deposit inputDeposit : inputs)
                        {
                            for (Account owner : inputDeposit.getOwners())
                            {
                                if (!owner.equals(internalAccount))
                                {
                                    deposit.setShare(owner, deposit.getShare(owner) + inputDeposit.getShare(owner));
                                }
                                owner.removeDeposit(inputDeposit);
                            }
                            depositDao.remove(inputDeposit);
                        }
                        depositDao.persist(deposit);
                        // Persist Account Changes
                        for (Account owner : deposit.getOwners())
                        {
                            accountDao.update(owner);
                        }
                        // Remove Fully Completed Withdraw Request
                        withdrawRequestDao.remove(withdrawRequest);
                    }
                }
                unspentTXIDs.add(utxo.getTxid());
            }
        }
        internalAccount.setProcessedDepositIDs(unspentTXIDs);
        accountDao.update(internalAccount);
    }
}