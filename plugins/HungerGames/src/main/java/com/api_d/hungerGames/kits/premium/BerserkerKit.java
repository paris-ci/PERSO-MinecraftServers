package com.api_d.hungerGames.kits.premium;

import com.api_d.hungerGames.kits.Kit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;

/**
 * The Berserker kit - explosive combat and rage
 */
public class BerserkerKit extends Kit {
    
    public BerserkerKit() {
        super("berserker", "The Berserker", "High damage and explosive death, costs 150 credits", 
              true, 150, Material.IRON_AXE);
    }
    
    @Override
    public List<ItemStack> getStartingItems() {
        // Create sharpness I iron axe
        ItemStack axe = new ItemStack(Material.IRON_AXE);
        ItemMeta axeMeta = axe.getItemMeta();
        if (axeMeta != null) {
            axeMeta.addEnchant(org.bukkit.enchantments.Enchantment.SHARPNESS, 1, true);
            axe.setItemMeta(axeMeta);
        }
        
        return Arrays.asList(
            new ItemStack(Material.CHAINMAIL_HELMET),
            new ItemStack(Material.CHAINMAIL_CHESTPLATE),
            new ItemStack(Material.CHAINMAIL_LEGGINGS),
            new ItemStack(Material.CHAINMAIL_BOOTS),
            axe,
            new ItemStack(Material.COOKED_PORKCHOP, 3)
        );
    }
    
    @Override
    public List<PotionEffect> getStartingEffects() {
        return Arrays.asList(
            new PotionEffect(PotionEffectType.STRENGTH, 2400, 0), // 2:00 Strength I
            new PotionEffect(PotionEffectType.SPEED, 4800, 0) // 4:00 Speed I
        );
    }
    
    @Override
    public void onDeath(Player player, Player killer) {
        // Create explosion at death location
        player.getWorld().createExplosion(player.getLocation(), 3.0f, false, false);
        
        // Damage nearby players (but don't destroy blocks)
        player.getWorld().getNearbyEntities(player.getLocation(), 5, 5, 5)
            .stream()
            .filter(entity -> entity instanceof Player)
            .filter(entity -> entity != player)
            .map(entity -> (Player) entity)
            .forEach(nearbyPlayer -> {
                double distance = nearbyPlayer.getLocation().distance(player.getLocation());
                double damage = Math.max(1, 8 - distance); // 8 damage at center, decreasing with distance
                nearbyPlayer.damage(damage);
            });
    }
    
    @Override
    public List<AfterDeathEffect> getAfterDeathEffects() {
        return Arrays.asList(
            new RageModeEffect()
        );
    }
    
    /**
     * Rage mode after-death effect
     */
    private static class RageModeEffect extends AfterDeathEffect {
        
        public RageModeEffect() {
            super("Rage Mode", "Play aggressive sounds for intimidation", 240, Material.TNT);
        }
        
        @Override
        public void execute(Player spectator) {
            // Play various aggressive sounds
            spectator.getWorld().playSound(spectator.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1.0f, 0.8f);
            spectator.getWorld().playSound(spectator.getLocation(), Sound.ENTITY_BLAZE_HURT, 1.0f, 0.5f);
            spectator.getWorld().playSound(spectator.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 1.0f, 0.7f);
            
            // Play sounds to nearby players as well
            spectator.getWorld().getPlayers().stream()
                .filter(p -> p != spectator)
                .filter(p -> p.getLocation().distance(spectator.getLocation()) <= 20)
                .forEach(p -> {
                    p.playSound(p.getLocation(), Sound.ENTITY_GHAST_AMBIENT, 0.5f, 0.8f);
                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.3f, 1.2f);
                });
        }
    }
}