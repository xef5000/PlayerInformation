package ca.xef5000.playerinformation.database;

import ca.xef5000.playerinformation.data.InformationDefinition;
import ca.xef5000.playerinformation.data.PlayerInformationData;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for database operations
 */
public interface DatabaseManager {
    
    /**
     * Initialize the database connection and create necessary tables
     * @return CompletableFuture that completes when initialization is done
     */
    CompletableFuture<Void> initialize();
    
    /**
     * Close the database connection
     * @return CompletableFuture that completes when shutdown is done
     */
    CompletableFuture<Void> shutdown();
    
    /**
     * Create or update table structure based on information definitions
     * @param definitions List of information definitions
     * @return CompletableFuture that completes when migration is done
     */
    CompletableFuture<Void> migrateDatabase(List<InformationDefinition> definitions);
    
    /**
     * Load all player data from the database
     * @return CompletableFuture containing map of UUID to PlayerInformationData
     */
    CompletableFuture<List<PlayerInformationData>> loadAllPlayerData();
    
    /**
     * Load player data for a specific player
     * @param playerUuid The player's UUID
     * @return CompletableFuture containing the player's data, or null if not found
     */
    CompletableFuture<PlayerInformationData> loadPlayerData(UUID playerUuid);
    
    /**
     * Save player data to the database
     * @param playerData The player data to save
     * @return CompletableFuture that completes when save is done
     */
    CompletableFuture<Void> savePlayerData(PlayerInformationData playerData);
    
    /**
     * Save multiple player data entries to the database
     * @param playerDataList List of player data to save
     * @return CompletableFuture that completes when save is done
     */
    CompletableFuture<Void> savePlayerDataBatch(List<PlayerInformationData> playerDataList);
    
    /**
     * Delete player data from the database
     * @param playerUuid The player's UUID
     * @return CompletableFuture that completes when deletion is done
     */
    CompletableFuture<Void> deletePlayerData(UUID playerUuid);
    
    /**
     * Set a specific information value for a player
     * @param playerUuid The player's UUID
     * @param playerName The player's name
     * @param informationName The information name
     * @param value The value to set
     * @return CompletableFuture that completes when update is done
     */
    CompletableFuture<Void> setPlayerInformation(UUID playerUuid, String playerName, String informationName, String value);
    
    /**
     * Get a specific information value for a player
     * @param playerUuid The player's UUID
     * @param informationName The information name
     * @return CompletableFuture containing the value, or null if not found
     */
    CompletableFuture<String> getPlayerInformation(UUID playerUuid, String informationName);
    
    /**
     * Remove a specific information for a player (set to null)
     * @param playerUuid The player's UUID
     * @param informationName The information name
     * @return CompletableFuture that completes when removal is done
     */
    CompletableFuture<Void> removePlayerInformation(UUID playerUuid, String informationName);
    
    /**
     * Check if the database connection is healthy
     * @return CompletableFuture containing true if healthy, false otherwise
     */
    CompletableFuture<Boolean> isHealthy();
    
    /**
     * Get the number of players in the database
     * @return CompletableFuture containing the player count
     */
    CompletableFuture<Integer> getPlayerCount();
    
    /**
     * Get a list of all player UUIDs in the database
     * @return CompletableFuture containing list of UUIDs
     */
    CompletableFuture<List<UUID>> getAllPlayerUuids();
    
    /**
     * Check if a player exists in the database
     * @param playerUuid The player's UUID
     * @return CompletableFuture containing true if player exists, false otherwise
     */
    CompletableFuture<Boolean> playerExists(UUID playerUuid);
    
    /**
     * Update player name in the database
     * @param playerUuid The player's UUID
     * @param newName The new player name
     * @return CompletableFuture that completes when update is done
     */
    CompletableFuture<Void> updatePlayerName(UUID playerUuid, String newName);
    
    /**
     * Get database statistics
     * @return CompletableFuture containing database statistics as a formatted string
     */
    CompletableFuture<String> getDatabaseStats();
}
