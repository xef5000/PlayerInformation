# PlayerInformation API Usage

This document explains how to use the PlayerInformation API in your own plugins.

## Adding PlayerInformation as a Dependency

### Maven
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.xef5000</groupId>
        <artifactId>PlayerInformation</artifactId>
        <version>1.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### Gradle
```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.xef5000:PlayerInformation:1.0'
}
```

## Getting the API Instance

### Method 1: Using Bukkit's Service Manager (Recommended)
```java
import ca.xef5000.playerinformation.api.PlayerInformationAPI;
import org.bukkit.Bukkit;

public class YourPlugin extends JavaPlugin {
    private PlayerInformationAPI playerInfoAPI;
    
    @Override
    public void onEnable() {
        // Get the API from Bukkit's service manager
        RegisteredServiceProvider<PlayerInformationAPI> provider = 
            Bukkit.getServicesManager().getRegistration(PlayerInformationAPI.class);
        
        if (provider != null) {
            playerInfoAPI = provider.getProvider();
            getLogger().info("PlayerInformation API found!");
        } else {
            getLogger().warning("PlayerInformation plugin not found!");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }
}
```

### Method 2: Direct Plugin Access
```java
import ca.xef5000.playerinformation.PlayerInformation;

public class YourPlugin extends JavaPlugin {
    private PlayerInformationAPI playerInfoAPI;
    
    @Override
    public void onEnable() {
        Plugin piPlugin = Bukkit.getPluginManager().getPlugin("PlayerInformation");
        if (piPlugin instanceof PlayerInformation) {
            playerInfoAPI = ((PlayerInformation) piPlugin).getAPI();
        }
    }
}
```

## Basic Usage Examples

### Getting Player Information
```java
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

// Get a string value
UUID playerUuid = player.getUniqueId();
CompletableFuture<String> future = playerInfoAPI.getPlayerInformation(player, "nickname");

future.thenAccept(nickname -> {
    if (nickname != null) {
        player.sendMessage("Your nickname is: " + nickname);
    } else {
        player.sendMessage("You don't have a nickname set.");
    }
});

// Get an integer value
playerInfoAPI.getPlayerInformationInt(player, "level", 1).thenAccept(level -> {
    player.sendMessage("Your level is: " + level);
});

// Get a boolean value
playerInfoAPI.getPlayerInformationBoolean(player, "premium", false).thenAccept(isPremium -> {
    if (isPremium) {
        player.sendMessage("You have premium status!");
    }
});
```

### Setting Player Information
```java
// Set a string value
playerInfoAPI.setPlayerInformation(player, "nickname", "CoolPlayer123").thenAccept(success -> {
    if (success) {
        player.sendMessage("Nickname set successfully!");
    } else {
        player.sendMessage("Failed to set nickname.");
    }
});

// Set an integer value
playerInfoAPI.setPlayerInformationInt(player, "coins", 1000).thenAccept(success -> {
    if (success) {
        player.sendMessage("Coins set to 1000!");
    }
});

// Add to an integer value
playerInfoAPI.addToPlayerInformation(player, "coins", 50).thenAccept(newValue -> {
    if (newValue != null) {
        player.sendMessage("Added 50 coins! New balance: " + newValue);
    }
});
```

### Working with Information Definitions
```java
// Check if an information type exists
if (playerInfoAPI.hasInformationDefinition("level")) {
    // Information exists, safe to use
}

// Get all available information types
Set<String> infoNames = playerInfoAPI.getInformationNames();
for (String name : infoNames) {
    InformationType type = playerInfoAPI.getInformationType(name);
    String defaultValue = playerInfoAPI.getDefaultValue(name);
    
    player.sendMessage(name + " (" + type + ") - Default: " + defaultValue);
}

// Get definitions by type
List<InformationDefinition> intDefinitions = 
    playerInfoAPI.getInformationDefinitionsByType(InformationType.INT);
```

