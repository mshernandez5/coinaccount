package com.mshernandez.vertconomy.vault;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.mshernandez.vertconomy.core.Vertconomy;

import org.bukkit.OfflinePlayer;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;

/**
 * This is the key to Vault support, at least to some degree.
 * <p>
 * If a plugin needs to transfer balances between players
 * through Vault, it might follow an approach
 * such as the following one taken by EssentialsX /pay command:
 * <p>
 * 1) Withdraw Entire Sending Player Balance
 * <br>
 * 2) Deposit Modified Sender Balance Back
 * <br>
 * 3) Withdraw Entire Receiving Player Balance
 * <br>
 * 4) Deposit Modified Receiver Balance Back
 * <p>
 * In a typical MC economy with fake money, this is no
 * issue as the operations should always succeed.
 * <p>
 * With Vertconomy, a player or server account
 * may not have the balances to cover one
 * of the operations.
 * <p>
 * This is problematic because the operations are
 * not transactional, if any one of them fail
 * the previous operations have already completed,
 * resulting in lost funds.
 * <p>
 * The solution taken by this class is a task which
 * accumulates Vault requests into groups which are
 * executed as a whole.
 * <p>
 * How are groups formed? Grouping works on
 * the assumption that related Vault requests will be
 * made as part of the same command or task.
 * <p>
 * Since both this task and other commands/tasks using
 * the Vault API run synchronously and this task is set
 * to run every tick there is a high chance that this
 * task will run in between each set of Vault API calls.
 * Of course, this is implementation dependent and far
 * from ideal but makes something possible that
 * otherwise couldn't be.
 * <p>
 * In a scenario where multiple unrelated sets of Vault
 * API calls are made before this task runs, the worst
 * case will be that one issue in either sets causes
 * both to fail but no dangerous changes should be made,
 * only causing confusion towards the mysterious failure.
 * <p>
 * Though this approach gains limited Vault support,
 * some plugins ignore the response types Vault
 * provides and simply expects the transaction to succeed.
 * This means that even though this plugin signals
 * failures as necessary some plugins will continue to
 * carry out their actions as if it succeeded and there
 * is nothing that can be done about that here.
 * <p>
 * This has unfortunate effects in situations like
 * Essentials Shop Signs, which will work fine in
 * situations where funds are available but if the
 * player can't afford to buy or server can't afford
 * to sell the items will still be given/taken.
 * Funds, however, will remain unaffected as they should be.
 */
public class VaultRequestExecutor implements Runnable
{
    private Vertconomy vertconomy;

    private Map<OfflinePlayer, Long> playerBalanceChanges;
    private boolean changesValid;

    /**
     * Create a new task to queue balance changes
     * initiated by Vault requests.
     * 
     * @param vertconomy A Vertconomy instance.
     */
    public VaultRequestExecutor(Vertconomy vertconomy)
    {
        this.vertconomy = vertconomy;
        reset();
    }

    /**
     * Resets the thread to default state to queue
     * a new group.
     */
    public void reset()
    {
        playerBalanceChanges = new HashMap<>();
        changesValid = true;
    }

    /**
     * Attempts to queue a balance change for the specified player.
     * 
     * @param player The player.
     * @param balanceChange The amount to change the player balance by.
     * @return True if the change is expected to succeed.
     */
    public boolean queueChange(OfflinePlayer player, long balanceChange)
    {
        // Don't Queue If Group Is Already Invalid
        if (!changesValid)
        {
            return false;
        }
        // Queue Change
        long netBalanceChange = balanceChange + playerBalanceChanges.getOrDefault(player, 0L);
        if (netBalanceChange == 0L)
        {
            playerBalanceChanges.remove(player);
        }
        else
        {
            playerBalanceChanges.put(player, netBalanceChange);
        }
        // Make Sure Each Player Can Afford Queued Changes
        long serverBalanceChange = 0L;
        for (OfflinePlayer p : playerBalanceChanges.keySet())
        {
            long change = playerBalanceChanges.get(p);
            if (change < 0L && vertconomy.getPlayerBalance(p) < Math.abs(change))
            {
                changesValid = false;
                break;
            }
            serverBalanceChange -= change;
        }
        // Make Sure Server Account Can Afford Total Changes
        if (serverBalanceChange < 0L && vertconomy.getServerBalance() < Math.abs(serverBalanceChange))
        {
            changesValid = false;
        }
        return changesValid;
    }

    /**
     * Continually checks the change queue
     * and executes or clears it depending
     * on whether the changes are valid.
     */
    @Override
    public void run()
    {
        if (!playerBalanceChanges.isEmpty())
        {
            if (changesValid)
            {
                executeGroup();
            }
            reset();
        }
    }

    /**
     * Executes the current group of changes
     * and notifies players if the changes
     * were successful.
     */
    private void executeGroup()
    {
        // Execute Changes
        if (vertconomy.batchTransfer(playerBalanceChanges))
        {
            // If Successful, Create Notification Indicating Changes
            ComponentBuilder cb = new ComponentBuilder()
                .append("\n----- Executing Balance Changes -----").color(ChatColor.GOLD);
            for (Entry<OfflinePlayer, Long> e : playerBalanceChanges.entrySet())
            {
                ChatColor color = e.getValue() < 0 ? ChatColor.RED : ChatColor.GREEN;
                cb.append("\n").color(color)
                    .append(e.getKey().getName())
                    .append(": ")
                    .append(vertconomy.getFormatter().format(e.getValue()));
            }
            cb.append("\n-------- End Balance Changes --------\n").color(ChatColor.GOLD);
            BaseComponent[] changeNotification = cb.create();
            // Notify All Online Players Involved
            for (OfflinePlayer p : playerBalanceChanges.keySet())
            {
                if (p.isOnline())
                {
                    p.getPlayer().spigot().sendMessage(changeNotification);
                }
            }
        }
    }
}