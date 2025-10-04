package ca.xef5000.playerinformation.commands;

import ca.xef5000.playerinformation.PlayerInformation;
import ca.xef5000.playerinformation.config.ConfigManager;
import ca.xef5000.playerinformation.config.InformationConfig;
import ca.xef5000.playerinformation.data.InformationDefinition;
import ca.xef5000.playerinformation.data.InformationType;
import ca.xef5000.playerinformation.database.PlayerDataRepository;
import ca.xef5000.playerinformation.gui.ManagementGUI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Main command handler for /information command
 */
public class InformationCommand implements CommandExecutor {
    private final PlayerInformation plugin;
    private final ConfigManager configManager;
    private final InformationConfig informationConfig;
    private final PlayerDataRepository dataRepository;
    
    public InformationCommand(PlayerInformation plugin, ConfigManager configManager, 
                            InformationConfig informationConfig, PlayerDataRepository dataRepository) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.informationConfig = informationConfig;
        this.dataRepository = dataRepository;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(configManager.getMessage("command.usage"));
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "set":
                return handleSetCommand(sender, args);
            case "get":
                return handleGetCommand(sender, args);
            case "add":
                return handleAddCommand(sender, args);
            case "remove":
                return handleRemoveCommand(sender, args);
            case "reset":
                return handleResetCommand(sender, args);
            case "manage":
                return handleManageCommand(sender, args);
            case "reload":
                return handleReloadCommand(sender, args);
            default:
                sender.sendMessage(configManager.getMessage("command.usage"));
                return true;
        }
    }
    
    private boolean handleSetCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("playerinformation.admin")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }
        
        if (args.length != 4) {
            sender.sendMessage(configManager.getMessage("command.set.usage"));
            return true;
        }
        
        String playerName = args[1];
        String informationName = args[2];
        String value = args[3];
        
        // Validate information exists
        InformationDefinition definition = informationConfig.getDefinition(informationName);
        if (definition == null) {
            sender.sendMessage(configManager.getMessage("command.set.invalid-data", "data", informationName));
            return true;
        }
        
        // Validate value
        if (!definition.isValidValue(value)) {
            sender.sendMessage(configManager.getMessage("command.set.invalid-value", "data", informationName, "value", value));
            return true;
        }
        
        // Get player UUID
        getPlayerUuid(playerName).thenAccept(playerUuid -> {
            if (playerUuid == null) {
                sender.sendMessage(configManager.getMessage("player-not-found", "player", playerName));
                return;
            }
            
            // Set the information
            dataRepository.setPlayerInformation(playerUuid, playerName, informationName, value).thenAccept(success -> {
                if (success) {
                    sender.sendMessage(configManager.getMessage("command.set.success", 
                        "player", playerName, "data", informationName, "value", value));
                } else {
                    sender.sendMessage(configManager.getMessage("command.set.invalid-value", 
                        "data", informationName, "value", value));
                }
            });
        });
        
        return true;
    }
    
    private boolean handleGetCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("playerinformation.view")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }
        
        if (args.length != 3) {
            sender.sendMessage(configManager.getMessage("command.get.usage"));
            return true;
        }
        
        String playerName = args[1];
        String informationName = args[2];
        
        // Validate information exists
        if (!informationConfig.hasDefinition(informationName)) {
            sender.sendMessage(configManager.getMessage("command.get.no-data", "player", playerName, "data", informationName));
            return true;
        }
        
        // Get player UUID
        getPlayerUuid(playerName).thenAccept(playerUuid -> {
            if (playerUuid == null) {
                sender.sendMessage(configManager.getMessage("player-not-found", "player", playerName));
                return;
            }
            
            // Get the information
            dataRepository.getPlayerInformation(playerUuid, playerName, informationName).thenAccept(value -> {
                if (value != null) {
                    sender.sendMessage(configManager.getMessage("command.get.result", 
                        "player", playerName, "data", informationName, "value", value));
                } else {
                    sender.sendMessage(configManager.getMessage("command.get.no-data", 
                        "player", playerName, "data", informationName));
                }
            });
        });
        
        return true;
    }
    
    private boolean handleAddCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("playerinformation.admin")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }
        
        if (args.length != 4) {
            sender.sendMessage(configManager.getMessage("command.add.usage"));
            return true;
        }
        
        String playerName = args[1];
        String informationName = args[2];
        String amountStr = args[3];
        
        // Validate information exists and is numeric
        InformationDefinition definition = informationConfig.getDefinition(informationName);
        if (definition == null || definition.getType() != InformationType.INT) {
            sender.sendMessage(configManager.getMessage("command.add.not-numeric", "data", informationName));
            return true;
        }
        
        // Validate amount is a number
        int amount;
        try {
            amount = Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(configManager.getMessage("invalid-number", "value", amountStr));
            return true;
        }
        
        // Get player UUID
        getPlayerUuid(playerName).thenAccept(playerUuid -> {
            if (playerUuid == null) {
                sender.sendMessage(configManager.getMessage("player-not-found", "player", playerName));
                return;
            }
            
            // Add to the information
            dataRepository.addToPlayerInformation(playerUuid, playerName, informationName, amount).thenAccept(newValue -> {
                if (newValue != null) {
                    sender.sendMessage(configManager.getMessage("command.add.success", 
                        "player", playerName, "data", informationName, "value", String.valueOf(amount), "new_value", String.valueOf(newValue)));
                } else {
                    sender.sendMessage(configManager.getMessage("command.add.not-numeric", "data", informationName));
                }
            });
        });
        
        return true;
    }
    
    private boolean handleRemoveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("playerinformation.admin")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }
        
        if (args.length != 4) {
            sender.sendMessage(configManager.getMessage("command.remove.usage"));
            return true;
        }
        
        String playerName = args[1];
        String informationName = args[2];
        String amountStr = args[3];
        
        // Validate information exists and is numeric
        InformationDefinition definition = informationConfig.getDefinition(informationName);
        if (definition == null || definition.getType() != InformationType.INT) {
            sender.sendMessage(configManager.getMessage("command.remove.not-numeric", "data", informationName));
            return true;
        }
        
        // Validate amount is a number
        int amount;
        try {
            amount = Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            sender.sendMessage(configManager.getMessage("invalid-number", "value", amountStr));
            return true;
        }
        
        // Get player UUID
        getPlayerUuid(playerName).thenAccept(playerUuid -> {
            if (playerUuid == null) {
                sender.sendMessage(configManager.getMessage("player-not-found", "player", playerName));
                return;
            }
            
            // Remove from the information (add negative amount)
            dataRepository.addToPlayerInformation(playerUuid, playerName, informationName, -amount).thenAccept(newValue -> {
                if (newValue != null) {
                    sender.sendMessage(configManager.getMessage("command.remove.success", 
                        "player", playerName, "data", informationName, "value", String.valueOf(amount), "new_value", String.valueOf(newValue)));
                } else {
                    sender.sendMessage(configManager.getMessage("command.remove.not-numeric", "data", informationName));
                }
            });
        });
        
        return true;
    }
    
    private boolean handleResetCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("playerinformation.admin")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }
        
        if (args.length != 3) {
            sender.sendMessage(configManager.getMessage("command.reset.usage"));
            return true;
        }
        
        String playerName = args[1];
        String informationName = args[2];
        
        // Validate information exists
        if (!informationConfig.hasDefinition(informationName)) {
            sender.sendMessage(configManager.getMessage("command.set.invalid-data", "data", informationName));
            return true;
        }
        
        // Get player UUID
        getPlayerUuid(playerName).thenAccept(playerUuid -> {
            if (playerUuid == null) {
                sender.sendMessage(configManager.getMessage("player-not-found", "player", playerName));
                return;
            }
            
            // Reset the information
            dataRepository.resetPlayerInformation(playerUuid, playerName, informationName).thenAccept(defaultValue -> {
                if (defaultValue != null) {
                    sender.sendMessage(configManager.getMessage("command.reset.success", 
                        "player", playerName, "data", informationName, "value", defaultValue));
                }
            });
        });
        
        return true;
    }
    
    private boolean handleManageCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        
        if (!sender.hasPermission("playerinformation.manage")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }
        
        if (args.length != 2) {
            sender.sendMessage(configManager.getMessage("command.manage.usage"));
            return true;
        }
        
        Player player = (Player) sender;
        String targetPlayerName = args[1];
        
        // Get player UUID
        getPlayerUuid(targetPlayerName).thenAccept(playerUuid -> {
            if (playerUuid == null) {
                player.sendMessage(configManager.getMessage("player-not-found", "player", targetPlayerName));
                return;
            }
            
            // Open management GUI
            player.sendMessage(configManager.getMessage("command.manage.opening", "player", targetPlayerName));
            ManagementGUI gui = new ManagementGUI(plugin, configManager, informationConfig, dataRepository);
            gui.openManagementGUI(player, playerUuid, targetPlayerName);
        });
        
        return true;
    }
    
    private boolean handleReloadCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("playerinformation.reload")) {
            sender.sendMessage(configManager.getMessage("no-permission"));
            return true;
        }
        
        try {
            // Reload configurations
            configManager.reloadConfigs();
            
            // Reload database schema
            dataRepository.reloadSchema().join();
            
            sender.sendMessage(configManager.getMessage("command.reload.success"));
        } catch (Exception e) {
            sender.sendMessage(configManager.getMessage("command.reload.error"));
            plugin.getLogger().severe("Error during reload: " + e.getMessage());
        }
        
        return true;
    }
    
    /**
     * Get player UUID from name (online or offline)
     */
    private CompletableFuture<UUID> getPlayerUuid(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            // Try online player first
            Player onlinePlayer = Bukkit.getPlayer(playerName);
            if (onlinePlayer != null) {
                return onlinePlayer.getUniqueId();
            }
            
            // Try offline player
            try {
                return Bukkit.getOfflinePlayer(playerName).getUniqueId();
            } catch (Exception e) {
                return null;
            }
        });
    }
}
