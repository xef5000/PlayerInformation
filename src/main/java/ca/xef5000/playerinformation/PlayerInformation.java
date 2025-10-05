package ca.xef5000.playerinformation;

import ca.xef5000.playerinformation.api.PlayerInformationAPI;
import ca.xef5000.playerinformation.api.PlayerInformationService;
import ca.xef5000.playerinformation.commands.InformationCommand;
import ca.xef5000.playerinformation.commands.InformationTabCompleter;
import ca.xef5000.playerinformation.config.ConfigManager;
import ca.xef5000.playerinformation.database.PlayerDataRepository;
import ca.xef5000.playerinformation.listeners.PlayerJoinQuitListener;
import ca.xef5000.playerinformation.listeners.ChatInputListener;
import ca.xef5000.playerinformation.placeholders.PlayerInformationExpansion;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class PlayerInformation extends JavaPlugin {

    private ConfigManager configManager;
    private PlayerDataRepository dataRepository;
    private PlayerInformationAPI api;
    private PlayerInformationExpansion placeholderExpansion;
    private ChatInputListener chatInputListener;

    @Override
    public void onEnable() {
        try {
            getLogger().info("Starting PlayerInformation plugin...");

            // Initialize configuration
            configManager = new ConfigManager(this);
            configManager.loadConfigs();

            // Initialize data repository
            dataRepository = new PlayerDataRepository(this, configManager, configManager.getInformationConfig());
            dataRepository.initialize().join();

            // Initialize API
            api = new PlayerInformationService(configManager.getInformationConfig(), dataRepository);

            // Register API service
            Bukkit.getServicesManager().register(PlayerInformationAPI.class, api, this, ServicePriority.Normal);

            // Register commands
            registerCommands();

            // Register event listeners
            registerListeners();

            // Register PlaceholderAPI expansion if available
            registerPlaceholderExpansion();

            getLogger().info("PlayerInformation plugin enabled successfully!");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable PlayerInformation plugin", e);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            getLogger().info("Shutting down PlayerInformation plugin...");

            // Unregister PlaceholderAPI expansion
            if (placeholderExpansion != null) {
                placeholderExpansion.unregister();
            }

            // Shutdown data repository
            if (dataRepository != null) {
                dataRepository.shutdown().join();
            }

            // Unregister API service
            Bukkit.getServicesManager().unregister(PlayerInformationAPI.class, api);

            getLogger().info("PlayerInformation plugin disabled successfully!");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during plugin shutdown", e);
        }
    }

    /**
     * Register plugin commands
     */
    private void registerCommands() {
        PluginCommand informationCommand = getCommand("information");
        if (informationCommand != null) {
            InformationCommand commandExecutor = new InformationCommand(this, configManager,
                configManager.getInformationConfig(), dataRepository);
            InformationTabCompleter tabCompleter = new InformationTabCompleter(configManager.getInformationConfig());

            informationCommand.setExecutor(commandExecutor);
            informationCommand.setTabCompleter(tabCompleter);
        }
    }

    /**
     * Register event listeners
     */
    private void registerListeners() {
        PlayerJoinQuitListener joinQuitListener = new PlayerJoinQuitListener(this, dataRepository);
        Bukkit.getPluginManager().registerEvents(joinQuitListener, this);

        // Chat input listener for GUI string inputs
        chatInputListener = new ChatInputListener(this, dataRepository);
        Bukkit.getPluginManager().registerEvents(chatInputListener, this);
    }

    /**
     * Get the chat input listener (used by GUIs to prompt for chat input)
     */
    public ChatInputListener getChatInputListener() {
        return chatInputListener;
    }

    /**
     * Register PlaceholderAPI expansion if PlaceholderAPI is available
     */
    private void registerPlaceholderExpansion() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                placeholderExpansion = new PlayerInformationExpansion(this, api);
                placeholderExpansion.register();
                getLogger().info("PlaceholderAPI expansion registered successfully!");
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Failed to register PlaceholderAPI expansion", e);
            }
        } else {
            getLogger().info("PlaceholderAPI not found, placeholders will not be available");
        }
    }

    /**
     * Get the configuration manager
     * @return Configuration manager
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * Get the data repository
     * @return Data repository
     */
    public PlayerDataRepository getDataRepository() {
        return dataRepository;
    }

    /**
     * Get the API instance
     * @return API instance
     */
    public PlayerInformationAPI getAPI() {
        return api;
    }

    /**
     * Reload the plugin
     */
    public void reloadPlugin() {
        try {
            getLogger().info("Reloading PlayerInformation plugin...");

            // Reload configurations
            configManager.reloadConfigs();

            // Reload database schema
            dataRepository.reloadSchema().join();

            getLogger().info("Plugin reloaded successfully!");

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during plugin reload", e);
            throw new RuntimeException("Plugin reload failed", e);
        }
    }
}
