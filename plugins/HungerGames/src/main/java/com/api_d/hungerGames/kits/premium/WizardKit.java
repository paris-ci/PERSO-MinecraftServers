package com.api_d.hungerGames.kits.premium;

import com.api_d.hungerGames.kits.Kit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.Arrays;
import java.util.List;

/**
 * The Wizard kit - magical attacks and fire resistance
 */
public class WizardKit extends Kit {
    
    public WizardKit() {
        super("wizard", "The Wizard", "Magical attacks with fire charges and potions, costs 200 credits", 
              true, 200, Material.FIRE_CHARGE);
    }
    
    @Override
    public List<ItemStack> getStartingItems() {
        // Create purple leather armor
        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        
        // Dye armor purple
        dyeArmor(helmet, Color.PURPLE);
        dyeArmor(chestplate, Color.PURPLE);
        dyeArmor(leggings, Color.PURPLE);
        dyeArmor(boots, Color.PURPLE);
        
        // Create harming splash potions
        ItemStack harmingPotion1 = createHarmingSplashPotion();
        ItemStack harmingPotion2 = createHarmingSplashPotion();
        ItemStack harmingPotion3 = createHarmingSplashPotion();
        
        return Arrays.asList(
            helmet, chestplate, leggings, boots,
            new ItemStack(Material.FIRE_CHARGE, 2),
            new ItemStack(Material.ENDER_PEARL, 1),
            harmingPotion1, harmingPotion2, harmingPotion3
        );
    }
    
    private void dyeArmor(ItemStack armor, Color color) {
        if (armor.getItemMeta() instanceof LeatherArmorMeta armorMeta) {
            armorMeta.setColor(color);
            armor.setItemMeta(armorMeta);
        }
    }
    
    private ItemStack createHarmingSplashPotion() {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta != null) {
            meta.setBasePotionType(PotionType.HARMING);
            potion.setItemMeta(meta);
        }
        return potion;
    }
    
    @Override
    public List<PotionEffect> getStartingEffects() {
        return Arrays.asList(
            new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 3600, 0) // 3:00 Fire resistance
        );
    }
    
    @Override
    public void onDeath(Player player, Player killer) {
        // Strike lightning at death location
        player.getWorld().strikeLightning(player.getLocation());
        
        // Additional lightning strikes nearby for dramatic effect
        for (int i = 0; i < 3; i++) {
            double offsetX = (Math.random() - 0.5) * 6; // Random offset within 3 blocks
            double offsetZ = (Math.random() - 0.5) * 6;
            player.getWorld().strikeLightning(player.getLocation().add(offsetX, 0, offsetZ));
        }
    }
    
    @Override
    public List<AfterDeathEffect> getAfterDeathEffects() {
        // No after-death effects for wizard
        return Arrays.asList();
    }
}