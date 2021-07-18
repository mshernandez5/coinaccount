package com.mshernandez.vertconomy.core.deposit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.persistence.EntityManager;

import com.mshernandez.vertconomy.core.account.Account;
import com.mshernandez.vertconomy.core.account.AccountRepository;
import com.mshernandez.vertconomy.core.account.DepositAccount;
import com.mshernandez.vertconomy.core.withdraw.WithdrawRequest;
import com.mshernandez.vertconomy.wallet_interface.RPCWalletConnection;
import com.mshernandez.vertconomy.wallet_interface.responses.UnspentOutputResponse;
import com.mshernandez.vertconomy.wallet_interface.responses.UnspentOutputResponse.UnspentOutput;

/**
 * Helps process UTXOs into deposits and allocate the funds
 * accordingly.
 * <p>
 * This includes both deposits made by users and
 * change UTXOs resulting from withdraw transactions.
 */
public class DepositHelper
{
    // Logger
    private Logger logger;

    // Wallet Access
    private RPCWalletConnection wallet;

    // Persistence
    private EntityManager entityManager;

    // Account Repository
    private AccountRepository accountRepository;

    // Withdraw Account
    private UUID withdrawAccountUUID;
    
    // Minimum Confirmations To Accept User Deposit
    private int minDepositConfirmations;

    // Minimum Confirmations To Process Withdraw Change
    private int minChangeConfirmations;

    /**
     * Create a new deposit helper instance.
     * 
     * @param wallet A connection to the wallet.
     * @param entityManager An entity manager for persistence.
     * @param withdrawAccount The server withdraw account.
     * @param minDepositConfirmations The minimum number of confirmations to accept user deposits.
     * @param minChangeConfirmations The minimum number of confirmations to process withdraw change.
     */
    public DepositHelper(Logger logger, RPCWalletConnection wallet, EntityManager entityManager,
                         AccountRepository accountRepository, UUID withdrawAccountUUID,
                         int minDepositConfirmations, int minChangeConfirmations)
    {
        this.logger = logger;
        this.wallet = wallet;
        this.entityManager = entityManager;
        this.accountRepository = accountRepository;
        this.withdrawAccountUUID = withdrawAccountUUID;
        this.minDepositConfirmations = minDepositConfirmations;
        this.minChangeConfirmations = minChangeConfirmations;
    }

    /**
     * Register new deposits for the given user account.
     * <p>
     * Returns the total value of newly confirmed balances.
     * 
     * @param account The account to check for new deposits.
     * @return Balance gained from newly registered deposits.
     */
    public long registerNewDeposits(DepositAccount account)
    {
        // Keep Track Of New Balances & Pending Unconfirmed Balances
        long addedBalance = 0L;
        long unconfirmedBalance = 0L;
        try
        {
            entityManager.getTransaction().begin();
            // Get Wallet Transactions For Addresses Associated With Account
            List<UnspentOutputResponse.UnspentOutput> unspentOutputs = wallet.getUnspentOutputs(account.getDepositAddress());
            // Remember Which Transactions Have Already Been Accounted For
            Set<String> oldTXIDs = account.getProcessedDepositIDs();
            Set<String> unspentTXIDs = new HashSet<>();
            // Check For New Unspent Outputs Deposited To Account
            for (UnspentOutput output : unspentOutputs)
            {
                if (output.confirmations >= minDepositConfirmations
                    && output.spendable && output.safe && output.solvable)
                {
                    if (!oldTXIDs.contains(output.txid))
                    {
                        long depositAmount = output.amount.satAmount;
                        // New Deposit Transaction Initially 100% Owned By Depositing Account
                        Deposit deposit = new Deposit(output.txid, output.vout, depositAmount);
                        entityManager.persist(deposit);
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
            entityManager.merge(account);
            entityManager.getTransaction().commit();
        }
        catch (Exception e)
        {
            logger.warning("Failed To Register New Deposits For Account: " + account.getAccountUUID());
            e.printStackTrace();
            entityManager.getTransaction().rollback();
            return 0L;
        }
        return addedBalance;
    }

    /**
     * Register change transactions from recent withdrawals.
     * <p>
     * Associate and distribute change among the proper owners.
     */
    public void registerChangeDeposits()
    {
        DepositAccount withdrawAccount = accountRepository.getOrCreateUserAccount(withdrawAccountUUID);
        try
        {
            entityManager.getTransaction().begin();
            // Get Wallet Transactions For Addresses Associated With Account
            List<UnspentOutputResponse.UnspentOutput> unspentOutputs = wallet.getUnspentOutputs(withdrawAccount.getDepositAddress());
            // Remember Which Transactions Have Already Been Accounted For
            Set<String> oldTXIDs = withdrawAccount.getProcessedDepositIDs();
            Set<String> unspentTXIDs = new HashSet<>();
            // Check For New Unspent Outputs Deposited To Account
            for (UnspentOutput output : unspentOutputs)
            {
                if (output.confirmations >= minChangeConfirmations
                    && output.spendable && output.safe && output.solvable)
                {
                    if (!oldTXIDs.contains(output.txid))
                    {
                        // Look For Withdraw Request The Transaction Was Created From
                        WithdrawRequest withdrawRequest = entityManager.find(WithdrawRequest.class, output.txid);
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
                                entityManager.remove(inputDeposit);
                            }
                            entityManager.persist(deposit);
                            // Persist Account Changes
                            for (Account owner : deposit.getOwners())
                            {
                                entityManager.merge(owner);
                            }
                            // Remove Fully Completed Withdraw Request
                            entityManager.remove(withdrawRequest);
                        }
                    }
                    unspentTXIDs.add(output.txid);
                }
            }
            oldTXIDs.clear();
            oldTXIDs.addAll(unspentTXIDs);
            withdrawAccount.setProcessedDepositIDs(oldTXIDs);
            entityManager.merge(withdrawAccount);
            entityManager.getTransaction().commit();
        }
        catch (Exception e)
        {
            logger.warning("Failed to register change deposits!");
            e.printStackTrace();
            entityManager.getTransaction().rollback();
        }
    }

    /**
     * Get the minimum number of confirmations required
     * for Vertconomy to consider a deposit valid.
     * 
     * @return The minimum number of confirmations to consider a deposit valid.
     */
    public int getMinDepositConfirmations()
    {
        return minDepositConfirmations;
    }

    /**
     * Get the minimum number of confirmations required
     * for Vertconomy to consider change UTXOs valid.
     * 
     * @return The minimum number of confirmations to use change.
     */
    public int getMinChangeConfirmations()
    {
        return minChangeConfirmations;
    }
}
