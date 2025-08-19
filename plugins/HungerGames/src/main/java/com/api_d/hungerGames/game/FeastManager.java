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
        
        // Don't start reminders if feast has already spawned
        if (feastSpawned) {
            logger.info("Feast already spawned, not starting reminders");
            return;
        }
        
        // Cancel any existing reminder task to prevent duplicates
        if (feastReminderTask != null) {
            feastReminderTask.cancel();
            logger.info("Cancelled existing feast reminder task");
        }
        
        logger.info("Starting feast reminder task");
        
        // Send reminders every 2 minutes
        feastReminderTask = new BukkitRunnable() {
            int timeUntilFeast = config.getFeastAppearsAfter();
            
            @Override
            public void run() {
                if (feastSpawned) {
                    this.cancel();
                    feastReminderTask = null;
                    logger.info("Feast reminder task cancelled - feast spawned");
                    return;
                }
                
                timeUntilFeast -= 120; // 2 minutes
                
                if (timeUntilFeast > 0) {
                    int minutes = timeUntilFeast / 60;
                    
                    logger.info("Sending feast reminder: " + minutes + " minutes remaining");
                    plugin.getServer().broadcast(
                        net.kyori.adventure.text.Component.text(config.getMessage("feast_spawning", "minutes", String.valueOf(minutes)))
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
                feastReminderTask = null;
                logger.info("Feast reminder task cancelled after feast spawned");
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
            feastReminderTask = null;
            logger.info("Feast reminder task cleaned up");
        }
    }
    
    /**
     * Reset feast state (for new games)
     */
    public void reset() {
        feastSpawned = false;
        feastLocation = null;
        cleanup();
        logger.info("Feast state reset");
    }
}
