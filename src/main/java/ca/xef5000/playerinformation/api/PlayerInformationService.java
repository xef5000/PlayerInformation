package ca.xef5000.playerinformation.api;

import ca.xef5000.playerinformation.config.InformationConfig;
import ca.xef5000.playerinformation.data.InformationDefinition;
import ca.xef5000.playerinformation.data.InformationType;
import ca.xef5000.playerinformation.data.PlayerInformationData;
import ca.xef5000.playerinformation.database.PlayerDataRepository;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of PlayerInformationAPI
 */
public class PlayerInformationService implements PlayerInformationAPI {
    private final InformationConfig informationConfig;
    private final PlayerDataRepository dataRepository;
    
    public PlayerInformationService(InformationConfig informationConfig, PlayerDataRepository dataRepository) {
        this.informationConfig = informationConfig;
        this.dataRepository = dataRepository;
    }
    
    // ===== INFORMATION DEFINITIONS =====
    
    @Override
    public Map<String, InformationDefinition> getAllInformationDefinitions() {
        return informationConfig.getAllDefinitions();
    }
    
    @Override
    public InformationDefinition getInformationDefinition(String informationName) {
        return informationConfig.getDefinition(informationName);
    }
    
    @Override
    public boolean hasInformationDefinition(String informationName) {
        return informationConfig.hasDefinition(informationName);
    }
    
    @Override
    public Set<String> getInformationNames() {
        return informationConfig.getInformationNames();
    }
    
    @Override
    public List<InformationDefinition> getInformationDefinitionsByType(InformationType type) {
        return informationConfig.getDefinitionsByType(type);
    }
    
    // ===== PLAYER DATA OPERATIONS =====
    
    @Override
    public CompletableFuture<String> getPlayerInformation(UUID playerUuid, String informationName) {
        return dataRepository.getPlayerInformation(playerUuid, "", informationName);
    }
    
    @Override
    public CompletableFuture<String> getPlayerInformation(Player player, String informationName) {
        return dataRepository.getPlayerInformation(player.getUniqueId(), player.getName(), informationName);
    }
    
    @Override
    public CompletableFuture<Boolean> setPlayerInformation(UUID playerUuid, String playerName, String informationName, String value) {
        return dataRepository.setPlayerInformation(playerUuid, playerName, informationName, value);
    }
    
    @Override
    public CompletableFuture<Boolean> setPlayerInformation(Player player, String informationName, String value) {
        return dataRepository.setPlayerInformation(player.getUniqueId(), player.getName(), informationName, value);
    }
    
    @Override
    public CompletableFuture<Integer> addToPlayerInformation(UUID playerUuid, String playerName, String informationName, int amount) {
        return dataRepository.addToPlayerInformation(playerUuid, playerName, informationName, amount);
    }
    
    @Override
    public CompletableFuture<Integer> addToPlayerInformation(Player player, String informationName, int amount) {
        return dataRepository.addToPlayerInformation(player.getUniqueId(), player.getName(), informationName, amount);
    }
    
    @Override
    public CompletableFuture<String> resetPlayerInformation(UUID playerUuid, String playerName, String informationName) {
        return dataRepository.resetPlayerInformation(playerUuid, playerName, informationName);
    }
    
    @Override
    public CompletableFuture<String> resetPlayerInformation(Player player, String informationName) {
        return dataRepository.resetPlayerInformation(player.getUniqueId(), player.getName(), informationName);
    }
    
    @Override
    public CompletableFuture<Map<String, String>> getAllPlayerInformation(UUID playerUuid, String playerName) {
        return dataRepository.getAllPlayerInformation(playerUuid, playerName);
    }
    
    @Override
    public CompletableFuture<Map<String, String>> getAllPlayerInformation(Player player) {
        return dataRepository.getAllPlayerInformation(player.getUniqueId(), player.getName());
    }
    
    @Override
    public CompletableFuture<PlayerInformationData> getPlayerData(UUID playerUuid, String playerName) {
        return dataRepository.loadPlayerData(playerUuid, playerName);
    }
    
    @Override
    public CompletableFuture<PlayerInformationData> getPlayerData(Player player) {
        return dataRepository.loadPlayerData(player.getUniqueId(), player.getName());
    }
    
    // ===== CONVENIENCE METHODS FOR SPECIFIC TYPES =====
    
    @Override
    public CompletableFuture<Integer> getPlayerInformationInt(UUID playerUuid, String informationName, int defaultValue) {
        return getPlayerInformation(playerUuid, informationName).thenApply(value -> {
            if (value == null) return defaultValue;
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        });
    }
    
    @Override
    public CompletableFuture<Integer> getPlayerInformationInt(Player player, String informationName, int defaultValue) {
        return getPlayerInformationInt(player.getUniqueId(), informationName, defaultValue);
    }
    
    @Override
    public CompletableFuture<Boolean> setPlayerInformationInt(UUID playerUuid, String playerName, String informationName, int value) {
        return setPlayerInformation(playerUuid, playerName, informationName, String.valueOf(value));
    }
    
    @Override
    public CompletableFuture<Boolean> setPlayerInformationInt(Player player, String informationName, int value) {
        return setPlayerInformationInt(player.getUniqueId(), player.getName(), informationName, value);
    }
    
    @Override
    public CompletableFuture<Boolean> getPlayerInformationBoolean(UUID playerUuid, String informationName, boolean defaultValue) {
        return getPlayerInformation(playerUuid, informationName).thenApply(value -> {
            if (value == null) return defaultValue;
            return Boolean.parseBoolean(value);
        });
    }
    
    @Override
    public CompletableFuture<Boolean> getPlayerInformationBoolean(Player player, String informationName, boolean defaultValue) {
        return getPlayerInformationBoolean(player.getUniqueId(), informationName, defaultValue);
    }
    
    @Override
    public CompletableFuture<Boolean> setPlayerInformationBoolean(UUID playerUuid, String playerName, String informationName, boolean value) {
        return setPlayerInformation(playerUuid, playerName, informationName, String.valueOf(value));
    }
    
    @Override
    public CompletableFuture<Boolean> setPlayerInformationBoolean(Player player, String informationName, boolean value) {
        return setPlayerInformationBoolean(player.getUniqueId(), player.getName(), informationName, value);
    }
    
    @Override
    public CompletableFuture<UUID> getPlayerInformationUUID(UUID playerUuid, String informationName) {
        return getPlayerInformation(playerUuid, informationName).thenApply(value -> {
            if (value == null) return null;
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException e) {
                return null;
            }
        });
    }
    
    @Override
    public CompletableFuture<UUID> getPlayerInformationUUID(Player player, String informationName) {
        return getPlayerInformationUUID(player.getUniqueId(), informationName);
    }
    
    @Override
    public CompletableFuture<Boolean> setPlayerInformationUUID(UUID playerUuid, String playerName, String informationName, UUID value) {
        return setPlayerInformation(playerUuid, playerName, informationName, value != null ? value.toString() : null);
    }
    
    @Override
    public CompletableFuture<Boolean> setPlayerInformationUUID(Player player, String informationName, UUID value) {
        return setPlayerInformationUUID(player.getUniqueId(), player.getName(), informationName, value);
    }
    
    // ===== VALIDATION METHODS =====
    
    @Override
    public boolean isValidValue(String informationName, String value) {
        return informationConfig.isValidValue(informationName, value);
    }
    
    @Override
    public String getDefaultValue(String informationName) {
        return informationConfig.getDefaultValue(informationName);
    }
    
    @Override
    public InformationType getInformationType(String informationName) {
        return informationConfig.getInformationType(informationName);
    }
}
