package com.api_d.hungerGames.game;

import com.api_d.hungerGames.config.GameConfig;
import com.api_d.hungerGames.util.HGLogger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Manages world border shrinking and related functionality
 */
public class BorderManager {
    
    private final Plugin plugin;
    private final GameConfig config;
    private final HGLogger logger;
    
    private WorldBorder worldBorder;
    private Location borderCenter;
    private BukkitTask borderShrinkTask;
    private boolean borderShrinking = false;
    
    public BorderManager(Plugin plugin, GameConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = new HGLogger(plugin);
    }
    
    /**
     * Initialize the world border
     */
    public void initializeBorder(World world, Location center) {
        this.borderCenter = center;
        this.worldBorder = world.getWorldBorder();
        
        // Set initial border
        worldBorder.setCenter(center);
        worldBorder.setSize(config.getWorldBorderInitialSize());
        
        logger.info("World border initialized at " + center.toString() + " with size " + config.getWorldBorderInitialSize());
    }
    
    /**
     * Start shrinking the border
     */
    public void startBorderShrinking() {
        if (borderShrinking) {
            return;
        }
        
        borderShrinking = true;
        logger.info("Starting border shrinking...");
        
        // Broadcast message
        plugin.getServer().broadcast(
            net.kyori.adventure.text.Component.text(config.getMessage("border_shrinking"))
        );
        
        // Start shrinking task
        borderShrinkTask = new BukkitRunnable() {
            @Override
            public void run() {
                shrinkBorder();
            }
        }.runTaskTimer(plugin, 20, 20); // Every second
    }
    
    /**
     * Shrink the border by one step
     */
    private void shrinkBorder() {
        if (worldBorder == null) {
            return;
        }
        
        double currentSize = worldBorder.getSize();
        double shrinkAmount = config.getWorldBorderShrinkSpeed();
        double newSize = Math.max(currentSize - shrinkAmount, config.getWorldBorderMinimumSize());
        
        if (newSize > config.getWorldBorderMinimumSize()) {
            worldBorder.setSize(newSize, 1L); // Shrink over 1 second
            logger.info("Border shrunk to size: " + newSize);
        } else {
            // Border has reached minimum size
            worldBorder.setSize(config.getWorldBorderMinimumSize());
            borderShrinking = false;
            
            if (borderShrinkTask != null) {
                borderShrinkTask.cancel();
            }
            
            logger.info("Border shrinking completed. Final size: " + config.getWorldBorderMinimumSize());
        }
    }
    
    /**
     * Check if border is currently shrinking
     */
    public boolean isBorderShrinking() {
        return borderShrinking;
    }
    
    /**
     * Get current border size
     */
    public double getCurrentBorderSize() {
        return worldBorder != null ? worldBorder.getSize() : config.getWorldBorderInitialSize();
    }
    
    /**
     * Get border center location
     */
    public Location getBorderCenter() {
        return borderCenter;
    }
    
    /**
     * Clean up border-related tasks
     */
    public void cleanup() {
        if (borderShrinkTask != null) {
            borderShrinkTask.cancel();
        }
        borderShrinking = false;
    }
    
    /**
     * Reset border state (for new games)
     */
    public void reset() {
        cleanup();
        worldBorder = null;
        borderCenter = null;
    }
}
