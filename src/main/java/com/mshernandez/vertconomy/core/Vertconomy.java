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
public class Vertconomy
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
    Vertconomy(Logger logger, RPCWalletConnection wallet, VertconomyConfiguration config,
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

    /**
     * Returns true if Vertconomy can make
     * successful requests to the wallet.
     * 
     * @return True if a wallet connection can be reached.
     */
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

    /**
     * Check for any new unprocessed UTXOs.
     * Register new UTXOs and allocate their
     * funds appropriately.
     * <p>
     * Processes both user deposits and withdraw
     * transaction change.
     */
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

    /**
     * Vault API Compatibility Use ONLY
     * <p>
     * Moves portions of a player's balance into
     * a temporary transfer fund where it will be
     * pending a move to another player's account
     * or into the server account fund if unclaimed.
     * <p>
     * The temporary transfer fund gives a set amount
     * of time for the funds to be reclaimed, which is
     * required for plugins which intend to conduct transfers
     * by burning sender balances and minting new currency
     * for the receiver.
     * 
     * @param player The player to take funds from.
     * @param amount The amount to send to the transfer fund.
     * @return True if the transfer was successful.
     */
    public boolean moveToTransferFund(OfflinePlayer player, double amount)
    {
        if (!Bukkit.isPrimaryThread())
        {
            logger.warning("Cannot Support Asynchronous Vault API Requests");
            return false;
        }
        long satAmount = formatter.absoluteAmount(amount);
        // Note: Need To Allow 0 Value Withdraw To Support Many Plugins, ex. Essentials
        if (satAmount < 0L)
        {
            return false;
        }
        return transferService.transferBalance(player.getUniqueId(), VertconomyConfiguration.TRANSFER_ACCOUNT_UUID, satAmount);
    }

    /**
     * Vault API Compatibility Use ONLY
     * <p>
     * Reclaim a balance from the temporary transfer
     * fund.
     * 
     * @param player The player to give the taken funds to.
     * @param amount The amount to take from the transfer fund.
     * @return True if the transfer was successful.
     */
    public boolean takeFromTransferFund(OfflinePlayer player, double amount)
    {
        if (!Bukkit.isPrimaryThread())
        {
            logger.warning("Cannot Support Asynchronous Vault API Requests");
            return false;
        }
        Account transferAccount = accountDao.findOrCreate(VertconomyConfiguration.TRANSFER_ACCOUNT_UUID);
        long satAmount = formatter.absoluteAmount(amount);
        // Note: Need To Allow 0 Value Deposit To Support Many Plugins, ex. Essentials
        if (satAmount < 0L)
        {
            return false;
        }
        satAmount = Math.min(satAmount, transferAccount.calculateBalance()); // TODO: temporary
        return transferService.transferBalance(VertconomyConfiguration.TRANSFER_ACCOUNT_UUID, player.getUniqueId(), satAmount);
    }

    /**
     * Return the useable balance held by the player's
     * account.
     * 
     * @param player The player associated with the account.
     * @return The balance associated with the account.
     */
    public long getPlayerBalance(OfflinePlayer player)
    {
        Account playerAccount = accountDao.findOrCreate(player.getUniqueId());
        return playerAccount == null ? 0L : playerAccount.calculateBalance();
    }

    /**
     * Return the withdrawable balance held by the player's
     * account.
     * 
     * @param player The player associated with the account.
     * @return The withdrawable balance associated with the account.
     */
    public long getPlayerWithdrawableBalance(OfflinePlayer player)
    {
        Account playerAccount = accountDao.findOrCreate(player.getUniqueId());
        return playerAccount == null ? 0L : playerAccount.calculateWithdrawableBalance();
    }

    /**
     * Return the total unconfirmed balances associated
     * with the player's account.
     * 
     * @param player The player associated with the account.
     * @return Unconfirmed deposit balances for the account.
     */
    public long getPlayerUnconfirmedBalance(OfflinePlayer player)
    {
        Account playerAccount = accountDao.findOrCreate(player.getUniqueId());
        return playerAccount == null ? 0L : playerAccount.getPendingBalance();
    }

    /**
     * Get the public wallet address allowing the player to
     * deposit funds into their account.
     * 
     * @param player The player associated with the account.
     * @return The deposit address associated with the account.
     */
    public String getPlayerDepositAddress(OfflinePlayer player)
    {
        Account playerAccount = accountDao.findOrCreate(player.getUniqueId());
        return playerAccount == null ? "ERROR" : playerAccount.getDepositAddress();
    }

    /**
     * Checks whether the player has an active withdraw request.
     * 
     * @param player The player to check.
     * @return True if an active withdraw request exists for the player.
     */
    public boolean checkIfPlayerHasWithdrawRequest(OfflinePlayer player)
    {
        Account playerAccount = accountDao.findOrCreate(player.getUniqueId());
        return playerAccount == null ? false : true;
    }

    /**
     * Initiate a withdraw request by the user.
     * 
     * @param player The player that initiated the request.
     * @param destAddress The address to withdraw to.
     * @param amount The amount to withdraw, or -1L for all.
     * @return A response object for the withdraw attempt.
     */
    public WithdrawRequestResponse initiatePlayerWithdrawRequest(OfflinePlayer player, String destAddress, long amount)
    {
        return withdrawService.initiateWithdraw(player.getUniqueId(), destAddress, amount);
    }

    /**
     * Completes any active withdraw request initiated by the user.
     * 
     * @param player The player that initiated the request.
     * @return The TXID of the withdraw transaction, or null if no request was found.
     */
    public String completePlayerWithdrawRequest(OfflinePlayer player)
    {
        return withdrawService.completeWithdraw(player.getUniqueId());
    }

    /**
     * Cancels any active withdraw request initiated by the user.
     * 
     * @param player The player that initiated the request.
     * @return True if the request was found and canceled.
     */
    public boolean cancelPlayerWithdrawRequest(OfflinePlayer player)
    {
        return withdrawService.cancelWithdraw(player.getUniqueId());
    }

    /**
     * Get the minimum number of confirmations required
     * for Vertconomy to consider a deposit valid.
     * 
     * @return The minimum number of confirmations to consider a deposit valid.
     */
    public int getMinDepositConfirmations()
    {
        return config.getMinDepositConfirmations();
    }

    /**
     * Get the minimum number of confirmations required
     * for Vertconomy to consider change UTXOs valid.
     * 
     * @return The minimum number of confirmations to use change.
     */
    public int getMinChangeConfirmations()
    {
        return config.getMinChangeConfirmations();
    }

    /**
     * Get a configured formatter to format and parse sat amounts.
     * 
     * @return A formatter for this Vertconomy instance.
     */
    public SatAmountFormat getFormatter()
    {
        return formatter;
    }
}