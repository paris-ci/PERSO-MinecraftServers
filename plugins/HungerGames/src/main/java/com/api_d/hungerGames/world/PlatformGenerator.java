package com.api_d.hungerGames.world;

import com.api_d.hungerGames.config.GameConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;
import com.api_d.hungerGames.util.HGLogger;

/**
 * Generates spawn and feast platforms
 */
public class PlatformGenerator {
    
    private final GameConfig config;
    private final HGLogger logger;
    private final Random random;
    
    public PlatformGenerator(GameConfig config, java.util.logging.Logger logger) {
        this.config = config;
        this.logger = new HGLogger(logger);
        this.random = new Random();
    }
    
    /**
     * Generate the spawn platform at the given location
     */
    public void generateSpawnPlatform(Location center) {
        logger.info("Generating spawn platform at " + center.toString());
        
        World world = center.getWorld();
        int radius = config.getSpawnRadius();
        
        // Create the platform
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distance = Math.sqrt(x * x + z * z);
                
                if (distance <= radius) {
                    // Create wooden platform
                    Block block = world.getBlockAt(center.getBlockX() + x, center.getBlockY(), center.getBlockZ() + z);
                    block.setType(Material.OAK_PLANKS);
                    
                    // Clear blocks above the platform
                    for (int y = 1; y <= 10; y++) {
                        Block above = world.getBlockAt(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z);
                        if (above.getType() != Material.AIR) {
                            above.setType(Material.AIR);
                        }
                    }
                }
            }
        }
        
        // Get spawn items for debugging
        List<String> spawnItems = config.getSpawnItems();
        logger.info("Spawn items configured: " + spawnItems.size() + " items");
        for (String item : spawnItems) {
            logger.info("  - " + item);
        }
        
        // Generate central fountain/pillar with chests
        generateCentralStructure(center, spawnItems);
        
        logger.info("Spawn platform generated successfully");
    }
    
    /**
     * Generate the feast platform at a random location
     */
    public Location generateFeastPlatform(World world, Location worldCenter, int worldBorderSize) {
        logger.info("Generating feast platform...");
        
        int radius = config.getFeastRadius();
        int borderDistance = config.getFeastBorderDistance();
        
        // Find a suitable location
        Location feastCenter = findSuitableFeastLocation(world, worldCenter, worldBorderSize, borderDistance);
        
        // Find the highest point in the area to create a flat platform
        int maxHeight = feastCenter.getBlockY();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distance = Math.sqrt(x * x + z * z);
                if (distance <= radius) {
                    int height = world.getHighestBlockYAt(feastCenter.getBlockX() + x, feastCenter.getBlockZ() + z);
                    if (height > maxHeight) {
                        maxHeight = height;
                    }
                }
            }
        }
        
        // Set platform height to be above the highest point in the area
        int platformHeight = maxHeight + 2;
        feastCenter.setY(platformHeight);
        
        logger.info("Creating flat feast platform at height " + platformHeight + " with radius " + radius);
        
        // Create the platform as a flat surface
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distance = Math.sqrt(x * x + z * z);
                
                if (distance <= radius) {
                    // Create stone platform at consistent height
                    Block block = world.getBlockAt(feastCenter.getBlockX() + x, platformHeight, feastCenter.getBlockZ() + z);
                    block.setType(Material.STONE);
                    
                    // Clear blocks above the platform
                    for (int clearY = platformHeight + 1; clearY <= platformHeight + 10; clearY++) {
                        Block above = world.getBlockAt(feastCenter.getBlockX() + x, clearY, feastCenter.getBlockZ() + z);
                        if (above.getType() != Material.AIR) {
                            above.setType(Material.AIR);
                        }
                    }
                }
            }
        }
        
        // Get feast items for debugging
        List<String> feastItems = config.getFeastItems();
        logger.info("Feast items configured: " + feastItems.size() + " items");
        for (String item : feastItems) {
            logger.info("  - " + item);
        }
        
        // Generate central structure with feast loot
        generateCentralStructure(feastCenter, feastItems);
        
        logger.info("Feast platform generated successfully at " + feastCenter.toString());
        return feastCenter;
    }
    
    /**
     * Find a suitable location for the feast platform
     */
    private Location findSuitableFeastLocation(World world, Location center, int borderSize, int borderDistance) {
        int maxAttempts = 50;
        int maxDistance = (borderSize / 2) - borderDistance;
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Generate random coordinates within the allowed area
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * maxDistance;
            
            int x = center.getBlockX() + (int) (Math.cos(angle) * distance);
            int z = center.getBlockZ() + (int) (Math.sin(angle) * distance);
            
            // Find the highest block at this location
            int y = world.getHighestBlockYAt(x, z);
            
            Location candidate = new Location(world, x, y, z);
            
            // Check if this location is suitable (not in water, lava, etc.)
            Block block = world.getBlockAt(x, y, z);
            if (isSuitableForPlatform(block)) {
                return candidate;
            }
        }
        
        // Fallback: use a location near the center
        return new Location(world, center.getX() + 100, center.getY(), center.getZ() + 100);
    }
    
    /**
     * Check if a location is suitable for platform generation
     */
    private boolean isSuitableForPlatform(Block block) {
        Material type = block.getType();
        
        // Avoid water, lava, and air
        return type != Material.WATER && 
               type != Material.LAVA && 
               type != Material.AIR &&
               type != Material.BEDROCK;
    }
    
    /**
     * Generate the central fountain/pillar structure with chests
     */
    private void generateCentralStructure(Location center, List<String> lootItems) {
        World world = center.getWorld();
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();
        
        logger.info("Generating central structure at " + center.toString() + " with " + lootItems.size() + " loot items");
        
        // Create a small pillar in the center
        for (int y = 1; y <= 4; y++) {
            Block pillarBlock = world.getBlockAt(centerX, centerY + y, centerZ);
            if (y <= 2) {
                pillarBlock.setType(Material.STONE_BRICKS);
                logger.info("Placed stone brick at " + centerX + ", " + (centerY + y) + ", " + centerZ);
            } else {
                pillarBlock.setType(Material.COBBLESTONE_WALL);
                logger.info("Placed cobblestone wall at " + centerX + ", " + (centerY + y) + ", " + centerZ);
            }
        }
        
        // Add chests around the pillar (two levels)
        addChestsAroundPillar(center, lootItems, 1); // Ground level
        addChestsAroundPillar(center, lootItems, 2); // Upper level
        
        logger.info("Central structure generation completed");
    }
    
    /**
     * Add chests around the central pillar at a specific level
     */
    private void addChestsAroundPillar(Location center, List<String> lootItems, int level) {
        World world = center.getWorld();
        int centerX = center.getBlockX();
        int centerY = center.getBlockY() + level;
        int centerZ = center.getBlockZ();
        
        // Place chests at the four cardinal directions
        Location[] chestLocations = {
            new Location(world, centerX + 2, centerY, centerZ),     // East
            new Location(world, centerX - 2, centerY, centerZ),     // West
            new Location(world, centerX, centerY, centerZ + 2),     // South
            new Location(world, centerX, centerY, centerZ - 2)      // North
        };
        
        for (Location chestLocation : chestLocations) {
            // Create a small platform for the chest if needed
            Block platform = world.getBlockAt(chestLocation.getBlockX(), chestLocation.getBlockY() - 1, chestLocation.getBlockZ());
            platform.setType(Material.STONE_BRICKS);
            
            // Place the chest
            Block chestBlock = world.getBlockAt(chestLocation);
            chestBlock.setType(Material.CHEST);
            
            // Get the block state and ensure it's a chest
            BlockState state = chestBlock.getState();
            if (state instanceof Chest chest) {
                logger.info("Creating and filling chest at " + chestLocation.toString() + " with " + lootItems.size() + " items");
                
                // Fill the chest inventory
                fillChestWithLoot(chest, lootItems);
                logger.info("Chest successfully created and filled at " + chestLocation.toString());
            } else {
                logger.warning("Failed to create chest at " + chestLocation.toString() + " - block state is not a chest");
            }
        }
        
        logger.info("Added " + chestLocations.length + " chests at level " + level + " around center " + center.toString());
    }
    
    /**
     * Fill a chest with loot items
     */
    private void fillChestWithLoot(Chest chest, List<String> lootItems) {
        Inventory inv = chest.getInventory();

        for (String itemString : lootItems) {
            // Parse item string format: "MATERIAL:AMOUNT"
            String[] parts = itemString.split(":");
            if (parts.length != 2) {
                logger.warning("Invalid loot item format: " + itemString + ". " + "Expected format: MATERIAL:AMOUNT");
                continue;
            }

            Material material;
            try {
                material = Material.valueOf(parts[0].toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid material in loot items: " + itemString + " - " + e.getMessage());
                continue;
            }
            
            int amount = Integer.parseInt(parts[1]);
            
            ItemStack item = new ItemStack(material, amount);
            
            // Add item to a random slot in the chest
            int slot = random.nextInt(inv.getSize());
            
            // Find an empty slot if the random one is occupied
            while (inv.getItem(slot) != null) {
                slot = (slot + 1) % inv.getSize();
            }
            
            inv.setItem(slot, item);
            // logger.info("Added " + amount + "x " + material.name() + " to chest slot " + slot);
        }
        
        // logger.info("Chest filled successfully with " + lootItems.size() + " items");
    }
    
    /**
     * Create a single chest at the specified location and fill it with items
     * This method follows the proper Paper API pattern for chest creation
     */
    public void createAndFillChest(Location location, List<String> lootItems) {
        World world = location.getWorld();
        Block block = world.getBlockAt(location);
        
        // Set the block to a chest
        block.setType(Material.CHEST);
        block.getState().update(true, false);
        
        // Get the block state and ensure it's a chest
        BlockState state = block.getState();
        if (state instanceof Chest chest) {
            logger.info("Creating and filling chest at " + location.toString());
            
            // Fill the chest inventory
            fillChestWithLoot(chest, lootItems);
            
            
            logger.info("Chest successfully created and filled at " + location.toString());
        } else {
            logger.warning("Failed to create chest at " + location.toString() + " - block state is not a chest");
        }
    }
    
    /**
     * Clear an area of blocks (useful for cleanup)
     */
    public void clearArea(Location center, int radius) {
        World world = center.getWorld();
        
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = 0; y <= 10; y++) {
                    double distance = Math.sqrt(x * x + z * z);
                    
                    if (distance <= radius) {
                        Block block = world.getBlockAt(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z);
                        if (block.getType() != Material.AIR && block.getType() != Material.BEDROCK) {
                            block.setType(Material.AIR);
                        }
                    }
                }
            }
        }
        
        logger.info("Cleared area at " + center.toString() + " with radius " + radius);
    }
}