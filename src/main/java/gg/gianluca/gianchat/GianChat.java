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
        File placeholdersFile = new File(getDataFolder(), "placeholders.txt");
        try {
            String content = """
                GianChat PlaceholderAPI Placeholders
                ==================================

                These placeholders are provided by GianChat through PlaceholderAPI.
                Make sure you have PlaceholderAPI installed to use these placeholders.

                Format Placeholders
                -------------------
                %gianchat_format_name%
                Description: Gets the name of the player's current chat format
                Example: %gianchat_format_name% -> VIP

                %gianchat_format_prefix%
                Description: Gets the prefix of the player's current chat format
                Example: %gianchat_format_prefix% -> [VIP]

                %gianchat_format_name_format%
                Description: Gets the name format of the player's current chat format
                Example: %gianchat_format_name_format% -> <gold>%player_name%

                %gianchat_format_separator%
                Description: Gets the separator of the player's current chat format
                Example: %gianchat_format_separator% -> :

                Message Placeholders
                -------------------
                %gianchat_message_toggle%
                Description: Returns "enabled" if the player has private messages enabled, "disabled" otherwise
                Example: %gianchat_message_toggle% -> enabled

                %gianchat_message_last_messager%
                Description: Gets the name of the last player who messaged the player
                Example: %gianchat_message_last_messager% -> Player123

                %gianchat_message_social_spy%
                Description: Returns "enabled" if the player has social spy enabled, "disabled" otherwise
                Example: %gianchat_message_social_spy% -> disabled

                %gianchat_message_ignored_count%
                Description: Gets the number of players the player is currently ignoring
                Example: %gianchat_message_ignored_count% -> 3

                Mention Placeholders
                -------------------
                %gianchat_mention_enabled%
                Description: Returns "true" if mentions are globally enabled, "false" otherwise
                Example: %gianchat_mention_enabled% -> true

                Note: All placeholders require the player to be online to work.
                Note: These placeholders are provided through PlaceholderAPI and require it to be installed.
                """;
            Files.writeString(placeholdersFile.toPath(), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            getLogger().warning("Failed to generate placeholders.txt: " + e.getMessage());
        }
    }
}