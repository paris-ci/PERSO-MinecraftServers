package com.api_d.hungerGames.game;

import com.api_d.hungerGames.util.HGLogger;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.UUID;

/**
 * Manages final fight mechanics including poison effects for long games
 */
public class FinalFightManager {
    
    private final Plugin plugin;
    private final HGLogger logger;
    private final Set<UUID> alivePlayers;
    
    private BukkitTask poisonTask;
    private int currentPoisonLevel = 0;
    private long poisonStartTime = 0;
    private boolean finalFightActive = false;
    
    public FinalFightManager(Plugin plugin, Set<UUID> alivePlayers) {
        this.plugin = plugin;
        this.logger = new HGLogger(plugin);
        this.alivePlayers = alivePlayers;
    }
    
    /**
     * Start the final fight phase
     */
    public void startFinalFight() {
        if (finalFightActive) {
            return;
        }
        
        finalFightActive = true;
        logger.info("Starting final fight phase...");
        
        // Start poison application task
        poisonTask = new BukkitRunnable() {
            @Override
            public void run() {
                applyPoisonEffects();
            }
        }.runTaskTimer(plugin, 1200, 1200); // Every minute
    }
    
    /**
     * Apply poison effects to all alive players
     */
    private void applyPoisonEffects() {
        if (alivePlayers.size() <= 1) {
            // Game is ending, stop poison
            stopFinalFight();
            return;
        }
        
        // Increase poison level
        currentPoisonLevel++;
        
        // Determine poison effect level
        PotionEffectType poisonType = PotionEffectType.POISON;
        int poisonLevel = Math.min(currentPoisonLevel, 10); // Cap at level 10
        
        // Apply poison to all alive players
        for (UUID playerId : alivePlayers) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null && player.isOnline() && !player.isDead()) {
                // Remove existing poison effect
                player.removePotionEffect(poisonType);
                
                // Apply new poison effect
                PotionEffect poisonEffect = new PotionEffect(poisonType, Integer.MAX_VALUE, poisonLevel - 1, false, false);
                player.addPotionEffect(poisonEffect);
                
                // Penalize credits for poison damage
                // This would need to be implemented in PlayerManager
                // playerManager.penalizeCredits(playerId, config.getPoisonDamagePenalty(), "Poison damage");
                
                player.sendMessage("§cYou are being poisoned by the final fight! Level: " + poisonLevel);
            }
        }
        
        // If this is the first poison application, start the timer
        if (currentPoisonLevel == 1) {
            poisonStartTime = System.currentTimeMillis();
        }
        
        // Check if players have been poisoned for too long
        if (currentPoisonLevel >= 10) {
            long timeSinceMaxPoison = System.currentTimeMillis() - poisonStartTime;
            if (timeSinceMaxPoison >= 10000) { // 10 seconds
                logger.info("Players have been at max poison for 10 seconds. Ending game...");
                killAllPlayers();
                return;
            }
        }
        
        logger.info("Applied poison level " + poisonLevel + " to " + alivePlayers.size() + " players");
    }
    
    /**
     * Kill all remaining players due to poison
     */
    private void killAllPlayers() {
        logger.info("Killing all players due to prolonged poison exposure");
        
        for (UUID playerId : alivePlayers) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.setHealth(0.0); // Kill the player
            }
        }
        
        // Stop the final fight
        stopFinalFight();
    }
    
    /**
     * Stop the final fight phase
     */
    public void stopFinalFight() {
        if (!finalFightActive) {
            return;
        }
        
        finalFightActive = false;
        logger.info("Stopping final fight phase...");
        
        // Remove poison effects from all players
        for (UUID playerId : alivePlayers) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.removePotionEffect(PotionEffectType.POISON);
            }
        }
        
        // Cancel poison task
        if (poisonTask != null) {
            poisonTask.cancel();
        }
        
        // Reset poison level
        currentPoisonLevel = 0;
        poisonStartTime = 0;
    }
    
    /**
     * Check if final fight is active
     */
    public boolean isFinalFightActive() {
        return finalFightActive;
    }
    
    /**
     * Get current poison level
     */
    public int getCurrentPoisonLevel() {
        return currentPoisonLevel;
    }
    
    /**
     * Clean up final fight tasks
     */
    public void cleanup() {
        if (poisonTask != null) {
            poisonTask.cancel();
        }
        finalFightActive = false;
    }
    
    /**
     * Reset final fight state (for new games)
     */
    public void reset() {
        cleanup();
        currentPoisonLevel = 0;
        poisonStartTime = 0;
    }
}
