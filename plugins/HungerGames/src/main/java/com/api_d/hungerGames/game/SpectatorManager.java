package com.api_d.hungerGames.game;

import com.api_d.hungerGames.HungerGames;
import com.api_d.hungerGames.config.GameConfig;
import com.api_d.hungerGames.kits.Kit;
import com.api_d.hungerGames.kits.KitManager;
import com.api_d.hungerGames.util.HGLogger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages spectator functionality for dead players
 */
public class SpectatorManager {
    
    private final HungerGames plugin;
    private final GameConfig config;
    private final KitManager kitManager;
    private final HGLogger logger;
    
    private final ConcurrentHashMap<UUID, Player> spectators = new ConcurrentHashMap<>();
    
    public SpectatorManager(HungerGames plugin, GameConfig config, KitManager kitManager) {
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
        // Ensure flight is enabled for spectators
        player.setAllowFlight(true);
        player.setFlying(true);
        
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
        // Create kit selection GUI
        Inventory kitMenu = Bukkit.createInventory(null, 54, net.kyori.adventure.text.Component.text("§6Kit Selection"));
        
        // Get all available kits
        Collection<Kit> allKits = kitManager.getAllKits();
        int slot = 0;
        
        for (Kit kit : allKits) {
            if (slot >= 54) break; // Prevent overflow
            
            // Check if player can afford the kit
            int playerCredits = plugin.getPlayerManager().getCachedPlayer(player.getUniqueId()).getCredits();
            boolean canAfford = kit.canPlayerUse(player, playerCredits);
            
            // Create display item for the kit
            ItemStack displayItem = kit.createDisplayItem(canAfford);
            
            // Add click handler
            kitMenu.setItem(slot, displayItem);
            slot++;
        }
        
        // Open the menu
        player.openInventory(kitMenu);
    }
    
    /**
     * Handle after-death effects for spectators
     */
    public void activateAfterDeathEffect(Player player) {
        // Get player's last used kit
        Kit lastKit = kitManager.getPlayerKit(player);
        
        if (lastKit == null) {
            player.sendMessage("§cNo kit found for after-death effects");
            return;
        }
        
        // Get available after-death effects for the kit
        List<Kit.AfterDeathEffect> effects = lastKit.getAfterDeathEffects();
        
        if (effects.isEmpty()) {
            player.sendMessage("§7No after-death effects available for " + lastKit.getDisplayName());
            return;
        }
        
        // For now, just execute the first available effect
        // In the future, this could show a selection menu
        Kit.AfterDeathEffect effect = effects.get(0);
        effect.execute(player);
        
        player.sendMessage("§aActivated " + effect.getName() + " effect!");
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
