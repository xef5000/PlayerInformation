package ca.xef5000.playerinformation.config;

import ca.xef5000.playerinformation.PlayerInformation;
import ca.xef5000.playerinformation.data.InformationType;
import com.github.xef5000.itemsapi.ItemsAPI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.logging.Level;

/**
 * Manages plugin configuration files
 */
public class ConfigManager {
    private final PlayerInformation plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private InformationConfig informationConfig;
    
    public ConfigManager(PlayerInformation plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Load all configuration files
     */
    public void loadConfigs() {
        // Save default configs if they don't exist
        plugin.saveDefaultConfig();
        saveDefaultConfig("messages.yml");
        saveDefaultConfig("informations.yml");
        
        // Load main config
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        // Load messages config
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Load information definitions
        informationConfig = new InformationConfig(plugin);
        informationConfig.loadInformations();
        
        plugin.getLogger().info("Configuration files loaded successfully");
    }
    
    /**
     * Reload all configuration files
     */
    public void reloadConfigs() {
        try {
            loadConfigs();
            plugin.getLogger().info("Configuration files reloaded successfully");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error reloading configuration files", e);
        }
    }
    
    /**
     * Save a default configuration file if it doesn't exist
     * @param fileName The name of the file to save
     */
    private void saveDefaultConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
    }
    
    /**
     * Get the main plugin configuration
     * @return Main configuration
     */
    public FileConfiguration getConfig() {
        return config;
    }
    
    /**
     * Get the messages configuration
     * @return Messages configuration
     */
    public FileConfiguration getMessages() {
        return messages;
    }
    
    /**
     * Get the information definitions configuration
     * @return Information configuration
     */
    public InformationConfig getInformationConfig() {
        return informationConfig;
    }
    
    /**
     * Get a message from the messages configuration with prefix
     * @param path The path to the message
     * @return The formatted message
     */
    public String getMessage(String path) {
        String prefix = messages.getString("prefix", "&8[&6PlayerInformation&8]&r ");
        String message = messages.getString(path, "&cMessage not found: " + path);
        return colorize(prefix + message);
    }
    
    /**
     * Get a message from the messages configuration without prefix
     * @param path The path to the message
     * @return The formatted message
     */
    public String getMessageNoPrefix(String path) {
        String message = messages.getString(path, "&cMessage not found: " + path);
        return colorize(message);
    }
    
    /**
     * Get a message with placeholder replacement
     * @param path The path to the message
     * @param placeholders Placeholder replacements (key, value pairs)
     * @return The formatted message with placeholders replaced
     */
    public String getMessage(String path, String... placeholders) {
        String message = getMessage(path);
        
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            String placeholder = placeholders[i];
            String value = placeholders[i + 1];
            message = message.replace("{" + placeholder + "}", value);
        }
        
        return message;
    }
    
    /**
     * Convert color codes to Minecraft colors
     * @param text The text to colorize
     * @return Colorized text
     */
    private String colorize(String text) {
        return text.replace('&', '§');
    }
    
    /**
     * Get database type from config
     * @return Database type (sqlite or mysql)
     */
    public String getDatabaseType() {
        return config.getString("database.type", "sqlite").toLowerCase();
    }
    
    /**
     * Get SQLite filename from config
     * @return SQLite filename
     */
    public String getSqliteFilename() {
        return config.getString("database.sqlite.filename", "playerdata.db");
    }
    
    /**
     * Get MySQL host from config
     * @return MySQL host
     */
    public String getMysqlHost() {
        return config.getString("database.mysql.host", "localhost");
    }
    
    /**
     * Get MySQL port from config
     * @return MySQL port
     */
    public int getMysqlPort() {
        return config.getInt("database.mysql.port", 3306);
    }
    
    /**
     * Get MySQL database name from config
     * @return MySQL database name
     */
    public String getMysqlDatabase() {
        return config.getString("database.mysql.database", "playerinformation");
    }
    
    /**
     * Get MySQL username from config
     * @return MySQL username
     */
    public String getMysqlUsername() {
        return config.getString("database.mysql.username", "root");
    }
    
    /**
     * Get MySQL password from config
     * @return MySQL password
     */
    public String getMysqlPassword() {
        return config.getString("database.mysql.password", "");
    }
    
    /**
     * Get MySQL maximum pool size from config
     * @return Maximum pool size
     */
    public int getMysqlMaxPoolSize() {
        return config.getInt("database.mysql.pool.maximum-pool-size", 10);
    }
    
    /**
     * Get MySQL minimum idle connections from config
     * @return Minimum idle connections
     */
    public int getMysqlMinIdle() {
        return config.getInt("database.mysql.pool.minimum-idle", 2);
    }
    
    /**
     * Get MySQL connection timeout from config
     * @return Connection timeout in milliseconds
     */
    public long getMysqlConnectionTimeout() {
        return config.getLong("database.mysql.pool.connection-timeout", 30000);
    }
    
    /**
     * Get MySQL idle timeout from config
     * @return Idle timeout in milliseconds
     */
    public long getMysqlIdleTimeout() {
        return config.getLong("database.mysql.pool.idle-timeout", 600000);
    }
    
    /**
     * Get MySQL max lifetime from config
     * @return Max lifetime in milliseconds
     */
    public long getMysqlMaxLifetime() {
        return config.getLong("database.mysql.pool.max-lifetime", 1800000);
    }
    
    /**
     * Check if debug mode is enabled
     * @return true if debug mode is enabled
     */
    public boolean isDebugEnabled() {
        return config.getBoolean("settings.debug", false);
    }
    
    /**
     * Check if auto-create definitions is enabled
     * @return true if auto-create definitions is enabled
     */
    public boolean isAutoCreateDefinitionsEnabled() {
        return config.getBoolean("settings.auto-create-definitions", false);
    }
    
    /**
     * Get GUI management title
     * @return GUI management title
     */
    public String getGuiManagementTitle() {
        return config.getString("gui.management-title", "Player Information - {player}");
    }
    
    /**
     * Get GUI edit title
     * @return GUI edit title
     */
    public String getGuiEditTitle() {
        return config.getString("gui.edit-title", "Edit {data} for {player}");
    }

    /**
     * Get an ItemStack to use for displaying an information type in GUIs.
     * Reads configuration section 'display-items.<type>' and falls back to a simple material mapping.
     * This will support a simple 'material' key (e.g. GOLD_NUGGET) and basic item meta (display name/lore) if present.
     */
    public ItemStack getDisplayItem(InformationType type) {
        if (config == null) return new ItemStack(Material.PAPER);

        String path = "display-items." + type.name().toLowerCase();
        ConfigurationSection section = config.getConfigurationSection(path);


        assert section != null;
        return ItemsAPI.fromConfiguration(section);
    }
}
