package com.mshernandez.vertconomy.core;

import com.google.inject.AbstractModule;
import com.mshernandez.vertconomy.core.entity.AccountDao;
import com.mshernandez.vertconomy.core.entity.DepositDao;
import com.mshernandez.vertconomy.core.entity.JPAAccountDao;
import com.mshernandez.vertconomy.core.entity.JPADepositDao;
import com.mshernandez.vertconomy.core.entity.JPAWithdrawRequestDao;
import com.mshernandez.vertconomy.core.entity.WithdrawRequestDao;
import com.mshernandez.vertconomy.core.service.DepositService;
import com.mshernandez.vertconomy.core.service.TransferService;
import com.mshernandez.vertconomy.core.service.WithdrawService;
import com.mshernandez.vertconomy.core.util.SatAmountFormatter;
import com.mshernandez.vertconomy.core.util.VertconomyFormatter;
import com.mshernandez.vertconomy.wallet_interface.RPCWalletConnection;

/**
 * A Guice module to handle injection for Vertconomy services
 * and DAO objects.
 */
public class VertconomyInjectorModule extends AbstractModule
{
    private VertconomyConfiguration config;
    private RPCWalletConnection wallet;

    public VertconomyInjectorModule(RPCWalletConnection wallet, VertconomyConfiguration config)
    {
        this.config = config;
        this.wallet = wallet;
    }

    @Override
    public void configure()
    {
        // Inject Wallet Connection
        bind(RPCWalletConnection.class).toInstance(wallet);

        // Inject Configuration
        bind(VertconomyConfiguration.class).toInstance(config);

        // Inject Sat Formatter
        bind(SatAmountFormatter.class).toInstance(new VertconomyFormatter(config.getScale(), config.getSymbol(), config.getBaseUnitSymbol()));

        // Inject DAOs
        bind(AccountDao.class).to(JPAAccountDao.class);
        bind(DepositDao.class).to(JPADepositDao.class);
        bind(WithdrawRequestDao.class).to(JPAWithdrawRequestDao.class);

        // Inject Services
        bind(DepositService.class);
        bind(TransferService.class);
        bind(WithdrawService.class);

        // Vertconomy
        bind(Vertconomy.class).to(VertconomyImpl.class);
    }
}
