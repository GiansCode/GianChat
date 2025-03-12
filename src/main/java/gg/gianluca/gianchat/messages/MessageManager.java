package gg.gianluca.gianchat.messages;

import gg.gianluca.gianchat.GianChat;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {
    private final GianChat plugin;
    private FileConfiguration messages;
    private File messagesFile;

    public MessageManager(GianChat plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        if (messagesFile == null) {
            messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        }

        if (!messagesFile.exists()) {
            try (InputStream in = plugin.getResource("messages.yml")) {
                if (in != null) {
                    Files.copy(in, messagesFile.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create messages.yml!");
                e.printStackTrace();
            }
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public Component getMessage(String path) {
        return getMessage(path, new HashMap<>());
    }

    public Component getMessage(String path, Map<String, String> placeholders) {
        return getMessage(path, null, placeholders);
    }

    public Component getMessage(String path, Player player) {
        return getMessage(path, player, new HashMap<>());
    }

    public Component getMessage(String path, Player player, Map<String, String> placeholders) {
        String message = messages.getString(path, "<red>Missing message: " + path);
        
        // Replace custom placeholders
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        
        // Process PlaceholderAPI placeholders
        if (player != null) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        } else if (placeholders != null && placeholders.containsKey("player")) {
            Player targetPlayer = Bukkit.getPlayer(placeholders.get("player"));
            if (targetPlayer != null) {
                message = PlaceholderAPI.setPlaceholders(targetPlayer, message);
            }
        }
        
        // Build tag resolvers for MiniMessage
        TagResolver.Builder tagResolverBuilder = TagResolver.builder();
        
        // Add placeholders as tag resolvers
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                tagResolverBuilder.resolver(Placeholder.parsed(entry.getKey(), entry.getValue()));
            }
        }
        
        return MiniMessage.miniMessage().deserialize(message, tagResolverBuilder.build());
    }

    public void reloadMessages() {
        loadMessages();
    }
} 