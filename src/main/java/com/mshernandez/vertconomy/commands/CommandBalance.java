package com.mshernandez.vertconomy.commands;

import com.mshernandez.vertconomy.Vertconomy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

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
        String message;
        if (sender instanceof Player)
        {
            message = ChatColor.RED + "Balance: " + ChatColor.GREEN
                + vertconomy.format(vertconomy.getBalance(((Player) sender).getUniqueId()));
        }
        else if (sender instanceof ConsoleCommandSender)
        {
            if (args.length == 1)
            {
                OfflinePlayer player = Bukkit.getOfflinePlayer(args[0]);
                if (player.hasPlayedBefore())
                {
                    message = ChatColor.RED + "Balance: " + ChatColor.GREEN
                        + vertconomy.format(vertconomy.getBalance((player).getUniqueId()));
                }
                else
                {
                    message = ChatColor.RED + "Unknown Player: " + ChatColor.DARK_RED + args[0];
                }
            }
            else
            {
                message = ChatColor.RED + "Combined Server Balance: " + ChatColor.GREEN
                    + vertconomy.format(vertconomy.getCombinedWalletBalance());
            }
        }
        else
        {
            message = ChatColor.DARK_RED + "Unsupported";
        }
        sender.sendMessage(message);
        return true;
    }
}