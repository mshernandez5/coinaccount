package com.mshernandez.vertconomy.commands;

import java.util.List;

import com.mshernandez.vertconomy.core.Vertconomy;
import com.mshernandez.vertconomy.core.util.InvalidSatAmountException;
import com.mshernandez.vertconomy.core.util.SatAmountFormatter;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;

public class CommandPay implements CommandExecutor, TabCompleter
{

    private Vertconomy vertconomy;

    public CommandPay(Vertconomy vertconomy)
    {
        this.vertconomy = vertconomy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        // Initial Checks
        if (!(sender instanceof Player || sender instanceof ConsoleCommandSender))
        {
            return false;
        }
        if (args.length != 2)
        {
            return false;
        }
        // Make Sure Receiving Player Is Valid
        Player receiver = Bukkit.getPlayer(args[0]);
        if (receiver == null || !receiver.hasPlayedBefore())
        {
            BaseComponent[] component = new ComponentBuilder()
                .append("Unknown Player: ").color(ChatColor.DARK_RED)
                .append(args[0]).color(ChatColor.RED)
                .create();
            sender.spigot().sendMessage(component);
            return true;
        }
        // Don't Send To Yourself, Unecessary Operation
        if (sender.equals(receiver))
        {
            BaseComponent[] component = new ComponentBuilder()
                .append("You can't send to yourself.").color(ChatColor.DARK_RED)
                .create();
            sender.spigot().sendMessage(component);
            return true;
        }
        // Parse Requested Amount
        SatAmountFormatter formatter = vertconomy.getFormatter();
        long amount;
        try
        {
            amount = formatter.parseSats(args[1]);
        }
        catch (InvalidSatAmountException e)
        {
            BaseComponent[] component = new ComponentBuilder()
                .append("Invalid amount!").color(ChatColor.DARK_RED)
                .create();
            sender.spigot().sendMessage(component);
            return true;
        }
        if (amount <= 0L)
        {
            BaseComponent[] component = new ComponentBuilder()
                .append("You can't send this amount!").color(ChatColor.DARK_RED)
                .create();
            sender.spigot().sendMessage(component);
            return true;
        }
        // Attempt To Send Requested Amount
        boolean success;
        if (sender instanceof Player)
        {
            success = vertconomy.transferPlayerBalance((Player) sender, receiver, amount);
        }
        else
        {
            success = vertconomy.takeFromServer(receiver, amount);
        }
        String formattedAmount = formatter.format(amount);
        // If Successful, Send Messages To Sender & Receiver
        if (success)
        {
            BaseComponent[] senderMsg = new ComponentBuilder()
                .append(formattedAmount).color(ChatColor.GREEN)
                .append(" has been sent to ").color(ChatColor.GOLD)
                .append(receiver.getName()).color(ChatColor.DARK_RED)
                .append(".").color(ChatColor.GOLD)
                .create();
            sender.spigot().sendMessage(senderMsg);
            BaseComponent[] receiverMsg = new ComponentBuilder()
                .append(formattedAmount).color(ChatColor.GREEN)
                .append(" has been received from ").color(ChatColor.GOLD)
                .append(sender.getName()).color(ChatColor.DARK_RED)
                .append(".").color(ChatColor.GOLD)
                .create();
            receiver.spigot().sendMessage(receiverMsg);
            return true;
        }
        // Not Successful, Send Failure Message To Sender
        BaseComponent[] component = new ComponentBuilder()
            .append("You cannot afford to send ").color(ChatColor.DARK_RED)
            .append(formattedAmount).color(ChatColor.RED)
            .create();
        sender.spigot().sendMessage(component);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        return null;
    }
    
}
