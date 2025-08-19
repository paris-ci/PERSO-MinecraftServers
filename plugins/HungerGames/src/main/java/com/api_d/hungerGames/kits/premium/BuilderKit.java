package com.api_d.hungerGames.kits.premium;

import com.api_d.hungerGames.kits.Kit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;

/**
 * The Builder kit - construction materials and utility
 */
public class BuilderKit extends Kit {
    
    public BuilderKit() {
        super("builder", "The Builder", "Construction materials and building utilities, costs 50 credits", 
              true, 50, Material.COBBLESTONE);
    }
    
    @Override
    public List<ItemStack> getStartingItems() {
        return Arrays.asList(
            new ItemStack(Material.DIRT, 64),
            new ItemStack(Material.COBBLESTONE, 64),
            new ItemStack(Material.COBBLESTONE, 64), // Second stack for 128 total
            new ItemStack(Material.WATER_BUCKET),
            new ItemStack(Material.LAVA_BUCKET),
            new ItemStack(Material.LADDER, 64),
            new ItemStack(Material.LADDER, 32), // 96 total ladders
            new ItemStack(Material.OAK_SAPLING)
        );
    }
    
    @Override
    public List<PotionEffect> getStartingEffects() {
        return Arrays.asList(
            new PotionEffect(PotionEffectType.JUMP, 2400, 0), // 2:00 Jump boost I
            new PotionEffect(PotionEffectType.REGENERATION, 2400, 1) // 2:00 Regeneration II
        );
    }
    
    @Override
    public void onDeath(Player player, Player killer) {
        // Transfer inventory into a chest at death location
        Block deathBlock = player.getLocation().getBlock();
        
        // Find a suitable location for the chest (on the ground)
        Block chestLocation = deathBlock;
        while (!chestLocation.getType().isSolid() && chestLocation.getY() > player.getWorld().getMinHeight()) {
            chestLocation = chestLocation.getRelative(0, -1, 0);
        }
        chestLocation = chestLocation.getRelative(0, 1, 0); // Place on top
        
        // Place the chest
        chestLocation.setType(Material.CHEST);
        
        // Transfer inventory items to the chest
        if (chestLocation.getState() instanceof Chest chest) {
            ItemStack[] inventory = player.getInventory().getContents();
            for (ItemStack item : inventory) {
                if (item != null && item.getType() != Material.AIR) {
                    chest.getInventory().addItem(item);
                }
            }
            chest.update();
        }
    }
    
    @Override
    public List<AfterDeathEffect> getAfterDeathEffects() {
        return Arrays.asList(
            new GlassBlocksEffect()
        );
    }
    
    /**
     * Glass blocks after-death effect
     */
    private static class GlassBlocksEffect extends AfterDeathEffect {
        
        public GlassBlocksEffect() {
            super("Glass Blocks", "Get 3 glass blocks to place on the map", 0, Material.GLASS);
        }
        
        @Override
        public void execute(Player spectator) {
            // Give the spectator 3 glass blocks they can place
            // Note: This would require special handling in the game mechanics
            // to allow spectators to place blocks temporarily
            spectator.getInventory().addItem(new ItemStack(Material.GLASS, 3));
            spectator.sendMessage("§aYou received 3 glass blocks that you can place as a spectator!");
        }
    }
}