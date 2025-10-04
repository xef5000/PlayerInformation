package ca.xef5000.playerinformation.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents all information data for a specific player
 */
public class PlayerInformationData {
    private final UUID playerUuid;
    private final String playerName;
    private final Map<String, String> data;
    
    public PlayerInformationData(UUID playerUuid, String playerName) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.data = new HashMap<>();
    }
    
    public PlayerInformationData(UUID playerUuid, String playerName, Map<String, String> data) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.data = new HashMap<>(data);
    }
    
    /**
     * Get the player's UUID
     * @return Player UUID
     */
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    /**
     * Get the player's name
     * @return Player name
     */
    public String getPlayerName() {
        return playerName;
    }
    
    /**
     * Get a specific information value
     * @param informationName The name of the information
     * @return The value as string, or null if not set
     */
    public String getValue(String informationName) {
        return data.get(informationName);
    }
    
    /**
     * Get a specific information value with a default fallback
     * @param informationName The name of the information
     * @param defaultValue The default value to return if not set
     * @return The value as string, or the default value if not set
     */
    public String getValue(String informationName, String defaultValue) {
        return data.getOrDefault(informationName, defaultValue);
    }
    
    /**
     * Set a specific information value
     * @param informationName The name of the information
     * @param value The value to set
     */
    public void setValue(String informationName, String value) {
        if (value == null) {
            data.remove(informationName);
        } else {
            data.put(informationName, value);
        }
    }
    
    /**
     * Get an integer value
     * @param informationName The name of the information
     * @param defaultValue The default value if not set or invalid
     * @return The integer value
     */
    public int getIntValue(String informationName, int defaultValue) {
        String value = getValue(informationName);
        if (value == null) return defaultValue;
        
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Set an integer value
     * @param informationName The name of the information
     * @param value The integer value to set
     */
    public void setIntValue(String informationName, int value) {
        setValue(informationName, String.valueOf(value));
    }
    
    /**
     * Add to an integer value
     * @param informationName The name of the information
     * @param amount The amount to add
     * @param defaultValue The default value if the information doesn't exist
     * @return The new value after addition
     */
    public int addToIntValue(String informationName, int amount, int defaultValue) {
        int currentValue = getIntValue(informationName, defaultValue);
        int newValue = currentValue + amount;
        setIntValue(informationName, newValue);
        return newValue;
    }
    
    /**
     * Get a boolean value
     * @param informationName The name of the information
     * @param defaultValue The default value if not set
     * @return The boolean value
     */
    public boolean getBooleanValue(String informationName, boolean defaultValue) {
        String value = getValue(informationName);
        if (value == null) return defaultValue;
        
        return Boolean.parseBoolean(value);
    }
    
    /**
     * Set a boolean value
     * @param informationName The name of the information
     * @param value The boolean value to set
     */
    public void setBooleanValue(String informationName, boolean value) {
        setValue(informationName, String.valueOf(value));
    }
    
    /**
     * Get a UUID value
     * @param informationName The name of the information
     * @return The UUID value, or null if not set or invalid
     */
    public UUID getUuidValue(String informationName) {
        String value = getValue(informationName);
        if (value == null) return null;
        
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Set a UUID value
     * @param informationName The name of the information
     * @param value The UUID value to set
     */
    public void setUuidValue(String informationName, UUID value) {
        setValue(informationName, value != null ? value.toString() : null);
    }
    
    /**
     * Check if the player has a specific information set
     * @param informationName The name of the information
     * @return true if the information is set, false otherwise
     */
    public boolean hasInformation(String informationName) {
        return data.containsKey(informationName);
    }
    
    /**
     * Remove a specific information
     * @param informationName The name of the information to remove
     */
    public void removeInformation(String informationName) {
        data.remove(informationName);
    }
    
    /**
     * Get all information data
     * @return Map of all information data
     */
    public Map<String, String> getAllData() {
        return new HashMap<>(data);
    }
    
    /**
     * Clear all information data
     */
    public void clearAllData() {
        data.clear();
    }
    
    @Override
    public String toString() {
        return "PlayerInformationData{" +
                "playerUuid=" + playerUuid +
                ", playerName='" + playerName + '\'' +
                ", dataCount=" + data.size() +
                '}';
    }
}
