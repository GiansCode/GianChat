package gg.gianluca.gianchat.messaging;

import gg.gianluca.gianchat.GianChat;
import gg.gianluca.gianchat.data.PlayerData;
import gg.gianluca.gianchat.events.GianChatPrivateMessageEvent;
import gg.gianluca.gianchat.events.GianChatPrivateMessageToggleEvent;
import gg.gianluca.gianchat.events.GianChatPlayerIgnoreEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class PrivateMessageManager {
    private final GianChat plugin;
    private final Map<UUID, UUID> lastMessagedPlayer;
    private final Set<UUID> socialSpyEnabled;
    private final Set<UUID> messagesEnabled;
    private final Map<UUID, Set<UUID>> ignoredPlayers; // Key: Player UUID, Value: Set of ignored player UUIDs
    private YamlConfiguration config;
    private boolean enabled;
    private boolean replyToLastSent;
    private boolean soundEnabled;
    private Sound notificationSound;
    private float soundVolume;
    private float soundPitch;
    private int autoSaveTaskId;

    public PrivateMessageManager(GianChat plugin) {
        this.plugin = plugin;
        this.lastMessagedPlayer = new HashMap<>();
        this.socialSpyEnabled = new HashSet<>();
        this.messagesEnabled = new HashSet<>();
        this.ignoredPlayers = new HashMap<>();
        loadConfig();
        loadData();
        startAutoSave();
    }

    @SuppressWarnings("deprecation")
    public void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "privatemessages.yml");
        if (!configFile.exists()) {
            try {
                Files.copy(plugin.getResource("privatemessages.yml"), configFile.toPath());
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save default private messages config: " + e.getMessage());
                return;
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        enabled = config.getBoolean("enabled", true);
        replyToLastSent = config.getBoolean("reply-to-last-sent", true);
        
        // Load sound settings
        soundEnabled = config.getBoolean("sound.enabled", true);
        try {
            notificationSound = Sound.valueOf(config.getString("sound.sound", "ENTITY_EXPERIENCE_ORB_PICKUP"));
        } catch (IllegalArgumentException e) {
            notificationSound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
            plugin.getLogger().warning("Invalid sound in config, defaulting to ENTITY_EXPERIENCE_ORB_PICKUP");
        }
        soundVolume = (float) config.getDouble("sound.volume", 1.0);
        soundPitch = (float) config.getDouble("sound.pitch", 1.0);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean sendPrivateMessage(Player sender, Player recipient, String message) {
        if (!hasMessagesEnabled(sender)) {
            sender.sendMessage(plugin.getMessageManager().getMessage("error.messages_disabled_self"));
            return false;
        }

        if (!hasMessagesEnabled(recipient)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", recipient.getName());
            sender.sendMessage(plugin.getMessageManager().getMessage("error.messages_disabled_other", placeholders));
            return false;
        }

        if (hasPlayerIgnored(recipient, sender) || hasPlayerIgnored(sender, recipient)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", recipient.getName());
            sender.sendMessage(plugin.getMessageManager().getMessage(hasPlayerIgnored(sender, recipient) ? 
                "error.ignoring_player" : "error.player_ignored", placeholders));
            return false;
        }

        // Get format configurations
        String senderFormatStr = config.getString("sender-format.format", "<gray>You -> %recipient_display_name%: %message%");
        String recipientFormatStr = config.getString("receiver-format.format", "<gray>%sender_display_name% -> You: %message%");
        String spyFormatStr = config.getString("social-spy.format", "<gray>[SPY] %sender_display_name% -> %recipient_display_name%: %message%");
        List<String> senderTooltip = config.getStringList("sender-format.tooltip");
        List<String> recipientTooltip = config.getStringList("receiver-format.tooltip");

        // Process message with PlaceholderAPI for both sender and recipient contexts
        String processedMessageSender = PlaceholderAPI.setPlaceholders(sender, message);
        String processedMessageRecipient = PlaceholderAPI.setPlaceholders(recipient, message);

        // Process tooltips with PlaceholderAPI
        List<Component> senderTooltipComponents = new ArrayList<>();
        List<Component> recipientTooltipComponents = new ArrayList<>();

        // Process sender tooltip
        for (String line : senderTooltip) {
            String processed = PlaceholderAPI.setPlaceholders(sender, line);
            processed = PlaceholderAPI.setPlaceholders(recipient, processed.replace("%recipient_", "%"));
            senderTooltipComponents.add(MiniMessage.miniMessage().deserialize(processed));
        }

        // Process recipient tooltip
        for (String line : recipientTooltip) {
            String processed = PlaceholderAPI.setPlaceholders(recipient, line);
            processed = PlaceholderAPI.setPlaceholders(sender, processed.replace("%sender_", "%"));
            recipientTooltipComponents.add(MiniMessage.miniMessage().deserialize(processed));
        }

        // Process format strings with PlaceholderAPI
        senderFormatStr = PlaceholderAPI.setPlaceholders(sender, senderFormatStr);
        senderFormatStr = PlaceholderAPI.setPlaceholders(recipient, senderFormatStr.replace("%recipient_", "%"));
        
        recipientFormatStr = PlaceholderAPI.setPlaceholders(recipient, recipientFormatStr);
        recipientFormatStr = PlaceholderAPI.setPlaceholders(sender, recipientFormatStr.replace("%sender_", "%"));
        
        spyFormatStr = PlaceholderAPI.setPlaceholders(sender, spyFormatStr);
        spyFormatStr = PlaceholderAPI.setPlaceholders(recipient, spyFormatStr.replace("%recipient_", "%"));

        // Create the final components with proper message replacement
        Component senderMessage = MiniMessage.miniMessage().deserialize(
            senderFormatStr.replace("%message%", MiniMessage.miniMessage().serialize(
                MiniMessage.miniMessage().deserialize(processedMessageSender)
            ))
        ).hoverEvent(Component.join(net.kyori.adventure.text.JoinConfiguration.newlines(), senderTooltipComponents));

        Component recipientMessage = MiniMessage.miniMessage().deserialize(
            recipientFormatStr.replace("%message%", MiniMessage.miniMessage().serialize(
                MiniMessage.miniMessage().deserialize(processedMessageRecipient)
            ))
        ).hoverEvent(Component.join(net.kyori.adventure.text.JoinConfiguration.newlines(), recipientTooltipComponents));

        Component spyMessage = MiniMessage.miniMessage().deserialize(
            spyFormatStr.replace("%message%", MiniMessage.miniMessage().serialize(
                MiniMessage.miniMessage().deserialize(processedMessageSender)
            ))
        );

        // Add click events if configured
        if (config.contains("sender-format.click_event")) {
            String type = config.getString("sender-format.click_event.type", "SUGGEST_COMMAND");
            String command = config.getString("sender-format.click_event.command", "/msg %recipient% ");
            command = PlaceholderAPI.setPlaceholders(recipient, command.replace("%recipient%", "%player_name%"));
            senderMessage = senderMessage.clickEvent(net.kyori.adventure.text.event.ClickEvent.clickEvent(
                net.kyori.adventure.text.event.ClickEvent.Action.valueOf(type),
                command
            ));
        }

        if (config.contains("receiver-format.click_event")) {
            String type = config.getString("receiver-format.click_event.type", "SUGGEST_COMMAND");
            String command = config.getString("receiver-format.click_event.command", "/r ");
            command = PlaceholderAPI.setPlaceholders(sender, command.replace("%sender%", "%player_name%"));
            recipientMessage = recipientMessage.clickEvent(net.kyori.adventure.text.event.ClickEvent.clickEvent(
                net.kyori.adventure.text.event.ClickEvent.Action.valueOf(type),
                command
            ));
        }

        // Create and call the event
        GianChatPrivateMessageEvent event = new GianChatPrivateMessageEvent(sender, recipient, senderMessage, recipientMessage, spyMessage, soundEnabled);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return false;
        }

        // Update last messager
        setLastMessager(recipient, sender);
        lastMessagedPlayer.put(sender.getUniqueId(), recipient.getUniqueId());

        // Send messages
        sender.sendMessage(event.getSenderMessage());
        recipient.sendMessage(event.getRecipientMessage());

        // Play sound if enabled
        if (soundEnabled && event.shouldPlaySoundToRecipient()) {
            recipient.playSound(recipient.getLocation(), notificationSound, soundVolume, soundPitch);
        }

        // Notify social spies
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasSocialSpyEnabled(player) && !player.equals(sender) && !player.equals(recipient)) {
                player.sendMessage(event.getSocialSpyMessage());
            }
        }

        return true;
    }

    public Optional<Player> getReplyTarget(Player player) {
        UUID targetUUID = replyToLastSent ? lastMessagedPlayer.get(player.getUniqueId()) : plugin.getDataManager().getPlayerData(player).getLastMessager();
        if (targetUUID == null) return Optional.empty();
        return Optional.ofNullable(Bukkit.getPlayer(targetUUID));
    }

    public boolean toggleSocialSpy(Player player) {
        boolean newState = !hasSocialSpyEnabled(player);
        plugin.getDataManager().getPlayerData(player).setSocialSpyEnabled(newState);
        plugin.getDataManager().savePlayerData(player);
        
        if (newState) {
            socialSpyEnabled.add(player.getUniqueId());
        } else {
            socialSpyEnabled.remove(player.getUniqueId());
        }
        
        return newState;
    }

    public boolean hasSocialSpyEnabled(Player player) {
        return socialSpyEnabled.contains(player.getUniqueId()) || 
               plugin.getDataManager().getPlayerData(player).isSocialSpyEnabled();
    }

    public void toggleMessages(Player player) {
        boolean newState = !hasMessagesEnabled(player);
        
        // Create and call the event
        GianChatPrivateMessageToggleEvent event = new GianChatPrivateMessageToggleEvent(player, newState);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        setMessagesEnabled(player, event.isEnabled());
        player.sendMessage(plugin.getMessageManager().getMessage(
            event.isEnabled() ? "message_toggle.enabled" : "message_toggle.disabled"
        ));
    }

    public boolean hasMessagesEnabled(Player player) {
        return plugin.getDataManager().getPlayerData(player).isMessagesEnabled();
    }

    public void setMessagesEnabled(Player player, boolean enabled) {
        plugin.getDataManager().getPlayerData(player).setMessagesEnabled(enabled);
    }

    public Player getLastMessager(Player player) {
        UUID lastMessagerUUID = plugin.getDataManager().getPlayerData(player).getLastMessager();
        return lastMessagerUUID != null ? plugin.getServer().getPlayer(lastMessagerUUID) : null;
    }

    public void setLastMessager(Player player, Player lastMessager) {
        plugin.getDataManager().getPlayerData(player).setLastMessager(lastMessager.getUniqueId());
    }

    public Set<UUID> getIgnoredPlayers(Player player) {
        return plugin.getDataManager().getPlayerData(player).getIgnoredPlayers();
    }

    public boolean hasPlayerIgnored(Player player, Player target) {
        return getIgnoredPlayers(player).contains(target.getUniqueId());
    }

    public void toggleIgnorePlayer(Player player, Player target) {
        Set<UUID> ignoredPlayers = getIgnoredPlayers(player);
        UUID targetUUID = target.getUniqueId();
        
        if (ignoredPlayers.contains(targetUUID)) {
            ignoredPlayers.remove(targetUUID);
        } else {
            ignoredPlayers.add(targetUUID);
        }
    }

    public void setPlayerIgnored(Player player, Player target, boolean ignored) {
        // Call the event
        GianChatPlayerIgnoreEvent event = new GianChatPlayerIgnoreEvent(player, target, ignored);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        // Update the ignored status
        Set<UUID> ignoredPlayers = plugin.getDataManager().getPlayerData(player).getIgnoredPlayers();
        UUID targetUUID = target.getUniqueId();
        
        if (ignored) {
            ignoredPlayers.add(targetUUID);
        } else {
            ignoredPlayers.remove(targetUUID);
        }
        
        // Save the data
        plugin.getDataManager().savePlayerData(player);
    }

    private void startAutoSave() {
        int interval = plugin.getConfig().getInt("auto-save-interval", 5) * 20 * 60; // Convert minutes to ticks
        autoSaveTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                plugin.getDataManager().savePlayerData(player);
            }
        }, interval, interval);
    }

    public void cleanup() {
        plugin.getServer().getScheduler().cancelTask(autoSaveTaskId);
        // Ensure any pending changes are saved through DataManager
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getDataManager().savePlayerData(player);
        }
    }

    private void loadData() {
        // Load data for all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = plugin.getDataManager().getPlayerData(player);
            
            // Load social spy status
            if (data.isSocialSpyEnabled()) {
                socialSpyEnabled.add(player.getUniqueId());
            }
            
            // Load messages enabled status
            if (data.isMessagesEnabled()) {
                messagesEnabled.add(player.getUniqueId());
            }
            
            // Load ignored players
            Set<UUID> ignored = data.getIgnoredPlayers();
            if (!ignored.isEmpty()) {
                ignoredPlayers.put(player.getUniqueId(), new HashSet<>(ignored));
            }
            
            // Load last messager
            UUID lastMessager = data.getLastMessager();
            if (lastMessager != null) {
                lastMessagedPlayer.put(player.getUniqueId(), lastMessager);
            }
        }
    }

    public void loadPlayerData(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player);
        
        // Load social spy status
        if (data.isSocialSpyEnabled()) {
            socialSpyEnabled.add(player.getUniqueId());
        }
        
        // Load messages enabled status
        if (data.isMessagesEnabled()) {
            messagesEnabled.add(player.getUniqueId());
        }
        
        // Load ignored players
        Set<UUID> ignored = data.getIgnoredPlayers();
        if (!ignored.isEmpty()) {
            ignoredPlayers.put(player.getUniqueId(), new HashSet<>(ignored));
        }
        
        // Load last messager
        UUID lastMessager = data.getLastMessager();
        if (lastMessager != null) {
            lastMessagedPlayer.put(player.getUniqueId(), lastMessager);
        }
    }

    public void removePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        socialSpyEnabled.remove(uuid);
        messagesEnabled.remove(uuid);
        ignoredPlayers.remove(uuid);
        lastMessagedPlayer.remove(uuid);
    }
} 