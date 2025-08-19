package com.api_d.hungerGames.game;

import com.api_d.hungerGames.HungerGames;
import com.api_d.hungerGames.config.GameConfig;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import com.api_d.hungerGames.util.HGLogger;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages game protection features like preventing world interaction,
 * mob targeting, and managing player flight when the game isn't started
 */
public class GameProtectionManager implements Listener {
    
    private final HungerGames plugin;
    private final GameConfig config;
    private final GameManager gameManager;
    private final HGLogger logger;
    
    // Track players who should have flight enabled
    private final ConcurrentHashMap<UUID, Boolean> playerFlightEnabled = new ConcurrentHashMap<>();
    
    // Task for forcing daytime
    private BukkitTask daytimeTask;
    
    public GameProtectionManager(HungerGames plugin, GameConfig config, GameManager gameManager) {
        this.plugin = plugin;
        this.config = config;
        this.gameManager = gameManager;
        this.logger = new HGLogger(plugin);
    }
    
    /**
     * Initialize the protection manager
     */
    public void initialize() {
        // Register event listeners
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // Start daytime management
        startDaytimeManagement();
        
        logger.info("Game protection manager initialized");
    }
    
    /**
     * Start managing daytime to keep it always day
     */
    private void startDaytimeManagement() {
        if (!config.isForcedDaytimeEnabled()) {
            return;
        }
        
        if (daytimeTask != null) {
            daytimeTask.cancel();
        }
        
        daytimeTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : plugin.getServer().getWorlds()) {
                    // Set time to day (6000 ticks = noon)
                    world.setTime(6000);
                    // Prevent weather changes
                    world.setStorm(false);
                    world.setThundering(false);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Run every second
        
        logger.info("Daytime management started");
    }
    
    /**
     * Stop daytime management
     */
    public void stopDaytimeManagement() {
        if (daytimeTask != null) {
            daytimeTask.cancel();
            daytimeTask = null;
            logger.info("Daytime management stopped");
        }
    }
    
    /**
     * Check if world interaction should be blocked
     */
    private boolean shouldBlockInteraction() {
        return config.isWorldInteractionBlocked() && 
               (!gameManager.isGameRunning() || 
                gameManager.getCurrentState() == null ||
                !gameManager.getCurrentState().isGameActive());
    }
    
    /**
     * Check if a player should have flight enabled
     */
    private boolean shouldPlayerFly(Player player) {
        return config.isFlightEnabledWhenWaiting() && 
               (!gameManager.isGameRunning() || 
                gameManager.getCurrentState() == null ||
                !gameManager.getCurrentState().isGameActive());
    }
    
    /**
     * Enable flight for a player
     */
    public void enableFlight(Player player) {
        if (shouldPlayerFly(player)) {
            player.setAllowFlight(true);
            player.setFlying(true);
            playerFlightEnabled.put(player.getUniqueId(), true);
        }
    }
    
    /**
     * Disable flight for a player
     */
    public void disableFlight(Player player) {
        player.setAllowFlight(false);
        player.setFlying(false);
        playerFlightEnabled.put(player.getUniqueId(), false);
    }
    
    /**
     * Update flight status for all online players
     */
    public void updateAllPlayersFlight() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (shouldPlayerFly(player)) {
                enableFlight(player);
            } else {
                disableFlight(player);
            }
        }
    }
    
    // Event handlers
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Enable flight if game isn't started and flight is enabled in config
        if (shouldPlayerFly(player)) {
            enableFlight(player);
            
            // Set player to creative mode if flight is enabled
            player.setGameMode(GameMode.CREATIVE);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (shouldBlockInteraction()) {
            Player player = event.getPlayer();
            
            // Allow compass interaction (for kit selection)
            if (event.getItem() != null && event.getItem().getType().name().contains("COMPASS")) {
                return;
            }
            
            // Block all other interactions
            event.setCancelled(true);
            
            // Send message to player
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                player.sendMessage("§cYou cannot interact with the world while the game hasn't started!");
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (shouldBlockInteraction()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou cannot break blocks while the game hasn't started!");
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (shouldBlockInteraction()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou cannot place blocks while the game hasn't started!");
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityTarget(EntityTargetEvent event) {
        if (config.isMobTargetingBlocked() && shouldBlockInteraction()) {
            // Prevent mobs from targeting players
            if (event.getTarget() instanceof Player) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        
        if (shouldPlayerFly(player)) {
            // Force flight on if player tries to turn it off
            if (!event.isFlying()) {
                event.setCancelled(true);
                player.setFlying(true);
                player.sendMessage("§eFlight is currently forced on while the game hasn't started!");
            }
        }
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        stopDaytimeManagement();
        
        // Disable flight for all players
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            disableFlight(player);
        }
        
        playerFlightEnabled.clear();
        logger.info("Game protection manager cleaned up");
    }
}
