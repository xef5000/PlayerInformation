package ca.xef5000.playerinformation.placeholders;

import ca.xef5000.playerinformation.PlayerInformation;
import ca.xef5000.playerinformation.api.PlayerInformationAPI;
import ca.xef5000.playerinformation.data.InformationDefinition;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * PlaceholderAPI expansion for PlayerInformation
 * Provides placeholders in the format: %playerinformation_<information_name>%
 */
public class PlayerInformationExpansion extends PlaceholderExpansion {
    private final PlayerInformation plugin;
    private final PlayerInformationAPI api;
    
    public PlayerInformationExpansion(PlayerInformation plugin, PlayerInformationAPI api) {
        this.plugin = plugin;
        this.api = api;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "playerinformation";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true; // This expansion should persist through PlaceholderAPI reloads
    }
    
    @Override
    public boolean canRegister() {
        return true;
    }
    
    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return null;
        }
        
        // Handle different placeholder formats
        String[] parts = params.split("_", 2);
        
        if (parts.length == 1) {
            // Simple format: %playerinformation_<information_name>%
            String informationName = parts[0];
            return getPlayerInformationValue(player, informationName);
        } else if (parts.length == 2) {
            // Extended format: %playerinformation_<information_name>_<modifier>%
            String informationName = parts[0];
            String modifier = parts[1];
            
            switch (modifier.toLowerCase()) {
                case "default":
                    return getDefaultValue(informationName);
                case "type":
                    return getInformationType(informationName);
                case "exists":
                    return String.valueOf(api.hasInformationDefinition(informationName));
                default:
                    // If modifier is not recognized, treat the whole thing as information name
                    return getPlayerInformationValue(player, params);
            }
        }
        
        return null;
    }
    
    /**
     * Get player information value
     */
    private String getPlayerInformationValue(OfflinePlayer player, String informationName) {
        try {
            // Check if information definition exists
            if (!api.hasInformationDefinition(informationName)) {
                return null;
            }
            
            // Get the value asynchronously with a timeout
            CompletableFuture<String> future = api.getPlayerInformation(player.getUniqueId(), informationName);
            String value = future.get(1, TimeUnit.SECONDS); // 1 second timeout
            
            // Return the value or default if null
            if (value == null) {
                return api.getDefaultValue(informationName);
            }
            
            return value;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error getting placeholder value for " + informationName + ": " + e.getMessage());
            return api.getDefaultValue(informationName);
        }
    }
    
    /**
     * Get default value for an information
     */
    private String getDefaultValue(String informationName) {
        String defaultValue = api.getDefaultValue(informationName);
        return defaultValue != null ? defaultValue : "N/A";
    }
    
    /**
     * Get information type
     */
    private String getInformationType(String informationName) {
        var type = api.getInformationType(informationName);
        return type != null ? type.name() : "UNKNOWN";
    }
}
