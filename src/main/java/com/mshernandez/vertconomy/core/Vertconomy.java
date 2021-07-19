package com.mshernandez.vertconomy.core;

import java.util.UUID;

import javax.persistence.EntityManager;

import com.mshernandez.vertconomy.core.account.Account;
import com.mshernandez.vertconomy.core.account.AccountRepository;
import com.mshernandez.vertconomy.core.account.DepositAccount;
import com.mshernandez.vertconomy.core.deposit.DepositHelper;
import com.mshernandez.vertconomy.core.transfer.TransferHelper;
import com.mshernandez.vertconomy.core.withdraw.WithdrawHelper;
import com.mshernandez.vertconomy.core.withdraw.WithdrawRequest;
import com.mshernandez.vertconomy.core.withdraw.WithdrawRequestResponse;
import com.mshernandez.vertconomy.wallet_interface.RPCWalletConnection;
import com.mshernandez.vertconomy.wallet_interface.exceptions.WalletRequestException;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;

/**
 * The core of the plugin, the duct tape
 * bonding Minecraft and Vertcoin together.
 * <p>
 * Delegates most responsibilities to helper
 * objects.
 */
public class Vertconomy
{
    // Account For Balances Owned By The Server Operators
    private static final UUID SERVER_ACCOUNT_UUID = UUID.fromString("a8a73687-8f8b-4199-8078-36e676f32d8f");

    // Account Allowing Intermediate Transfers For Vault Compatibility
    private static final UUID TRANSFER_ACCOUNT_UUID = UUID.fromString("ced87bc1-4730-41e1-955b-c4c45b4e9ccf");

    // Account To Hold Funds For Pending Withdrawals & Receive Change Transactions
    private static final UUID WITHDRAW_ACCOUNT_UUID = UUID.fromString("884b2231-6c7a-4db5-b022-1cc5aeb949a8");

    // Plugin
    private Plugin plugin;

    // Database Persistence
    EntityManager entityManager;
    
    // RPC Wallet API - Connection To The Wallet
    private RPCWalletConnection wallet;

    // Sat Amount Formatter - Utilities To Format & Parse Sat Amounts
    private SatAmountFormat formatter;

    // Account Repository - Lookup & Create Accounts
    private AccountRepository accountRepository;

    // Deposit Helper - Processes UTXOs Into Deposits
    private DepositHelper depositHelper;

    // Withdraw Helper - Helps Withdraw Deposit Balances
    private WithdrawHelper withdrawHelper;

    // Transfer Helper - Helps Transfer Balances Between Accounts
    private TransferHelper transferHelper;

