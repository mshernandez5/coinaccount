package com.mshernandez.vertconomy.commands;

import java.util.ArrayList;
import java.util.List;

import com.mshernandez.vertconomy.core.InvalidSatAmountException;
import com.mshernandez.vertconomy.core.Vertconomy;
import com.mshernandez.vertconomy.database.WithdrawRequest;

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
        if (args.length < 1)
        {
            return false;
        }
        TextComponent messageComponent = new TextComponent();
        StringBuilder message = new StringBuilder();
        if (sender instanceof Player)
        {
            if (args[0].equals("all"))
            {
                // Withdraw All
                if (args.length != 1)
                {
                    return false;
                }
                message.append(ChatColor.DARK_RED);
                message.append("Not currently supported!");
            }
            else if (args[0].equals("confirm"))
            {
                // Confirm Pending Withdrawal
                if (args.length != 1)
                {
                    return false;
                }
                // Get Withdraw Request
                WithdrawRequest request = vertconomy.getPlayerWithdrawRequest((Player) sender);
                if (request == null)
                {
                    message.append(ChatColor.DARK_RED);
                    message.append("No withdraw request was found.");
                }
                else
                {
                    // Broadcast TX
                    String txid = vertconomy.completeWithdraw(request);
                    if (txid == null)
                    {
                        message.append(ChatColor.DARK_RED);
                        message.append("There was an error processing your withdraw request.");
                    }
                    else
                    {
                        // Inform User
                        message.append("\n\n");
                        message.append(ChatColor.AQUA);
                        message.append(ChatColor.UNDERLINE);
                        message.append("Withdraw Request Completed:");
                        message.append(ChatColor.RESET);
                        message.append('\n');
                        message.append(ChatColor.YELLOW);
                        message.append("TXID: ");
                        message.append(ChatColor.GREEN);
                        message.append(ChatColor.BOLD);
                        // Copyable TXID
                        messageComponent.addExtra(message.toString());
                        TextComponent txidMsg = new TextComponent(txid);
                        txidMsg.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, txid));
                        txidMsg.setColor(ChatColor.GREEN);
                        messageComponent.addExtra(txidMsg);
                        message = new StringBuilder();
                        message.append(ChatColor.RESET);
                        message.append("\n\n");
                        message.append(ChatColor.YELLOW);
                        message.append("Allow some time for the transaction to be confirmed before making another withdraw request.");
                    }
                }
            }
            else if (args[0].equals("cancel"))
            {
                // Cancel Pending Withdrawal
                if (args.length != 1)
                {
                    return false;
                }
                // Get Withdraw Request
                WithdrawRequest request = vertconomy.getPlayerWithdrawRequest((Player) sender);
                if (request == null)
                {
                    message.append(ChatColor.DARK_RED);
                    message.append("No withdraw request was found.");
                }
                else
                {
                    vertconomy.cancelWithdraw(request);
                    message.append(ChatColor.YELLOW);
                    message.append("The withdraw request has been canceled.");
                }
            }
            else
            {
                if (args.length != 2)
                {
                    return false;
                }
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
                WithdrawRequest request = vertconomy.initiateWithdraw((Player) sender, satAmount, args[1]);
                if (request == null)
                {
                    message.append(ChatColor.DARK_RED);
                    message.append("Failed to withdraw the specified amount!");
                }
                else
                {
                    message.append("\n\n");
                    message.append(ChatColor.AQUA);
                    message.append(ChatColor.UNDERLINE);
                    message.append("Withdraw Request Created:");
                    message.append(ChatColor.RESET);
                    message.append('\n');
                    message.append(ChatColor.YELLOW);
                    message.append("Amount Excluding Fees: ");
                    message.append(ChatColor.GREEN);
                    message.append(ChatColor.BOLD);
                    message.append(vertconomy.getFormatter().format(request.getWithdrawAmount()));
                    message.append(ChatColor.RESET);
                    message.append('\n');
                    message.append(ChatColor.YELLOW);
                    message.append("Fees: ");
                    message.append(ChatColor.GREEN);
                    message.append(ChatColor.BOLD);
                    message.append(vertconomy.getFormatter().format(request.getFeeAmount()));
                    message.append(ChatColor.RESET);
                    message.append('\n');
                    message.append(ChatColor.YELLOW);
                    message.append("Total Cost: ");
                    message.append(ChatColor.RED);
                    message.append(ChatColor.BOLD);
                    message.append(vertconomy.getFormatter().format(request.getTotalCost()));
                    message.append(ChatColor.RESET);
                    message.append('\n');
                    message.append(ChatColor.YELLOW);
                    message.append("Withdraw Address: ");
                    message.append(ChatColor.RED);
                    message.append(ChatColor.BOLD);
                    message.append(args[1]);
                    message.append(ChatColor.RESET);
                    message.append("\n\n");
                    message.append(ChatColor.AQUA);
                    message.append("To ");
                    message.append(ChatColor.GREEN);
                    message.append("confirm");
                    message.append(ChatColor.AQUA);
                    message.append(" and send the withdrawal, use ");
                    message.append(ChatColor.GREEN);
                    message.append("/withdraw confirm");
                    message.append(ChatColor.RESET);
                    message.append('\n');
                    message.append(ChatColor.AQUA);
                    message.append("To ");
                    message.append(ChatColor.RED);
                    message.append("cancel");
                    message.append(ChatColor.AQUA);
                    message.append(" the withdrawal, use ");
                    message.append(ChatColor.RED);
                    message.append("/withdraw cancel");
                    message.append(ChatColor.RESET);
                    message.append("\n\n");
                    message.append(ChatColor.YELLOW);
                    message.append("This request will automatically be canceled if not confirmed soon.");
                }
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
        if (args.length == 1)
        {
            options.add("<amount>");
            options.add("all");
            options.add("confirm");
            options.add("cancel");
        }
        else if (args.length == 2)
        {
            options.add("<address>");
        }
        return options;
    }
}