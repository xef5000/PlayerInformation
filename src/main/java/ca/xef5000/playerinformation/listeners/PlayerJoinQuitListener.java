package ca.xef5000.playerinformation.listeners;

import ca.xef5000.playerinformation.PlayerInformation;
import ca.xef5000.playerinformation.database.PlayerDataRepository;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.logging.Level;

/**
 * Listener for player join and quit events to manage data loading and saving
 */
public class PlayerJoinQuitListener implements Listener {
    private final PlayerInformation plugin;
    private final PlayerDataRepository dataRepository;
    
    public PlayerJoinQuitListener(PlayerInformation plugin, PlayerDataRepository dataRepository) {
        this.plugin = plugin;
        this.dataRepository = dataRepository;
    }
    
    /**
     * Handle player join - load their data
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Load player data asynchronously
        dataRepository.loadPlayerData(player.getUniqueId(), player.getName())
            .thenAccept(playerData -> {
                if (playerData != null) {
                    plugin.getLogger().info("Loaded data for player: " + player.getName());
                } else {
                    plugin.getLogger().info("Created new data for player: " + player.getName());
                }
            })
            .exceptionally(throwable -> {
                plugin.getLogger().log(Level.SEVERE, "Failed to load data for player: " + player.getName(), throwable);
                return null;
            });
    }
    
    /**
     * Handle player quit - save and unload their data
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Save and unload player data asynchronously
        dataRepository.unloadPlayerData(player.getUniqueId())
            .thenRun(() -> {
                plugin.getLogger().info("Saved and unloaded data for player: " + player.getName());
            })
            .exceptionally(throwable -> {
                plugin.getLogger().log(Level.SEVERE, "Failed to save data for player: " + player.getName(), throwable);
                return null;
            });
    }
}
