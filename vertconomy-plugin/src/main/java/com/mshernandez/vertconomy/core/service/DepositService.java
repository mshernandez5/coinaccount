package com.mshernandez.vertconomy.core.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.persist.Transactional;
import com.mshernandez.vertconomy.core.VertconomyConfiguration;
import com.mshernandez.vertconomy.core.entity.Account;
import com.mshernandez.vertconomy.core.entity.Deposit;
import com.mshernandez.vertconomy.core.entity.DepositDao;
import com.mshernandez.vertconomy.core.entity.JPAAccountDao;
import com.mshernandez.vertconomy.core.entity.WithdrawRequest;
import com.mshernandez.vertconomy.core.entity.WithdrawRequestDao;
import com.mshernandez.vertconomy.wallet_interface.RPCWalletConnection;
import com.mshernandez.vertconomy.wallet_interface.exceptions.WalletRequestException;
import com.mshernandez.vertconomy.wallet_interface.responses.UnspentOutputResponse;
import com.mshernandez.vertconomy.wallet_interface.responses.UnspentOutputResponse.UnspentOutput;

/**
 * Helps process UTXOs into deposits and allocate the funds
 * accordingly.
 * <p>
 * This includes both deposits made by users and
 * change UTXOs resulting from withdraw transactions.
 */
@Singleton
public class DepositService
{
    private final Logger logger;

    private final RPCWalletConnection wallet;

    private final JPAAccountDao accountDao;

    private final DepositDao depositDao;

    private final WithdrawRequestDao withdrawRequestDao;

    private final VertconomyConfiguration config;

    /**
     * Create a new deposit service instance.
     * 
     * @param logger A logger for this service to use.
     * @param wallet A wallet connection.
     * @param accountDao An account DAO.
     * @param depositDao A deposit DAO.
     * @param withdrawRequestDao A withdraw request DAO.
     * @param config A Vertconomy configuration object.
     */
    @Inject
    public DepositService(Logger logger, RPCWalletConnection wallet,
                         JPAAccountDao accountDao, DepositDao depositDao,
                         WithdrawRequestDao withdrawRequestDao, VertconomyConfiguration config)
    {
        this.logger = logger;
        this.wallet = wallet;
        this.accountDao = accountDao;
        this.depositDao = depositDao;
        this.withdrawRequestDao = withdrawRequestDao;
        this.config = config;
    }

    /**
     * Register new deposits for the given user account.
     * <p>
     * Returns the total value of newly confirmed balances.
     * 
     * @param account The account to check for new deposits.
     * @return Balance gained from newly registered deposits.
     */
    @Transactional
    public long registerNewDeposits(UUID accountId)
    {
        Account account = accountDao.findOrCreate(accountId);
        // Keep Track Of New Balances & Pending Unconfirmed Balances
        long addedBalance = 0L;
        long unconfirmedBalance = 0L;
        // Get Wallet Transactions For Addresses Associated With Account
        List<UnspentOutputResponse.UnspentOutput> unspentOutputs = null;
        try
        {
            unspentOutputs = wallet.getUnspentOutputs(account.getDepositAddress());
        }
        catch (WalletRequestException e)
        {
            logger.warning("Failed To Register New Deposits For Account: " + account.getAccountUUID());
            e.printStackTrace();
            return 0L;
        }
        // Remember Which Transactions Have Already Been Accounted For
        Set<String> oldTXIDs = account.getProcessedDepositIDs();
        Set<String> unspentTXIDs = new HashSet<>();
        // Associate New Unspent Outputs With Account
        for (UnspentOutput output : unspentOutputs)
        {
            if (output.confirmations >= config.getMinDepositConfirmations()
                && output.spendable && output.safe && output.solvable)
            {
                if (!oldTXIDs.contains(output.txid))
                {
                    long depositAmount = output.amount.satAmount;
                    // New Deposit Transaction Initially 100% Owned By Depositing Account
                    Deposit deposit = new Deposit(output.txid, output.vout, depositAmount);
                    depositDao.persist(deposit);
                    // Associate With Account
                    deposit.setShare(account, depositAmount);
                    addedBalance += depositAmount;
                }
                unspentTXIDs.add(output.txid);
            }
            else
            {
                unconfirmedBalance += output.amount.satAmount;
            }
        }
        oldTXIDs.clear();
        oldTXIDs.addAll(unspentTXIDs);
        account.setProcessedDepositIDs(oldTXIDs);
        account.setPendingBalance(unconfirmedBalance);
        accountDao.update(account);
        return addedBalance;
    }

    /**
     * Register change transactions from recent withdrawals.
     * <p>
     * Associate and distribute change among the proper owners.
     */
    @Transactional
    public void registerChangeDeposits()
    {
        Account withdrawAccount = accountDao.findOrCreate(VertconomyConfiguration.WITHDRAW_ACCOUNT_UUID);
        // Get Wallet Transactions For Addresses Associated With Account
        List<UnspentOutputResponse.UnspentOutput> unspentOutputs = null;
        try
        {
            unspentOutputs = wallet.getUnspentOutputs(withdrawAccount.getDepositAddress());
        }
        catch (Exception e)
        {
            logger.warning("Failed to register change deposits!");
            e.printStackTrace();
            return;
        }
        // Remember Which Transactions Have Already Been Accounted For
        Set<String> oldTXIDs = withdrawAccount.getProcessedDepositIDs();
        Set<String> unspentTXIDs = new HashSet<>();
        // Associate Change UTXOs Back To Original Owners
        for (UnspentOutput output : unspentOutputs)
        {
            if (output.confirmations >= config.getMinChangeConfirmations()
                && output.spendable && output.safe && output.solvable)
            {
                if (!oldTXIDs.contains(output.txid))
                {
                    // Look For Withdraw Request The Transaction Was Created From
                    WithdrawRequest withdrawRequest = withdrawRequestDao.find(output.txid);
                    if (withdrawRequest != null)
                    {
                        // Create Change Deposit
                        Deposit deposit = new Deposit(output.txid, output.vout, output.amount.satAmount);
                        // Lookup Inputs To Change Deposit, Distribute Unused Balances
                        Set<Deposit> inputs = withdrawRequest.getInputs();
                        for (Deposit inputDeposit : inputs)
                        {
                            for (Account owner : inputDeposit.getOwners())
                            {
                                if (!owner.equals(withdrawAccount))
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
                unspentTXIDs.add(output.txid);
            }
        }
        oldTXIDs.clear();
        oldTXIDs.addAll(unspentTXIDs);
        withdrawAccount.setProcessedDepositIDs(oldTXIDs);
        accountDao.update(withdrawAccount);
    }
}
