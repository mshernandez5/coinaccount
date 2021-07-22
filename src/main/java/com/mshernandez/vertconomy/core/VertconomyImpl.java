package com.mshernandez.vertconomy.core;

import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.persist.PersistService;
import com.mshernandez.vertconomy.core.entity.Account;
import com.mshernandez.vertconomy.core.entity.AccountDao;
import com.mshernandez.vertconomy.core.service.DepositService;
import com.mshernandez.vertconomy.core.service.TransferService;
import com.mshernandez.vertconomy.core.service.WithdrawRequestResponse;
import com.mshernandez.vertconomy.core.service.WithdrawService;
import com.mshernandez.vertconomy.core.util.SatAmountFormat;
import com.mshernandez.vertconomy.wallet_interface.RPCWalletConnection;
import com.mshernandez.vertconomy.wallet_interface.exceptions.WalletRequestException;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;

/**
 * The core of the plugin, the duct tape
 * bonding Minecraft and Vertcoin together.
 * <p>
 * Makes requests to internal service objects
 * based on calls from in-game commands, tasks.
 */
@Singleton
public class VertconomyImpl implements Vertconomy
{
    private final Logger logger;

    private final RPCWalletConnection wallet;

    private final VertconomyConfiguration config;

    private final SatAmountFormat formatter;

    private final AccountDao accountDao;

    private final DepositService depositService;

    private final WithdrawService withdrawService;

    private final TransferService transferService;

    /**
     * Please use <code>VertconomyBuilder</code> to create Vertconomy instances.
     * <p>
     * Create an instance of Vertconomy.
     */
    @Inject
    VertconomyImpl(Logger logger, RPCWalletConnection wallet, VertconomyConfiguration config,
               SatAmountFormat formatter, AccountDao accountDao, DepositService depositService,
               WithdrawService withdrawService, TransferService transferService,
               PersistService persistService)
    {
        this.logger = logger;
        this.wallet = wallet;
        this.config = config;
        this.formatter = formatter;
        this.accountDao = accountDao;
        this.depositService = depositService;
        this.withdrawService = withdrawService;
        this.transferService = transferService;
        persistService.start();
    }

    @Override
    public boolean hasWalletConnection()
    {
        try
        {
            wallet.getWalletInfo();
            return true;
        }
        catch (WalletRequestException e)
        {
            return false;
        }
    }

    @Override
    public void checkForNewDeposits()
    {
        // Don't Attempt To Check For Deposits If Wallet Unreachable
        if (!hasWalletConnection())
        {
            logger.warning("Wallet not currently available, cannot check for deposits!");
            return;
        }
        // Only Check Deposits For Online Players
        for (Player player : Bukkit.getOnlinePlayers())
        {
            long addedBalance = depositService.registerNewDeposits(player.getUniqueId());
            if (addedBalance != 0L)
            {
                BaseComponent[] component = new ComponentBuilder()
                    .append("[Vertconomy] Processed Deposits: ").color(ChatColor.BLUE)
                    .append(formatter.format(addedBalance)).color(ChatColor.GREEN)
                    .create();
                player.spigot().sendMessage(component);
            }
        }
        // Check For Change Deposits
        depositService.registerChangeDeposits();
    }

    @Override
    public long getServerBalance()
    {
        Account serverAccount = accountDao.findOrCreate(VertconomyConfiguration.SERVER_ACCOUNT_UUID);
        return serverAccount.calculateBalance();
    }

    @Override
    public long getWithdrawableServerBalance()
    {
        Account serverAccount = accountDao.findOrCreate(VertconomyConfiguration.SERVER_ACCOUNT_UUID);
        return serverAccount.calculateWithdrawableBalance();
    }

    @Override
    public boolean moveToServer(OfflinePlayer player, long amount)
    {
        return transferService.transferBalance(player.getUniqueId(), VertconomyConfiguration.SERVER_ACCOUNT_UUID, amount);
    }

    @Override
    public boolean takeFromServer(OfflinePlayer player, long amount)
    {
        return transferService.transferBalance(VertconomyConfiguration.SERVER_ACCOUNT_UUID, player.getUniqueId(), amount);
    }

    @Override
    public long getPlayerBalance(OfflinePlayer player)
    {
        Account playerAccount = accountDao.findOrCreate(player.getUniqueId());
        return playerAccount == null ? 0L : playerAccount.calculateBalance();
    }

    @Override
    public long getPlayerWithdrawableBalance(OfflinePlayer player)
    {
        Account playerAccount = accountDao.findOrCreate(player.getUniqueId());
        return playerAccount == null ? 0L : playerAccount.calculateWithdrawableBalance();
    }

    @Override
    public long getPlayerUnconfirmedBalance(OfflinePlayer player)
    {
        Account playerAccount = accountDao.findOrCreate(player.getUniqueId());
        return playerAccount == null ? 0L : playerAccount.getPendingBalance();
    }

    @Override
    public boolean transferPlayerBalance(OfflinePlayer sender, OfflinePlayer receiver, long amount)
    {
        return transferService.transferBalance(sender.getUniqueId(), receiver.getUniqueId(), amount);
    }

    @Override
    public String getPlayerDepositAddress(OfflinePlayer player)
    {
        Account playerAccount = accountDao.findOrCreate(player.getUniqueId());
        return playerAccount == null ? "ERROR" : playerAccount.getDepositAddress();
    }

    @Override
    public boolean checkIfPlayerHasWithdrawRequest(OfflinePlayer player)
    {
        Account playerAccount = accountDao.findOrCreate(player.getUniqueId());
        return playerAccount == null ? false : true;
    }

    @Override
    public WithdrawRequestResponse initiatePlayerWithdrawRequest(OfflinePlayer player, String destAddress, long amount)
    {
        return withdrawService.initiateWithdraw(player.getUniqueId(), destAddress, amount);
    }

    @Override
    public String completePlayerWithdrawRequest(OfflinePlayer player)
    {
        return withdrawService.completeWithdraw(player.getUniqueId());
    }

    @Override
    public boolean cancelPlayerWithdrawRequest(OfflinePlayer player)
    {
        return withdrawService.cancelWithdraw(player.getUniqueId());
    }

    @Override
    public int getMinDepositConfirmations()
    {
        return config.getMinDepositConfirmations();
    }

    @Override
    public int getMinChangeConfirmations()
    {
        return config.getMinChangeConfirmations();
    }

    @Override
    public SatAmountFormat getFormatter()
    {
        return formatter;
    }
}