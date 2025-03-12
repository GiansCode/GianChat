package gg.gianluca.gianchat.commands;

import gg.gianluca.gianchat.GianChat;
import gg.gianluca.gianchat.events.GianChatPrivateMessageToggleEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MessageToggleCommand implements CommandExecutor {
    private final GianChat plugin;

    public MessageToggleCommand(GianChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.player_only"));
            return true;
        }

        if (!sender.hasPermission("gianchat.commands.messagetoggle")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
            return true;
        }

        boolean newState = !plugin.getPrivateMessageManager().hasMessagesEnabled(player);
        
        // Create and call the event
        GianChatPrivateMessageToggleEvent event = new GianChatPrivateMessageToggleEvent(player, newState);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return true;
        }

        plugin.getPrivateMessageManager().setMessagesEnabled(player, event.isEnabled());
        player.sendMessage(plugin.getMessageManager().getMessage(
            event.isEnabled() ? "message_toggle.enabled" : "message_toggle.disabled"
        ));

        return true;
    }
} 