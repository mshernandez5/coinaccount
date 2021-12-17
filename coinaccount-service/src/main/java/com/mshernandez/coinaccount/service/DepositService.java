package com.mshernandez.coinaccount.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.mshernandez.coinaccount.dao.AccountDao;
import com.mshernandez.coinaccount.dao.AddressDao;
import com.mshernandez.coinaccount.dao.DepositDao;
import com.mshernandez.coinaccount.entity.Account;
import com.mshernandez.coinaccount.entity.Address;
import com.mshernandez.coinaccount.entity.Deposit;
import com.mshernandez.coinaccount.service.wallet_rpc.WalletService;
import com.mshernandez.coinaccount.service.wallet_rpc.exception.WalletRequestException;
import com.mshernandez.coinaccount.service.wallet_rpc.parameter.DepositType;
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
    @ConfigProperty(name = "coinaccount.account.change")
    UUID changeAccountId;

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
    AddressDao addressDao;

    @Inject
    DepositDao depositDao;

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
        Set<String> addresses = account.getAddresses()
            .stream()
            .map(a -> a.getAddress())
            .collect(Collectors.toSet());
        if (addresses.isEmpty())
        {
            return 0L;
        }
        // Get UTXOs For Account
        ListUnspentQuery utxoQuery = new ListUnspentQuery()
            .setMinConfirmations(0)
            .setAddresses(addresses)
            .setMinimumAmount(minDepositAmount);
        List<ListUnspentUTXO> utxos = walletService.listUnspent(utxoQuery);
        // Process UTXOs
        long addedBalance = 0L;
        long unconfirmedBalance = 0L;
        for (ListUnspentUTXO utxo : utxos)
        {
            if (utxo.getConfirmations() >= minDepositConfirmations
                && utxo.isSpendable() && utxo.isSafe() && utxo.isSolvable())
            {
                if (depositDao.find(utxo.getTxid(), utxo.getVout()) == null)
                {
                    // Determine Deposit Type
                    DepositType type = getDepositType(utxo.getDesc());
                    if (type == null)
                    {
                        // Wallet Generated Non-Supported Addresses For Account Deposits
                        logger.log(Level.ERROR, "Unsupported Deposit Received By " + accountId  + ": " + utxo.getDesc());
                        continue;
                    }
                    // Determine Deposit Amount
                    long depositAmount = utxo.getAmount().getSatAmount();
                    // Create, Persist, & Distribute New Deposit
                    Deposit deposit = new Deposit(utxo.getTxid(), utxo.getVout(), type, depositAmount);
                    depositDao.persist(deposit);
                    logger.log(Level.INFO, accountId + " Received New Deposit: " + deposit);
                    addedBalance += depositAmount;
                }
            }
            else
            {
                unconfirmedBalance += utxo.getAmount().getSatAmount();
            }
            // Indicate The Active Address Has Been Used
            Address address = addressDao.find(utxo.getAddress());
            address.setUsed();
            addressDao.update(address);
        }
        // Change Account Should Not Have Balance
        if (!accountId.equals(changeAccountId))
        {
            account.setBalance(account.getBalance() + addedBalance);
            account.setPendingBalance(unconfirmedBalance);
        }
        accountDao.update(account);
        return addedBalance;
    }

    /**
     * Get the DepositType corresponding to the given descriptor.
     * 
     * @param descriptor The receiving address descriptor.
     * @return The deposit type, or null if unknown.
     */
    private DepositType getDepositType(String descriptor)
    {
        if (descriptor.startsWith("pkh"))
        {
            return DepositType.P2PKH;
        }
        else if (descriptor.startsWith("sh(wpkh"))
        {
            return DepositType.P2SH_P2WPKH;
        }
        else if (descriptor.startsWith("wpkh"))
        {
            return DepositType.P2WPKH;
        }
        else if (descriptor.startsWith("tr"))
        {
            return DepositType.P2TR;
        }
        return null;
    }
}