package com.mshernandez.vertconomy.commands;

import java.util.ArrayList;
import java.util.List;

import com.mshernandez.vertconomy.core.Vertconomy;
import com.mshernandez.vertconomy.core.util.SatAmountFormat;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
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
 *  /balance
 *  <ul>
 *      <li>Allows user to check their balance.</li>
 *      <li>Allows users with <code>vertconomy.balance.others</code> to check other player balances.</li>
 *      <li>Allows console to check server-owned wallet balance.</li>
 *  </ul>
 */
public class CommandBalance implements CommandExecutor, TabCompleter
{
    private static final String PERMISSION_VIEW_OTHER_BALANCES = "vertconomy.balance.others";

    private Vertconomy vertconomy;

    public CommandBalance(Vertconomy vertconomy)
    {
        this.vertconomy = vertconomy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        SatAmountFormat formatter = vertconomy.getFormatter();
        // Hidden Commands To Provide Explanations When Clicking Text
        if (args.length == 2 && args[0].equals("explanation"))
        {
            if (args[1].equals("withdrawable"))
            {
                BaseComponent[] component = new ComponentBuilder()
                    .append("\n")
                    .append("Your funds are backed by unspent transaction outputs on the blockchain.\n").color(ChatColor.YELLOW)
                    .append("When you transfer funds in game, you are sharing a part of that output with another player.\n")
                    .append("When a player withdraws any of their funds, the full outputs backing those funds must be spent including your share.\n")
                    .append("Your funds will become withdrawable again once the withdraw completes and returns your share to the server as change.")
                    .append("\n")
                    .create();
                sender.spigot().sendMessage(component);
                return true;
            }
            else
            {
                return false;
            }
        }
        // Check Balance
        boolean otherPlayerLookup = args.length == 1 && sender.hasPermission(PERMISSION_VIEW_OTHER_BALANCES);
        if (otherPlayerLookup || sender instanceof Player)
        {
            OfflinePlayer player;
            if (otherPlayerLookup)
            {
                player = Bukkit.getPlayer(args[0]);
                if (player == null || !player.hasPlayedBefore())
                {
                    BaseComponent[] component = new ComponentBuilder()
                        .append("Unknown Player: ").color(ChatColor.DARK_RED)
                        .append(args[0]).color(ChatColor.RED)
                        .create();
                    sender.spigot().sendMessage(component);
                    return true;
                }
            }
            else
            {
                player = (Player) sender;
            }
            ComponentBuilder cb = new ComponentBuilder();
            if (otherPlayerLookup)
            {
                cb.append(player.getName()).color(ChatColor.GOLD)
                    .append("'s ").color(ChatColor.GOLD);
            }
            // Show Balance
            long playerBalance = vertconomy.getPlayerBalance(player);
            cb.append("Balance: ").color(ChatColor.GOLD)
                .append(formatter.format(playerBalance)).color(ChatColor.GREEN);
            // Detailed Balance Info Belongs Only To Holder
            if (!otherPlayerLookup)
            {
                // Show Unconfirmed Balances If Applicable
                long unconfirmedBalance = vertconomy.getPlayerUnconfirmedBalance(player);
                if (unconfirmedBalance != 0L)
                {
                    cb.append(" (Pending: ").color(ChatColor.GRAY)
                        .append(formatter.format(unconfirmedBalance)).color(ChatColor.GRAY)
                        .append(")").color(ChatColor.GRAY);
                }
                // Show Withdrawable Portion Of Balance If Applicable
                long withdrawablePlayerBalance = vertconomy.getPlayerWithdrawableBalance(player);
                if (playerBalance != withdrawablePlayerBalance)
                {
                    // Text Link To Explanation For Non-Withdrawable Balances
                    TextComponent explanationMsg = new TextComponent("Why?");
                    explanationMsg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/balance explanation withdrawable"));
                    explanationMsg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click for explanation!")));
                    cb.append("\n").append("Notice: ").color(ChatColor.GRAY)
                        .append(formatter.format(withdrawablePlayerBalance))
                        .append(" may be withdrawn at this time. ")
                        .append(explanationMsg).color(ChatColor.YELLOW);
                }
            }
            sender.spigot().sendMessage(cb.create());
            return true;
        }
        else if (sender instanceof ConsoleCommandSender)
        {
            // Get Balance
            long serverBalance = vertconomy.getServerBalance();
            ComponentBuilder cb = new ComponentBuilder()
                .append("Server-Owned Balance: ").color(ChatColor.GOLD)
                .append(formatter.format(vertconomy.getServerBalance())).color(ChatColor.GREEN);
            // Show Unconfirmed Balances If Applicable
            long unconfirmedBalance = vertconomy.getServerUnconfirmedBalance();
            if (unconfirmedBalance != 0L)
            {
                cb.append(" (Pending: ").color(ChatColor.GRAY)
                    .append(formatter.format(unconfirmedBalance)).color(ChatColor.GRAY)
                    .append(")").color(ChatColor.GRAY);
            }
            // Show Withdrawable Portion Of Balance If Applicable
            long withdrawableServerBalance = vertconomy.getServerWithdrawableBalance();
            if (serverBalance != withdrawableServerBalance)
            {
                cb.append("\n").append("Notice: ").color(ChatColor.GRAY)
                    .append(formatter.format(withdrawableServerBalance))
                    .append(" may be withdrawn at this time. ");
            }
            // Send Message
            sender.spigot().sendMessage(cb.create());
            return true;
        }
        else
        {
            // Unknown Sender
            BaseComponent[] component = new ComponentBuilder()
                .append("UNSUPPORTED").color(ChatColor.DARK_RED)
                .create();
            sender.spigot().sendMessage(component);
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args)
    {
        List<String> options = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission(PERMISSION_VIEW_OTHER_BALANCES))
        {
            for (OfflinePlayer p : Bukkit.getOnlinePlayers())
            {
                options.add(p.getName());
            }
        }
        return options;
    }
}