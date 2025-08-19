package com.api_d.hungerGames.game;

import com.api_d.hungerGames.config.GameConfig;
import com.api_d.hungerGames.kits.KitManager;
import com.api_d.hungerGames.util.HGLogger;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages spectator functionality for dead players
 */
public class SpectatorManager {
    
    private final Plugin plugin;
    private final GameConfig config;
    private final KitManager kitManager;
    private final HGLogger logger;
    
    private final ConcurrentHashMap<UUID, Player> spectators = new ConcurrentHashMap<>();
    
    public SpectatorManager(Plugin plugin, GameConfig config, KitManager kitManager) {
        this.plugin = plugin;
        this.config = config;
        this.kitManager = kitManager;
        this.logger = new HGLogger(plugin);
    }
    
    /**
     * Set a player as spectator
     */
    public void setPlayerAsSpectator(Player player) {
        UUID playerId = player.getUniqueId();
        spectators.put(playerId, player);
        
        // Set game mode
        player.setGameMode(org.bukkit.GameMode.SPECTATOR);
        
        // Clear inventory
        player.getInventory().clear();
        
        // Give spectator items
        giveSpectatorItems(player);
        
        // Send message
        player.sendMessage(config.getMessage("spectator_mode"));
        
        logger.info("Player " + player.getName() + " is now a spectator");
    }
    
    /**
     * Give spectator items to a player
     */
    private void giveSpectatorItems(Player player) {
        // Give compass for tracking
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta compassMeta = compass.getItemMeta();
        if (compassMeta != null) {
            compassMeta.displayName(net.kyori.adventure.text.Component.text("§6Spectator Compass"));
            compassMeta.lore(Arrays.asList(
                net.kyori.adventure.text.Component.text("§7Track alive players"),
                net.kyori.adventure.text.Component.text("§7Right-click to change target")
            ));
            compass.setItemMeta(compassMeta);
        }
        
        // Give kit menu access
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta bookMeta = book.getItemMeta();
        if (bookMeta != null) {
            bookMeta.displayName(net.kyori.adventure.text.Component.text("§aKit Menu"));
            bookMeta.lore(Arrays.asList(
                net.kyori.adventure.text.Component.text("§7Select your default kit"),
                net.kyori.adventure.text.Component.text("§7Right-click to open")
            ));
            book.setItemMeta(bookMeta);
        }
        
        // Give after-death effect activator
        ItemStack emerald = new ItemStack(Material.EMERALD);
        ItemMeta emeraldMeta = emerald.getItemMeta();
        if (emeraldMeta != null) {
            emeraldMeta.displayName(net.kyori.adventure.text.Component.text("§aAfter-Death Effects"));
            emeraldMeta.lore(Arrays.asList(
                net.kyori.adventure.text.Component.text("§7Use your kit's after-death abilities"),
                net.kyori.adventure.text.Component.text("§7Right-click to activate")
            ));
            emerald.setItemMeta(emeraldMeta);
        }
        
        // Add items to inventory
        player.getInventory().addItem(compass);
        player.getInventory().addItem(book);
        player.getInventory().addItem(emerald);
    }
    
    /**
     * Remove a player from spectators
     */
    public void removeSpectator(UUID playerId) {
        spectators.remove(playerId);
    }
    
    /**
     * Check if a player is a spectator
     */
    public boolean isSpectator(UUID playerId) {
        return spectators.containsKey(playerId);
    }
    
    /**
     * Get all spectators
     */
    public ConcurrentHashMap<UUID, Player> getSpectators() {
        return spectators;
    }
    
    /**
     * Handle spectator compass usage
     */
    public void handleSpectatorCompass(Player player) {
        // Find closest alive player to track
        Player closestPlayer = null;
        double closestDistance = Double.MAX_VALUE;
        
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            if (onlinePlayer.equals(player) || onlinePlayer.isDead()) {
                continue;
            }
            
            double distance = player.getLocation().distance(onlinePlayer.getLocation());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestPlayer = onlinePlayer;
            }
        }
        
        if (closestPlayer != null) {
            // Teleport to the player's location
            player.teleport(closestPlayer.getLocation());
            player.sendMessage("§6Teleported to " + closestPlayer.getName());
        } else {
            player.sendMessage("§cNo alive players to track");
        }
    }
    
    /**
     * Handle kit menu access for spectators
     */
    public void openKitMenu(Player player) {
        // This would integrate with the existing kit system
        // For now, just send a message
        player.sendMessage("§aKit menu functionality will be implemented here");
        // TODO: Integrate with KitManager to show kit selection GUI
    }
    
    /**
     * Handle after-death effects for spectators
     */
    public void activateAfterDeathEffect(Player player) {
        // Get player's last used kit
        String lastKitId = kitManager.getPlayerKit(player).getId();
        
        // This would integrate with the kit system to activate after-death effects
        player.sendMessage("§aAfter-death effects for " + lastKitId + " will be implemented here");
        // TODO: Integrate with KitManager to activate kit-specific after-death effects
    }
    
    /**
     * Clean up spectator data
     */
    public void cleanup() {
        spectators.clear();
    }
    
    /**
     * Reset spectator state (for new games)
     */
    public void reset() {
        cleanup();
    }
}
