package gg.gianluca.gianchat.commands;

import gg.gianluca.gianchat.GianChat;
import gg.gianluca.gianchat.events.GianChatPlayerIgnoreEvent;
import gg.gianluca.gianchat.messaging.PrivateMessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IgnoreCommand implements CommandExecutor, TabCompleter {
    private final GianChat plugin;
    private final PrivateMessageManager privateMessageManager;

    public IgnoreCommand(GianChat plugin, PrivateMessageManager privateMessageManager) {
        this.plugin = plugin;
        this.privateMessageManager = privateMessageManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.player_only"));
            return true;
        }

        if (!sender.hasPermission("gianchat.commands.ignore")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
            return true;
        }

        if (args.length != 1) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("usage", "/" + label + " <player>");
            sender.sendMessage(plugin.getMessageManager().getMessage("error.invalid_usage", placeholders));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", args[0]);
            sender.sendMessage(plugin.getMessageManager().getMessage("error.player_not_found", placeholders));
            return true;
        }

        if (target == player) {
            sender.sendMessage(plugin.getMessageManager().getMessage("ignore.cannot_ignore_self"));
            return true;
        }

        boolean currentlyIgnored = privateMessageManager.hasPlayerIgnored(player, target);
        boolean shouldIgnore = !currentlyIgnored;

        // Call the ignore event
        GianChatPlayerIgnoreEvent event = new GianChatPlayerIgnoreEvent(player, target, shouldIgnore);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return true;
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());

        if (shouldIgnore) {
            if (currentlyIgnored) {
                sender.sendMessage(plugin.getMessageManager().getMessage("ignore.already_ignored", placeholders));
                return true;
            }
            privateMessageManager.setPlayerIgnored(player, target, true);
            sender.sendMessage(plugin.getMessageManager().getMessage("ignore.player_ignored", placeholders));
        } else {
            if (!currentlyIgnored) {
                sender.sendMessage(plugin.getMessageManager().getMessage("ignore.not_ignored", placeholders));
                return true;
            }
            privateMessageManager.setPlayerIgnored(player, target, false);
            sender.sendMessage(plugin.getMessageManager().getMessage("ignore.player_unignored", placeholders));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!(sender instanceof Player player) || !sender.hasPermission("gianchat.commands.ignore")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                .filter(p -> p != player)
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
} 