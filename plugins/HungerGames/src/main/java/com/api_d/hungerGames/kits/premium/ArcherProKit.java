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
        super("archer_pro", "Archer Pro", "Explosive arrows and advanced archery, costs 1000 credits", 
              true, 1000, Material.BOW);
    }
    
    @Override
    public List<ItemStack> getStartingItems() {
        return Arrays.asList(
            new ItemStack(Material.BOW),
            new ItemStack(Material.ARROW, 15),
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
        // The explosive arrow effect and spectator collision need to be handled
        // by the game mechanics system through event listeners
        // This would be implemented in the main game manager
    }
    
    @Override
    public List<AfterDeathEffect> getAfterDeathEffects() {
        // No after-death effects for archer pro
        return Arrays.asList();
    }
}