package ca.xef5000.playerinformation.database;

import ca.xef5000.playerinformation.PlayerInformation;
import ca.xef5000.playerinformation.config.ConfigManager;
import ca.xef5000.playerinformation.data.InformationDefinition;
import ca.xef5000.playerinformation.data.PlayerInformationData;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;

/**
 * MySQL implementation of DatabaseManager using HikariCP connection pooling
 */
public class MySQLDatabase implements DatabaseManager {
    private final PlayerInformation plugin;
    private final ConfigManager configManager;
    private HikariDataSource dataSource;
    private final Set<String> existingColumns = new HashSet<>();
    
    public MySQLDatabase(PlayerInformation plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }
    
    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Configure HikariCP
                HikariConfig config = new HikariConfig();
                
                String host = configManager.getMysqlHost();
                int port = configManager.getMysqlPort();
                String database = configManager.getMysqlDatabase();
                String username = configManager.getMysqlUsername();
                String password = configManager.getMysqlPassword();
                
                String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", 
                                             host, port, database);
                
                config.setJdbcUrl(jdbcUrl);
                config.setUsername(username);
                config.setPassword(password);
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                
                // Connection pool settings
                config.setMaximumPoolSize(configManager.getMysqlMaxPoolSize());
                config.setMinimumIdle(configManager.getMysqlMinIdle());
                config.setConnectionTimeout(configManager.getMysqlConnectionTimeout());
                config.setIdleTimeout(configManager.getMysqlIdleTimeout());
                config.setMaxLifetime(configManager.getMysqlMaxLifetime());
                
                // Additional settings
                config.setPoolName("PlayerInformation-MySQL");
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                config.addDataSourceProperty("useServerPrepStmts", "true");
                config.addDataSourceProperty("useLocalSessionState", "true");
                config.addDataSourceProperty("rewriteBatchedStatements", "true");
                config.addDataSourceProperty("cacheResultSetMetadata", "true");
                config.addDataSourceProperty("cacheServerConfiguration", "true");
                config.addDataSourceProperty("elideSetAutoCommits", "true");
                config.addDataSourceProperty("maintainTimeStats", "false");
                
                // Create data source
                dataSource = new HikariDataSource(config);
                
                // Test connection
                try (Connection conn = dataSource.getConnection()) {
                    plugin.getLogger().info("Successfully connected to MySQL database");
                }
                
                // Create base table
                createBaseTable();
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to initialize MySQL database", e);
                throw new CompletionException(e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (dataSource != null && !dataSource.isClosed()) {
                    dataSource.close();
                    plugin.getLogger().info("MySQL database connection pool closed");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error closing MySQL database connection pool", e);
            }
        });
    }
    
    private void createBaseTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS player_information (
                uuid VARCHAR(36) PRIMARY KEY,
                player_name VARCHAR(16) NOT NULL,
                last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_player_name (player_name),
                INDEX idx_last_updated (last_updated)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;
        
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
        
        // Load existing columns
        loadExistingColumns();
    }
    
    private void loadExistingColumns() throws SQLException {
        existingColumns.clear();
        
        String sql = "SHOW COLUMNS FROM player_information";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String columnName = rs.getString("Field");
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
        String sqlType = getMySQLType(definition.getType().getSqlType());
        String defaultValue = definition.getDefaultValue();
        
        String sql = String.format("ALTER TABLE player_information ADD COLUMN %s %s DEFAULT ?", 
                                 columnName, sqlType);
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, defaultValue);
            stmt.execute();
        }
        
        plugin.getLogger().info("Added column: " + columnName + " (" + sqlType + ")");
    }
    
    private String getMySQLType(String sqliteType) {
        switch (sqliteType.toUpperCase()) {
            case "INTEGER":
                return "INT";
            case "TEXT":
                return "TEXT";
            case "BOOLEAN":
                return "BOOLEAN";
            default:
                return "TEXT";
        }
    }
    
    @Override
    public CompletableFuture<List<PlayerInformationData>> loadAllPlayerData() {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerInformationData> playerDataList = new ArrayList<>();
            
            try (Connection conn = dataSource.getConnection()) {
                String sql = "SELECT * FROM player_information";
                try (Statement stmt = conn.createStatement();
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
            try (Connection conn = dataSource.getConnection()) {
                String sql = "SELECT * FROM player_information WHERE uuid = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
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
            try (Connection conn = dataSource.getConnection()) {
                // First, insert or update the base player record
                String upsertSql = """
                    INSERT INTO player_information (uuid, player_name) 
                    VALUES (?, ?)
                    ON DUPLICATE KEY UPDATE 
                        player_name = VALUES(player_name),
                        last_updated = CURRENT_TIMESTAMP
                    """;
                
                try (PreparedStatement stmt = conn.prepareStatement(upsertSql)) {
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
                        try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
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
            try (Connection conn = dataSource.getConnection()) {
                conn.setAutoCommit(false);
                
                try {
                    for (PlayerInformationData playerData : playerDataList) {
                        // This is not the most efficient way, but it's simple and works
                        // For better performance, we could use batch statements
                        savePlayerDataInternal(conn, playerData);
                    }
                    
                    conn.commit();
                    
                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save player data batch", e);
                throw new CompletionException(e);
            }
        });
    }
    
    private void savePlayerDataInternal(Connection conn, PlayerInformationData playerData) throws SQLException {
        // Insert or update base player record
        String upsertSql = """
            INSERT INTO player_information (uuid, player_name) 
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE 
                player_name = VALUES(player_name),
                last_updated = CURRENT_TIMESTAMP
            """;
        
        try (PreparedStatement stmt = conn.prepareStatement(upsertSql)) {
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
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setString(1, value);
                    stmt.setString(2, playerData.getPlayerUuid().toString());
                    stmt.execute();
                }
            }
        }
    }
    
    @Override
    public CompletableFuture<Void> deletePlayerData(UUID playerUuid) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                String sql = "DELETE FROM player_information WHERE uuid = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
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
            try (Connection conn = dataSource.getConnection()) {
                // Ensure player exists
                String upsertPlayerSql = """
                    INSERT INTO player_information (uuid, player_name) 
                    VALUES (?, ?)
                    ON DUPLICATE KEY UPDATE 
                        player_name = VALUES(player_name),
                        last_updated = CURRENT_TIMESTAMP
                    """;
                
                try (PreparedStatement stmt = conn.prepareStatement(upsertPlayerSql)) {
                    stmt.setString(1, playerUuid.toString());
                    stmt.setString(2, playerName);
                    stmt.execute();
                }
                
                // Update information
                String columnName = "info_" + informationName.toLowerCase().replaceAll("[^a-z0-9_]", "_");
                if (existingColumns.contains(columnName)) {
                    String updateSql = "UPDATE player_information SET " + columnName + " = ? WHERE uuid = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
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
            try (Connection conn = dataSource.getConnection()) {
                String columnName = "info_" + informationName.toLowerCase().replaceAll("[^a-z0-9_]", "_");
                if (!existingColumns.contains(columnName)) {
                    return null;
                }
                
                String sql = "SELECT " + columnName + " FROM player_information WHERE uuid = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
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
            try (Connection conn = dataSource.getConnection()) {
                if (conn == null || conn.isClosed()) {
                    return false;
                }
                
                try (Statement stmt = conn.createStatement();
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
            try (Connection conn = dataSource.getConnection()) {
                String sql = "SELECT COUNT(*) FROM player_information";
                try (Statement stmt = conn.createStatement();
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
            
            try (Connection conn = dataSource.getConnection()) {
                String sql = "SELECT uuid FROM player_information";
                try (Statement stmt = conn.createStatement();
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
            try (Connection conn = dataSource.getConnection()) {
                String sql = "SELECT 1 FROM player_information WHERE uuid = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
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
            try (Connection conn = dataSource.getConnection()) {
                String sql = "UPDATE player_information SET player_name = ?, last_updated = CURRENT_TIMESTAMP WHERE uuid = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
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
            try (Connection conn = dataSource.getConnection()) {
                StringBuilder stats = new StringBuilder();
                stats.append("MySQL Database Statistics:\n");
                
                // Get player count
                int playerCount = getPlayerCount().join();
                stats.append("Total Players: ").append(playerCount).append("\n");
                
                // Get table info
                String sql = "SHOW COLUMNS FROM player_information";
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    
                    int columnCount = 0;
                    while (rs.next()) {
                        columnCount++;
                    }
                    stats.append("Total Columns: ").append(columnCount).append("\n");
                }
                
                // Get connection pool stats
                stats.append("Connection Pool - Active: ").append(dataSource.getHikariPoolMXBean().getActiveConnections()).append("\n");
                stats.append("Connection Pool - Idle: ").append(dataSource.getHikariPoolMXBean().getIdleConnections()).append("\n");
                stats.append("Connection Pool - Total: ").append(dataSource.getHikariPoolMXBean().getTotalConnections()).append("\n");
                
                return stats.toString();
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get database stats", e);
                return "Error retrieving database statistics";
            }
        });
    }
}
