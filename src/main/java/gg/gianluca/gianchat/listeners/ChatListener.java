package gg.gianluca.gianchat.listeners;

import gg.gianluca.gianchat.GianChat;
import gg.gianluca.gianchat.events.GianChatChatEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;


public class ChatListener implements Listener {
    private final GianChat plugin;
    private boolean consoleEnabled;
    private String consoleFormat;

    public ChatListener(GianChat plugin) {
        this.plugin = plugin;
        loadConsoleConfig();
    }

    public void loadConsoleConfig() {
        this.consoleEnabled = plugin.getConfig().getBoolean("send-to-console.enabled", true);
        this.consoleFormat = plugin.getConfig().getString("send-to-console.format", "[GianChat] %player% -> %message%");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        event.setCancelled(true);
        
        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        
        // Send to console if enabled
        if (consoleEnabled) {
            String consoleMessage = consoleFormat
                .replace("%player%", player.getName())
                .replace("%message%", message);
            plugin.getLogger().info(consoleMessage);
        }
        
        plugin.getFormatManager().getFormatForPlayer(player).ifPresent(format -> {
            // Process mentions first
            Component messageComponent = MiniMessage.miniMessage().deserialize(message);
            String rawMessage = message;
            Component processedMessage = plugin.getMentionManager().processMentions(player, messageComponent, rawMessage);
            
            // Get the format string and process PlaceholderAPI placeholders first
            String formatStr = MiniMessage.miniMessage().serialize(format.buildComponent());
            formatStr = PlaceholderAPI.setPlaceholders(player, formatStr);
            
            // Process message with PlaceholderAPI
            String processedMessageStr = PlainTextComponentSerializer.plainText().serialize(processedMessage);
            String papiProcessedMessage = PlaceholderAPI.setPlaceholders(player, processedMessageStr);
            
            // Create the message component with processed placeholders
            Component messageContentComponent = MiniMessage.miniMessage().deserialize(papiProcessedMessage);
            
            // Add click event if configured
            if (format.getMessageClickEvent() != null) {
                String value = PlaceholderAPI.setPlaceholders(player, format.getMessageClickEvent().value());
                messageContentComponent = messageContentComponent.clickEvent(ClickEvent.clickEvent(
                    format.getMessageClickEvent().action(),
                    value
                ));
            }
            
            // Replace %message% placeholder in the format with the actual message
            String finalStr = formatStr.replace("%message%", MiniMessage.miniMessage().serialize(messageContentComponent));
            Component finalMessage = MiniMessage.miniMessage().deserialize(finalStr);
            
            // Call our custom event directly since it's now async
            GianChatChatEvent chatEvent = new GianChatChatEvent(player, finalMessage, format.getName());
            Bukkit.getPluginManager().callEvent(chatEvent);
            
            if (!chatEvent.isCancelled()) {
                // Send message to all players who haven't ignored the sender
                for (var viewer : event.viewers()) {
                    if (viewer instanceof Player recipient && !plugin.getPrivateMessageManager().hasPlayerIgnored(recipient, player)) {
                        // Get personalized message for the recipient if they were mentioned
                        Component personalMessage = plugin.getMentionManager().getPersonalizedMessage(processedMessage, recipient);
                        String personalizedStr = formatStr.replace("%message%", MiniMessage.miniMessage().serialize(personalMessage));
                        // Process PlaceholderAPI placeholders for the recipient
                        personalizedStr = PlaceholderAPI.setPlaceholders(recipient, personalizedStr);
                        Component personalizedFinal = MiniMessage.miniMessage().deserialize(personalizedStr);
                        recipient.sendMessage(personalizedFinal);
                    }
                }
            }
            
            // Clean up personalized messages
            plugin.getMentionManager().clearPersonalizedMessage(processedMessage);
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Load player data
        plugin.getDataManager().loadPlayerData(player);
        
        // Load format
        String format = plugin.getDataManager().getPlayerData(player).getFormat();
        if (format != null) {
            plugin.getFormatManager().setPlayerFormat(player, plugin.getFormatManager().getFormat(format));
        }
        
        // Load message settings
        plugin.getPrivateMessageManager().loadPlayerData(player);
        
        // Load mention settings
        plugin.getMentionManager().loadPlayerData(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Save all player data
        plugin.getDataManager().savePlayerData(player);
        
        // Clean up format data
        plugin.getFormatManager().removePlayerFormat(player);
        
        // Clean up message manager data
        plugin.getPrivateMessageManager().removePlayerData(player);
        
        // Clean up mention manager data
        plugin.getMentionManager().removePlayerData(player);
    }
}