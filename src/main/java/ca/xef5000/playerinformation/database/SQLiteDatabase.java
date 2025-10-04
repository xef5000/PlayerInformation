package ca.xef5000.playerinformation.database;

import ca.xef5000.playerinformation.PlayerInformation;
import ca.xef5000.playerinformation.data.InformationDefinition;
import ca.xef5000.playerinformation.data.PlayerInformationData;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;

/**
 * SQLite implementation of DatabaseManager
 */
public class SQLiteDatabase implements DatabaseManager {
    private final PlayerInformation plugin;
    private final String filename;
    private Connection connection;
    private final Set<String> existingColumns = new HashSet<>();
    
    public SQLiteDatabase(PlayerInformation plugin, String filename) {
        this.plugin = plugin;
        this.filename = filename;
    }
    
    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Create data folder if it doesn't exist
                if (!plugin.getDataFolder().exists()) {
                    plugin.getDataFolder().mkdirs();
                }
                
                // Create database file path
                File dbFile = new File(plugin.getDataFolder(), filename);
                String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
                
                // Load SQLite driver
                Class.forName("org.sqlite.JDBC");
                
                // Create connection
                connection = DriverManager.getConnection(url);
                connection.setAutoCommit(true);
                
                // Enable foreign keys
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON");
                }
                
                // Create base table
                createBaseTable();
                
                plugin.getLogger().info("Connected to SQLite database: " + filename);
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to initialize SQLite database", e);
                throw new CompletionException(e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    plugin.getLogger().info("SQLite database connection closed");
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Error closing SQLite database connection", e);
            }
        });
    }
    
    private void createBaseTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS player_information (
                uuid TEXT PRIMARY KEY,
                player_name TEXT NOT NULL,
                last_updated INTEGER DEFAULT (strftime('%s', 'now'))
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
        
        // Load existing columns
        loadExistingColumns();
    }
    
    private void loadExistingColumns() throws SQLException {
        existingColumns.clear();
        
        String sql = "PRAGMA table_info(player_information)";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String columnName = rs.getString("name");
                existingColumns.add(columnName);
            }
        }
    }
    
    @Override
    public CompletableFuture<Void> migrateDatabase(List<InformationDefinition> definitions) {
        return CompletableFuture.runAsync(() -> {
            try {
                for (InformationDefinition definition : definitions) {
                    String columnName = definition.getColumnName();
                    
                    if (!existingColumns.contains(columnName)) {
                        addColumn(definition);
                        existingColumns.add(columnName);
                    }
                }
                
                plugin.getLogger().info("Database migration completed for " + definitions.size() + " definitions");
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to migrate database", e);
                throw new CompletionException(e);
            }
        });
    }

    private void addColumn(InformationDefinition definition) throws SQLException {
        String columnName = definition.getColumnName();
        String sqlType = definition.getType().getSqlType();
        String defaultValue = definition.getDefaultValue();

        // Handle proper quoting
        String defaultClause = "";
        if (defaultValue != null && !defaultValue.isEmpty()) {
            // If it’s numeric, don’t quote it; otherwise, wrap in single quotes and escape internal quotes
            if (defaultValue.matches("-?\\d+(\\.\\d+)?")) {
                defaultClause = " DEFAULT " + defaultValue;
            } else {
                defaultClause = " DEFAULT '" + defaultValue.replace("'", "''") + "'";
            }
        }

        String sql = String.format(
                "ALTER TABLE player_information ADD COLUMN %s %s%s",
                columnName, sqlType, defaultClause
        );

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }

        plugin.getLogger().info("Added column: " + columnName + " (" + sqlType + ")");
    }
    
    @Override
    public CompletableFuture<List<PlayerInformationData>> loadAllPlayerData() {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerInformationData> playerDataList = new ArrayList<>();
            
            try {
                String sql = "SELECT * FROM player_information";
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    
                    while (rs.next()) {
                        PlayerInformationData playerData = createPlayerDataFromResultSet(rs);
                        playerDataList.add(playerData);
                    }
                }
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load all player data", e);
                throw new CompletionException(e);
            }
            
            return playerDataList;
        });
    }
    
    @Override
    public CompletableFuture<PlayerInformationData> loadPlayerData(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT * FROM player_information WHERE uuid = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerUuid.toString());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return createPlayerDataFromResultSet(rs);
                        }
                    }
                }
                
                return null;
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load player data for " + playerUuid, e);
                throw new CompletionException(e);
            }
        });
    }
    
    private PlayerInformationData createPlayerDataFromResultSet(ResultSet rs) throws SQLException {
        UUID playerUuid = UUID.fromString(rs.getString("uuid"));
        String playerName = rs.getString("player_name");
        
        Map<String, String> data = new HashMap<>();
        
        // Get all columns that start with "info_"
        ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String columnName = metaData.getColumnName(i);
            if (columnName.startsWith("info_")) {
                String value = rs.getString(columnName);
                if (value != null) {
                    // Convert column name back to information name
                    String informationName = columnName.substring(5); // Remove "info_" prefix
                    data.put(informationName, value);
                }
            }
        }
        
        return new PlayerInformationData(playerUuid, playerName, data);
    }
    
    @Override
    public CompletableFuture<Void> savePlayerData(PlayerInformationData playerData) {
        return CompletableFuture.runAsync(() -> {
            try {
                // First, insert or update the base player record
                String upsertSql = """
                    INSERT INTO player_information (uuid, player_name, last_updated) 
                    VALUES (?, ?, strftime('%s', 'now'))
                    ON CONFLICT(uuid) DO UPDATE SET 
                        player_name = excluded.player_name,
                        last_updated = excluded.last_updated
                    """;
                
                try (PreparedStatement stmt = connection.prepareStatement(upsertSql)) {
                    stmt.setString(1, playerData.getPlayerUuid().toString());
                    stmt.setString(2, playerData.getPlayerName());
                    stmt.execute();
                }
                
                // Update information columns
                for (Map.Entry<String, String> entry : playerData.getAllData().entrySet()) {
                    String columnName = "info_" + entry.getKey().toLowerCase().replaceAll("[^a-z0-9_]", "_");
                    String value = entry.getValue();
                    
                    if (existingColumns.contains(columnName)) {
                        String updateSql = "UPDATE player_information SET " + columnName + " = ? WHERE uuid = ?";
                        try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
                            stmt.setString(1, value);
                            stmt.setString(2, playerData.getPlayerUuid().toString());
                            stmt.execute();
                        }
                    }
                }
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + playerData.getPlayerUuid(), e);
                throw new CompletionException(e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> savePlayerDataBatch(List<PlayerInformationData> playerDataList) {
        return CompletableFuture.runAsync(() -> {
            try {
                connection.setAutoCommit(false);
                
                for (PlayerInformationData playerData : playerDataList) {
                    savePlayerData(playerData).join();
                }
                
                connection.commit();
                connection.setAutoCommit(true);
                
            } catch (Exception e) {
                try {
                    connection.rollback();
                    connection.setAutoCommit(true);
                } catch (SQLException rollbackEx) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to rollback transaction", rollbackEx);
                }
                
                plugin.getLogger().log(Level.SEVERE, "Failed to save player data batch", e);
                throw new CompletionException(e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> deletePlayerData(UUID playerUuid) {
        return CompletableFuture.runAsync(() -> {
            try {
                String sql = "DELETE FROM player_information WHERE uuid = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerUuid.toString());
                    stmt.execute();
                }
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete player data for " + playerUuid, e);
                throw new CompletionException(e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> setPlayerInformation(UUID playerUuid, String playerName, String informationName, String value) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Ensure player exists
                String upsertPlayerSql = """
                    INSERT INTO player_information (uuid, player_name, last_updated) 
                    VALUES (?, ?, strftime('%s', 'now'))
                    ON CONFLICT(uuid) DO UPDATE SET 
                        player_name = excluded.player_name,
                        last_updated = excluded.last_updated
                    """;
                
                try (PreparedStatement stmt = connection.prepareStatement(upsertPlayerSql)) {
                    stmt.setString(1, playerUuid.toString());
                    stmt.setString(2, playerName);
                    stmt.execute();
                }
                
                // Update information
                String columnName = "info_" + informationName.toLowerCase().replaceAll("[^a-z0-9_]", "_");
                if (existingColumns.contains(columnName)) {
                    String updateSql = "UPDATE player_information SET " + columnName + " = ? WHERE uuid = ?";
                    try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
                        stmt.setString(1, value);
                        stmt.setString(2, playerUuid.toString());
                        stmt.execute();
                    }
                }
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to set player information", e);
                throw new CompletionException(e);
            }
        });
    }
    
    @Override
    public CompletableFuture<String> getPlayerInformation(UUID playerUuid, String informationName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String columnName = "info_" + informationName.toLowerCase().replaceAll("[^a-z0-9_]", "_");
                if (!existingColumns.contains(columnName)) {
                    return null;
                }
                
                String sql = "SELECT " + columnName + " FROM player_information WHERE uuid = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerUuid.toString());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return rs.getString(columnName);
                        }
                    }
                }
                
                return null;
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get player information", e);
                throw new CompletionException(e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> removePlayerInformation(UUID playerUuid, String informationName) {
        return setPlayerInformation(playerUuid, "", informationName, null);
    }
    
    @Override
    public CompletableFuture<Boolean> isHealthy() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (connection == null || connection.isClosed()) {
                    return false;
                }
                
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT 1")) {
                    return rs.next();
                }
                
            } catch (Exception e) {
                return false;
            }
        });
    }
    
    @Override
    public CompletableFuture<Integer> getPlayerCount() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT COUNT(*) FROM player_information";
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
                
                return 0;
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get player count", e);
                throw new CompletionException(e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<UUID>> getAllPlayerUuids() {
        return CompletableFuture.supplyAsync(() -> {
            List<UUID> uuids = new ArrayList<>();
            
            try {
                String sql = "SELECT uuid FROM player_information";
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    
                    while (rs.next()) {
                        uuids.add(UUID.fromString(rs.getString("uuid")));
                    }
                }
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get all player UUIDs", e);
                throw new CompletionException(e);
            }
            
            return uuids;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> playerExists(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT 1 FROM player_information WHERE uuid = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, playerUuid.toString());
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        return rs.next();
                    }
                }
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to check if player exists", e);
                throw new CompletionException(e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> updatePlayerName(UUID playerUuid, String newName) {
        return CompletableFuture.runAsync(() -> {
            try {
                String sql = "UPDATE player_information SET player_name = ?, last_updated = strftime('%s', 'now') WHERE uuid = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setString(1, newName);
                    stmt.setString(2, playerUuid.toString());
                    stmt.execute();
                }
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to update player name", e);
                throw new CompletionException(e);
            }
        });
    }
    
    @Override
    public CompletableFuture<String> getDatabaseStats() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                StringBuilder stats = new StringBuilder();
                stats.append("SQLite Database Statistics:\n");
                stats.append("Database File: ").append(filename).append("\n");
                
                // Get player count
                int playerCount = getPlayerCount().join();
                stats.append("Total Players: ").append(playerCount).append("\n");
                
                // Get table info
                String sql = "PRAGMA table_info(player_information)";
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    
                    int columnCount = 0;
                    while (rs.next()) {
                        columnCount++;
                    }
                    stats.append("Total Columns: ").append(columnCount).append("\n");
                }
                
                // Get database size
                File dbFile = new File(plugin.getDataFolder(), filename);
                if (dbFile.exists()) {
                    long sizeBytes = dbFile.length();
                    String size = String.format("%.2f KB", sizeBytes / 1024.0);
                    stats.append("Database Size: ").append(size).append("\n");
                }
                
                return stats.toString();
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get database stats", e);
                return "Error retrieving database statistics";
            }
        });
    }
}
