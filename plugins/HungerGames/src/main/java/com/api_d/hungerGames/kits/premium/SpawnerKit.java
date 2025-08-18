package com.api_d.hungerGames.kits.premium;

import com.api_d.hungerGames.kits.Kit;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * The Spawner kit - creature summoning and mob eggs
 */
public class SpawnerKit extends Kit {
    
    public SpawnerKit() {
        super("spawner", "The Spawner", "Summon creatures and mobs to fight for you, costs 500 credits", 
              true, 500, Material.SPAWNER);
    }
    
    @Override
    public List<ItemStack> getStartingItems() {
        return Arrays.asList(
            new ItemStack(Material.CHAINMAIL_HELMET),
            new ItemStack(Material.CHAINMAIL_CHESTPLATE),
            new ItemStack(Material.CHAINMAIL_LEGGINGS),
            new ItemStack(Material.CHAINMAIL_BOOTS),
            new ItemStack(Material.CREEPER_SPAWN_EGG, 3),
            new ItemStack(Material.SPIDER_SPAWN_EGG, 3),
            new ItemStack(Material.SPAWNER), // Zombie spawner
            new ItemStack(Material.INFESTED_COBBLESTONE, 64)
        );
    }
    
    @Override
    public List<PotionEffect> getStartingEffects() {
        // No starting effects for spawner kit
        return Arrays.asList();
    }
    
    @Override
    public void onDeath(Player player, Player killer) {
        // Spawn a charged creeper on the corpse
        Creeper creeper = (Creeper) player.getWorld().spawnEntity(player.getLocation(), EntityType.CREEPER);
        creeper.setPowered(true);
        creeper.setTarget(killer); // Target the killer if available
    }
    
    @Override
    public List<AfterDeathEffect> getAfterDeathEffects() {
        return Arrays.asList(
            new SpawnPassiveMobEffect()
        );
    }
    
    /**
     * Spawn passive mob after-death effect
     */
    private static class SpawnPassiveMobEffect extends AfterDeathEffect {
        
        private static final EntityType[] PASSIVE_MOBS = {
            EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.CHICKEN,
            EntityType.RABBIT, EntityType.HORSE, EntityType.DONKEY
        };
        
        public SpawnPassiveMobEffect() {
            super("Spawn Mob", "Spawn a random passive mob at your position", 60, Material.EGG);
        }
        
        @Override
        public void execute(Player spectator) {
            Random random = new Random();
            EntityType mobType = PASSIVE_MOBS[random.nextInt(PASSIVE_MOBS.length)];
            
            Entity spawnedMob = spectator.getWorld().spawnEntity(spectator.getLocation(), mobType);
            
            // Remove the mob after 2 minutes to prevent server lag
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (spawnedMob.isValid()) {
                        spawnedMob.remove();
                    }
                }
            }.runTaskLater(spectator.getServer().getPluginManager().getPlugin("HungerGames"), 2400); // 2 minutes
            
            spectator.sendMessage("§aYou spawned a " + mobType.name().toLowerCase().replace("_", " ") + "!");
        }
    }
}