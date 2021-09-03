package com.mshernandez.coinaccount.service;

import java.util.Collection;
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
import com.mshernandez.coinaccount.entity.DepositType;
import com.mshernandez.coinaccount.entity.WithdrawRequest;
import com.mshernandez.coinaccount.service.wallet_rpc.WalletService;
import com.mshernandez.coinaccount.service.wallet_rpc.exception.WalletRequestException;
import com.mshernandez.coinaccount.service.wallet_rpc.parameter.ListUnspentQuery;
import com.mshernandez.coinaccount.service.wallet_rpc.result.ListUnspentUTXO;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;

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

    @ConfigProperty(name = "coinaccount.deposit.minimum")
    long minDepositAmount;

    @ConfigProperty(name = "coinaccount.deposit.confirmations")
    int minDepositConfirmations;

    @ConfigProperty(name = "coinaccount.change.confirmations")
    int minChangeConfirmations;

    @Inject
    Logger logger;

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
        ListUnspentQuery utxoQuery = new ListUnspentQuery()
            .setMinConfirmations(0)
            .setAddresses(account.getDepositAddress())
            .setMinimumAmount(minDepositAmount);
        List<ListUnspentUTXO> utxos = walletService.listUnspent(utxoQuery);
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
                    // Determine Deposit Type
                    DepositType type;
                    String descriptor = utxo.getDesc();
                    if (descriptor.startsWith("pkh"))
                    {
                        type = DepositType.P2PKH;
                    }
                    else if (descriptor.startsWith("sh(wpkh"))
                    {
                        type = DepositType.P2SH_P2WPKH;
                    }
                    else if (descriptor.startsWith("wpkh"))
                    {
                        type = DepositType.P2WPKH;
                    }
                    else if (descriptor.startsWith("tr"))
                    {
                        type = DepositType.P2TR;
                    }
                    else
                    {
                        // Wallet Generated Non-Supported Addresses For Account Deposits, Would Require Update To Address
                        logger.log(Level.ERROR, "Unsupported Deposit Received By " + accountId  + ": " + descriptor);
                        continue;
                    }
                    // Determine Deposit Amount
                    long depositAmount = utxo.getAmount().getSatAmount();
                    // Create, Persist, & Distribute New Deposit
                    Deposit deposit = new Deposit(utxo.getTxid(), utxo.getVout(), type, depositAmount);
                    depositDao.persist(deposit);
                    account.setShare(deposit, depositAmount);
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
                        // Determine Deposit Type
                        DepositType type;
                        String descriptor = utxo.getDesc();
                        if (descriptor.startsWith("pkh"))
                        {
                            type = DepositType.P2PKH;
                        }
                        else if (descriptor.startsWith("sh(wpkh"))
                        {
                            type = DepositType.P2SH_P2WPKH;
                        }
                        else if (descriptor.startsWith("wpkh"))
                        {
                            type = DepositType.P2WPKH;
                        }
                        else if (descriptor.startsWith("tr"))
                        {
                            type = DepositType.P2TR;
                        }
                        else
                        {
                            // Wallet Generated A Non-Supported Addresses For Change Deposits, Would Require Update To Address
                            logger.log(Level.ERROR, "Unsupported Change Deposit Received: " + descriptor);
                            continue;
                        }
                        // Create Change Deposit
                        Deposit deposit = new Deposit(utxo.getTxid(), utxo.getVout(), type, utxo.getAmount().getSatAmount());
                        depositDao.persist(deposit);
                        // Lookup Inputs To Change Deposit, Distribute Unused Balances
                        Set<Deposit> inputs = withdrawRequest.getInputs();
                        for (Deposit inputDeposit : inputs)
                        {
                            Collection<Account> owners = accountDao.findAllWithDeposit(inputDeposit);
                            for (Account owner : owners)
                            {
                                if (!owner.equals(internalAccount))
                                {
                                    owner.setShare(deposit, owner.getShare(deposit) + owner.getShare(inputDeposit));
                                }
                                owner.setShare(inputDeposit, 0L);
                                accountDao.update(owner);
                            }
                            depositDao.remove(inputDeposit);
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