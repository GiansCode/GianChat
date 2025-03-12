package gg.gianluca.gianchat.format;

import gg.gianluca.gianchat.GianChat;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class FormatManager {
    private final GianChat plugin;
    private final Map<String, ChatFormat> formats;
    private final Map<UUID, String> playerFormats;
    private final File formatsDirectory;

    public FormatManager(GianChat plugin) {
        this.plugin = plugin;
        this.formats = new HashMap<>();
        this.playerFormats = new HashMap<>();
        this.formatsDirectory = new File(plugin.getDataFolder(), "formats");
        loadFormats();
    }

    public void loadPlayerFormats() {
        playerFormats.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            String format = plugin.getDataManager().getPlayerData(player).getFormat();
            if (format != null) {
                playerFormats.put(player.getUniqueId(), format);
            }
        }
    }

    public void loadFormats() {
        formats.clear();

        if (!formatsDirectory.exists()) {
            formatsDirectory.mkdirs();
            saveDefaultFormat();
        }

        File[] files = formatsDirectory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String formatName = file.getName().replace(".yml", "");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            formats.put(formatName, new ChatFormat(formatName, config));
        }

        // Load player formats after loading format definitions
        loadPlayerFormats();
    }

    private void saveDefaultFormat() {
        File defaultFormat = new File(formatsDirectory, "default.yml");
        if (!defaultFormat.exists()) {
            try {
                Files.copy(plugin.getResource("formats/default.yml"), defaultFormat.toPath());
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save default format: " + e.getMessage());
            }
        }
    }

    public Collection<ChatFormat> getFormats() {
        return formats.values();
    }

    public ChatFormat getFormat(String name) {
        return formats.get(name);
    }

    public ChatFormat getPlayerFormat(Player player) {
        String formatName = playerFormats.get(player.getUniqueId());
        if (formatName == null) {
            // Return default format if no format is set
            return formats.values().stream()
                .min(Comparator.comparingInt(ChatFormat::getPriority))
                .orElse(null);
        }
        return formats.get(formatName);
    }

    public void setPlayerFormat(Player player, ChatFormat format) {
        if (format == null) {
            playerFormats.remove(player.getUniqueId());
        } else {
            playerFormats.put(player.getUniqueId(), format.getName());
            plugin.getDataManager().getPlayerData(player).setFormat(format.getName());
        }
    }

    public Component getPrefix(Player player) {
        ChatFormat format = getPlayerFormat(player);
        return format != null ? format.getPrefix() : Component.empty();
    }

    public Component getNameFormat(Player player) {
        ChatFormat format = getPlayerFormat(player);
        return format != null ? format.getNameFormat() : Component.empty();
    }

    public Component getSeparator(Player player) {
        ChatFormat format = getPlayerFormat(player);
        return format != null ? format.getSeparator() : Component.empty();
    }

    public Optional<ChatFormat> getFormatForPlayer(Player player) {
        String formatName = playerFormats.get(player.getUniqueId());
        if (formatName == null) {
            // Return default format if no format is set
            return formats.values().stream()
                .min(Comparator.comparingInt(ChatFormat::getPriority));
        }
        return Optional.ofNullable(formats.get(formatName));
    }

    public void removePlayerFormat(Player player) {
        playerFormats.remove(player.getUniqueId());
    }
} 