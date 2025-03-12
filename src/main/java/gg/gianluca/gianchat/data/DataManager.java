package gg.gianluca.gianchat.data;

import gg.gianluca.gianchat.GianChat;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class DataManager {
    private final GianChat plugin;
    private final Map<UUID, PlayerData> playerData;
    private final File dataFolder;
    private final File dataFile;
    private YamlConfiguration config;
    private BukkitTask autoSaveTask;

    public DataManager(GianChat plugin) {
        this.plugin = plugin;
        this.playerData = new HashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        this.dataFile = new File(dataFolder, "playerdata.yml");
        ensureDirectoryExists();
    }

    private void ensureDirectoryExists() {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public void loadData() {
        // Create data file if it doesn't exist
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create playerdata.yml: " + e.getMessage());
                return;
            }
        }

        // Load configuration
        config = YamlConfiguration.loadConfiguration(dataFile);

        // Load player data
        if (config.contains("players")) {
            for (String uuidStr : config.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    PlayerData data = new PlayerData();
                    
                    String path = "players." + uuidStr + ".";
                    data.setMessagesEnabled(config.getBoolean(path + "messages-enabled", true));
                    data.setSocialSpyEnabled(config.getBoolean(path + "social-spy-enabled", false));
                    
                    // Load ignored players
                    if (config.contains(path + "ignored-players")) {
                        data.setIgnoredPlayers(config.getStringList(path + "ignored-players").stream()
                            .map(UUID::fromString)
                            .collect(Collectors.toSet()));
                    }
                    
                    // Load last messager
                    String lastMessagerStr = config.getString(path + "last-messager");
                    if (lastMessagerStr != null) {
                        data.setLastMessager(UUID.fromString(lastMessagerStr));
                    }
                    
                    playerData.put(uuid, data);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in playerdata.yml: " + uuidStr);
                }
            }
        }

        startAutoSave();
    }

    public void saveData() {
        if (config == null) {
            config = new YamlConfiguration();
        }

        // Clear existing data
        config.set("players", null);

        // Save player data
        for (Map.Entry<UUID, PlayerData> entry : playerData.entrySet()) {
            String path = "players." + entry.getKey().toString() + ".";
            PlayerData data = entry.getValue();

            config.set(path + "messages-enabled", data.isMessagesEnabled());
            config.set(path + "social-spy-enabled", data.isSocialSpyEnabled());
            
            // Save ignored players
            config.set(path + "ignored-players", 
                data.getIgnoredPlayers().stream()
                    .map(UUID::toString)
                    .collect(Collectors.toList()));
            
            // Save last messager
            if (data.getLastMessager() != null) {
                config.set(path + "last-messager", data.getLastMessager().toString());
            }
        }

        // Save to file
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save playerdata.yml: " + e.getMessage());
        }
    }

    public void startAutoSave() {
        // Cancel existing task if any
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }

        // Get auto-save interval from config (in minutes)
        int intervalMinutes = plugin.getConfig().getInt("auto-save-interval", 5);
        long intervalTicks = intervalMinutes * 60L * 20L; // Convert minutes to ticks

        // Start new auto-save task
        autoSaveTask = plugin.getServer().getScheduler().runTaskTimer(
            plugin,
            this::saveData,
            intervalTicks,
            intervalTicks
        );
    }

    public void cleanup() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
        saveData();
    }

    public PlayerData getPlayerData(Player player) {
        return playerData.computeIfAbsent(player.getUniqueId(), k -> new PlayerData());
    }

    public void removePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        playerData.remove(uuid);
    }

    public void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        if (!playerData.containsKey(uuid)) {
            playerData.put(uuid, new PlayerData());
        }
        
        PlayerData data = playerData.get(uuid);
        String path = "players." + uuid;
        
        if (config.contains(path)) {
            data.setFormat(config.getString(path + ".format"));
            data.setMessagesEnabled(config.getBoolean(path + ".messages-enabled", true));
            data.setSocialSpyEnabled(config.getBoolean(path + ".social-spy", false));
            data.setMentionsEnabled(config.getBoolean(path + ".mentions-enabled", true));
            
            if (config.contains(path + ".last-messager")) {
                data.setLastMessager(UUID.fromString(config.getString(path + ".last-messager")));
            }
            
            if (config.contains(path + ".ignored-players")) {
                for (String ignoredUUID : config.getStringList(path + ".ignored-players")) {
                    data.getIgnoredPlayers().add(UUID.fromString(ignoredUUID));
                }
            }
        }
    }

    public void savePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = playerData.get(uuid);
        if (data == null) return;
        
        String path = "players." + uuid;
        config.set(path + ".format", data.getFormat());
        config.set(path + ".messages-enabled", data.isMessagesEnabled());
        config.set(path + ".social-spy", data.isSocialSpyEnabled());
        config.set(path + ".mentions-enabled", data.hasMentionsEnabled());
        
        if (data.getLastMessager() != null) {
            config.set(path + ".last-messager", data.getLastMessager().toString());
        }
        
        if (!data.getIgnoredPlayers().isEmpty()) {
            config.set(path + ".ignored-players", 
                data.getIgnoredPlayers().stream()
                    .map(UUID::toString)
                    .toList());
        }
        
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data for player " + player.getName() + ": " + e.getMessage());
        }
    }

    public void saveAllData() {
        for (Map.Entry<UUID, PlayerData> entry : playerData.entrySet()) {
            String path = "players." + entry.getKey();
            PlayerData data = entry.getValue();
            
            config.set(path + ".format", data.getFormat());
            config.set(path + ".messages-enabled", data.isMessagesEnabled());
            config.set(path + ".social-spy", data.isSocialSpyEnabled());
            config.set(path + ".mentions-enabled", data.hasMentionsEnabled());
            
            if (data.getLastMessager() != null) {
                config.set(path + ".last-messager", data.getLastMessager().toString());
            }
            
            if (!data.getIgnoredPlayers().isEmpty()) {
                config.set(path + ".ignored-players", 
                    data.getIgnoredPlayers().stream()
                        .map(UUID::toString)
                        .toList());
            }
        }
        
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }
} 