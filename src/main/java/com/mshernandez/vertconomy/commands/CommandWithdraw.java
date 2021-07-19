package com.mshernandez.vertconomy.commands;

import java.util.ArrayList;
import java.util.List;

import com.mshernandez.vertconomy.core.InvalidSatAmountException;
import com.mshernandez.vertconomy.core.Vertconomy;
import com.mshernandez.vertconomy.core.withdraw.WithdrawRequestResponse;
import com.mshernandez.vertconomy.core.withdraw.WithdrawRequestResponseType;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

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
        if (sender instanceof Player)
        {
            Player player = (Player) sender;
            if (args[0].equals("confirm"))
            {
                // Confirm Pending Withdrawal
                if (args.length != 1)
                {
                    return false;
                }
                // Broadcast TX
                String txid = vertconomy.completePlayerWithdrawRequest(player);
                if (txid == null)
                {
                    BaseComponent[] component = new ComponentBuilder()
                        .append("No withdraw request was found.").color(ChatColor.DARK_RED)
                        .create();
                    sender.spigot().sendMessage(component);
                    return true;
                }
                else
                {
                    // Copyable TXID
                    TextComponent txidMsg = new TextComponent(txid);
                    txidMsg.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, txid));
                    txidMsg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to copy TXID!")));
                    // Build Message
                    BaseComponent[] component = new ComponentBuilder()
                        .append("\n")
                        .append("Withdraw Request Completed:").color(ChatColor.AQUA).underlined(true)
                        .append("\n\n").reset()
                        .append("TXID: ").color(ChatColor.YELLOW)
                        .append(txidMsg).color(ChatColor.GREEN).bold(true)
                        .append("\n\n").reset()
                        .append("Some of your remaining funds may be unavailable for withdraw until the transaction is confirmed.")
                        .color(ChatColor.YELLOW)
                        .append("\n")
                        .create();
                    sender.spigot().sendMessage(component);
                    return true;
                }
            }
            else if (args[0].equals("cancel"))
            {
                // Cancel Pending Withdrawal
                if (args.length != 1)
                {
                    return false;
                }
                if (vertconomy.cancelPlayerWithdrawRequest(player))
                {
                    BaseComponent[] component = new ComponentBuilder()
                        .append("The withdraw request has been canceled.").color(ChatColor.YELLOW)
                        .create();
                    sender.spigot().sendMessage(component);
                    return true;
                }
                else
                {
                    BaseComponent[] component = new ComponentBuilder()
                        .append("No withdraw request was found.").color(ChatColor.DARK_RED)
                        .create();
                    sender.spigot().sendMessage(component);
                    return true;
                }
            }
            else
            {
                if (args.length != 2)
                {
                    return false;
                }
                long satAmount = 0L;
                if (args[0].equals("all"))
                {
                    // Withdraw All
                    satAmount = -1L;
                }
                else
                {
                    // Parse Specified Amount
                    try
                    {
                        satAmount = vertconomy.getFormatter().parseSats(args[0]);
                    }
                    catch (InvalidSatAmountException e)
                    {
                        BaseComponent[] component = new ComponentBuilder()
                            .append(e.getMessage()).color(ChatColor.DARK_RED)
                            .create();
                        sender.spigot().sendMessage(component);
                        return true;
                    }
                    if (satAmount <= 0)
                    {
                        BaseComponent[] component = new ComponentBuilder()
                            .append("You can't withdraw nothing!").color(ChatColor.DARK_RED)
                            .create();
                        sender.spigot().sendMessage(component);
                        return true;
                    }
                }
                // Withdraw Amount
                WithdrawRequestResponse request = vertconomy.initiatePlayerWithdrawRequest(player, args[1], satAmount);
                if (request.getResponseType() != WithdrawRequestResponseType.SUCCESS)
                {
                    String errorMessage;
                    switch (request.getResponseType())
                    {
                        case NOT_ENOUGH_WITHDRAWABLE_FUNDS:
                            errorMessage = "You don't have enough withdrawable funds for " + vertconomy.getFormatter().format(satAmount) + "!";
                            break;
                        case CANNOT_AFFORD_FEES:
                            errorMessage = "You cannot afford the fees to complete this withdrawal!";
                            break;
                        case REQUEST_ALREADY_EXISTS:
                            errorMessage = "A withdraw request already exists, please cancel it before making a new one!";
                            break;
                        case INVALID_ADDRESS:
                            errorMessage = "Invalid withdraw address!";
                            break;
                        default:
                            errorMessage = "An unknown error occured while attempting to make the withdrawal.";
                    }
                    BaseComponent[] component = new ComponentBuilder()
                        .append(errorMessage).color(ChatColor.DARK_RED)
                        .create();
                    sender.spigot().sendMessage(component);
                    return true;
                }
                else
                {
                    // Confirm & Cancel Click Commands
                    TextComponent confirmCmdMsg = new TextComponent("confirm");
                    confirmCmdMsg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/withdraw confirm"));
                    confirmCmdMsg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to confirm the transaction!")));
                    TextComponent cancelCmdMsg = new TextComponent("cancel");
                    cancelCmdMsg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/withdraw cancel"));
                    cancelCmdMsg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to cancel the transaction!")));
                    // Show Withdraw Request Details
                    BaseComponent[] component = new ComponentBuilder()
                        .append("\n")
                        .append("Withdraw Request Created:").color(ChatColor.AQUA).underlined(true)
                        .append("\n\n").reset()
                        .append("Amount Excluding Fees: ").color(ChatColor.YELLOW)
                        .append(vertconomy.getFormatter().format(request.getWithdrawAmount())).color(ChatColor.GREEN)
                        .append("\n").reset()
                        .append("Fees: ").color(ChatColor.YELLOW)
                        .append(vertconomy.getFormatter().format(request.getFeeAmount())).color(ChatColor.GOLD)
                        .append("\n").reset()
                        .append("Total Cost: ").color(ChatColor.YELLOW)
                        .append(vertconomy.getFormatter().format(request.getTotalCost())).color(ChatColor.RED)
                        .append("\n").reset()
                        .append("Withdraw Address: ").color(ChatColor.YELLOW)
                        .append(args[1]).color(ChatColor.LIGHT_PURPLE)
                        .append("\n\n").reset()
                        .append("To ").color(ChatColor.AQUA)
                        .append(confirmCmdMsg).color(ChatColor.GREEN).bold(true)
                        .append(" and send the withdrawal, use ").reset().color(ChatColor.AQUA)
                        .append("/withdraw confirm").color(ChatColor.GREEN)
                        .append("\n")
                        .append("To ").color(ChatColor.AQUA)
                        .append(cancelCmdMsg).color(ChatColor.RED).bold(true)
                        .append(" the withdrawal, use ").reset().color(ChatColor.AQUA)
                        .append("/withdraw cancel").color(ChatColor.RED)
                        .append("\n\n")
                        .append("This request will automatically be canceled if not confirmed soon.").color(ChatColor.YELLOW)
                        .append("\n")
                        .create();
                    sender.spigot().sendMessage(component);
                    return true;
                }
            }
        }
        else
        {
            BaseComponent[] component = new ComponentBuilder()
                .append("Unsupported").color(ChatColor.DARK_RED)
                .create();
            sender.spigot().sendMessage(component);
            return true;
        }
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