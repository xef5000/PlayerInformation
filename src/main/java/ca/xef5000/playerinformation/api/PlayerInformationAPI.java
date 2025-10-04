package ca.xef5000.playerinformation.api;

import ca.xef5000.playerinformation.data.InformationDefinition;
import ca.xef5000.playerinformation.data.InformationType;
import ca.xef5000.playerinformation.data.PlayerInformationData;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Public API interface for PlayerInformation plugin
 * This interface allows other plugins to interact with player information data
 */
public interface PlayerInformationAPI {
    
    // ===== INFORMATION DEFINITIONS =====
    
    /**
     * Get all available information definitions
     * @return Map of information name to definition
     */
    Map<String, InformationDefinition> getAllInformationDefinitions();
    
    /**
     * Get a specific information definition
     * @param informationName The name of the information
     * @return The definition, or null if not found
     */
    InformationDefinition getInformationDefinition(String informationName);
    
    /**
     * Check if an information definition exists
     * @param informationName The name of the information
     * @return true if the definition exists
     */
    boolean hasInformationDefinition(String informationName);
    
    /**
     * Get all information names
     * @return Set of information names
     */
    Set<String> getInformationNames();
    
    /**
     * Get all definitions of a specific type
     * @param type The information type
     * @return List of definitions of the specified type
     */
    List<InformationDefinition> getInformationDefinitionsByType(InformationType type);
    
    // ===== PLAYER DATA OPERATIONS =====
    
    /**
     * Get a specific information value for a player
     * @param playerUuid The player's UUID
     * @param informationName The information name
     * @return CompletableFuture containing the value, or null if not found
     */
    CompletableFuture<String> getPlayerInformation(UUID playerUuid, String informationName);
    
    /**
     * Get a specific information value for an online player
     * @param player The player
     * @param informationName The information name
     * @return CompletableFuture containing the value, or null if not found
     */
    CompletableFuture<String> getPlayerInformation(Player player, String informationName);
    
    /**
     * Set a specific information value for a player
     * @param playerUuid The player's UUID
     * @param playerName The player's name
     * @param informationName The information name
     * @param value The value to set
     * @return CompletableFuture containing true if successful, false otherwise
     */
    CompletableFuture<Boolean> setPlayerInformation(UUID playerUuid, String playerName, String informationName, String value);
    
    /**
     * Set a specific information value for an online player
     * @param player The player
     * @param informationName The information name
     * @param value The value to set
     * @return CompletableFuture containing true if successful, false otherwise
     */
    CompletableFuture<Boolean> setPlayerInformation(Player player, String informationName, String value);
    
    /**
     * Add to an integer information value for a player
     * @param playerUuid The player's UUID
     * @param playerName The player's name
     * @param informationName The information name (must be INT type)
     * @param amount The amount to add
     * @return CompletableFuture containing the new value, or null if failed
     */
    CompletableFuture<Integer> addToPlayerInformation(UUID playerUuid, String playerName, String informationName, int amount);
    
    /**
     * Add to an integer information value for an online player
     * @param player The player
     * @param informationName The information name (must be INT type)
     * @param amount The amount to add
     * @return CompletableFuture containing the new value, or null if failed
     */
    CompletableFuture<Integer> addToPlayerInformation(Player player, String informationName, int amount);
    
    /**
     * Reset a player's information to default value
     * @param playerUuid The player's UUID
     * @param playerName The player's name
     * @param informationName The information name
     * @return CompletableFuture containing the default value, or null if failed
     */
    CompletableFuture<String> resetPlayerInformation(UUID playerUuid, String playerName, String informationName);
    
    /**
     * Reset a player's information to default value
     * @param player The player
     * @param informationName The information name
     * @return CompletableFuture containing the default value, or null if failed
     */
    CompletableFuture<String> resetPlayerInformation(Player player, String informationName);
    
    /**
     * Get all information for a player
     * @param playerUuid The player's UUID
     * @param playerName The player's name
     * @return CompletableFuture containing map of information name to value
     */
    CompletableFuture<Map<String, String>> getAllPlayerInformation(UUID playerUuid, String playerName);
    
    /**
     * Get all information for an online player
     * @param player The player
     * @return CompletableFuture containing map of information name to value
     */
    CompletableFuture<Map<String, String>> getAllPlayerInformation(Player player);
    
    /**
     * Get complete player information data
     * @param playerUuid The player's UUID
     * @param playerName The player's name
     * @return CompletableFuture containing PlayerInformationData, or null if not found
     */
    CompletableFuture<PlayerInformationData> getPlayerData(UUID playerUuid, String playerName);
    
