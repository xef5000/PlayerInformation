# PlayerInformation

A powerful and flexible Minecraft plugin framework for managing custom player data with support for multiple data types, GUI management, and a comprehensive API for other plugins.

## Features

### 🎯 Core Features
- **Flexible Data Types**: Support for integers, strings, UUIDs, enums, ladders, and permissions
- **Database Support**: Both SQLite and MySQL with automatic schema migration
- **GUI Management**: Intuitive inventory-based GUIs for managing player data
- **Command System**: Complete command interface with tab completion
- **PlaceholderAPI Integration**: Use player information in any plugin that supports placeholders
- **Developer API**: Comprehensive API for other plugins to interact with player data
- **Hot Reload**: Reload configuration and schema without restarting the server

### 📊 Data Types
- **INT**: Numeric values that can be added to or subtracted from
- **STRING**: Text data for names, descriptions, etc.
- **UUID**: Unique identifiers for linking to other systems
- **ENUM**: Predefined set of options (e.g., colors, categories)
- **LADDER**: Hierarchical ranking system (e.g., ranks, levels)
- **PERMISSION**: Boolean permission states

### 🗄️ Database Support
- **SQLite**: Simple file-based database (default)
- **MySQL**: Full MySQL support with connection pooling via HikariCP
- **Automatic Migration**: Schema updates automatically when information definitions change

## Installation

1. Download the latest release from [Releases](https://github.com/xef5000/PlayerInformation/releases)
2. Place the JAR file in your server's `plugins` folder
3. Start your server to generate default configuration files
4. Configure your information definitions in `informations.yml`
5. Restart or reload the plugin

## Configuration

### Main Configuration (`config.yml`)
```yaml
database:
  type: sqlite  # or mysql
  sqlite:
    filename: playerdata.db
  mysql:
    host: localhost
    port: 3306
    database: playerinformation
    username: root
    password: ""
```

### Information Definitions (`informations.yml`)
```yaml
level:
  type: int
  default: 1

coins:
  type: int
  default: 0

nickname:
  type: string
  default: ""

rank:
  type: ladder
  default: "member"
  ladder: ["member", "vip", "premium", "admin"]

favorite_color:
  type: enum
  default: "blue"
  values: ["red", "blue", "green", "yellow"]
```

## Commands

### Main Command: `/information` (aliases: `/info`, `/pi`)

- `/information set <player> <data> <value>` - Set a player's information
- `/information get <player> <data>` - Get a player's information
- `/information add <player> <data> <value>` - Add to an integer information
- `/information remove <player> <data> <value>` - Remove from an integer information
- `/information reset <player> <data>` - Reset information to default value
- `/information manage <player>` - Open GUI to manage player's information
- `/information reload` - Reload plugin configuration

### Permissions
- `playerinformation.admin` - Full access to all commands
- `playerinformation.manage` - Access to manage player information
- `playerinformation.view` - Access to view player information
- `playerinformation.reload` - Access to reload the plugin

## GUI System

The plugin provides an intuitive GUI system for managing player information:

### Management GUI
- Access via `/information manage <player>`
- Shows all information types for a player
- Click any item to edit that information

### Edit GUIs
- **Integer**: Buttons for +1, +10, -1, -10, and reset
- **String/UUID**: Chat input for setting values
- **Enum**: Next/Previous buttons to cycle through options
- **Ladder**: Promote/Demote buttons for rank progression
- **Permission**: True/False toggle buttons

## PlaceholderAPI Integration

Use player information in any plugin that supports PlaceholderAPI:

- `%playerinformation_<information_name>%` - Get player's information value
- `%playerinformation_<information_name>_default%` - Get default value
- `%playerinformation_<information_name>_type%` - Get information type
- `%playerinformation_<information_name>_exists%` - Check if information exists

### Examples
- `%playerinformation_level%` - Player's level
- `%playerinformation_coins%` - Player's coins
- `%playerinformation_rank%` - Player's rank

## Developer API

PlayerInformation provides a comprehensive API for other plugins. See [API_USAGE.md](API_USAGE.md) for detailed documentation.

### Quick Example
```java
// Get the API
PlayerInformationAPI api = Bukkit.getServicesManager()
    .getRegistration(PlayerInformationAPI.class).getProvider();

// Get player's level
api.getPlayerInformationInt(player, "level", 1).thenAccept(level -> {
    player.sendMessage("Your level is: " + level);
});

// Add coins
api.addToPlayerInformation(player, "coins", 100).thenAccept(newAmount -> {
    player.sendMessage("Added 100 coins! New balance: " + newAmount);
});
```

## Building from Source

### Requirements
- Java 17 or higher
- Gradle 7.0 or higher

### Build Steps
```bash
git clone https://github.com/xef5000/PlayerInformation.git
cd PlayerInformation
./gradlew shadowJar
```

The compiled JAR will be in `build/libs/PlayerInformation-1.0-all.jar`

## Dependencies

### Runtime Dependencies (Included)
- [InventoryFramework](https://github.com/stefvanschie/IF) - GUI framework
- [HikariCP](https://github.com/brettwooldridge/HikariCP) - Database connection pooling
- SQLite JDBC Driver
- MySQL Connector/J

### Optional Dependencies
- [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI) - For placeholder support

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

- **Issues**: [GitHub Issues](https://github.com/xef5000/PlayerInformation/issues)
- **Wiki**: [GitHub Wiki](https://github.com/xef5000/PlayerInformation/wiki)
- **Discord**: [Join our Discord](https://discord.gg/your-discord)

## Changelog

### Version 1.0
- Initial release
- Support for all data types (INT, STRING, UUID, ENUM, LADDER, PERMISSION)
- SQLite and MySQL database support
- Complete GUI system
- Command interface with tab completion
- PlaceholderAPI integration
- Comprehensive developer API
- Hot reload functionality

## Roadmap

- [ ] Web interface for managing player data
- [ ] Import/Export functionality
- [ ] Advanced permission integration
- [ ] Custom data validation rules
- [ ] Backup and restore system
- [ ] Performance analytics dashboard

---

**Made with ❤️ for the Minecraft community**
