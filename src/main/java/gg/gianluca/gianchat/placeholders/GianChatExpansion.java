package gg.gianluca.gianchat.placeholders;

import gg.gianluca.gianchat.GianChat;
import gg.gianluca.gianchat.format.ChatFormat;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GianChatExpansion extends PlaceholderExpansion {
    private final GianChat plugin;

    public GianChatExpansion(GianChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "gianchat";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Gianluca";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline()) {
            return "";
        }

        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer == null) {
            return "";
        }

        // Format related placeholders
        if (params.startsWith("format_")) {
            String formatParam = params.substring(7);
            return handleFormatPlaceholder(onlinePlayer, formatParam);
        }

        // Message related placeholders
        if (params.startsWith("message_")) {
            String messageParam = params.substring(8);
            return handleMessagePlaceholder(onlinePlayer, messageParam);
        }

        // Mention related placeholders
        if (params.startsWith("mention_")) {
            String mentionParam = params.substring(8);
            return handleMentionPlaceholder(onlinePlayer, mentionParam);
        }

        return null;
    }

    private String handleFormatPlaceholder(Player player, String param) {
        ChatFormat format = plugin.getFormatManager().getPlayerFormat(player);
        if (format == null) {
            return "";
        }

        switch (param) {
            case "prefix":
                return PlainTextComponentSerializer.plainText().serialize(format.getPrefix());
            case "name_format":
                return PlainTextComponentSerializer.plainText().serialize(format.getNameFormat());
            case "separator":
                return PlainTextComponentSerializer.plainText().serialize(format.getSeparator());
            case "name":
                return format.getName();
            default:
                return null;
        }
    }

    private String handleMessagePlaceholder(Player player, String param) {
        return switch (param) {
            case "toggle" -> plugin.getPrivateMessageManager()
                .hasMessagesEnabled(player) ? "enabled" : "disabled";
            case "last_messager" -> {
                Player lastMessager = plugin.getPrivateMessageManager().getLastMessager(player);
                yield lastMessager != null ? lastMessager.getName() : "";
            }
            case "social_spy" -> plugin.getPrivateMessageManager()
                .hasSocialSpyEnabled(player) ? "enabled" : "disabled";
            case "ignored_count" -> String.valueOf(
                plugin.getPrivateMessageManager().getIgnoredPlayers(player).size()
            );
            default -> null;
        };
    }

    private String handleMentionPlaceholder(Player player, String param) {
        return switch (param) {
            case "enabled" -> plugin.getMentionManager().isEnabled() ? "true" : "false";
            default -> null;
        };
    }
} 