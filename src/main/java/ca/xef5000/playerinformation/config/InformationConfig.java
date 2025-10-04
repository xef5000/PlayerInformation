package ca.xef5000.playerinformation.config;

import ca.xef5000.playerinformation.PlayerInformation;
import ca.xef5000.playerinformation.data.InformationDefinition;
import ca.xef5000.playerinformation.data.InformationType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages information definitions from informations.yml
 */
public class InformationConfig {
    private final PlayerInformation plugin;
    private final Map<String, InformationDefinition> definitions;
    private FileConfiguration informationsConfig;
    
    public InformationConfig(PlayerInformation plugin) {
        this.plugin = plugin;
        this.definitions = new HashMap<>();
    }
    
    /**
     * Load information definitions from informations.yml
     */
    public void loadInformations() {
        definitions.clear();
        
        File informationsFile = new File(plugin.getDataFolder(), "informations.yml");
        if (!informationsFile.exists()) {
            plugin.getLogger().warning("informations.yml not found, creating default file");
            return;
        }
        
        informationsConfig = YamlConfiguration.loadConfiguration(informationsFile);
        
        for (String key : informationsConfig.getKeys(false)) {
            try {
                InformationDefinition definition = loadDefinition(key, informationsConfig.getConfigurationSection(key));
                if (definition != null) {
                    definitions.put(key, definition);
                    plugin.getLogger().info("Loaded information definition: " + key + " (" + definition.getType() + ")");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load information definition: " + key, e);
            }
        }
        
        plugin.getLogger().info("Loaded " + definitions.size() + " information definitions");
    }
    
    /**
     * Load a single information definition from a configuration section
     * @param name The name of the information
     * @param section The configuration section
     * @return The loaded definition, or null if invalid
     */
    private InformationDefinition loadDefinition(String name, ConfigurationSection section) {
        if (section == null) {
            plugin.getLogger().warning("Invalid configuration section for information: " + name);
            return null;
        }
        
        // Get type
        String typeString = section.getString("type");
        if (typeString == null) {
            plugin.getLogger().warning("No type specified for information: " + name);
            return null;
        }
        
        InformationType type;
        try {
            type = InformationType.valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid type '" + typeString + "' for information: " + name);
            return null;
        }
        
        // Get default value
        String defaultValue = section.getString("default");
        if (defaultValue == null) {
            defaultValue = type.getDefaultValue();
        }
        
        // Get type-specific configurations
        List<String> enumValues = null;
        List<String> multiEnumValues = null;
        List<String> ladderValues = null;
        String permissionNode = null;
        
        if (type == InformationType.ENUM) {
            enumValues = section.getStringList("values");
            if (enumValues.isEmpty()) {
                plugin.getLogger().warning("No values specified for enum information: " + name);
                return null;
            }
            
            // Validate default value is in enum values
            if (!enumValues.contains(defaultValue)) {
                plugin.getLogger().warning("Default value '" + defaultValue + "' not in enum values for: " + name);
                defaultValue = enumValues.get(0);
            }
        }

        if (type == InformationType.MULTIENUM) {
            multiEnumValues = section.getStringList("values");
            if (multiEnumValues.isEmpty()) {
                plugin.getLogger().warning("No values specified for multienum information: " + name);
                return null;
            }

            // Validate default value is in enum values
            if (!multiEnumValues.contains(defaultValue)) {
                plugin.getLogger().warning("Default value '" + defaultValue + "' not in multienum values for: " + name);
                defaultValue = multiEnumValues.get(0);
            }
        }
        
        if (type == InformationType.LADDER) {
            ladderValues = section.getStringList("ladder");
            if (ladderValues.isEmpty()) {
                plugin.getLogger().warning("No ladder values specified for ladder information: " + name);
                return null;
            }
            
            // Validate default value is in ladder values
            if (!ladderValues.contains(defaultValue)) {
                plugin.getLogger().warning("Default value '" + defaultValue + "' not in ladder values for: " + name);
                defaultValue = ladderValues.get(0);
            }
        }
        
        if (type == InformationType.PERMISSION) {
            permissionNode = section.getString("permission-node");
            if (permissionNode == null) {
                plugin.getLogger().warning("No permission-node specified for permission information: " + name);
                permissionNode = "playerinformation.permission." + name.toLowerCase();
            }
        }
        
        // Validate the default value for the type
        InformationDefinition tempDef = new InformationDefinition(name, type, defaultValue, enumValues, ladderValues, permissionNode);
        if (!type.isValidValue(defaultValue, tempDef)) {
            plugin.getLogger().warning("Invalid default value '" + defaultValue + "' for type " + type + " in information: " + name);
            defaultValue = type.getDefaultValue();
        }
        
        return new InformationDefinition(name, type, defaultValue, enumValues, ladderValues, permissionNode, multiEnumValues);
    }
    
    /**
     * Get all information definitions
     * @return Map of information definitions
     */
    public Map<String, InformationDefinition> getAllDefinitions() {
        return new HashMap<>(definitions);
    }
    
    /**
     * Get a specific information definition
     * @param name The name of the information
     * @return The definition, or null if not found
     */
    public InformationDefinition getDefinition(String name) {
        return definitions.get(name);
    }
    
    /**
     * Check if an information definition exists
     * @param name The name of the information
     * @return true if the definition exists
     */
    public boolean hasDefinition(String name) {
        return definitions.containsKey(name);
    }
    
    /**
     * Get all information names
     * @return Set of information names
     */
    public Set<String> getInformationNames() {
        return new HashSet<>(definitions.keySet());
    }
    
    /**
     * Get all definitions of a specific type
     * @param type The information type
     * @return List of definitions of the specified type
     */
    public List<InformationDefinition> getDefinitionsByType(InformationType type) {
        List<InformationDefinition> result = new ArrayList<>();
        for (InformationDefinition definition : definitions.values()) {
            if (definition.getType() == type) {
                result.add(definition);
            }
        }
        return result;
    }
    
    /**
     * Validate if a value is valid for a specific information
     * @param informationName The name of the information
     * @param value The value to validate
     * @return true if valid, false otherwise
     */
    public boolean isValidValue(String informationName, String value) {
        InformationDefinition definition = getDefinition(informationName);
        if (definition == null) {
            return false;
        }
        return definition.isValidValue(value);
    }
    
    /**
     * Get the default value for a specific information
     * @param informationName The name of the information
     * @return The default value, or null if information doesn't exist
     */
    public String getDefaultValue(String informationName) {
        InformationDefinition definition = getDefinition(informationName);
        if (definition == null) {
            return null;
        }
        return definition.getDefaultValue();
    }
    
    /**
     * Get the type of a specific information
     * @param informationName The name of the information
     * @return The information type, or null if information doesn't exist
     */
    public InformationType getInformationType(String informationName) {
        InformationDefinition definition = getDefinition(informationName);
        if (definition == null) {
            return null;
        }
        return definition.getType();
    }
    
    /**
     * Add a new information definition dynamically
     * @param definition The definition to add
     */
    public void addDefinition(InformationDefinition definition) {
        definitions.put(definition.getName(), definition);
        plugin.getLogger().info("Added new information definition: " + definition.getName());
    }
    
    /**
     * Remove an information definition
     * @param name The name of the information to remove
     * @return true if removed, false if not found
     */
    public boolean removeDefinition(String name) {
        InformationDefinition removed = definitions.remove(name);
        if (removed != null) {
            plugin.getLogger().info("Removed information definition: " + name);
            return true;
        }
        return false;
    }
}
