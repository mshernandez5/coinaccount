package com.mshernandez.coinaccount.service;

import java.util.UUID;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import com.mshernandez.coinaccount.dao.AccountDao;
import com.mshernandez.coinaccount.entity.Account;
import com.mshernandez.coinaccount.service.exception.InvalidAddressException;
import com.mshernandez.coinaccount.service.result.AccountBalanceInfo;
import com.mshernandez.coinaccount.service.wallet_rpc.WalletService;
import com.mshernandez.coinaccount.service.wallet_rpc.result.ValidateAddressResult;

@ApplicationScoped
public class AccountInfoService
{
    // Match sus Address Patterns
    private static final Pattern SUS_PATTERN = Pattern.compile(".*[^a-zA-Z0-9].*");
    
    @Inject
    WalletService walletService;

    @Inject
    AccountDao accountDao;

    @Transactional
    public AccountBalanceInfo getBalanceInfo(UUID accountId)
    {
        Account account = accountDao.find(accountId);
        if (account == null)
        {
            return new AccountBalanceInfo(0L, 0L, 0L);
        }
        return new AccountBalanceInfo()
            .setConfirmedBalance(account.calculateBalance())
            .setWithdrawableBalance(account.calculateWithdrawableBalance())
            .setUnconfirmedBalance(account.getPendingBalance());
    }

    @Transactional
    public String getDepositAddress(UUID accountId)
    {
        Account account = accountDao.findOrCreate(accountId);
        return account.getDepositAddress();
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