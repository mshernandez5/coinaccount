package com.mshernandez.vertconomy.commands;

import com.mshernandez.vertconomy.core.Pair;
import com.mshernandez.vertconomy.core.Vertconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/**
 * /balance
 * <ul>
 *     <li>Allows user to check their balance.</li>
 *     <li>Allows console to check server-owned wallet balance.</li>
 *     <li>Allows console to check the balance of a specific player.</li>
 * </ul>
 */
public class CommandBalance implements CommandExecutor
{
    private Vertconomy vertconomy;

    public CommandBalance(Vertconomy vertconomy)
    {
        this.vertconomy = vertconomy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        StringBuilder message = new StringBuilder();
        if (sender instanceof Player)
        {
            Pair<Long, Long> balances = vertconomy.getPlayerBalances(((Player) sender));
            message.append(ChatColor.RED);
            message.append("Balance: ");
            message.append(ChatColor.GREEN);
            message.append(vertconomy.format(balances.getKey()));
            if (balances.getVal() != 0L)
            {
                message.append(ChatColor.GRAY);
                message.append(" (Pending: ");
                message.append(vertconomy.format(balances.getVal()));
                message.append(")");
            }
        }
        else if (sender instanceof ConsoleCommandSender)
        {
            if (args.length == 1)
            {
                OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
                if (player.hasPlayedBefore())
                {
                    Pair<Long, Long> balances = vertconomy.getPlayerBalances(player);
                    message.append(ChatColor.RED);
                    message.append("Balance: ");
                    message.append(ChatColor.GREEN);
                    message.append(vertconomy.format(balances.getKey()));
                    if (balances.getVal() != 0L)
                    {
                        message.append(ChatColor.GRAY);
                        message.append(" ( Pending: ");
                        message.append(vertconomy.format(balances.getVal()));
                        message.append(" )");
                    }
                }
                else
                {
                    message.append(ChatColor.RED);
                    message.append("Unknown Player: ");
                    message.append(ChatColor.DARK_RED);
                    message.append(args[0]);
                }
            }
            else
            {
                message.append(ChatColor.RED);
                message.append("Server-Owned Balance: ");
                message.append(ChatColor.GREEN);
                // TODO: message.append(vertconomy.format(vertconomy.getServerAccountBalance()));
            }
        }
        else
        {
            message.append(ChatColor.DARK_RED);
            message.append("UNSUPPORTED");
        }
        sender.sendMessage(message.toString());
        return true;
    }
}