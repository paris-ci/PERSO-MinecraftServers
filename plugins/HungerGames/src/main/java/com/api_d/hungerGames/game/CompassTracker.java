package com.api_d.hungerGames.game;

import com.api_d.hungerGames.database.models.GameParty;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages compass tracking for players
 */
public class CompassTracker implements Listener {
    
    public enum TrackingMode {
        SPAWN,
        FEAST,
        PARTY_MEMBER,
        ENEMY
    }
    
    private final Plugin plugin;
    private final Map<UUID, TrackingMode> playerTrackingModes = new ConcurrentHashMap<>();
    private final Map<UUID, GameParty> playerParties;
    
    // Tracking targets
    private Location spawnLocation;
    private Location feastLocation;
    private boolean feastSpawned = false;
    
    public CompassTracker(Plugin plugin, Map<UUID, GameParty> playerParties) {
        this.plugin = plugin;
        this.playerParties = playerParties;
    }
    
    /**
     * Create and initialize a new CompassTracker
     */
    public static CompassTracker create(Plugin plugin, Map<UUID, GameParty> playerParties) {
        CompassTracker tracker = new CompassTracker(plugin, playerParties);
        tracker.initializeEventListeners();
        return tracker;
    }
    
    /**
     * Initialize event listeners after construction
     */
    private void initializeEventListeners() {
        // Register event listeners
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Set the spawn location for tracking
     */
    public void setSpawnLocation(Location location) {
        this.spawnLocation = location;
    }
    
    /**
     * Set the feast location for tracking
     */
    public void setFeastLocation(Location location) {
        this.feastLocation = location;
        this.feastSpawned = true;
    }
    
    /**
     * Give a compass to a player
     */
    public void giveCompass(Player player) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        CompassMeta meta = (CompassMeta) compass.getItemMeta();
        
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("§6Tracking Compass"));
            meta.lore(java.util.Arrays.asList(
                net.kyori.adventure.text.Component.text("§7Right-click to change tracking mode"),
                net.kyori.adventure.text.Component.text("§7Current: §a" + getTrackingModeName(TrackingMode.SPAWN))
            ));
            compass.setItemMeta(meta);
        }
        
        // Set initial tracking mode
        playerTrackingModes.put(player.getUniqueId(), TrackingMode.SPAWN);
        
        // Update compass to point to spawn initially
        updateCompass(player, TrackingMode.SPAWN);
        
        // Give the compass
        player.getInventory().addItem(compass);
    }
    
    /**
     * Update a player's compass to point to the current tracking target
     */
    public void updateCompass(Player player, TrackingMode mode) {
        Location targetLocation = getTargetLocation(player, mode);
        
        if (targetLocation != null) {
            CompassMeta meta = (CompassMeta) player.getInventory().getItemInMainHand().getItemMeta();
            if (meta != null && player.getInventory().getItemInMainHand().getType() == Material.COMPASS) {
                meta.setLodestone(targetLocation);
                meta.setLodestoneTracked(true);
                player.getInventory().getItemInMainHand().setItemMeta(meta);
            }
        }
    }
    
    /**
     * Get the target location for a specific tracking mode
     */
    private Location getTargetLocation(Player player, TrackingMode mode) {
        switch (mode) {
            case SPAWN:
                return spawnLocation;
            case FEAST:
                return feastSpawned ? feastLocation : null;
            case PARTY_MEMBER:
                return getClosestPartyMemberLocation(player);
            case ENEMY:
                return getClosestEnemyLocation(player);
            default:
                return spawnLocation;
        }
    }
    
    /**
     * Get the location of the closest party member
     */
    private Location getClosestPartyMemberLocation(Player player) {
        GameParty playerParty = playerParties.get(player.getUniqueId());
        if (playerParty == null) {
            return spawnLocation;
        }
        
        Location closestLocation = null;
        double closestDistance = Double.MAX_VALUE;
        
        for (Map.Entry<UUID, GameParty> entry : playerParties.entrySet()) {
            UUID memberId = entry.getKey();
            GameParty memberParty = entry.getValue();
            
            // Skip if not same party or if it's the player themselves
            if (memberParty.getId() != playerParty.getId() || memberId.equals(player.getUniqueId())) {
                continue;
            }
            
            Player member = plugin.getServer().getPlayer(memberId);
            if (member != null && member.isOnline() && !member.isDead()) {
                double distance = player.getLocation().distance(member.getLocation());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestLocation = member.getLocation();
                }
            }
        }
        
        return closestLocation != null ? closestLocation : spawnLocation;
    }
    
    /**
     * Get the location of the closest enemy
     */
    private Location getClosestEnemyLocation(Player player) {
        Location closestLocation = null;
        double closestDistance = Double.MAX_VALUE;
        
        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            // Skip if it's the same player or if they're dead
            if (onlinePlayer.equals(player) || onlinePlayer.isDead()) {
                continue;
            }
            
            // Skip if they're in the same party
            GameParty playerParty = playerParties.get(player.getUniqueId());
            GameParty otherParty = playerParties.get(onlinePlayer.getUniqueId());
            if (playerParty != null && otherParty != null && 
                playerParty.getId() == otherParty.getId()) {
                continue;
            }
            
            double distance = player.getLocation().distance(onlinePlayer.getLocation());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestLocation = onlinePlayer.getLocation();
            }
        }
        
        return closestLocation != null ? closestLocation : spawnLocation;
    }
    
    /**
     * Get the display name for a tracking mode
     */
    private String getTrackingModeName(TrackingMode mode) {
        switch (mode) {
            case SPAWN:
                return "Spawn";
            case FEAST:
                return "Feast";
            case PARTY_MEMBER:
                return "Party Member";
            case ENEMY:
                return "Enemy";
            default:
                return "Unknown";
        }
    }
    
    /**
     * Handle right-click on compass to change tracking mode
     */
    @EventHandler
    public void onCompassClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || item.getType() != Material.COMPASS) {
            return;
        }
        
        // Cycle through tracking modes
        TrackingMode currentMode = playerTrackingModes.getOrDefault(player.getUniqueId(), TrackingMode.SPAWN);
        TrackingMode newMode = getNextTrackingMode(currentMode);
        
        // Update player's tracking mode
        playerTrackingModes.put(player.getUniqueId(), newMode);
        
        // Update compass
        updateCompass(player, newMode);
        
        // Update lore
        CompassMeta meta = (CompassMeta) item.getItemMeta();
        if (meta != null) {
            meta.lore(java.util.Arrays.asList(
                net.kyori.adventure.text.Component.text("§7Right-click to change tracking mode"),
                net.kyori.adventure.text.Component.text("§7Current: §a" + getTrackingModeName(newMode))
            ));
            item.setItemMeta(meta);
        }
        
        // Send message to player
        player.sendMessage("§6Compass now tracking: §a" + getTrackingModeName(newMode));
        
        event.setCancelled(true);
    }
    
    /**
     * Get the next tracking mode in the cycle
     */
    private TrackingMode getNextTrackingMode(TrackingMode currentMode) {
        switch (currentMode) {
            case SPAWN:
                return feastSpawned ? TrackingMode.FEAST : TrackingMode.PARTY_MEMBER;
            case FEAST:
                return TrackingMode.PARTY_MEMBER;
            case PARTY_MEMBER:
                return TrackingMode.ENEMY;
            case ENEMY:
                return TrackingMode.SPAWN;
            default:
                return TrackingMode.SPAWN;
        }
    }
    
    /**
     * Update all player compasses (called periodically)
     */
    public void updateAllCompasses() {
        for (Map.Entry<UUID, TrackingMode> entry : playerTrackingModes.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                updateCompass(player, entry.getValue());
            }
        }
    }
    
    /**
     * Remove a player's tracking data
     */
    public void removePlayer(UUID playerId) {
        playerTrackingModes.remove(playerId);
    }

    /**
     * Change the tracking mode for a player
     */
    public boolean changeTrackingMode(Player player, String modeString) {
        try {
            TrackingMode mode = TrackingMode.valueOf(modeString.toUpperCase());
            playerTrackingModes.put(player.getUniqueId(), mode);
            updateCompass(player, mode);
            return true;
        } catch (IllegalArgumentException e) {
            return false; // Invalid mode
        }
    }
    
    /**
     * Get the current tracking mode for a player
     */
    public TrackingMode getTrackingMode(Player player) {
        return playerTrackingModes.getOrDefault(player.getUniqueId(), TrackingMode.SPAWN);
    }
}
