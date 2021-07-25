package com.mshernandez.vertconomy.core;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.persist.PersistService;
import com.mshernandez.vertconomy.core.entity.Account;
import com.mshernandez.vertconomy.core.entity.AccountDao;
import com.mshernandez.vertconomy.core.response.WithdrawRequestResponse;
import com.mshernandez.vertconomy.core.service.DepositService;
import com.mshernandez.vertconomy.core.service.TransferService;
import com.mshernandez.vertconomy.core.service.WithdrawService;
import com.mshernandez.vertconomy.core.service.exception.InsufficientFundsException;
import com.mshernandez.vertconomy.core.util.SatAmountFormatter;
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

    private final SatAmountFormatter formatter;

    private final AccountDao accountDao;

    private final DepositService depositService;

    private final WithdrawService withdrawService;

    private final TransferService transferService;

    private final PersistService persistService;

    /**
     * Please use <code>VertconomyBuilder</code> to create Vertconomy instances.
     * <p>
     * Create an instance of Vertconomy.
     */
    @Inject
    VertconomyImpl(Logger logger, RPCWalletConnection wallet, VertconomyConfiguration config,
                   SatAmountFormatter formatter, AccountDao accountDao, DepositService depositService,
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
        this.persistService = persistService;
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
        // Check For Server Account Deposits
        long addedServerBalance = depositService.registerNewDeposits(VertconomyConfiguration.SERVER_ACCOUNT_UUID);
        if (addedServerBalance != 0L)
        {
            BaseComponent[] component = new ComponentBuilder()
                .append("[Vertconomy] Processed Server Deposits: ").color(ChatColor.BLUE)
                .append(formatter.format(addedServerBalance)).color(ChatColor.GREEN)
                .create();
            Bukkit.getConsoleSender().spigot().sendMessage(component);
        }
        // Check For Change Deposits
        depositService.registerChangeDeposits();
    }

    @Override
    public void cancelExpiredRequests()
    {
        Set<UUID> expiredRequestAccountIDs = withdrawService.cancelExpiredRequests();
        BaseComponent[] component = new ComponentBuilder()
                .append("[Vertconomy] ").color(ChatColor.BLUE)
                .append("Your withdraw request expired.").color(ChatColor.YELLOW)
                .create();
        for (UUID id : expiredRequestAccountIDs)
        {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline())
            {
                player.spigot().sendMessage(component);
            }
            else if (id.equals(VertconomyConfiguration.SERVER_ACCOUNT_UUID))
            {
                Bukkit.getConsoleSender().spigot().sendMessage(component);
            }
        }
    }

    @Override
    public long getServerBalance()
    {
        Account serverAccount = accountDao.findOrCreate(VertconomyConfiguration.SERVER_ACCOUNT_UUID);
        return serverAccount.calculateBalance();
    }

    @Override
    public long getServerWithdrawableBalance()
    {
        Account serverAccount = accountDao.findOrCreate(VertconomyConfiguration.SERVER_ACCOUNT_UUID);
        return serverAccount.calculateWithdrawableBalance();
    }

    @Override
    public long getServerUnconfirmedBalance()
    {
        Account serverAccount = accountDao.findOrCreate(VertconomyConfiguration.SERVER_ACCOUNT_UUID);
        return serverAccount.getPendingBalance();
    }

    @Override
    public String getServerDepositAddress()
    {
        Account serverAccount = accountDao.findOrCreate(VertconomyConfiguration.SERVER_ACCOUNT_UUID);
        return serverAccount.getDepositAddress();
    }

    @Override
    public WithdrawRequestResponse initiateServerWithdrawRequest(String destAddress, long amount)
    {
        return withdrawService.initiateWithdraw(VertconomyConfiguration.SERVER_ACCOUNT_UUID, destAddress, amount);
    }

    @Override
    public boolean cancelServerWithdrawRequest()
    {
        return withdrawService.cancelWithdraw(VertconomyConfiguration.SERVER_ACCOUNT_UUID);
    }

    @Override
    public String completeServerWithdrawRequest()
    {
        return withdrawService.completeWithdraw(VertconomyConfiguration.SERVER_ACCOUNT_UUID);
    }

    @Override
    public boolean checkIfServerHasWithdrawRequest()
    {
        Account serverAccount = accountDao.findOrCreate(VertconomyConfiguration.SERVER_ACCOUNT_UUID);
        return serverAccount.getWithdrawRequest() != null;
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
    public String getPlayerDepositAddress(OfflinePlayer player)
    {
        Account playerAccount = accountDao.findOrCreate(player.getUniqueId());
        return playerAccount == null ? "ERROR" : playerAccount.getDepositAddress();
    }

    @Override
    public String getPlayerReturnAddress(OfflinePlayer player)
    {
        Account playerAccount = accountDao.findOrCreate(player.getUniqueId());
        return playerAccount.getReturnAddress();
    }

    @Override
    public boolean setPlayerReturnAddress(OfflinePlayer player, String address)
    {
        try
        {
            if (!wallet.isAddressValid(address))
            {
                return false;
            }
        }
        catch (WalletRequestException e)
        {
            return false;
        }
        Account playerAccount = accountDao.findOrCreate(player.getUniqueId());
        playerAccount.setReturnAddress(address);
        return true;
    }

    @Override
    public WithdrawRequestResponse initiatePlayerWithdrawRequest(OfflinePlayer player, String destAddress, long amount)
    {
        return withdrawService.initiateWithdraw(player.getUniqueId(), destAddress, amount);
    }

    @Override
    public boolean cancelPlayerWithdrawRequest(OfflinePlayer player)
    {
        return withdrawService.cancelWithdraw(player.getUniqueId());
    }

    @Override
    public String completePlayerWithdrawRequest(OfflinePlayer player)
    {
        return withdrawService.completeWithdraw(player.getUniqueId());
    }

    @Override
    public boolean checkIfPlayerHasWithdrawRequest(OfflinePlayer player)
    {
        Account playerAccount = accountDao.findOrCreate(player.getUniqueId());
        return playerAccount != null;
    }

    @Override
    public boolean transferPlayerBalance(OfflinePlayer sender, OfflinePlayer receiver, long amount)
    {
        try
        {
            transferService.transferBalance(sender.getUniqueId(), receiver.getUniqueId(), amount);
        }
        catch (InsufficientFundsException e)
        {
            return false;
        }
        return true;
    }

    @Override
    public boolean moveToServer(OfflinePlayer player, long amount)
    {
        try
        {
            transferService.transferBalance(player.getUniqueId(), VertconomyConfiguration.SERVER_ACCOUNT_UUID, amount);
        }
        catch (InsufficientFundsException e)
        {
            return false;
        }
        return true;
    }

    @Override
    public boolean takeFromServer(OfflinePlayer player, long amount)
    {
        try
        {
            transferService.transferBalance(VertconomyConfiguration.SERVER_ACCOUNT_UUID, player.getUniqueId(), amount);
        }
        catch (InsufficientFundsException e)
        {
            return false;
        }
        return true;
    }

    @Override
    public boolean batchTransfer(Map<OfflinePlayer, Long> changes)
    {
        // Map From Account ID Instead Of Player
        Map<UUID, Long> idMapChanges = changes.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().getUniqueId(), e -> e.getValue()));
        // Attempt To Make Changes
        try
        {
            transferService.batchTransfer(idMapChanges);
        }
        catch (InsufficientFundsException e)
        {
            return false;
        }
        return true;
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
    public SatAmountFormatter getFormatter()
    {
        return formatter;
    }

    /**
     * End the persistence service used by this Vertconomy
     * implementation, only use on plugin disable.
     */
    public void endPersistenceService()
    {
        persistService.stop();
    }
}