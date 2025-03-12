package gg.gianluca.gianchat.commands;

import gg.gianluca.gianchat.GianChat;
import gg.gianluca.gianchat.messaging.PrivateMessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SocialSpyCommand implements CommandExecutor {
    private final GianChat plugin;
    private final PrivateMessageManager privateMessageManager;

    public SocialSpyCommand(GianChat plugin, PrivateMessageManager privateMessageManager) {
        this.plugin = plugin;
        this.privateMessageManager = privateMessageManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.player_only"));
            return true;
        }

        if (!sender.hasPermission("gianchat.commands.socialspy")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
            return true;
        }

        boolean enabled = privateMessageManager.toggleSocialSpy(player);
        sender.sendMessage(plugin.getMessageManager().getMessage(enabled ? "social_spy.enabled" : "social_spy.disabled"));
        return true;
    }
} 