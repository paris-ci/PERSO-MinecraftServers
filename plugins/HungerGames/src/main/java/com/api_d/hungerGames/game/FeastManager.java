package com.api_d.hungerGames.game;

import com.api_d.hungerGames.config.GameConfig;
import com.api_d.hungerGames.events.FeastSpawnEvent;
import com.api_d.hungerGames.world.PlatformGenerator;
import com.api_d.hungerGames.util.HGLogger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;



/**
 * Manages feast spawning and related functionality
 */
public class FeastManager {
    
    private final Plugin plugin;
    private final GameConfig config;
    private final PlatformGenerator platformGenerator;
    private final HGLogger logger;
    
    private Location feastLocation;
    private boolean feastSpawned = false;
    private BukkitTask feastReminderTask;
    
    public FeastManager(Plugin plugin, GameConfig config, PlatformGenerator platformGenerator) {
        this.plugin = plugin;
        this.config = config;
        this.platformGenerator = platformGenerator;
        this.logger = new HGLogger(plugin);
    }
    
    /**
     * Schedule feast spawn reminders
     */
    public void startFeastReminders() {
        if (!config.isFeastEnabled()) {
            return;
        }
        
        // Send reminders every 2 minutes
        feastReminderTask = new BukkitRunnable() {
            int timeUntilFeast = config.getFeastAppearsAfter();
            
            @Override
            public void run() {
                if (feastSpawned) {
                    this.cancel();
                    return;
                }
                
                timeUntilFeast -= 120; // 2 minutes
                
                if (timeUntilFeast > 0) {
                    int minutes = timeUntilFeast / 60;
                    int seconds = timeUntilFeast % 60;
                    String timeString = String.format("%d:%02d", minutes, seconds);
                    
                    plugin.getServer().broadcast(
                        net.kyori.adventure.text.Component.text(config.getMessage("feast_spawning", "minutes", timeString))
                    );
                }
            }
        }.runTaskTimer(plugin, 2400, 2400); // Every 2 minutes
    }
    
    /**
     * Spawn the feast at a random location
     */
    public Location spawnFeast(World world, Location worldCenter) {
        if (feastSpawned) {
            return feastLocation;
        }
        
        logger.info("Spawning feast...");
        
        try {
            // Generate feast platform
            feastLocation = platformGenerator.generateFeastPlatform(world, worldCenter, config.getWorldBorderInitialSize());
            
            // Mark feast as spawned
            feastSpawned = true;
            
            // Fire event
            FeastSpawnEvent event = new FeastSpawnEvent(feastLocation);
            plugin.getServer().getPluginManager().callEvent(event);
            
            // Cancel reminder task
            if (feastReminderTask != null) {
                feastReminderTask.cancel();
            }
            
            // Broadcast feast location
            int x = feastLocation.getBlockX();
            int z = feastLocation.getBlockZ();
            plugin.getServer().broadcast(
                net.kyori.adventure.text.Component.text(config.getMessage("feast_spawned", "x", String.valueOf(x), "z", String.valueOf(z)))
            );
            
            logger.info("Feast spawned successfully at " + feastLocation.toString());
            
            return feastLocation;
            
        } catch (Exception e) {
            logger.severe("Failed to spawn feast: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Check if feast has spawned
     */
    public boolean isFeastSpawned() {
        return feastSpawned;
    }
    
    /**
     * Get the feast location
     */
    public Location getFeastLocation() {
        return feastLocation;
    }
    
    /**
     * Clean up feast-related tasks
     */
    public void cleanup() {
        if (feastReminderTask != null) {
            feastReminderTask.cancel();
        }
    }
    
    /**
     * Reset feast state (for new games)
     */
    public void reset() {
        feastSpawned = false;
        feastLocation = null;
        cleanup();
    }
}
