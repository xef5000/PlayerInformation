package ca.xef5000.playerinformation.database;

import ca.xef5000.playerinformation.PlayerInformation;
import ca.xef5000.playerinformation.config.ConfigManager;
import ca.xef5000.playerinformation.config.InformationConfig;
import ca.xef5000.playerinformation.data.InformationDefinition;
import ca.xef5000.playerinformation.data.InformationType;
import ca.xef5000.playerinformation.data.PlayerInformationData;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Repository class that manages player data operations and caching
 */
public class PlayerDataRepository {
    private final PlayerInformation plugin;
    private final ConfigManager configManager;
    private final InformationConfig informationConfig;
    private DatabaseManager databaseManager;
    
    // Cache for player data
    private final Map<UUID, PlayerInformationData> playerDataCache = new ConcurrentHashMap<>();
    
    // Cache for quick lookups
    private final Set<UUID> loadedPlayers = ConcurrentHashMap.newKeySet();
    
    public PlayerDataRepository(PlayerInformation plugin, ConfigManager configManager, InformationConfig informationConfig) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.informationConfig = informationConfig;
    }
    
    /**
     * Initialize the repository and database connection
     */
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Create appropriate database manager based on config
                String databaseType = configManager.getDatabaseType();
                
                if ("mysql".equalsIgnoreCase(databaseType)) {
                    databaseManager = new MySQLDatabase(plugin, configManager);
                } else {
                    databaseManager = new SQLiteDatabase(plugin, configManager.getSqliteFilename());
                }
                
                // Initialize database
                databaseManager.initialize().join();
                
                // Migrate database schema
                List<InformationDefinition> definitions = new ArrayList<>(informationConfig.getAllDefinitions().values());
                databaseManager.migrateDatabase(definitions).join();
                
                plugin.getLogger().info("PlayerDataRepository initialized with " + databaseType + " database");
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to initialize PlayerDataRepository", e);
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Shutdown the repository and save all cached data
     */
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Save all cached data
                if (!playerDataCache.isEmpty()) {
                    plugin.getLogger().info("Saving " + playerDataCache.size() + " cached player data entries...");
                    List<PlayerInformationData> dataToSave = new ArrayList<>(playerDataCache.values());
                    databaseManager.savePlayerDataBatch(dataToSave).join();
                }
                
                // Clear cache
                playerDataCache.clear();
                loadedPlayers.clear();
                
                // Shutdown database
                if (databaseManager != null) {
                    databaseManager.shutdown().join();
                }
                
                plugin.getLogger().info("PlayerDataRepository shutdown complete");
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error during PlayerDataRepository shutdown", e);
            }
        });
    }
    
    /**
     * Load player data (from cache or database)
     */
    public CompletableFuture<PlayerInformationData> loadPlayerData(UUID playerUuid, String playerName) {
        // Check cache first
        PlayerInformationData cachedData = playerDataCache.get(playerUuid);
        if (cachedData != null) {
            return CompletableFuture.completedFuture(cachedData);
        }
        
        // Load from database
        return databaseManager.loadPlayerData(playerUuid).thenApply(data -> {
            if (data == null) {
                // Create new player data with default values
                data = createNewPlayerData(playerUuid, playerName);
            } else {
                // Update player name if it changed
                if (!data.getPlayerName().equals(playerName)) {
                    data = new PlayerInformationData(playerUuid, playerName, data.getAllData());
                    databaseManager.updatePlayerName(playerUuid, playerName);
                }
            }
            
            // Cache the data
            playerDataCache.put(playerUuid, data);
            loadedPlayers.add(playerUuid);
            
            return data;
        });
    }
    
    /**
     * Create new player data with default values
     */
    private PlayerInformationData createNewPlayerData(UUID playerUuid, String playerName) {
        PlayerInformationData data = new PlayerInformationData(playerUuid, playerName);
        
        // Set default values for all defined information
        for (InformationDefinition definition : informationConfig.getAllDefinitions().values()) {
            data.setValue(definition.getName(), definition.getDefaultValue());
        }
        
        return data;
    }
    
    /**
     * Save player data to database and update cache
     */
    public CompletableFuture<Void> savePlayerData(PlayerInformationData playerData) {
        // Update cache
        playerDataCache.put(playerData.getPlayerUuid(), playerData);
        
        // Save to database
        return databaseManager.savePlayerData(playerData);
    }
    
    /**
     * Get player data from cache (must be loaded first)
     */
    public PlayerInformationData getPlayerData(UUID playerUuid) {
        return playerDataCache.get(playerUuid);
    }
    
    /**
     * Check if player data is loaded in cache
     */
    public boolean isPlayerDataLoaded(UUID playerUuid) {
        return loadedPlayers.contains(playerUuid);
    }
    
    /**
     * Unload player data from cache (save first)
     */
    public CompletableFuture<Void> unloadPlayerData(UUID playerUuid) {
        PlayerInformationData data = playerDataCache.get(playerUuid);
        if (data == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        return databaseManager.savePlayerData(data).thenRun(() -> {
            playerDataCache.remove(playerUuid);
            loadedPlayers.remove(playerUuid);
        });
    }
    
    /**
     * Set a specific information value for a player
     */
    public CompletableFuture<Boolean> setPlayerInformation(UUID playerUuid, String playerName, String informationName, String value) {
        return CompletableFuture.supplyAsync(() -> {
            // Validate information exists
            InformationDefinition definition = informationConfig.getDefinition(informationName);
            if (definition == null) {
                return false;
            }
            
            // Validate value
            if (!definition.isValidValue(value)) {
                return false;
            }
            
            // Load player data if not cached
            PlayerInformationData playerData = playerDataCache.get(playerUuid);
            if (playerData == null) {
                playerData = loadPlayerData(playerUuid, playerName).join();
            }
            
            // Set the value
            playerData.setValue(informationName, value);
            
            // Save to database
            databaseManager.setPlayerInformation(playerUuid, playerName, informationName, value);
            
            return true;
        });
    }
    
    /**
     * Get a specific information value for a player
     */
    public CompletableFuture<String> getPlayerInformation(UUID playerUuid, String playerName, String informationName) {
        return CompletableFuture.supplyAsync(() -> {
            // Check if information exists
            InformationDefinition definition = informationConfig.getDefinition(informationName);
            if (definition == null) {
                return null;
            }
            
            // Load player data if not cached
            PlayerInformationData playerData = playerDataCache.get(playerUuid);
            if (playerData == null) {
                playerData = loadPlayerData(playerUuid, playerName).join();
            }
            
            String value = playerData.getValue(informationName);
            return value != null ? value : definition.getDefaultValue();
        });
    }
    
    /**
     * Add to an integer information value
     */
    public CompletableFuture<Integer> addToPlayerInformation(UUID playerUuid, String playerName, String informationName, int amount) {
        return CompletableFuture.supplyAsync(() -> {
            // Validate information exists and is integer type
            InformationDefinition definition = informationConfig.getDefinition(informationName);
            if (definition == null || definition.getType() != InformationType.INT) {
                return null;
            }
            
            // Load player data if not cached
            PlayerInformationData playerData = playerDataCache.get(playerUuid);
            if (playerData == null) {
                playerData = loadPlayerData(playerUuid, playerName).join();
            }
            
            // Add to the value
            int defaultValue = Integer.parseInt(definition.getDefaultValue());
            int newValue = playerData.addToIntValue(informationName, amount, defaultValue);
            
            // Save to database
            databaseManager.setPlayerInformation(playerUuid, playerName, informationName, String.valueOf(newValue));
            
            return newValue;
        });
    }
    
    /**
     * Reset a player's information to default value
     */
    public CompletableFuture<String> resetPlayerInformation(UUID playerUuid, String playerName, String informationName) {
        return CompletableFuture.supplyAsync(() -> {
            // Validate information exists
            InformationDefinition definition = informationConfig.getDefinition(informationName);
            if (definition == null) {
                return null;
            }
            
            String defaultValue = definition.getDefaultValue();
            
            // Set to default value
            setPlayerInformation(playerUuid, playerName, informationName, defaultValue).join();
            
            return defaultValue;
        });
    }
    
    /**
     * Get all information for a player
     */
    public CompletableFuture<Map<String, String>> getAllPlayerInformation(UUID playerUuid, String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            // Load player data if not cached
            PlayerInformationData playerData = playerDataCache.get(playerUuid);
            if (playerData == null) {
                playerData = loadPlayerData(playerUuid, playerName).join();
            }
            
            Map<String, String> result = new HashMap<>();
            
            // Get all defined information
            for (InformationDefinition definition : informationConfig.getAllDefinitions().values()) {
                String value = playerData.getValue(definition.getName());
                if (value == null) {
                    value = definition.getDefaultValue();
                }
                result.put(definition.getName(), value);
            }
            
            return result;
        });
    }
    
    /**
     * Reload database schema based on current information definitions
     */
    public CompletableFuture<Void> reloadSchema() {
        List<InformationDefinition> definitions = new ArrayList<>(informationConfig.getAllDefinitions().values());
        return databaseManager.migrateDatabase(definitions);
    }
    
    /**
     * Get database statistics
     */
    public CompletableFuture<String> getDatabaseStats() {
        return databaseManager.getDatabaseStats();
    }
    
    /**
     * Check if database is healthy
     */
    public CompletableFuture<Boolean> isDatabaseHealthy() {
        return databaseManager.isHealthy();
    }
    
    /**
     * Get the number of cached players
     */
    public int getCachedPlayerCount() {
        return playerDataCache.size();
    }
    
    /**
     * Get all cached player UUIDs
     */
    public Set<UUID> getCachedPlayerUuids() {
        return new HashSet<>(playerDataCache.keySet());
    }
    
    /**
     * Force save all cached data
     */
    public CompletableFuture<Void> saveAllCachedData() {
        if (playerDataCache.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        
        List<PlayerInformationData> dataToSave = new ArrayList<>(playerDataCache.values());
        return databaseManager.savePlayerDataBatch(dataToSave);
    }
}
