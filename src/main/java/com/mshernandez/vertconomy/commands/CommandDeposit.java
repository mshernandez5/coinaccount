package com.mshernandez.vertconomy.commands;

import com.mshernandez.vertconomy.core.Vertconomy;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

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
        TextComponent messageComponent = new TextComponent();
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
            messageComponent.addExtra(message.toString());
            // Copyable Address
            String address = vertconomy.getPlayerDepositAddress(((Player) sender));
            TextComponent addressMsg = new TextComponent(address);
            addressMsg.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, address));
            addressMsg.setColor(ChatColor.GREEN);
            messageComponent.addExtra(addressMsg);
            // Show Minimum Confirmations
            message = new StringBuilder();
            message.append(ChatColor.RESET);
            message.append('\n');
            message.append("2) ");
            message.append(ChatColor.YELLOW);
            message.append("Wait For Your Transaction To Reach ");
            message.append(ChatColor.LIGHT_PURPLE);
            message.append("" + vertconomy.getMinimumConfirmations());
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

        messageComponent.addExtra(message.toString());
        sender.spigot().sendMessage(messageComponent);
        return true;
    }
}