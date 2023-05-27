package com.mshernandez.coinaccount.service;

import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.mshernandez.coinaccount.dao.AccountDao;
import com.mshernandez.coinaccount.dao.AddressDao;
import com.mshernandez.coinaccount.dao.DepositDao;
import com.mshernandez.coinaccount.entity.Account;
import com.mshernandez.coinaccount.service.exception.InvalidAddressException;
import com.mshernandez.coinaccount.service.result.AccountBalanceInfo;
import com.mshernandez.coinaccount.service.wallet_rpc.WalletService;
import com.mshernandez.coinaccount.service.wallet_rpc.parameter.DepositType;
import com.mshernandez.coinaccount.service.wallet_rpc.result.ValidateAddressResult;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AccountInfoService
{
    // Match sus Address Patterns
    private static final Pattern SUS_PATTERN = Pattern.compile(".*[^a-zA-Z0-9].*");
    
    @ConfigProperty(name = "coinaccount.address.type")
    DepositType defaultAddressType;

    @ConfigProperty(name = "coinaccount.address.user.reuse")
    boolean reuseUserAddresses;

    @Inject
    WalletService walletService;

    @Inject
    AccountDao accountDao;

    @Inject
    AddressDao addressDao;

    @Inject
    DepositDao depositDao;

    @Transactional
    public AccountBalanceInfo getBalanceInfo(UUID accountId)
    {
        Account account = accountDao.find(accountId);
        if (account == null)
        {
            return new AccountBalanceInfo(0L, 0L, 0L);
        }
        return new AccountBalanceInfo()
            .setConfirmedBalance(account.getBalance())
            .setWithdrawableBalance(Math.min(account.getBalance(), depositDao.getWithdrawableBalance()))
            .setUnconfirmedBalance(account.getPendingBalance());
    }

    @Transactional
    public String getDepositAddress(UUID accountId, DepositType type)
    {
        Account account = accountDao.findOrCreate(accountId);
        if (type == null)
        {
            type = defaultAddressType;
        }
        return addressDao.findOrCreate(account, type, !reuseUserAddresses).getAddress();
    }

    @Transactional
    public String getReturnAddress(UUID accountId)
    {
        Account account = accountDao.find(accountId);
        if (account == null)
        {
            return "";
        }
        return account.getReturnAddress();
    }

    @Transactional
    public void setReturnAddress(UUID accountId, String returnAddress)
    {
        // Prevent Possibility Of JSON-RPC Injection, Just In Case
        if (SUS_PATTERN.matcher(returnAddress).matches())
        {
            throw new InvalidAddressException();
        }
        ValidateAddressResult validateResult = walletService.validateAddress(returnAddress);
        if (!validateResult.isValid())
        {
            throw new InvalidAddressException();
        }
        Account account = accountDao.findOrCreate(accountId);
        account.setReturnAddress(returnAddress);
        accountDao.update(account);
    }
}