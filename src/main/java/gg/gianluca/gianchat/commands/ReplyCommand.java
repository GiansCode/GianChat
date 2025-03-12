package gg.gianluca.gianchat.commands;

import gg.gianluca.gianchat.GianChat;
import gg.gianluca.gianchat.messaging.PrivateMessageManager;
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

public class ReplyCommand implements CommandExecutor, TabCompleter {
    private final GianChat plugin;
    private final PrivateMessageManager privateMessageManager;
    private final List<String> QUICK_REPLIES = Arrays.asList(
        "Yes", 
        "No", 
        "Maybe",
        "Thanks!",
        "One moment please",
        "I'll be right back",
        "Sorry, I'm busy right now"
    );

    public ReplyCommand(GianChat plugin, PrivateMessageManager privateMessageManager) {
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

        if (args.length == 0) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("usage", "/" + label + " <message>");
            sender.sendMessage(plugin.getMessageManager().getMessage("error.invalid_usage", placeholders));
            return true;
        }

        Player lastMessager = privateMessageManager.getLastMessager(player);
        if (lastMessager == null || !lastMessager.isOnline()) {
            sender.sendMessage(plugin.getMessageManager().getMessage("message.no_reply_target"));
            return true;
        }

        if (!privateMessageManager.hasMessagesEnabled(lastMessager)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", lastMessager.getName());
            sender.sendMessage(plugin.getMessageManager().getMessage("message.recipient_messages_disabled", placeholders));
            return true;
        }

        StringBuilder messageBuilder = new StringBuilder();
        for (String arg : args) {
            messageBuilder.append(arg).append(" ");
        }
        String message = messageBuilder.toString().trim();

        privateMessageManager.sendPrivateMessage(player, lastMessager, message);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!(sender instanceof Player player) || !sender.hasPermission("gianchat.commands.message")) {
            return new ArrayList<>();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First argument - quick replies
            completions.addAll(QUICK_REPLIES);

            // Add context-based replies
            Player lastMessager = privateMessageManager.getLastMessager(player);
            if (lastMessager != null && lastMessager.isOnline()) {
                completions.add("Hi " + lastMessager.getName() + "!");
                completions.add("Bye " + lastMessager.getName() + "!");
            }
        }

        return completions.stream()
            .filter(completion -> completion.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
} 