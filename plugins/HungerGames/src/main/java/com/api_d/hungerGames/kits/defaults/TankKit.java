package com.api_d.hungerGames.kits.defaults;

import com.api_d.hungerGames.kits.Kit;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;

/**
 * The Tank kit - heavily armored but slow
 */
public class TankKit extends Kit {
    
    public TankKit() {
        super("tank", "The Tank", "Heavy armor and resistance, but slower movement", 
              false, 0, Material.IRON_CHESTPLATE);
    }
    
    @Override
    public List<ItemStack> getStartingItems() {
        return Arrays.asList(
            new ItemStack(Material.IRON_HELMET),
            new ItemStack(Material.IRON_CHESTPLATE),
            new ItemStack(Material.IRON_LEGGINGS),
            new ItemStack(Material.IRON_BOOTS),
            new ItemStack(Material.STONE_SWORD),
            new ItemStack(Material.COOKED_BEEF, 2)
        );
    }
    
    @Override
    public List<PotionEffect> getStartingEffects() {
        return Arrays.asList(
            new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 6000, 0), // 5:00 Resistance I
            new PotionEffect(PotionEffectType.SLOWNESS, 3600, 0) // 3:00 Slowness I
        );
    }
    
    @Override
    public List<AfterDeathEffect> getAfterDeathEffects() {
        return Arrays.asList(
            new IronDefenseEffect()
        );
    }
    
    /**
     * Iron defense after-death effect
     */
    private static class IronDefenseEffect extends AfterDeathEffect {
        
        public IronDefenseEffect() {
            super("Iron Defense", "Spawn an iron ingot for 15 seconds", 120, Material.IRON_INGOT);
        }
        
        @Override
        public void execute(Player spectator) {
            // Drop an uncollectable iron ingot at spectator's feet
            ItemStack ironIngot = new ItemStack(Material.IRON_INGOT);
            Item droppedItem = spectator.getWorld().dropItemNaturally(spectator.getLocation(), ironIngot);
            
            // Make it uncollectable
            droppedItem.setPickupDelay(Integer.MAX_VALUE);
            
            // Remove after 15 seconds
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (droppedItem.isValid()) {
                        droppedItem.remove();
                    }
                }
            }.runTaskLater(spectator.getServer().getPluginManager().getPlugin("HungerGames"), 300); // 15 seconds
        }
    }
}