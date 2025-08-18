package com.api_d.hungerGames.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages database connections and schema creation for the HungerGames plugin
 */
public class DatabaseManager {
    
    private final Plugin plugin;
    private final Logger logger;
    private HikariDataSource dataSource;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    
    public DatabaseManager(Plugin plugin, String host, int port, String database, String username, String password) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }
    
    /**
     * Initialize the database connection and create tables if needed
     */
    public boolean initialize() {
        try {
            setupDataSource();
            createTables();
            logger.info("Database connection established and tables created/verified");
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize database", e);
            return false;
        }
    }
    
    private void setupDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s", host, port, database));
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
        
        // Connection pool settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        // Connection validation
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        
        this.dataSource = new HikariDataSource(config);
    }
    
    /**
     * Create all necessary database tables
     */
    private void createTables() throws SQLException {
        try (Connection connection = getConnection()) {
            // Create Players table
            executeUpdate(connection, """
                CREATE TABLE IF NOT EXISTS players (
                    id SERIAL PRIMARY KEY,
                    uuid UUID NOT NULL UNIQUE,
                    credits INTEGER NOT NULL DEFAULT 0,
                    last_kit_used VARCHAR(50),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
            
            // Create Game table
            executeUpdate(connection, """
                CREATE TABLE IF NOT EXISTS games (
                    id SERIAL PRIMARY KEY,
                    server_id VARCHAR(100) NOT NULL,
                    waiting_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    started_at TIMESTAMP,
                    ended_at TIMESTAMP,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
            
            // Create GameParty table
            executeUpdate(connection, """
                CREATE TABLE IF NOT EXISTS game_parties (
                    id SERIAL PRIMARY KEY,
                    game_id INTEGER NOT NULL REFERENCES games(id) ON DELETE CASCADE,
                    name VARCHAR(100) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
            
            // Create GameLog table
            executeUpdate(connection, """
                CREATE TABLE IF NOT EXISTS game_logs (
                    id SERIAL PRIMARY KEY,
                    game_id INTEGER NOT NULL REFERENCES games(id) ON DELETE CASCADE,
                    player_id INTEGER NOT NULL REFERENCES players(id) ON DELETE CASCADE,
                    party_id INTEGER NOT NULL REFERENCES game_parties(id) ON DELETE CASCADE,
                    died_at TIMESTAMP,
                    death_reason VARCHAR(50),
                    killer_id INTEGER REFERENCES players(id) ON DELETE SET NULL,
                    death_message TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
            
            // Create indexes for better performance
            executeUpdate(connection, "CREATE INDEX IF NOT EXISTS idx_players_uuid ON players(uuid)");
            executeUpdate(connection, "CREATE INDEX IF NOT EXISTS idx_games_server_id ON games(server_id)");
            executeUpdate(connection, "CREATE INDEX IF NOT EXISTS idx_game_logs_game_id ON game_logs(game_id)");
            executeUpdate(connection, "CREATE INDEX IF NOT EXISTS idx_game_logs_player_id ON game_logs(player_id)");
            executeUpdate(connection, "CREATE INDEX IF NOT EXISTS idx_game_parties_game_id ON game_parties(game_id)");
            
            logger.info("Database schema created/updated successfully");
        }
    }
    
    private void executeUpdate(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
    
    /**
     * Get a connection from the connection pool
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("DataSource is not initialized or has been closed");
        }
        return dataSource.getConnection();
    }
    
    /**
     * Execute a prepared statement with automatic resource management
     */
    public void execute(String sql, Object... params) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            
            statement.executeUpdate();
        }
    }
    
    /**
     * Close the database connection pool
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }
    
    /**
     * Test database connection
     */
    public boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection.isValid(5);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Database connection test failed", e);
            return false;
        }
    }
    
    public DataSource getDataSource() {
        return dataSource;
    }
}