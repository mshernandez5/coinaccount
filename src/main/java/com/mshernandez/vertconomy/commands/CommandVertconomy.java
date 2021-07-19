package com.mshernandez.vertconomy.commands;

import com.mshernandez.vertconomy.core.Vertconomy;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

/**
 *  /vertconomy
 *  <ul>
 *      <li>
 *          Shows whether the plugin is able to make a connection
 *          with the wallet over RPC.
 *      </li>
 *      <li>Gives general plugin information.</li>
 *  </ul>
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
        // Wallet Connection Status Message
        TextComponent statusMsg = new TextComponent();
        if (vertconomy.hasWalletConnection())
        {
            statusMsg.setText("Connected");
            statusMsg.setColor(ChatColor.GREEN);
        }
        else
        {
            statusMsg.setText("Wallet Unreachable / Not Ready");
            statusMsg.setColor(ChatColor.YELLOW);
        }
        // Github Links
        TextComponent authorLinkMsg = new TextComponent("https://github.com/mshernandez5");
        authorLinkMsg.setColor(ChatColor.BLUE);
        authorLinkMsg.setUnderlined(true);
        authorLinkMsg.setBold(true);
        authorLinkMsg.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, authorLinkMsg.getText()));
        authorLinkMsg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to open URL!")));
        TextComponent repoLinkMsg = new TextComponent("https://github.com/mshernandez5/vertconomy");
        repoLinkMsg.setColor(ChatColor.BLUE);
        repoLinkMsg.setUnderlined(true);
        repoLinkMsg.setBold(true);
        repoLinkMsg.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, repoLinkMsg.getText()));
        repoLinkMsg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to open URL!")));
        // Build Message
        BaseComponent[] component = new ComponentBuilder()
            .append("\n")
            .append("Vertconomy Status: ").color(ChatColor.DARK_GREEN).underlined(true)
            .append("\n\n").reset()
            .append("Wallet Connection: ").color(ChatColor.YELLOW)
            .append(statusMsg)
            .append("\n\n").reset()
            .append("Plugin Information: ").color(ChatColor.DARK_GREEN).underlined(true)
            .append("\n\n").reset()
            .append("Author: ").color(ChatColor.YELLOW)
            .append(authorLinkMsg)
            .append("\n").reset()
            .append("Official Repository: ").color(ChatColor.YELLOW)
            .append(repoLinkMsg)
            .append("\n")
            .create();
        sender.spigot().sendMessage(component);
        return true;
    }
}