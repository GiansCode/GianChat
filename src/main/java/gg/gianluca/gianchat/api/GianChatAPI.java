package gg.gianluca.gianchat.api;

import gg.gianluca.gianchat.GianChat;
import gg.gianluca.gianchat.format.ChatFormat;
import gg.gianluca.gianchat.format.FormatManager;
import gg.gianluca.gianchat.messaging.PrivateMessageManager;
import gg.gianluca.gianchat.mentions.MentionManager;
import gg.gianluca.gianchat.data.DataManager;
import gg.gianluca.gianchat.messages.MessageManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Public API for the GianChat plugin.
 * This class provides methods to interact with various features of GianChat.
 */
public class GianChatAPI {
    private static GianChatAPI instance;
    private final GianChat plugin;

    public GianChatAPI(GianChat plugin) {
        this.plugin = plugin;
    }

    /**
     * Gets the instance of the GianChatAPI.
     *
     * @return The API instance
     * @throws IllegalStateException if the API hasn't been initialized
     */
    public static GianChatAPI getInstance() {
        if (instance == null) {
            throw new IllegalStateException("GianChatAPI has not been initialized!");
        }
        return instance;
    }

    /**
     * Initializes the GianChatAPI.
     * This should only be called by the GianChat plugin.
     *
     * @param plugin The GianChat plugin instance
     */
    public static void init(GianChat plugin) {
        if (instance != null) {
            throw new IllegalStateException("GianChatAPI has already been initialized!");
        }
        instance = new GianChatAPI(plugin);
    }

    /**
     * Gets all available chat formats.
     *
     * @return List of all chat formats
     */
    public Collection<ChatFormat> getFormats() {
        return plugin.getFormatManager().getFormats();
    }

    /**
     * Gets a chat format by name.
     *
     * @param name The name of the format
     * @return The format, or null if not found
     */
    @Nullable
    public ChatFormat getFormat(String name) {
        return plugin.getFormatManager().getFormat(name);
    }

    /**
     * Gets the format for a specific player.
     *
     * @param player The player
     * @return The player's format, or the default format if none is set
     */
    @NotNull
    public ChatFormat getPlayerFormat(Player player) {
        return plugin.getFormatManager().getPlayerFormat(player);
    }

    /**
     * Sets a player's chat format.
     *
     * @param player The player
     * @param format The format to set
     */
    public void setPlayerFormat(Player player, ChatFormat format) {
        plugin.getFormatManager().setPlayerFormat(player, format);
    }

    /**
     * Checks if a player has messages enabled.
     *
     * @param player The player to check
     * @return true if messages are enabled, false otherwise
     */
    public boolean hasMessagesEnabled(Player player) {
        return plugin.getPrivateMessageManager().hasMessagesEnabled(player);
    }

    /**
     * Sets whether a player has messages enabled.
     *
     * @param player The player
     * @param enabled Whether messages should be enabled
     */
    public void setMessagesEnabled(Player player, boolean enabled) {
        plugin.getPrivateMessageManager().setMessagesEnabled(player, enabled);
    }

    /**
     * Gets the last player who messaged the specified player.
     *
     * @param player The player
     * @return The last messager, or null if none
     */
    @Nullable
    public Player getLastMessager(Player player) {
        return plugin.getPrivateMessageManager().getLastMessager(player);
    }

    /**
     * Sends a private message between two players.
     *
     * @param sender The sender
     * @param recipient The recipient
     * @param message The message
     * @return true if the message was sent successfully, false if blocked (e.g., by ignore)
     */
    public boolean sendPrivateMessage(Player sender, Player recipient, String message) {
        return plugin.getPrivateMessageManager().sendPrivateMessage(sender, recipient, message);
    }

    /**
     * Checks if a player has ignored another player.
     *
     * @param player The player who might have ignored someone
     * @param target The potentially ignored player
     * @return true if player has ignored target, false otherwise
     */
    public boolean hasPlayerIgnored(Player player, Player target) {
        return plugin.getPrivateMessageManager().hasPlayerIgnored(player, target);
    }

    /**
     * Sets whether a player is ignoring another player.
     *
     * @param player The player doing the ignoring
     * @param target The player being ignored
     * @param ignored Whether target should be ignored
     */
    public void setPlayerIgnored(Player player, Player target, boolean ignored) {
        plugin.getPrivateMessageManager().setPlayerIgnored(player, target, ignored);
    }

    /**
     * Checks if mentions are enabled globally.
     *
     * @return true if mentions are enabled, false otherwise
     */
    public boolean areMentionsEnabled() {
        return plugin.getMentionManager().isEnabled();
    }

    /**
     * Gets whether a player has mentions enabled.
     *
     * @param player The player to check
     * @return true if the player has mentions enabled, false otherwise
     */
    public boolean hasMentionsEnabled(Player player) {
        return plugin.getMentionManager().hasMentionsEnabled(player);
    }

    /**
     * Sets whether a player has mentions enabled.
     *
     * @param player The player
     * @param enabled Whether mentions should be enabled
     */
    public void setMentionsEnabled(Player player, boolean enabled) {
        plugin.getMentionManager().setMentionsEnabled(player, enabled);
    }

    /**
     * Gets the FormatManager instance.
     *
     * @return The FormatManager
     */
    public FormatManager getFormatManager() {
        return plugin.getFormatManager();
    }

    /**
     * Gets the PrivateMessageManager instance.
     *
     * @return The PrivateMessageManager
     */
    public PrivateMessageManager getPrivateMessageManager() {
        return plugin.getPrivateMessageManager();
    }

    /**
     * Gets the MentionManager instance.
     *
     * @return The MentionManager
     */
    public MentionManager getMentionManager() {
        return plugin.getMentionManager();
    }

    /**
     * Gets the DataManager instance.
     *
     * @return The DataManager
     */
    public DataManager getDataManager() {
        return plugin.getDataManager();
    }

    /**
     * Gets the MessageManager instance.
     *
     * @return The MessageManager
     */
    public MessageManager getMessageManager() {
        return plugin.getMessageManager();
    }

    /**
     * Reloads all configurations and data.
     */
    public void reload() {
        plugin.reloadConfig();
        plugin.getFormatManager().loadFormats();
        plugin.getPrivateMessageManager().loadConfig();
        plugin.getMentionManager().loadConfig();
        plugin.getDataManager().loadData();
    }
} 