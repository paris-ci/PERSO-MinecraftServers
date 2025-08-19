package com.api_d.hungerGames.kits;

import com.api_d.hungerGames.kits.defaults.*;
import com.api_d.hungerGames.kits.premium.*;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Manages all available kits and kit selection
 */
public class KitManager {
    
    private Map<String, Kit> kits;
    private Map<UUID, String> playerKitSelection;
    
    public KitManager() {
        // Initialize collections first
        kits = new HashMap<>();
        playerKitSelection = new HashMap<>();
    }
    
    /**
     * Create and initialize a new KitManager
     */
    public static KitManager create() {
        KitManager manager = new KitManager();
        manager.registerDefaultKits();
        manager.registerPremiumKits();
        return manager;
    }
    
    /**
     * Initialize all kits after construction
     */
    private void initializeKits() {
        registerDefaultKits();
        registerPremiumKits();
    }
    
    /**
     * Register all default (free) kits
     */
    private void registerDefaultKits() {
        registerKit(new SwordsmanKit());
        registerKit(new TankKit());
        registerKit(new ArcherKit());
        registerKit(new AssassinKit());
        registerKit(new MedicKit());
    }
    
    /**
     * Register all premium (paid) kits
     */
    private void registerPremiumKits() {
        registerKit(new BerserkerKit());
        registerKit(new WizardKit());
        registerKit(new BuilderKit());
        registerKit(new SpawnerKit());
        registerKit(new ArcherProKit());
    }
    
    /**
     * Register a kit
     */
    public void registerKit(Kit kit) {
        kits.put(kit.getId(), kit);
    }
    
    /**
     * Get a kit by ID
     */
    public Kit getKit(String kitId) {
        return kits.get(kitId);
    }
    
    /**
     * Get all available kits
     */
    public Collection<Kit> getAllKits() {
        return kits.values();
    }
    
    /**
     * Get all default (free) kits
     */
    public Collection<Kit> getDefaultKits() {
        return kits.values().stream()
            .filter(kit -> !kit.isPremium())
            .toList();
    }
    
    /**
     * Get all premium (paid) kits
     */
    public Collection<Kit> getPremiumKits() {
        return kits.values().stream()
            .filter(Kit::isPremium)
            .toList();
    }
    
    /**
     * Set a player's selected kit
     */
    public void setPlayerKit(Player player, String kitId) {
        setPlayerKit(player.getUniqueId(), kitId);
    }
    
    /**
     * Set a player's selected kit by UUID
     */
    public void setPlayerKit(UUID playerUuid, String kitId) {
        if (kits.containsKey(kitId)) {
            playerKitSelection.put(playerUuid, kitId);
        }
    }
    
    /**
     * Get a player's selected kit
     */
    public Kit getPlayerKit(Player player) {
        return getPlayerKit(player.getUniqueId());
    }
    
    /**
     * Get a player's selected kit by UUID
     */
    public Kit getPlayerKit(UUID playerUuid) {
        String kitId = playerKitSelection.get(playerUuid);
        if (kitId != null) {
            return kits.get(kitId);
        }
        
        // Default to swordsman if no kit selected
        return kits.get("swordsman");
    }
    
    /**
     * Check if a player has selected a kit
     */
    public boolean hasPlayerSelectedKit(Player player) {
        return playerKitSelection.containsKey(player.getUniqueId());
    }
    
    /**
     * Check if a player can afford a kit
     */
    public boolean canPlayerAffordKit(Player player, String kitId, int playerCredits) {
        Kit kit = getKit(kitId);
        if (kit == null) {
            return false;
        }
        
        return kit.canPlayerUse(player, playerCredits);
    }
    
    /**
     * Apply a kit to a player
     */
    public void applyKitToPlayer(Player player) {
        Kit kit = getPlayerKit(player);
        if (kit != null) {
            kit.applyKit(player);
        }
    }
    
    /**
     * Clear all kit selections (useful for game reset)
     */
    public void clearAllSelections() {
        playerKitSelection.clear();
    }
    
    /**
     * Remove a player's kit selection
     */
    public void clearPlayerSelection(Player player) {
        playerKitSelection.remove(player.getUniqueId());
    }
    
    /**
     * Get kit selection statistics
     */
    public Map<String, Integer> getKitSelectionStats() {
        Map<String, Integer> stats = new HashMap<>();
        
        for (String kitId : playerKitSelection.values()) {
            stats.put(kitId, stats.getOrDefault(kitId, 0) + 1);
        }
        
        return stats;
    }
}