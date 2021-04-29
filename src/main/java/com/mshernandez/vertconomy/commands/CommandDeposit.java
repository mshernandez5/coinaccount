package com.mshernandez.vertconomy.commands;

import com.mshernandez.vertconomy.Vertconomy;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /deposit
 * 
 * - Informs players how to deposit VTC to their accounts.
 * - Does not actually have to start any processes, the server
 *   will always check for new deposits when looking up player balances.
 */
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
        StringBuilder message = new StringBuilder();
        if (sender instanceof Player)
        {
            message.append('\n');
            message.append(ChatColor.AQUA);
            message.append("Deposit ");
            message.append(ChatColor.GREEN);
            message.append(vertconomy.getSymbol());
            message.append(ChatColor.RESET);
            message.append('\n');
            message.append("1) ");
            message.append(ChatColor.AQUA);
            message.append("Deposit To: ");
            message.append(ChatColor.GREEN);
            message.append(vertconomy.getDepositAddress(((Player) sender).getUniqueId()));
            message.append(ChatColor.RESET);
            message.append('\n');
            message.append("2) ");
            message.append(ChatColor.YELLOW);
            message.append("Wait For Your Transaction To Reach ");
            message.append(ChatColor.LIGHT_PURPLE);
            message.append(vertconomy.getMinimumConfirmations());
            message.append(ChatColor.YELLOW);
            message.append(" Confirmations");
            message.append(ChatColor.RESET);
            message.append('\n');
        }
        else
        {
            message.append(ChatColor.DARK_RED);
            message.append("Unsupported");
        }
        sender.sendMessage(message.toString());
        return true;
    }
}