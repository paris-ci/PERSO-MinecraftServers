package com.api_d.hungerGames.kits;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Abstract base class for all kits in the Hunger Games
 */
public abstract class Kit {
    
    protected final String id;
    protected final String displayName;
    protected final String description;
    protected final boolean isPremium;
    protected final int cost;
    protected final Material icon;
    
    // Permission for admins to bypass kit costs
    public static final String ADMIN_BYPASS_PERMISSION = "hungergames.admin.bypass.kitcost";
    
    public Kit(String id, String displayName, String description, boolean isPremium, int cost, Material icon) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.isPremium = isPremium;
        this.cost = cost;
        this.icon = icon;
    }
    
    /**
     * Get the unique identifier for this kit
     */
    public String getId() {
        return id;
    }
    
    /**
     * Get the display name of this kit
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get the description of this kit
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if this is a premium kit requiring credits
     */
    public boolean isPremium() {
        return isPremium;
    }
    
    /**
     * Get the cost of this kit in credits
     */
    public int getCost() {
        return cost;
    }
    
    /**
     * Get the icon material for GUI display
     */
    public Material getIcon() {
        return icon;
    }
    
    /**
     * Get the starting items for this kit
     */
    public abstract List<ItemStack> getStartingItems();
    
    /**
     * Get the starting potion effects for this kit
     */
    public abstract List<PotionEffect> getStartingEffects();
    
    /**
     * Apply the kit to a player (items and effects)
     */
    public void applyKit(Player player) {
        // Clear inventory first
        player.getInventory().clear();
        
        // Add starting items
        List<ItemStack> items = getStartingItems();
        for (ItemStack item : items) {
            if (item != null) {
                // Check if this is armor and equip it automatically
                if (isArmor(item.getType())) {
                    equipArmor(player, item);
                } else {
                    player.getInventory().addItem(item);
                }
            }
        }
        
        // Apply starting effects
        List<PotionEffect> effects = getStartingEffects();
        for (PotionEffect effect : effects) {
            if (effect != null) {
                player.addPotionEffect(effect);
            }
        }
        
        // Apply kit-specific modifications
        applySpecialEffects(player);
    }
    
    /**
     * Check if an item is armor
     */
    private boolean isArmor(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || 
               name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }
    
    /**
     * Equip armor in the correct slot
     */
    private void equipArmor(Player player, ItemStack armor) {
        Material material = armor.getType();
        String name = material.name();
        
        if (name.endsWith("_HELMET")) {
            player.getInventory().setHelmet(armor);
        } else if (name.endsWith("_CHESTPLATE")) {
            player.getInventory().setChestplate(armor);
        } else if (name.endsWith("_LEGGINGS")) {
            player.getInventory().setLeggings(armor);
        } else if (name.endsWith("_BOOTS")) {
            player.getInventory().setBoots(armor);
        }
    }
    
    /**
     * Apply special effects specific to this kit (like health modifications)
     */
    protected void applySpecialEffects(Player player) {
        // Override in subclasses if needed
    }
    
    /**
     * Called when the player dies with this kit
     */
    public void onDeath(Player player, Player killer) {
        // Override in subclasses if needed
    }
    
    /**
     * Get available after-death effects for spectators
     */
    public List<AfterDeathEffect> getAfterDeathEffects() {
        return new ArrayList<>();
    }
    
    /**
     * Check if a player can use this kit (based on credits or admin permissions)
     */
    public boolean canPlayerUse(Player player, int playerCredits) {
        if (!isPremium) {
            return true;
        }
        
        // Check if player has admin bypass permission
        if (player.hasPermission(ADMIN_BYPASS_PERMISSION)) {
            return true;
        }
        
        return playerCredits >= cost;
    }
    
    /**
     * Check if a player should pay for this kit (admins with bypass permission don't pay)
     */
    public boolean shouldPlayerPay(Player player) {
        if (!isPremium) {
            return false;
        }
        
        // Admins with bypass permission don't pay
        if (player.hasPermission(ADMIN_BYPASS_PERMISSION)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if a player has admin bypass permission
     */
    public boolean hasAdminBypass(Player player) {
        return player.hasPermission(ADMIN_BYPASS_PERMISSION);
    }
    
    /**
     * Get the effective cost for a player (0 if admin bypass, normal cost otherwise)
     */
    public int getEffectiveCost(Player player) {
        if (!isPremium) {
            return 0;
        }
        
        if (hasAdminBypass(player)) {
            return 0;
        }
        
        return cost;
    }
    
    /**
     * Create a display item for GUI menus
     */
    public ItemStack createDisplayItem(boolean canAfford) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Set display name with color coding
            String color = canAfford ? "§a" : "§c";
            meta.displayName(net.kyori.adventure.text.Component.text(color + displayName));
            
            // Create lore with kit information
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(net.kyori.adventure.text.Component.text("§7" + description));
            lore.add(net.kyori.adventure.text.Component.text(""));
            
            if (isPremium) {
                lore.add(net.kyori.adventure.text.Component.text("§6Premium Kit"));
                lore.add(net.kyori.adventure.text.Component.text("§7Cost: §e" + cost + " credits"));
                if (!canAfford) {
                    lore.add(net.kyori.adventure.text.Component.text("§cInsufficient credits"));
                }
                // Add admin bypass info if applicable
                if (canAfford && cost > 0) {
                    lore.add(net.kyori.adventure.text.Component.text("§a§l[ADMIN] §7Use /kit " + id + " to bypass cost"));
                }
            } else {
                lore.add(net.kyori.adventure.text.Component.text("§aFree Kit"));
            }
            
            lore.add(net.kyori.adventure.text.Component.text(""));
            lore.add(net.kyori.adventure.text.Component.text("§7Starting Items:"));
            for (ItemStack startingItem : getStartingItems()) {
                if (startingItem != null && startingItem.getItemMeta() != null) {
                    String itemName = startingItem.getItemMeta().displayName() != null ? 
                        startingItem.getItemMeta().displayName().toString() : 
                        startingItem.getType().name().toLowerCase().replace("_", " ");
                    lore.add(net.kyori.adventure.text.Component.text("§8- §7" + itemName));
                }
            }
            
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Represents an after-death effect for spectators
     */
    public static class AfterDeathEffect {
        private final String name;
        private final String description;
        private final int cooldownSeconds;
        private final Material icon;
        
        public AfterDeathEffect(String name, String description, int cooldownSeconds, Material icon) {
            this.name = name;
            this.description = description;
            this.cooldownSeconds = cooldownSeconds;
            this.icon = icon;
        }
        
        public String getName() {
            return name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public int getCooldownSeconds() {
            return cooldownSeconds;
        }
        
        public Material getIcon() {
            return icon;
        }
        
        /**
         * Execute this after-death effect
         */
        public void execute(Player spectator) {
            // Override in implementations
        }
    }
}