    /**
     * Please use <code>VertconomyBuilder</code> to create Vertconomy instances.
     * <p>
     * Create an instance of Vertconomy.
     */
    Vertconomy(Plugin plugin, RPCWalletConnection wallet,
               int minDepositConfirmations, int minChangeConfirmations, int targetBlockTime,
               String symbol, String baseUnitSymbol, CoinScale scale)
    {
        // Store Properties
        this.plugin = plugin;
        this.wallet = wallet;
        // Get Entity Manager For Persistence
        entityManager = JPAUtil.getEntityManager();
        // Initialize Account Repository
        accountRepository = new AccountRepository(plugin.getLogger(), wallet, entityManager);
        // Initialize Helper Objects
        depositHelper = new DepositHelper(plugin.getLogger(), wallet, entityManager,accountRepository,
                                          WITHDRAW_ACCOUNT_UUID, minDepositConfirmations,
                                          minChangeConfirmations);
        withdrawHelper = new WithdrawHelper(plugin.getLogger(), wallet, entityManager, accountRepository,
                                            WITHDRAW_ACCOUNT_UUID, targetBlockTime);
        transferHelper = new TransferHelper(plugin.getLogger(), entityManager);
        formatter = new SatAmountFormat(scale, symbol, baseUnitSymbol);
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
            plugin.getLogger().warning("Wallet not currently available, cannot check for deposits!");
            return;
        }
        // Only Check Deposits For Online Players
        for (Player player : Bukkit.getOnlinePlayers())
        {
            DepositAccount account = accountRepository.getOrCreateUserAccount(player.getUniqueId());
            long addedBalance = depositHelper.registerNewDeposits(account);
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
        depositHelper.registerChangeDeposits();
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
            plugin.getLogger().warning("Cannot Support Asynchronous Vault API Requests");
            return false;
        }
        Account transferAccount = accountRepository.getOrCreateHoldingAccount(TRANSFER_ACCOUNT_UUID);
        DepositAccount sender = accountRepository.getOrCreateUserAccount(player.getUniqueId());
        long satAmount = formatter.absoluteAmount(amount);
        // Note: Need To Allow 0 Value Withdraw To Support Many Plugins, ex. Essentials
        if (satAmount < 0L)
        {
            return false;
        }
        return transferHelper.transferBalance(sender, transferAccount, satAmount);
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
            plugin.getLogger().warning("Cannot Support Asynchronous Vault API Requests");
            return false;
        }
        Account transferAccount = accountRepository.getOrCreateHoldingAccount(TRANSFER_ACCOUNT_UUID);
        DepositAccount receiver = accountRepository.getOrCreateUserAccount(player.getUniqueId());
        long satAmount = formatter.absoluteAmount(amount);
        // Note: Need To Allow 0 Value Deposit To Support Many Plugins, ex. Essentials
        if (satAmount < 0L)
        {
            return false;
        }
        satAmount = Math.min(satAmount, transferAccount.calculateBalance()); // TODO: temporary
        return transferHelper.transferBalance(transferAccount, receiver, satAmount);
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
        DepositAccount playerAccount = accountRepository.getOrCreateUserAccount(player.getUniqueId());
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
        DepositAccount playerAccount = accountRepository.getOrCreateUserAccount(player.getUniqueId());
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
        DepositAccount playerAccount = accountRepository.getOrCreateUserAccount(player.getUniqueId());
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
        DepositAccount playerAccount = accountRepository.getOrCreateUserAccount(player.getUniqueId());
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
        DepositAccount playerAccount = accountRepository.getOrCreateUserAccount(player.getUniqueId());
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
        DepositAccount playerAccount = accountRepository.getOrCreateUserAccount(player.getUniqueId());
        return withdrawHelper.initiateWithdraw(playerAccount, destAddress, amount);
    }

    /**
     * Completes any active withdraw request initiated by the user.
     * 
     * @param player The player that initiated the request.
     * @return The TXID of the withdraw transaction, or null if no request was found.
     */
    public String completePlayerWithdrawRequest(OfflinePlayer player)
    {
        DepositAccount playerAccount = accountRepository.getOrCreateUserAccount(player.getUniqueId());
        WithdrawRequest withdrawRequest = playerAccount.getWithdrawRequest();
        return withdrawRequest == null ? null : withdrawHelper.completeWithdraw(withdrawRequest);
    }

    /**
     * Cancels any active withdraw request initiated by the user.
     * 
     * @param player The player that initiated the request.
     * @return True if the request was found and canceled.
     */
    public boolean cancelPlayerWithdrawRequest(OfflinePlayer player)
    {
        DepositAccount playerAccount = accountRepository.getOrCreateUserAccount(player.getUniqueId());
        WithdrawRequest withdrawRequest = playerAccount.getWithdrawRequest();
        if (withdrawRequest == null)
        {
            return false;
        }
        withdrawHelper.cancelWithdraw(withdrawRequest);
        return true;
    }

    /**
     * Get the minimum number of confirmations required
     * for Vertconomy to consider a deposit valid.
     * 
     * @return The minimum number of confirmations to consider a deposit valid.
     */
    public int getMinDepositConfirmations()
    {
        return depositHelper.getMinDepositConfirmations();
    }

    /**
     * Get the minimum number of confirmations required
     * for Vertconomy to consider change UTXOs valid.
     * 
     * @return The minimum number of confirmations to use change.
     */
    public int getMinChangeConfirmations()
    {
        return depositHelper.getMinChangeConfirmations();
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