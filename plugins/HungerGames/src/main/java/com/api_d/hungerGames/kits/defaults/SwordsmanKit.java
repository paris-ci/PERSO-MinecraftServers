package com.api_d.hungerGames.kits.defaults;

import com.api_d.hungerGames.kits.Kit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;

/**
 * The Swordsman kit - basic melee fighter
 */
public class SwordsmanKit extends Kit {
    
    public SwordsmanKit() {
        super("swordsman", "The Swordsman", "A balanced fighter with speed and sword skills", 
              false, 0, Material.IRON_SWORD);
    }
    
    @Override
    public List<ItemStack> getStartingItems() {
        return Arrays.asList(
            new ItemStack(Material.LEATHER_HELMET),
            new ItemStack(Material.LEATHER_CHESTPLATE),
            new ItemStack(Material.LEATHER_LEGGINGS),
            new ItemStack(Material.LEATHER_BOOTS),
            new ItemStack(Material.IRON_SWORD)
        );
    }
    
    @Override
    public List<PotionEffect> getStartingEffects() {
        return Arrays.asList(
            new PotionEffect(PotionEffectType.SPEED, 3600, 0) // 3:00 Speed I
        );
    }
    
    @Override
    public List<AfterDeathEffect> getAfterDeathEffects() {
        return Arrays.asList(
            new SwordFightEffect()
        );
    }
    
    /**
     * Sword fight after-death effect
     */
    private static class SwordFightEffect extends AfterDeathEffect {
        
        public SwordFightEffect() {
            super("Sword Fight", "Display sword fight particles", 60, Material.IRON_SWORD);
        }
        
        @Override
        public void execute(Player spectator) {
            // Spawn sword fight particles around the spectator
            for (int i = 0; i < 20; i++) {
                double angle = (Math.PI * 2 * i) / 20;
                double x = Math.cos(angle) * 2;
                double z = Math.sin(angle) * 2;
                
                spectator.getWorld().spawnParticle(
                    Particle.SWEEP_ATTACK,
                    spectator.getLocation().add(x, 1, z),
                    1, 0, 0, 0, 0
                );
            }
        }
    }
}