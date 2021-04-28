package com.mshernandez.vertconomy.commands;

import com.mshernandez.vertconomy.Vertconomy;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandDeposit implements CommandExecutor
{
    private Vertconomy vertconomy;

    public CommandDeposit(Vertconomy vertconomy)
    {
        this.vertconomy = vertconomy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        String message;
        if (sender instanceof Player)
        {
            message = ChatColor.RED + "Deposit To: " + ChatColor.GREEN
                + vertconomy.getDepositAddress(((Player) sender).getUniqueId());
        }
        else
        {
            message = ChatColor.DARK_RED + "Unsupported";
        }
        sender.sendMessage(message);
        return true;
    }
}