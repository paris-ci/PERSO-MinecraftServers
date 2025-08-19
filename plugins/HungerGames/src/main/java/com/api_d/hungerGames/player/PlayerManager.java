package com.api_d.hungerGames.player;

import com.api_d.hungerGames.database.DatabaseManager;
import com.api_d.hungerGames.database.models.Player;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages player data, credits, and database operations
 */
public class PlayerManager {
    
    private final DatabaseManager databaseManager;
    private final Logger logger;
    private final Map<UUID, Player> playerCache = new HashMap<>();
    
    public PlayerManager(DatabaseManager databaseManager, Logger logger) {
        this.databaseManager = databaseManager;
        this.logger = logger;
    }
    
    /**
     * Load or create a player in the database
     */
    public CompletableFuture<Player> loadPlayer(org.bukkit.entity.Player bukkitPlayer) {
        return loadPlayer(bukkitPlayer.getUniqueId());
    }
    
    /**
     * Load or create a player in the database by UUID
     */
    public CompletableFuture<Player> loadPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if player exists in cache
                if (playerCache.containsKey(uuid)) {
                    return playerCache.get(uuid);
                }
                
                // Try to load from database
                Player player = loadPlayerFromDatabase(uuid);
                
                if (player == null) {
                    // Create new player
                    player = createNewPlayer(uuid);
                }
                
                // Cache the player
                playerCache.put(uuid, player);
                return player;
                
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to load player: " + uuid, e);
                return null;
            }
        });
    }
    
    /**
     * Load player from database
     */
    private Player loadPlayerFromDatabase(UUID uuid) throws SQLException {
        String query = "SELECT id, uuid, credits, last_kit_used, created_at, updated_at FROM players WHERE uuid = ?";
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            
            statement.setObject(1, uuid);
            ResultSet resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                return new Player(
                    resultSet.getInt("id"),
                    UUID.fromString(resultSet.getString("uuid")),
                    resultSet.getInt("credits"),
                    resultSet.getString("last_kit_used"),
                    resultSet.getTimestamp("created_at"),
                    resultSet.getTimestamp("updated_at")
                );
            }
            
            return null;
        }
    }
    
    /**
     * Create a new player in the database
     */
    private Player createNewPlayer(UUID uuid) throws SQLException {
        String query = "INSERT INTO players (uuid, credits) VALUES (?, ?) RETURNING id, created_at, updated_at";
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            
            statement.setObject(1, uuid);
            statement.setInt(2, 0); // Start with 0 credits
            
            ResultSet resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                Player player = new Player(
                    resultSet.getInt("id"),
                    uuid,
                    0,
                    null,
                    resultSet.getTimestamp("created_at"),
                    resultSet.getTimestamp("updated_at")
                );
                
                logger.info("Created new player record for UUID: " + uuid);
                return player;
            }
            
            throw new SQLException("Failed to create new player");
        }
    }
    
    /**
     * Save player data to database
     */
    public CompletableFuture<Void> savePlayer(Player player) {
        return CompletableFuture.runAsync(() -> {
            try {
                String query = "UPDATE players SET credits = ?, last_kit_used = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
                
                try (Connection connection = databaseManager.getConnection();
                     PreparedStatement statement = connection.prepareStatement(query)) {
                    
                    statement.setInt(1, player.getCredits());
                    statement.setString(2, player.getLastKitUsed());
                    statement.setInt(3, player.getId());
                    
                    int updatedRows = statement.executeUpdate();
                    
                    if (updatedRows == 0) {
                        logger.warning("No rows updated when saving player: " + player.getUuid());
                    }
                }
                
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to save player: " + player.getUuid(), e);
            }
        });
    }
    
    /**
     * Award credits to a player
     */
    public CompletableFuture<Void> awardCredits(UUID uuid, int credits, String reason) {
        return CompletableFuture.runAsync(() -> {
            try {
                Player player = playerCache.get(uuid);
                if (player == null) {
                    logger.warning("Attempted to award credits to non-loaded player: " + uuid);
                    return;
                }
                
                int oldCredits = player.getCredits();
                player.setCredits(oldCredits + credits);
                
                logger.info(String.format("Awarded %d credits to player %s (%s). Old: %d, New: %d", 
                    credits, uuid, reason, oldCredits, player.getCredits()));
                
                // Save to database
                savePlayer(player).join();
                
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to award credits to player: " + uuid, e);
            }
        });
    }
    
    /**
     * Deduct credits from a player
     */
    public CompletableFuture<Boolean> deductCredits(UUID uuid, int credits, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Player player = playerCache.get(uuid);
                if (player == null) {
                    logger.warning("Attempted to deduct credits from non-loaded player: " + uuid);
                    return false;
                }
                
                if (player.getCredits() < credits) {
                    logger.info("Player " + uuid + " does not have enough credits. Required: " + credits + ", Has: " + player.getCredits());
                    return false;
                }
                
                int oldCredits = player.getCredits();
                player.setCredits(oldCredits - credits);
                
                logger.info(String.format("Deducted %d credits from player %s (%s). Old: %d, New: %d", 
                    credits, uuid, reason, oldCredits, player.getCredits()));
                
                // Save to database
                savePlayer(player).join();
                
                return true;
                
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to deduct credits from player: " + uuid, e);
                return false;
            }
        });
    }
    
    /**
     * Get a player's credits
     */
    public int getPlayerCredits(UUID uuid) {
        Player player = playerCache.get(uuid);
        return player != null ? player.getCredits() : 0;
    }
    
    /**
     * Get a player's credits
     */
    public int getPlayerCredits(org.bukkit.entity.Player bukkitPlayer) {
        return getPlayerCredits(bukkitPlayer.getUniqueId());
    }
    
    /**
     * Set a player's last used kit
     */
    public void setPlayerLastKit(UUID uuid, String kitId) {
        Player player = playerCache.get(uuid);
        if (player != null) {
            player.setLastKitUsed(kitId);
            savePlayer(player);
        }
    }
    
    /**
     * Get a player's last used kit
     */
    public String getPlayerLastKit(UUID uuid) {
        Player player = playerCache.get(uuid);
        return player != null ? player.getLastKitUsed() : null;
    }
    
    /**
     * Get cached player data
     */
    public Player getCachedPlayer(UUID uuid) {
        return playerCache.get(uuid);
    }
    
    /**
     * Get cached player data
     */
    public Player getCachedPlayer(org.bukkit.entity.Player bukkitPlayer) {
        return playerCache.get(bukkitPlayer.getUniqueId());
    }
    
    /**
     * Unload a player from cache (call when player leaves)
     */
    public CompletableFuture<Void> unloadPlayer(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            Player player = playerCache.remove(uuid);
            if (player != null) {
                // Save final state to database
                savePlayer(player).join();
                logger.info("Unloaded player from cache: " + uuid);
            }
        });
    }
    
    /**
     * Save all cached players to database
     */
    public CompletableFuture<Void> saveAllPlayers() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Saving all cached players to database...");
            
            for (Player player : playerCache.values()) {
                try {
                    savePlayer(player).join();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to save player during mass save: " + player.getUuid(), e);
                }
            }
            
            logger.info("Finished saving all cached players");
        });
    }
    
    /**
     * Clear all cached data
     */
    public void clearCache() {
        playerCache.clear();
    }
    
    /**
     * Get player statistics
     */
    public Map<String, Object> getPlayerStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cached_players", playerCache.size());
        
        int totalCredits = playerCache.values().stream()
            .mapToInt(Player::getCredits)
            .sum();
        stats.put("total_credits", totalCredits);
        
        return stats;
    }
}