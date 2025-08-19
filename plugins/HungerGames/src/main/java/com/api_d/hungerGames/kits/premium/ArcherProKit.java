package com.api_d.hungerGames.kits.premium;

import com.api_d.hungerGames.kits.Kit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Arrays;
import java.util.List;

/**
 * The Archer Pro kit - advanced archery with explosive arrows
 */
public class ArcherProKit extends Kit {
    
    public ArcherProKit() {
        super("archer_pro", "Archer Pro", "Explosive arrows that explode on impact! Advanced archery, costs 1000 credits", 
              true, 1000, Material.BOW);
    }
    
    @Override
    public List<ItemStack> getStartingItems() {
        return Arrays.asList(
            new ItemStack(Material.BOW),
            new ItemStack(Material.ARROW, 32), // More arrows since they explode
            new ItemStack(Material.FLINT_AND_STEEL)
        );
    }
    
    @Override
    public List<PotionEffect> getStartingEffects() {
        // Special effects are handled by the game mechanics for explosive arrows
        // and spectator collision
        return Arrays.asList();
    }
    
    @Override
    protected void applySpecialEffects(Player player) {
        // The explosive arrow effect is handled by the main plugin's ProjectileHitEvent
        // All arrows shot by players with this kit will explode on impact
        player.sendMessage("§a§l[Archer Pro] §7Your arrows will now explode on impact!");
        player.sendMessage("§a§l[Archer Pro] §7You have 32 explosive arrows - use them wisely!");
    }
    
    @Override
    public List<AfterDeathEffect> getAfterDeathEffects() {
        // No after-death effects for archer pro
        return Arrays.asList();
    }
}