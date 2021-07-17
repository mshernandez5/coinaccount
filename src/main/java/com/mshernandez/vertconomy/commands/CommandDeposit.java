package com.mshernandez.vertconomy.commands;

import com.mshernandez.vertconomy.core.Vertconomy;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

/**
 *  /deposit
 *  <ul>
 *      <li>Informs players how to deposit VTC to their accounts.</li>
 *      <li>
 *          Does not actually have to start any processes, the server
 *          periodically checks for new deposits automatically.
 *      </li>
 *  </ul>
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
        if (sender instanceof Player)
        {
            // Copyable Deposit Address
            String address = vertconomy.getPlayerDepositAddress(((Player) sender));
            TextComponent addressMsg = new TextComponent(address);
            addressMsg.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, address));
            addressMsg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to copy address!")));
            addressMsg.setColor(ChatColor.GREEN);
            // Build Message
            BaseComponent[] component = new ComponentBuilder()
                .append("\n")
                .append("Deposit ").color(ChatColor.AQUA)
                .append(vertconomy.getFormatter().getSymbol()).color(ChatColor.GREEN)
                .append("\n").append("1) ").reset()
                .append("Deposit To: ").color(ChatColor.AQUA)
                .append(addressMsg).color(ChatColor.GREEN)
                .append("\n").append("2) ").reset()
                .append("Wait For Your Transaction To Reach ").color(ChatColor.YELLOW)
                .append("" + vertconomy.getMinDepositConfirmations()).color(ChatColor.LIGHT_PURPLE)
                .append(" Confirmations").color(ChatColor.YELLOW)
                .append("\n")
                .create();
            sender.spigot().sendMessage(component);
        }
        else
        {
            BaseComponent[] component = new ComponentBuilder()
                .append("Unsupported").color(ChatColor.DARK_RED)
                .create();
            sender.spigot().sendMessage(component);
        }
        return true;
    }
}