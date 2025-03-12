package gg.gianluca.gianchat.commands;

import gg.gianluca.gianchat.GianChat;
import gg.gianluca.gianchat.messaging.PrivateMessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageCommand implements CommandExecutor, TabCompleter {
    private final GianChat plugin;
    private final PrivateMessageManager privateMessageManager;
    private final List<String> COMMON_MESSAGES = Arrays.asList(
        "Hey!", 
        "How are you?", 
        "Can you help me?",
        "Are you available to talk?",
        "Nice to meet you!"
    );

    public MessageCommand(GianChat plugin, PrivateMessageManager privateMessageManager) {
        this.plugin = plugin;
        this.privateMessageManager = privateMessageManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.player_only"));
            return true;
        }

        if (!sender.hasPermission("gianchat.commands.message")) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
            return true;
        }

        if (args.length < 2) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("usage", "/" + label + " <player> <message>");
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
            sender.sendMessage(plugin.getMessageManager().getMessage("message.cannot_message_self"));
            return true;
        }

        if (!privateMessageManager.hasMessagesEnabled(target)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", target.getName());
            sender.sendMessage(plugin.getMessageManager().getMessage("message.recipient_messages_disabled", placeholders));
            return true;
        }

        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            messageBuilder.append(args[i]).append(" ");
        }
        String message = messageBuilder.toString().trim();

        privateMessageManager.sendPrivateMessage(player, target, message);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!(sender instanceof Player player) || !sender.hasPermission("gianchat.commands.message")) {
            return new ArrayList<>();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First argument - online players who haven't ignored the sender
            completions.addAll(
                Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p != player && 
                               !privateMessageManager.hasPlayerIgnored(p, player) &&
                               privateMessageManager.hasMessagesEnabled(p))
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList())
            );
        } else if (args.length == 2) {
            // Second argument - common messages and last message to player if exists
            completions.addAll(COMMON_MESSAGES);
            
            // Add the last message sent to this player if it exists
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                Player lastMessager = privateMessageManager.getLastMessager(target);
                if (lastMessager != null && lastMessager.equals(sender)) {
                    completions.add("(Reply to last message)");
                }
            }
        }

        return completions.stream()
            .filter(completion -> completion.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
} 