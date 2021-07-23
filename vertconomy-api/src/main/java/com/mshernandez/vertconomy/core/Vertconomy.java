package com.mshernandez.vertconomy.core;

import java.util.Map;

import com.mshernandez.vertconomy.core.response.WithdrawRequestResponse;
import com.mshernandez.vertconomy.core.util.SatAmountFormatter;

import org.bukkit.OfflinePlayer;

/**
 * The duct tape binding Minecraft and cryptocurrency
 * together.
 * <p>
 * Makes methods available to transfer cryptocurrency
 * between players.
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
     * Cancel any expired withdraw requests.
     */
    public void cancelExpiredRequests();

    /**
     * Return the usable balance held by the server
     * account.
     * 
     * @return The balance associated with the server account.
     */
    public long getServerBalance();

    /**
     * Return the withdrawable balance held by the server
     * account.
     * 
     * @return The withdrawable balance associated with the server account.
     */
    public long getServerWithdrawableBalance();

    /**
     * Return the total unconfirmed balances associated
     * with the server account.
     * 
     * @return Unconfirmed deposit balances for the server account.
     */
    public long getServerUnconfirmedBalance();

    /**
     * Get the public wallet address allowing funds to be
     * given to the server account.
     * 
     * @return The deposit address associated with the server account.
     */
    public String getServerDepositAddress();

    /**
     * Initiate a withdraw request by the server.
     * 
     * @param destAddress The address to withdraw to.
     * @param amount The amount to withdraw, or -1L for all.
     * @return A response object for the withdraw attempt.
     */
    public WithdrawRequestResponse initiateServerWithdrawRequest(String destAddress, long amount);

    /**
     * Cancels any active withdraw request initiated by the server.
     * 
     * @return True if the request was found and canceled.
     */
    public boolean cancelServerWithdrawRequest();

    /**
     * Completes any active withdraw request initiated by the server.
     * 
     * @return The TXID of the withdraw transaction, or null if no request was found.
     */
    public String completeServerWithdrawRequest();

    /**
     * Checks whether the server has an active withdraw request.
     * 
     * @return True if an active withdraw request exists for the server.
     */
    public boolean checkIfServerHasWithdrawRequest();

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
     * Initiate a withdraw request by the user.
     * 
     * @param player The player that initiated the request.
     * @param destAddress The address to withdraw to.
     * @param amount The amount to withdraw, or -1L for all.
     * @return A response object for the withdraw attempt.
     */
    public WithdrawRequestResponse initiatePlayerWithdrawRequest(OfflinePlayer player, String destAddress, long amount);

    /**
     * Cancels any active withdraw request initiated by the user.
     * 
     * @param player The player that initiated the request.
     * @return True if the request was found and canceled.
     */
    public boolean cancelPlayerWithdrawRequest(OfflinePlayer player);

    /**
     * Completes any active withdraw request initiated by the user.
     * 
     * @param player The player that initiated the request.
     * @return The TXID of the withdraw transaction, or null if no request was found.
     */
    public String completePlayerWithdrawRequest(OfflinePlayer player);

    /**
     * Checks whether the player has an active withdraw request.
     * 
     * @param player The player to check.
     * @return True if an active withdraw request exists for the player.
     */
    public boolean checkIfPlayerHasWithdrawRequest(OfflinePlayer player);

    /**
     * Transfer the given amount from one player
     * to another.
     *  
     * @param sender The player sending funds.
     * @param receiver The player receiving funds.
     * @param amount The amount to transfer.
     * @return True if the transfer was successful.
     */
    public boolean transferPlayerBalance(OfflinePlayer sender, OfflinePlayer receiver, long amount);

    /**
     * Gives player funds to the server account.
     * 
     * @param player The player to take funds from.
     * @param amount The amount, in sats, to send to the server account.
     * @return True if the transfer was successful.
     */
    public boolean moveToServer(OfflinePlayer player, long amount);

    /**
     * Gives server funds to the specified player.
     * 
     * @param player The player to give the taken funds to.
     * @param amount The amount, in sats, to take from the server.
     * @return True if the transfer was successful.
     */
    public boolean takeFromServer(OfflinePlayer player, long amount);

    /**
     * Specify multiple account balance changes
     * to make as one transaction, where the entire
     * batch will fail if any one change fails.
     * <p>
     * If the total of all balance changes is not
     * zero, then funds will be given or taken
     * from the server account to attempt to complete
     * the batch.
     * <p>
     * For example, using the following map:
     * <table>
     *    <tr>
     *       <td>Player</td>
     *       <td>Change</td>
     *    </tr>
     *    <tr>
     *       <td>#1</td>
     *       <td>-5L</td>
     *    </tr>
     *    <tr>
     *       <td>#2</td>
     *       <td>10L</td>
     *    </tr>
     * </table>
     * Then player #2 will receive 5 sats from player #1 as well
     * as an additional 5 sats from the server account, assuming
     * player #1 and the server account have the balances to
     * complete this operation.
     * 
     * @param changes A map of players to account balance changes.
     * @return True if the changes were successfully executed.
     */
    public boolean batchTransfer(Map<OfflinePlayer, Long> changes);

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
    public SatAmountFormatter getFormatter();
}