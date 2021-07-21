package com.mshernandez.vertconomy.core;

import com.mshernandez.vertconomy.core.service.WithdrawRequestResponse;
import com.mshernandez.vertconomy.core.util.SatAmountFormat;

import org.bukkit.OfflinePlayer;

/**
 * The core of the plugin, the duct tape
 * bonding Minecraft and Vertcoin together.
 * <p>
 * Makes requests to internal service objects
 * based on calls from in-game commands, tasks.
 */
public interface Vertconomy
{
    /**
     * Returns true if Vertconomy can make
     * successful requests to the wallet.
     * 
     * @return True if a wallet connection can be reached.
     */
    public boolean hasWalletConnection();

    /**
     * Check for any new unprocessed UTXOs.
     * Register new UTXOs and allocate their
     * funds appropriately.
     * <p>
     * Processes both user deposits and withdraw
     * transaction change.
     */
    public void checkForNewDeposits();

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
    public boolean moveToTransferFund(OfflinePlayer player, double amount);

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
    public boolean takeFromTransferFund(OfflinePlayer player, double amount);

    /**
     * Return the useable balance held by the player's
     * account.
     * 
     * @param player The player associated with the account.
     * @return The balance associated with the account.
     */
    public long getPlayerBalance(OfflinePlayer player);

    /**
     * Return the withdrawable balance held by the player's
     * account.
     * 
     * @param player The player associated with the account.
     * @return The withdrawable balance associated with the account.
     */
    public long getPlayerWithdrawableBalance(OfflinePlayer player);

    /**
     * Return the total unconfirmed balances associated
     * with the player's account.
     * 
     * @param player The player associated with the account.
     * @return Unconfirmed deposit balances for the account.
     */
    public long getPlayerUnconfirmedBalance(OfflinePlayer player);

    /**
     * Get the public wallet address allowing the player to
     * deposit funds into their account.
     * 
     * @param player The player associated with the account.
     * @return The deposit address associated with the account.
     */
    public String getPlayerDepositAddress(OfflinePlayer player);

    /**
     * Checks whether the player has an active withdraw request.
     * 
     * @param player The player to check.
     * @return True if an active withdraw request exists for the player.
     */
    public boolean checkIfPlayerHasWithdrawRequest(OfflinePlayer player);

    /**
     * Initiate a withdraw request by the user.
     * 
     * @param player The player that initiated the request.
     * @param destAddress The address to withdraw to.
     * @param amount The amount to withdraw, or -1L for all.
     * @return A response object for the withdraw attempt.
     */
    public WithdrawRequestResponse initiatePlayerWithdrawRequest(OfflinePlayer player, String destAddress, long amount);

    /**
     * Completes any active withdraw request initiated by the user.
     * 
     * @param player The player that initiated the request.
     * @return The TXID of the withdraw transaction, or null if no request was found.
     */
    public String completePlayerWithdrawRequest(OfflinePlayer player);

    /**
     * Cancels any active withdraw request initiated by the user.
     * 
     * @param player The player that initiated the request.
     * @return True if the request was found and canceled.
     */
    public boolean cancelPlayerWithdrawRequest(OfflinePlayer player);

    /**
     * Get the minimum number of confirmations required
     * for Vertconomy to consider a deposit valid.
     * 
     * @return The minimum number of confirmations to consider a deposit valid.
     */
    public int getMinDepositConfirmations();

    /**
     * Get the minimum number of confirmations required
     * for Vertconomy to consider change UTXOs valid.
     * 
     * @return The minimum number of confirmations to use change.
     */
    public int getMinChangeConfirmations();

    /**
     * Get a configured formatter to format and parse sat amounts.
     * 
     * @return A formatter for this Vertconomy instance.
     */
    public SatAmountFormat getFormatter();
}