### Getting All Player Information
```java
playerInfoAPI.getAllPlayerInformation(player).thenAccept(allInfo -> {
    player.sendMessage("Your information:");
    for (Map.Entry<String, String> entry : allInfo.entrySet()) {
        player.sendMessage("- " + entry.getKey() + ": " + entry.getValue());
    }
});
```

## Advanced Usage

### Working with Offline Players
```java
// You can work with offline players using their UUID
UUID offlinePlayerUuid = UUID.fromString("...");
String playerName = "OfflinePlayer";

playerInfoAPI.getPlayerInformation(offlinePlayerUuid, "level").thenAccept(level -> {
    // Handle the result
});

playerInfoAPI.setPlayerInformation(offlinePlayerUuid, playerName, "level", "5").thenAccept(success -> {
    // Handle the result
});
```

### Error Handling
```java
playerInfoAPI.getPlayerInformation(player, "level").thenAccept(level -> {
    // Success case
    player.sendMessage("Your level is: " + level);
}).exceptionally(throwable -> {
    // Error case
    getLogger().warning("Failed to get player level: " + throwable.getMessage());
    player.sendMessage("Error retrieving your level.");
    return null;
});
```

### Validation
```java
String informationName = "level";
String value = "10";

// Validate before setting
if (playerInfoAPI.isValidValue(informationName, value)) {
    playerInfoAPI.setPlayerInformation(player, informationName, value);
} else {
    player.sendMessage("Invalid value for " + informationName);
}
```

## Plugin.yml Dependencies

Make sure to add PlayerInformation as a dependency in your plugin.yml:

```yaml
name: YourPlugin
version: 1.0
main: com.yourpackage.YourPlugin
depend: [PlayerInformation]
# or use soft-depend if it's optional:
# softdepend: [PlayerInformation]
```

## Best Practices

1. **Always use CompletableFuture**: All API methods return CompletableFuture for async operations
2. **Handle errors**: Use `.exceptionally()` to handle potential errors
3. **Check for null values**: API methods may return null if data doesn't exist
4. **Validate input**: Use `isValidValue()` before setting values
5. **Use appropriate types**: Use the type-specific methods (getPlayerInformationInt, etc.) when possible
6. **Cache API instance**: Get the API instance once during plugin initialization

## Example Plugin

Here's a complete example of a plugin that uses the PlayerInformation API:

```java
public class ExamplePlugin extends JavaPlugin {
    private PlayerInformationAPI playerInfoAPI;
    
    @Override
    public void onEnable() {
        // Get API
        RegisteredServiceProvider<PlayerInformationAPI> provider = 
            Bukkit.getServicesManager().getRegistration(PlayerInformationAPI.class);
        
        if (provider != null) {
            playerInfoAPI = provider.getProvider();
        } else {
            getLogger().severe("PlayerInformation not found!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        
        // Register command
        getCommand("coins").setExecutor(new CoinsCommand(playerInfoAPI));
    }
}

class CoinsCommand implements CommandExecutor {
    private final PlayerInformationAPI api;
    
    public CoinsCommand(PlayerInformationAPI api) {
        this.api = api;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            // Show current coins
            api.getPlayerInformationInt(player, "coins", 0).thenAccept(coins -> {
                player.sendMessage("You have " + coins + " coins!");
            });
        } else if (args.length == 2 && args[0].equals("add")) {
            // Add coins
            try {
                int amount = Integer.parseInt(args[1]);
                api.addToPlayerInformation(player, "coins", amount).thenAccept(newAmount -> {
                    if (newAmount != null) {
                        player.sendMessage("Added " + amount + " coins! New balance: " + newAmount);
                    } else {
                        player.sendMessage("Failed to add coins!");
                    }
                });
            } catch (NumberFormatException e) {
                player.sendMessage("Invalid number!");
            }
        }
        
        return true;
    }
}
```

This example shows how to create a simple coins system using the PlayerInformation API.
