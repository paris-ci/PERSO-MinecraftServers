package com.api_d.hungerGames.kits.defaults;

import com.api_d.hungerGames.kits.Kit;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;

/**
 * The Archer kit - ranged combat specialist
 */
public class ArcherKit extends Kit {
    
    public ArcherKit() {
        super("archer", "The Archer", "Ranged combat with bow and arrows", 
              false, 0, Material.BOW);
    }
    
    @Override
    public List<ItemStack> getStartingItems() {
        return Arrays.asList(
            new ItemStack(Material.LEATHER_HELMET),
            new ItemStack(Material.LEATHER_CHESTPLATE),
            new ItemStack(Material.LEATHER_LEGGINGS),
            new ItemStack(Material.LEATHER_BOOTS),
            new ItemStack(Material.IRON_HELMET), // Additional iron helmet
            new ItemStack(Material.BOW),
            new ItemStack(Material.ARROW, 32),
            new ItemStack(Material.WOODEN_SWORD)
        );
    }
    
    @Override
    public List<PotionEffect> getStartingEffects() {
        return Arrays.asList(
            new PotionEffect(PotionEffectType.JUMP, 1200, 1) // 1:00 Jump II
        );
    }
    
    @Override
    public void onDeath(Player player, Player killer) {
        // Release a torrent of arrows in all directions
        for (int i = 0; i < 16; i++) {
            double angle = (Math.PI * 2 * i) / 16;
            Vector direction = new Vector(Math.cos(angle), 0.5, Math.sin(angle)).normalize();
            
            Arrow arrow = player.getWorld().spawnArrow(
                player.getLocation().add(0, 1, 0),
                direction,
                1.0f,
                0.1f
            );
            
            // Make arrows uncollectable
            arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
            
            // Remove arrows after 10 seconds
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (arrow.isValid()) {
                        arrow.remove();
                    }
                }
            }.runTaskLater(player.getServer().getPluginManager().getPlugin("HungerGames"), 200);
        }
    }
    
    @Override
    public List<AfterDeathEffect> getAfterDeathEffects() {
        return Arrays.asList(
            new EagleEyeEffect()
        );
    }
    
    /**
     * Eagle eye after-death effect
     */
    private static class EagleEyeEffect extends AfterDeathEffect {
        
        public EagleEyeEffect() {
            super("Eagle Eye", "Highlight nearby players with glowing effect", 90, Material.ENDER_EYE);
        }
        
        @Override
        public void execute(Player spectator) {
            // Find nearby players within 30 blocks and apply glowing effect
            spectator.getWorld().getPlayers().stream()
                .filter(p -> p != spectator)
                .filter(p -> p.getLocation().distance(spectator.getLocation()) <= 30)
                .filter(p -> !p.isDead())
                .forEach(p -> {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0)); // 10 seconds
                });
        }
    }
}