package gg.gianluca.gianchat.commands;

import gg.gianluca.gianchat.GianChat;
import gg.gianluca.gianchat.format.ChatFormat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

public class GianChatCommand implements CommandExecutor, TabCompleter {
    private final GianChat plugin;
    private final List<String> SUBCOMMANDS = Arrays.asList("list", "reload", "test");

    public GianChatCommand(GianChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.player_only"));
            return true;
        }

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!player.hasPermission("gianchat.reload")) {
                    player.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
                    return true;
                }
                plugin.reload();
                player.sendMessage(plugin.getMessageManager().getMessage("reload.success"));
                break;

            case "format":
                if (!player.hasPermission("gianchat.format")) {
                    player.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
                    return true;
                }
                if (args.length < 2) {
                    showFormatHelp(player);
                    return true;
                }
                handleFormatCommand(player, args);
                break;

            case "list":
                if (!player.hasPermission("gianchat.list")) {
                    player.sendMessage(plugin.getMessageManager().getMessage("error.no_permission"));
                    return true;
                }
                listFormats(player);
                break;

            case "help":
            default:
                showHelp(player);
                break;
        }

        return true;
    }

    private void showHelp(Player player) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        
        player.sendMessage(plugin.getMessageManager().getMessage("help.header", placeholders));
        
        if (player.hasPermission("gianchat.reload")) {
            player.sendMessage(plugin.getMessageManager().getMessage("help.reload", placeholders));
        }
        if (player.hasPermission("gianchat.format")) {
            player.sendMessage(plugin.getMessageManager().getMessage("help.format", placeholders));
        }
        if (player.hasPermission("gianchat.list")) {
            player.sendMessage(plugin.getMessageManager().getMessage("help.list", placeholders));
        }
        
        player.sendMessage(plugin.getMessageManager().getMessage("help.footer", placeholders));
    }

    private void showFormatHelp(Player player) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        player.sendMessage(plugin.getMessageManager().getMessage("format.help", placeholders));
    }

    private void handleFormatCommand(Player player, String[] args) {
        String formatName = args[1];
        ChatFormat format = plugin.getFormatManager().getFormat(formatName);
        
        if (format == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("format", formatName);
            player.sendMessage(plugin.getMessageManager().getMessage("format.not_found", placeholders));
            return;
        }

        plugin.getFormatManager().setPlayerFormat(player, format);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("format", formatName);
        player.sendMessage(plugin.getMessageManager().getMessage("format.set", placeholders));
    }

    private void listFormats(Player player) {
        Collection<ChatFormat> formats = plugin.getFormatManager().getFormats();
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("count", String.valueOf(formats.size()));
        player.sendMessage(plugin.getMessageManager().getMessage("format.list.header", placeholders));
        
        for (ChatFormat format : formats) {
            if (player.hasPermission("gianchat.format." + format.getName())) {
                placeholders.put("format", format.getName());
                player.sendMessage(plugin.getMessageManager().getMessage("format.list.entry", placeholders));
            }
        }
        
        player.sendMessage(plugin.getMessageManager().getMessage("format.list.footer", placeholders));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First argument - show available subcommands based on permissions
            for (String subcommand : SUBCOMMANDS) {
                if (sender.hasPermission("gianchat.commands." + subcommand)) {
                    completions.add(subcommand);
                }
            }
        } else if (args.length == 2) {
            // Second argument - format names for test command
            if (args[0].equalsIgnoreCase("test") && sender.hasPermission("gianchat.commands.test")) {
                Collection<ChatFormat> formats = plugin.getFormatManager().getFormats();
                completions.addAll(formats.stream()
                    .map(ChatFormat::getName)
                    .collect(Collectors.toList()));
            }
        } else if (args.length == 3) {
            // Third argument - example messages for test command
            if (args[0].equalsIgnoreCase("test") && sender.hasPermission("gianchat.commands.test")) {
                completions.addAll(Arrays.asList(
                    "Hello world!",
                    "This is a test message",
                    "Testing format " + args[1]
                ));
            }
        }

        return completions.stream()
            .filter(completion -> completion.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
} 