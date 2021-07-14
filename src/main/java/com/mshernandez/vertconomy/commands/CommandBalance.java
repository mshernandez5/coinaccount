package com.mshernandez.vertconomy.commands;

import java.util.ArrayList;
import java.util.List;

import com.mshernandez.vertconomy.core.Pair;
import com.mshernandez.vertconomy.core.SatAmountFormat;
import com.mshernandez.vertconomy.core.Vertconomy;

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
import net.md_5.bungee.api.chat.ComponentBuilder;

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
        boolean otherPlayerLookup = args.length == 1 && sender.hasPermission(PERMISSION_VIEW_OTHER_BALANCES);
        if (otherPlayerLookup || sender instanceof Player)
        {
            OfflinePlayer player;
            if (otherPlayerLookup)
            {
                player = Bukkit.getOfflinePlayer(args[0]);
                if (!player.hasPlayedBefore())
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
            Pair<Long, Long> balances = vertconomy.getPlayerBalances(player);
            ComponentBuilder cb = new ComponentBuilder();
            if (otherPlayerLookup)
            {
                cb.append(player.getName()).color(ChatColor.GOLD)
                    .append("'s ").color(ChatColor.GOLD);
            }
            cb.append("Balance: ").color(ChatColor.GOLD)
                .append(formatter.format(balances.getKey())).color(ChatColor.GREEN);
            if (balances.getVal() != 0L)
            {
                cb.append(" (Pending: ").color(ChatColor.GRAY)
                    .append(formatter.format(balances.getVal())).color(ChatColor.GRAY)
                    .append(")").color(ChatColor.GRAY);
            }
            sender.spigot().sendMessage(cb.create());
            return true;
        }
        else if (sender instanceof ConsoleCommandSender)
        {
            BaseComponent[] component = new ComponentBuilder()
                .append("Server-Owned Balance: ").color(ChatColor.RED)
                .append("NOT YET SUPPORTED").color(ChatColor.GREEN)
                .create();
            sender.spigot().sendMessage(component);
            return true;
        }
        else
        {
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
            for (OfflinePlayer p : Bukkit.getOfflinePlayers())
            {
                options.add(p.getName());
            }
        }
        return options;
    }
}