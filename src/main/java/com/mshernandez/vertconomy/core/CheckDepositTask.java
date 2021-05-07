package com.mshernandez.vertconomy.core;

import java.util.logging.Logger;

import com.mshernandez.vertconomy.database.DepositAccount;
import com.mshernandez.vertconomy.wallet_interface.ResponseError;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * A task run periodically to check for new player
 * deposits.
 */
public class CheckDepositTask implements Runnable
{
    private Vertconomy vertconomy;

    public CheckDepositTask(Vertconomy vertconomy)
    {
        this.vertconomy = vertconomy;
    }

    @Override
    public void run()
    {
        // Get Logger
        Logger logger = vertconomy.getPlugin().getLogger();
        // Don't Attempt To Check For Deposits If Wallet Unreachable
        ResponseError error = vertconomy.checkWalletConnection();
        if (error != null)
        {
            logger.warning("Wallet Request Error, Can't Check For Deposits: " + error.message);
            return;
        }
        // Only Check Deposits For Online Players
        for (Player p : Bukkit.getOnlinePlayers())
        {
            DepositAccount account = vertconomy.getOrCreateUserAccount(p.getUniqueId());
            Pair<Long, Long> changes = vertconomy.registerNewDeposits(account);
            if (changes.getKey() != 0L)
            {
                StringBuilder message = new StringBuilder();
                message.append(ChatColor.BLUE);
                message.append("[Vertconomy] Processed Deposits: ");
                message.append(ChatColor.GREEN);
                message.append(vertconomy.format(changes.getKey()));
                p.sendMessage(message.toString());
            }
        }
    }
}
