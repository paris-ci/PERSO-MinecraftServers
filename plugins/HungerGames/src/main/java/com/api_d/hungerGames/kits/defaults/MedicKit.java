package com.api_d.hungerGames.kits.defaults;

import com.api_d.hungerGames.kits.Kit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;

/**
 * The Medic kit - healing and support
 */
public class MedicKit extends Kit {
    
    public MedicKit() {
        super("medic", "The Medic", "Healing potions and regeneration support", 
              false, 0, Material.POTION);
    }
    
    @Override
    public List<ItemStack> getStartingItems() {
        // Create pink leather armor
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        
        // Dye armor pink
        dyeArmor(helmet, Color.FUCHSIA);
        dyeArmor(chestplate, Color.FUCHSIA);
        dyeArmor(leggings, Color.FUCHSIA);
        dyeArmor(boots, Color.FUCHSIA);
        
        // Create health potions II
        ItemStack healthPotion1 = createHealthPotion();
        ItemStack healthPotion2 = createHealthPotion();
        ItemStack healthPotion3 = createHealthPotion();
        
        // Create regeneration splash potions
        ItemStack regenPotion1 = createRegenerationSplashPotion();
        ItemStack regenPotion2 = createRegenerationSplashPotion();
        
        return Arrays.asList(
            helmet, chestplate, leggings, boots,
            healthPotion1, healthPotion2, healthPotion3,
            regenPotion1, regenPotion2,
            new ItemStack(Material.PORKCHOP, 6)
        );
    }
    
    private void dyeArmor(ItemStack armor, Color color) {
        if (armor.getItemMeta() instanceof LeatherArmorMeta armorMeta) {
            armorMeta.setColor(color);
            armor.setItemMeta(armorMeta);
        }
    }
    
    private ItemStack createHealthPotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta != null) {
            meta.setBasePotionType(PotionType.STRONG_HEALING);
            potion.setItemMeta(meta);
        }
        return potion;
    }
    
    private ItemStack createRegenerationSplashPotion() {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta != null) {
            meta.setBasePotionType(PotionType.REGENERATION);
            potion.setItemMeta(meta);
        }
        return potion;
    }
    
    @Override
    public List<PotionEffect> getStartingEffects() {
        return Arrays.asList(
            new PotionEffect(PotionEffectType.REGENERATION, 12000, 0) // 10:00 Regeneration I
        );
    }
    
    @Override
    public List<AfterDeathEffect> getAfterDeathEffects() {
        return Arrays.asList(
            new HealingAuraEffect()
        );
    }
    
    /**
     * Healing aura after-death effect
     */
    private static class HealingAuraEffect extends AfterDeathEffect {
        
        public HealingAuraEffect() {
            super("Healing Aura", "Create a healing aura for 3 seconds", 180, Material.GOLDEN_APPLE);
        }
        
        @Override
        public void execute(Player spectator) {
            new BukkitRunnable() {
                int ticks = 0;
                final int maxTicks = 60; // 3 seconds
                
                @Override
                public void run() {
                    if (ticks >= maxTicks) {
                        this.cancel();
                        return;
                    }
                    
                    // Spawn heart particles around the spectator
                    for (int i = 0; i < 10; i++) {
                        double angle = (Math.PI * 2 * i) / 10;
                        double radius = 15.0; // 15 blocks radius
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        
                        spectator.getWorld().spawnParticle(
                            Particle.HEART,
                            spectator.getLocation().add(x, 1, z),
                            1, 0, 0, 0, 0
                        );
                    }
                    
                    // Apply regeneration to all living entities within 15 blocks
                    spectator.getWorld().getNearbyEntities(spectator.getLocation(), 15, 15, 15)
                        .stream()
                        .filter(entity -> entity instanceof LivingEntity)
                        .filter(entity -> entity != spectator)
                        .map(entity -> (LivingEntity) entity)
                        .forEach(entity -> {
                            entity.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0)); // 2 seconds
                        });
                    
                    ticks++;
                }
            }.runTaskTimer(spectator.getServer().getPluginManager().getPlugin("HungerGames"), 0, 1);
        }
    }
}