package com.mshernandez.vertconomy.commands;

import java.util.ArrayList;
import java.util.List;

import com.mshernandez.vertconomy.core.InvalidSatAmountException;
import com.mshernandez.vertconomy.core.Vertconomy;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 *  /withdraw
 *  <ul>
 *      <li>Allows players to withdraw VTC from their accounts.</li>
 *  </ul>
 */
public class CommandWithdraw implements CommandExecutor, TabCompleter
{
    private Vertconomy vertconomy;

    public CommandWithdraw(Vertconomy vertconomy)
    {
        this.vertconomy = vertconomy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        TextComponent messageComponent = new TextComponent();
        StringBuilder message = new StringBuilder();
        if (sender instanceof Player)
        {
            if (args.length != 1)
            {
                return false;
            }
            if (args[0].equals("all"))
            {
                // Withdraw All
            }
            if (args[0].equals("confirm"))
            {
                // Confirm Pending Withdrawal
            }
            if (args[0].equals("cancel"))
            {
                // Cancel Pending Withdrawal
            }
            else
            {
                // Parse Specified Amount
                long satAmount = 0;
                try
                {
                    satAmount = vertconomy.getFormatter().parseSats(args[0]);
                }
                catch (InvalidSatAmountException e)
                {
                    sender.sendMessage(e.getMessage());
                    return false;
                }
                // Withdraw Amount
                
            }
        }
        else
        {
            message.append(ChatColor.DARK_RED);
            message.append("Unsupported");
        }

        messageComponent.addExtra(message.toString());
        sender.spigot().sendMessage(messageComponent);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        List<String> options = new ArrayList<>();
        if (args.length == 0)
        {
            options.add("<amount>");
            options.add("all");
            options.add("confirm");
            options.add("cancel");
        }
        return options;
    }
}