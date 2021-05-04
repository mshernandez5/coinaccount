package com.mshernandez.vertconomy.commands;

import com.mshernandez.vertconomy.Vertconomy;
import com.mshernandez.vertconomy.wallet_interface.ResponseError;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * /vertconomy
 * 
 * - Shows whether the plugin is able to make a connection
 *   with the wallet over RPC.
 * - Gives general plugin information.
 */
public class CommandVertconomy implements CommandExecutor
{
    private Vertconomy vertconomy;

    public CommandVertconomy(Vertconomy vertconomy)
    {
        this.vertconomy = vertconomy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        ResponseError walletError = vertconomy.getWalletError();
        StringBuilder message = new StringBuilder();
        message.append('\n');
        message.append(ChatColor.DARK_GREEN);
        message.append(ChatColor.UNDERLINE);
        message.append("Vertconomy Status");
        message.append(ChatColor.RESET);
        message.append('\n');
        message.append(ChatColor.GOLD);
        message.append("Wallet Connection: ");
        if (walletError != null)
        {
            if (walletError.code == 0)
            {
                message.append(ChatColor.DARK_RED);
                message.append("WALLET CANNOT BE REACHED");
            }
            else
            {
                message.append(ChatColor.YELLOW);
                message.append("Wallet Not Ready");
            }
        }
        else
        {
            message.append(ChatColor.GREEN);
            message.append("Connected");
        }
        message.append(ChatColor.RESET);
        message.append("\n\n");
        message.append(ChatColor.DARK_GREEN);
        message.append(ChatColor.UNDERLINE);
        message.append("Author");
        message.append(ChatColor.RESET);
        message.append('\n');
        message.append(ChatColor.BLUE);
        message.append("https://github.com/mshernandez5");
        message.append(ChatColor.RESET);
        message.append("\n\n");
        message.append(ChatColor.DARK_GREEN);
        message.append(ChatColor.UNDERLINE);
        message.append("Official Repository");
        message.append(ChatColor.RESET);
        message.append('\n');
        message.append(ChatColor.BLUE);
        message.append("https://github.com/mshernandez5/vertconomy");
        message.append('\n');

        sender.sendMessage(message.toString());
        return true;
    }
}