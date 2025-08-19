package com.api_d.hungerGames.kits.defaults;

import com.api_d.hungerGames.kits.Kit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.Arrays;
import java.util.List;

/**
 * The Assassin kit - stealth and high damage but low health
 */
public class AssassinKit extends Kit {
    
    public AssassinKit() {
        super("assassin", "The Assassin", "High damage and stealth, but very low health", 
              false, 0, Material.LEATHER_CHESTPLATE);
    }
    
    @Override
    public List<ItemStack> getStartingItems() {
        // Create black leather armor
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        
        // Dye armor black
        dyeArmor(helmet, Color.BLACK);
        dyeArmor(chestplate, Color.BLACK);
        dyeArmor(leggings, Color.BLACK);
        dyeArmor(boots, Color.BLACK);
        
        // Create sharpness II iron sword
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        ItemMeta swordMeta = sword.getItemMeta();
        if (swordMeta != null) {
            swordMeta.addEnchant(org.bukkit.enchantments.Enchantment.SHARPNESS, 2, true);
            sword.setItemMeta(swordMeta);
        }
        
        // Create poison splash potions
        ItemStack poisonPotion1 = createPoisonSplashPotion();
        ItemStack poisonPotion2 = createPoisonSplashPotion();
        
        return Arrays.asList(
            helmet, chestplate, leggings, boots,
            sword,
            poisonPotion1, poisonPotion2
        );
    }
    
    private void dyeArmor(ItemStack armor, Color color) {
        if (armor.getItemMeta() instanceof LeatherArmorMeta armorMeta) {
            armorMeta.setColor(color);
            armor.setItemMeta(armorMeta);
        }
    }
    
    private ItemStack createPoisonSplashPotion() {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta != null) {
            meta.setBasePotionType(PotionType.POISON);
            potion.setItemMeta(meta);
        }
        return potion;
    }
    
    @Override
    public List<PotionEffect> getStartingEffects() {
        return Arrays.asList(
            new PotionEffect(PotionEffectType.INVISIBILITY, 2400, 0), // 2:00 Invisibility I
            new PotionEffect(PotionEffectType.SPEED, 6000, 1) // 5:00 Speed II
        );
    }
    
    @Override
    protected void applySpecialEffects(Player player) {
        // Set maximum health to 4 hearts (8 health points)
        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(8.0);
        player.setHealth(8.0);
    }
    
    @Override
    public List<AfterDeathEffect> getAfterDeathEffects() {
        // No after-death effects for assassin
        return Arrays.asList();
    }
}