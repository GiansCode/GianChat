package gg.gianluca.gianchat;

import gg.gianluca.gianchat.commands.*;
import gg.gianluca.gianchat.format.FormatManager;
import gg.gianluca.gianchat.messaging.PrivateMessageManager;
import gg.gianluca.gianchat.mentions.MentionManager;
import gg.gianluca.gianchat.placeholders.GianChatExpansion;
import gg.gianluca.gianchat.data.DataManager;
import gg.gianluca.gianchat.messages.MessageManager;
import gg.gianluca.gianchat.listeners.*;
import gg.gianluca.gianchat.api.GianChatAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

public class GianChat extends JavaPlugin {
    private FormatManager formatManager;
    private PrivateMessageManager privateMessageManager;
    private MentionManager mentionManager;
    private DataManager dataManager;
    private MessageManager messageManager;
    private GianChatAPI api;

    @Override
    public void onEnable() {
        // Create plugin directory if it doesn't exist
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // Save default config files
        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("privatemessages.yml", false);
        
        // Generate placeholders.txt
        generatePlaceholdersFile();

        // Initialize API
        GianChatAPI.init(this);
        
        this.messageManager = new MessageManager(this);
        this.dataManager = new DataManager(this);
        this.dataManager.loadData();
        this.dataManager.startAutoSave();
        
        this.formatManager = new FormatManager(this);
        this.formatManager.loadFormats();
        
        this.privateMessageManager = new PrivateMessageManager(this);
        this.mentionManager = new MentionManager(this);
        this.api = new GianChatAPI(this);
        
        // Register PlaceholderAPI expansion
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new GianChatExpansion(this).register();
        }
        
        // Register commands
        getCommand("gianchat").setExecutor(new GianChatCommand(this));
        getCommand("msg").setExecutor(new MessageCommand(this, privateMessageManager));
        getCommand("reply").setExecutor(new ReplyCommand(this, privateMessageManager));
        getCommand("msgtoggle").setExecutor(new MessageToggleCommand(this));
        getCommand("socialspy").setExecutor(new SocialSpyCommand(this, privateMessageManager));
        getCommand("ignore").setExecutor(new IgnoreCommand(this, privateMessageManager));
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.cleanup();
        }
        if (privateMessageManager != null) {
            privateMessageManager.cleanup();
        }
        if (mentionManager != null) {
            mentionManager.saveData();
        }
        getLogger().info("GianChat has been disabled!");
    }

    public MentionManager getMentionManager() {
        return mentionManager;
    }

    public FormatManager getFormatManager() {
        return formatManager;
    }

    public PrivateMessageManager getPrivateMessageManager() {
        return privateMessageManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public GianChatAPI getApi() {
        return api;
    }

    public void reload() {
        reloadConfig();
        formatManager.loadFormats();
        privateMessageManager.loadConfig();
        mentionManager.loadConfig();
        dataManager.loadData();
    }

    private void generatePlaceholdersFile() {
        File file = new File(getDataFolder(), "placeholders.txt");
        if (!file.exists()) {
            try {
                file.createNewFile();
                List<String> placeholders = Arrays.asList(
                    "Available Placeholders:",
                    "",
                    "General Placeholders:",
                    "%player_name% - Player's name",
                    "%player_displayname% - Player's display name",
                    "%player_uuid% - Player's UUID",
                    "",
                    "Vault Placeholders:",
                    "%vault_prefix% - Player's prefix from permissions plugin",
                    "%vault_suffix% - Player's suffix from permissions plugin",
                    "%vault_group% - Player's primary group",
                    "",
                    "Statistics Placeholders:",
                    "%statistic_hours_played% - Player's total hours played",
                    "%statistic_blocks_broken% - Total blocks broken by player",
                    "%statistic_blocks_placed% - Total blocks placed by player",
                    "",
                    "Private Message Placeholders:",
                    "%sender_player_name% - Name of the player sending the message",
                    "%recipient_player_name% - Name of the player receiving the message",
                    "%sender_vault_group% - Group of the player sending the message",
                    "%recipient_vault_group% - Group of the player receiving the message",
                    "",
                    "Mention Placeholders:",
                    "%mentioned_player_name% - Name of the mentioned player",
                    "%mentioner_player_name% - Name of the player who mentioned someone",
                    "",
                    "Note: All placeholders support PlaceholderAPI expansion placeholders."
                );
                Files.write(file.toPath(), placeholders, StandardCharsets.UTF_8);
            } catch (IOException e) {
                getLogger().warning("Failed to create placeholders.txt: " + e.getMessage());
            }
        }
    }
}