    /**
     * Get complete player information data for an online player
     * @param player The player
     * @return CompletableFuture containing PlayerInformationData, or null if not found
     */
    CompletableFuture<PlayerInformationData> getPlayerData(Player player);
    
    // ===== CONVENIENCE METHODS FOR SPECIFIC TYPES =====
    
    /**
     * Get an integer information value for a player
     * @param playerUuid The player's UUID
     * @param informationName The information name
     * @param defaultValue The default value if not found or invalid
     * @return CompletableFuture containing the integer value
     */
    CompletableFuture<Integer> getPlayerInformationInt(UUID playerUuid, String informationName, int defaultValue);
    
    /**
     * Get an integer information value for an online player
     * @param player The player
     * @param informationName The information name
     * @param defaultValue The default value if not found or invalid
     * @return CompletableFuture containing the integer value
     */
    CompletableFuture<Integer> getPlayerInformationInt(Player player, String informationName, int defaultValue);
    
    /**
     * Set an integer information value for a player
     * @param playerUuid The player's UUID
     * @param playerName The player's name
     * @param informationName The information name
     * @param value The integer value to set
     * @return CompletableFuture containing true if successful, false otherwise
     */
    CompletableFuture<Boolean> setPlayerInformationInt(UUID playerUuid, String playerName, String informationName, int value);
    
    /**
     * Set an integer information value for an online player
     * @param player The player
     * @param informationName The information name
     * @param value The integer value to set
     * @return CompletableFuture containing true if successful, false otherwise
     */
    CompletableFuture<Boolean> setPlayerInformationInt(Player player, String informationName, int value);
    
    /**
     * Get a boolean information value for a player
     * @param playerUuid The player's UUID
     * @param informationName The information name
     * @param defaultValue The default value if not found or invalid
     * @return CompletableFuture containing the boolean value
     */
    CompletableFuture<Boolean> getPlayerInformationBoolean(UUID playerUuid, String informationName, boolean defaultValue);
    
    /**
     * Get a boolean information value for an online player
     * @param player The player
     * @param informationName The information name
     * @param defaultValue The default value if not found or invalid
     * @return CompletableFuture containing the boolean value
     */
    CompletableFuture<Boolean> getPlayerInformationBoolean(Player player, String informationName, boolean defaultValue);
    
    /**
     * Set a boolean information value for a player
     * @param playerUuid The player's UUID
     * @param playerName The player's name
     * @param informationName The information name
     * @param value The boolean value to set
     * @return CompletableFuture containing true if successful, false otherwise
     */
    CompletableFuture<Boolean> setPlayerInformationBoolean(UUID playerUuid, String playerName, String informationName, boolean value);
    
    /**
     * Set a boolean information value for an online player
     * @param player The player
     * @param informationName The information name
     * @param value The boolean value to set
     * @return CompletableFuture containing true if successful, false otherwise
     */
    CompletableFuture<Boolean> setPlayerInformationBoolean(Player player, String informationName, boolean value);
    
    /**
     * Get a UUID information value for a player
     * @param playerUuid The player's UUID
     * @param informationName The information name
     * @return CompletableFuture containing the UUID value, or null if not found or invalid
     */
    CompletableFuture<UUID> getPlayerInformationUUID(UUID playerUuid, String informationName);
    
    /**
     * Get a UUID information value for an online player
     * @param player The player
     * @param informationName The information name
     * @return CompletableFuture containing the UUID value, or null if not found or invalid
     */
    CompletableFuture<UUID> getPlayerInformationUUID(Player player, String informationName);
    
    /**
     * Set a UUID information value for a player
     * @param playerUuid The player's UUID
     * @param playerName The player's name
     * @param informationName The information name
     * @param value The UUID value to set
     * @return CompletableFuture containing true if successful, false otherwise
     */
    CompletableFuture<Boolean> setPlayerInformationUUID(UUID playerUuid, String playerName, String informationName, UUID value);
    
    /**
     * Set a UUID information value for an online player
     * @param player The player
     * @param informationName The information name
     * @param value The UUID value to set
     * @return CompletableFuture containing true if successful, false otherwise
     */
    CompletableFuture<Boolean> setPlayerInformationUUID(Player player, String informationName, UUID value);
    
    // ===== VALIDATION METHODS =====
    
    /**
     * Validate if a value is valid for a specific information
     * @param informationName The information name
     * @param value The value to validate
     * @return true if valid, false otherwise
     */
    boolean isValidValue(String informationName, String value);
    
    /**
     * Get the default value for a specific information
     * @param informationName The information name
     * @return The default value, or null if information doesn't exist
     */
    String getDefaultValue(String informationName);
    
    /**
     * Get the type of a specific information
     * @param informationName The information name
     * @return The information type, or null if information doesn't exist
     */
    InformationType getInformationType(String informationName);
}
