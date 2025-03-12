package gg.gianluca.gianchat.mentions;

import gg.gianluca.gianchat.GianChat;
import gg.gianluca.gianchat.events.GianChatPlayerMentionEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.time.Duration;
import java.util.regex.Pattern;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

public class MentionManager {
    private final GianChat plugin;
    private boolean enabled;
    private String replaceMentionWith;
    private boolean soundEnabled;
    private Sound mentionSound;
    private float soundVolume;
    private float soundPitch;
    private boolean titleEnabled;
    private String titleText;
    private String subtitleText;
    private int fadeIn;
    private int stay;
    private int fadeOut;
    private boolean actionBarEnabled;
    private String actionBarText;
    private Set<UUID> disabledMentions = new HashSet<>();
    private final Map<Component, Map<UUID, Component>> personalizedMessages = new HashMap<>();

    public MentionManager(GianChat plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    @SuppressWarnings("deprecation")
    public void loadConfig() {
        plugin.reloadConfig();
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("mentions");
        if (config == null) return;

        enabled = config.getBoolean("enabled", true);
        replaceMentionWith = config.getString("replace-mention-with", "<yellow>@%mentioned_player_name%");

        // Load sound settings
        ConfigurationSection soundConfig = config.getConfigurationSection("sound");
        if (soundConfig != null) {
            soundEnabled = soundConfig.getBoolean("enabled", true);
            try {
                String soundName = soundConfig.getString("type", "ENTITY_EXPERIENCE_ORB_PICKUP");
                mentionSound = Sound.valueOf(soundName);
            } catch (Exception e) {
                mentionSound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
                plugin.getLogger().warning("Invalid mention sound in config, defaulting to ENTITY_EXPERIENCE_ORB_PICKUP");
            }
            soundVolume = (float) soundConfig.getDouble("volume", 1.0);
            soundPitch = (float) soundConfig.getDouble("pitch", 1.0);
        }

        // Load title settings
        ConfigurationSection titleConfig = config.getConfigurationSection("title");
        if (titleConfig != null) {
            titleEnabled = titleConfig.getBoolean("enabled", true);
            titleText = titleConfig.getString("title", "<yellow><bold>Mention!</bold></yellow>");
            subtitleText = titleConfig.getString("subtitle", 
                "<gray>You were mentioned by <yellow>%mentioner_player_name%</yellow> in chat</gray>");
            fadeIn = titleConfig.getInt("fade-in", 10);
            stay = titleConfig.getInt("stay", 40);
            fadeOut = titleConfig.getInt("fade-out", 10);
        }

        // Load action bar settings
        ConfigurationSection actionBarConfig = config.getConfigurationSection("action-bar");
        if (actionBarConfig != null) {
            actionBarEnabled = actionBarConfig.getBoolean("enabled", true);
            actionBarText = actionBarConfig.getString("message", 
                "<yellow>You were mentioned by %mentioner_player_name%!</yellow>");
        }
    }

    public Component processMentions(Player sender, Component originalMessage, String rawMessage) {
        if (!enabled || !sender.hasPermission("gianchat.mentions")) {
            return originalMessage;
        }

        Component processedMessage = originalMessage;
        Map<UUID, Component> personalizedMessages = new HashMap<>();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            // Skip if player is trying to mention themselves
            if (sender.equals(onlinePlayer)) {
                continue;
            }
            
            String playerName = onlinePlayer.getName();
            String displayName = PlainTextComponentSerializer.plainText().serialize(onlinePlayer.displayName());
            
            // Check if the message contains the player's name or display name
            if (containsIgnoreCase(rawMessage, playerName) || containsIgnoreCase(rawMessage, displayName)) {
                // Process PlaceholderAPI placeholders for both players
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("mentioned_player_name", PlaceholderAPI.setPlaceholders(onlinePlayer, "%player_name%"));
                placeholders.put("mentioner_player_name", PlaceholderAPI.setPlaceholders(sender, "%player_name%"));
                
                // Replace placeholders in the mention format
                String processedFormat = replaceMentionWith;
                for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                    processedFormat = processedFormat.replace("%" + entry.getKey() + "%", entry.getValue());
                }

                // Create mention component
                Component mentionComponent = MiniMessage.miniMessage().deserialize(processedFormat);

                // Create personalized message for the mentioned player
                Component personalMessage = replaceMention(processedMessage, playerName, mentionComponent);
                personalizedMessages.put(onlinePlayer.getUniqueId(), personalMessage);

                // Schedule the event and effects on the main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Call event
                    GianChatPlayerMentionEvent event = new GianChatPlayerMentionEvent(
                        sender, onlinePlayer, mentionComponent, soundEnabled, titleEnabled, actionBarEnabled);
                    Bukkit.getPluginManager().callEvent(event);

                    if (!event.isCancelled()) {
                        // Handle sound
                        if (event.isPlaySound() && soundEnabled) {
                            onlinePlayer.playSound(onlinePlayer.getLocation(), mentionSound, soundVolume, soundPitch);
                        }

                        // Handle title
                        if (event.isShowTitle() && titleEnabled) {
                            Component title = MiniMessage.miniMessage().deserialize(
                                PlaceholderAPI.setPlaceholders(sender, titleText.replace("%mentioner_", "%")));
                            Component subtitle = MiniMessage.miniMessage().deserialize(
                                PlaceholderAPI.setPlaceholders(sender, subtitleText.replace("%mentioner_", "%")));

                            Title.Times times = Title.Times.times(
                                Duration.ofMillis(fadeIn * 50),
                                Duration.ofMillis(stay * 50),
                                Duration.ofMillis(fadeOut * 50)
                            );
                            onlinePlayer.showTitle(Title.title(title, subtitle, times));
                        }

                        // Handle action bar
                        if (event.isShowActionBar() && actionBarEnabled) {
                            Component actionBar = MiniMessage.miniMessage().deserialize(
                                PlaceholderAPI.setPlaceholders(sender, actionBarText.replace("%mentioner_", "%")));
                            onlinePlayer.sendActionBar(actionBar);
                        }
                    }
                });
            }
        }

        // Store the personalized messages for later use
        this.personalizedMessages.put(processedMessage, personalizedMessages);
        return processedMessage;
    }

    private boolean containsIgnoreCase(String text, String search) {
        return Pattern.compile(Pattern.quote(search), Pattern.CASE_INSENSITIVE)
            .matcher(text)
            .find();
    }

    private Component replaceMention(Component original, String playerName, Component replacement) {
        String originalString = MiniMessage.miniMessage().serialize(original);
        
        // Create a pattern that matches the exact player name with word boundaries
        String pattern = "(?i)\\b" + Pattern.quote(playerName) + "\\b";
        
        // Find the match and its position
        java.util.regex.Matcher matcher = Pattern.compile(pattern).matcher(originalString);
        if (!matcher.find()) return original;
        
        // Split the string into three parts: before, match, and after
        String before = originalString.substring(0, matcher.start());
        String after = originalString.substring(matcher.end());
        
        // Get the replacement as a MiniMessage string and wrap it to prevent color bleeding
        String replacementStr = "<reset>" + MiniMessage.miniMessage().serialize(replacement) + "<reset>";
        
        // Combine the parts while preserving formatting
        String replacedString = before + replacementStr + after;
        
        // Parse the final string back into a component
        return MiniMessage.miniMessage().deserialize(replacedString);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean hasMentionsEnabled(Player player) {
        return plugin.getDataManager().getPlayerData(player).hasMentionsEnabled();
    }

    public void setMentionsEnabled(Player player, boolean enabled) {
        plugin.getDataManager().getPlayerData(player).setMentionsEnabled(enabled);
        plugin.getDataManager().savePlayerData(player);
    }

    public void saveData() {
        // No need to implement this method anymore since we're using DataManager
        // All data is automatically saved through DataManager
    }

    public void loadData() {
        // No need to implement this method anymore since we're using DataManager
        // All data is automatically loaded through DataManager
    }

    public void cleanup() {
        // No need to save data here since DataManager handles it
        // Just make sure any pending changes are saved
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getDataManager().savePlayerData(player);
        }
    }

    public void loadPlayerData(Player player) {
        // Load mention settings from DataManager
        if (!plugin.getDataManager().getPlayerData(player).hasMentionsEnabled()) {
            disabledMentions.add(player.getUniqueId());
        }
    }

    public void removePlayerData(Player player) {
        disabledMentions.remove(player.getUniqueId());
    }

    public Component getPersonalizedMessage(Component baseMessage, Player viewer) {
        Map<UUID, Component> messages = personalizedMessages.get(baseMessage);
        if (messages != null) {
            Component personal = messages.get(viewer.getUniqueId());
            if (personal != null) {
                return personal;
            }
        }
        return baseMessage;
    }

    public void clearPersonalizedMessage(Component message) {
        personalizedMessages.remove(message);
    }
} 