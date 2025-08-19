package com.api_d.hungerGames;

import com.api_d.hungerGames.commands.CreditsCommand;
import com.api_d.hungerGames.commands.KitCommand;
import com.api_d.hungerGames.commands.CompassCommand;
import com.api_d.hungerGames.commands.SpectateCommand;
import com.api_d.hungerGames.commands.AdminCommand;
import com.api_d.hungerGames.config.GameConfig;
import com.api_d.hungerGames.database.DatabaseManager;
import com.api_d.hungerGames.game.GameManager;
import com.api_d.hungerGames.game.GameState;
import com.api_d.hungerGames.kits.KitManager;
import com.api_d.hungerGames.player.PlayerManager;
import com.api_d.hungerGames.world.PlatformGenerator;
import com.api_d.hungerGames.util.HGLogger;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.logging.Level;

/**
 * Main plugin class for the Hunger Games plugin
 */
public final class HungerGames extends JavaPlugin implements Listener {

    // Core managers
    private GameConfig config;
    private DatabaseManager databaseManager;
    private PlayerManager playerManager;
    private KitManager kitManager;
    private GameManager gameManager;
    private PlatformGenerator platformGenerator;
    
    // Custom logger with [HG] prefix
    private HGLogger hgLogger;
    
    // Plugin state
    private boolean initialized = false;

    @Override
    public void onEnable() {
        // Initialize custom logger first
        hgLogger = new HGLogger(this);
        
        hgLogger.info("Starting HungerGames plugin...");
        
        try {
            // Initialize core systems
            initializeConfig();
            initializeDatabase();
            initializeManagers();
            initializeEventListeners();
            initializeCommands();
            
            // Set up the world
            initializeWorld();
            
            // Don't start the game automatically - wait for players to join
            hgLogger.info("Plugin initialized. Waiting for players to join before starting game...");
            
            initialized = true;
            hgLogger.info("HungerGames plugin enabled successfully!");
            
        } catch (Exception e) {
            hgLogger.log(Level.SEVERE, "Failed to initialize HungerGames plugin", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    /**
     * Initialize configuration system
     */
    private void initializeConfig() {
        hgLogger.info("Initializing configuration...");
        config = new GameConfig(this);
        hgLogger.info("Configuration initialized");
    }
    
    /**
     * Initialize database connection and schema
     */
    private void initializeDatabase() {
        hgLogger.info("Initializing database...");
        
        databaseManager = new DatabaseManager(
            this,
            config.getDatabaseHost(),
            config.getDatabasePort(),
            config.getDatabaseName(),
            config.getDatabaseUsername(),
            config.getDatabasePassword()
        );
        
        if (!databaseManager.initialize()) {
            throw new RuntimeException("Failed to initialize database");
        }
        
        hgLogger.info("Database initialized successfully");
    }
    
    /**
     * Initialize all manager classes
     */
    private void initializeManagers() {
        hgLogger.info("Initializing managers...");
        
        // Initialize player manager
        playerManager = new PlayerManager(databaseManager, hgLogger.getBukkitLogger());
        
        // Initialize kit manager
        kitManager = KitManager.create();
        
        // Initialize platform generator
        platformGenerator = new PlatformGenerator(config, hgLogger.getBukkitLogger());
        
        // Initialize game manager
        gameManager = GameManager.create(this, config, databaseManager, playerManager, kitManager);
        
        hgLogger.info("All managers initialized");
    }
    
    /**
     * Initialize event listeners
     */
    private void initializeEventListeners() {
        hgLogger.info("Registering event listeners...");
        
        // Register this plugin as a listener for basic events
        Bukkit.getPluginManager().registerEvents(this, this);
        
        // Game manager is already registered in its constructor
        
        hgLogger.info("Event listeners registered");
    }
    
    /**
     * Initialize command handlers
     */
    private void initializeCommands() {
        hgLogger.info("Registering commands...");
        
        // Register commands
        getCommand("credits").setExecutor(new CreditsCommand(this));
        getCommand("credits").setTabCompleter(new CreditsCommand(this));
        KitCommand kitCommand = new KitCommand(this);
        getCommand("kit").setExecutor(kitCommand);
        getCommand("kit").setTabCompleter(kitCommand);
        getCommand("compass").setExecutor(new CompassCommand(this));
        getCommand("compass").setTabCompleter(new CompassCommand(this));
        getCommand("spectate").setExecutor(new SpectateCommand(this));
        getCommand("spectate").setTabCompleter(new SpectateCommand(this));
        getCommand("admin").setExecutor(new AdminCommand(this));
        getCommand("admin").setTabCompleter(new AdminCommand(this));
        
        hgLogger.info("Commands registered");
    }
    
    /**
     * Initialize the world for the game
     */
    private void initializeWorld() {
        hgLogger.info("Setting up world for Hunger Games...");
        
        // Generate spawn platform
        if (Bukkit.getWorlds().size() > 0) {
            platformGenerator.generateSpawnPlatform(Bukkit.getWorlds().get(0).getSpawnLocation());
        }
        
        hgLogger.info("World setup completed");
    }
    
    /**
     * Start the hunger games
     */
    private void startGame() {
        hgLogger.info("Starting Hunger Games match...");
        gameManager.initializeGame();
    }
    
    /**
     * Check if we should start the game and start it if conditions are met
     */
    private void checkAndStartGame() {
        // Only start the game if it hasn't been started yet
        if (!gameManager.isGameRunning()) {
            int onlinePlayers = Bukkit.getOnlinePlayers().size();
            hgLogger.info("Player joined. Online players: " + onlinePlayers);
            
            // Start the game when the first player joins
            if (onlinePlayers >= 1) {
                hgLogger.info("First player joined. Starting Hunger Games match...");
                startGame();
            }
        }
    }
    
    /**
     * Check if we should cancel the game due to insufficient players
     */
    private void checkAndCancelGame() {
        if (gameManager.isGameRunning()) {
            GameState currentState = gameManager.getCurrentState();
            if (currentState.canPlayersJoin()) {
                int onlinePlayers = Bukkit.getOnlinePlayers().size();
                hgLogger.info("Player left. Online players: " + onlinePlayers);
                
                // If we have less than 2 players and the game is still in waiting state, cancel it
                if (onlinePlayers < 2) {
                    hgLogger.info("Not enough players to continue. Cancelling game...");
                    gameManager.cancelGame();
                }
            }
        }
    }

    @Override
    public void onDisable() {
        hgLogger.info("Shutting down HungerGames plugin...");
        
        try {
            // Save all player data
            if (playerManager != null) {
                playerManager.saveAllPlayers().join();
            }
            
            // Close database connections
            if (databaseManager != null) {
                databaseManager.shutdown();
            }
            
            hgLogger.info("HungerGames plugin disabled successfully");
            
        } catch (Exception e) {
            hgLogger.log(Level.SEVERE, "Error during plugin shutdown", e);
        }
    }
    
    /**
     * Handle player join events
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!initialized) {
            return;
        }
        
        try {
            // Load player data
            playerManager.loadPlayer(event.getPlayer()).thenAccept(player -> {
                if (player != null) {
                    hgLogger.info("Loaded player data for " + event.getPlayer().getName() + 
                                   " (Credits: " + player.getCredits() + ")");
                } else {
                    hgLogger.warning("Failed to load player data for " + event.getPlayer().getName());
                }
            });
            
            // Check if we should start the game
            checkAndStartGame();
            
            // Set custom join message - using modern API
            String joinMessage = config.getPrefix() + "&e" + event.getPlayer().getName() + " joined the Hunger Games!";
            event.joinMessage(LegacyComponentSerializer.legacySection().deserialize(joinMessage));
            
            // Update player flight and game mode through protection manager
            if (gameManager != null) {
                gameManager.getProtectionManager().updateAllPlayersFlight();
            }
            
        } catch (Exception e) {
            hgLogger.log(Level.SEVERE, "Error handling player join: " + event.getPlayer().getName(), e);
        }
    }
    
    /**
     * Handle player quit events
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!initialized) {
            return;
        }
        
        try {
            // Handle player leaving during the game
            if (gameManager.getAlivePlayers().contains(event.getPlayer().getUniqueId())) {
                gameManager.handlePlayerDeath(event.getPlayer(), null, "Disconnected");
            }
            
            // Unload player data
            playerManager.unloadPlayer(event.getPlayer().getUniqueId());
            
            // Check if we should cancel the game due to insufficient players
            checkAndCancelGame();
            
            // Set custom quit message - using modern API
            String quitMessage = config.getPrefix() + "&7" + event.getPlayer().getName() + " left the Hunger Games!";
            event.quitMessage(LegacyComponentSerializer.legacySection().deserialize(quitMessage));
            
        } catch (Exception e) {
            hgLogger.log(Level.SEVERE, "Error handling player quit: " + event.getPlayer().getName(), e);
        }
    }
    
    /**
     * Handle player death events to prevent respawning
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!initialized || gameManager == null) {
            return;
        }
        
        Player player = event.getEntity();
        
        // Check if this player is in an active game
        if (gameManager.getAlivePlayers().contains(player.getUniqueId())) {
            // Cancel the death event to prevent respawning
            event.setCancelled(true);
            
            // Handle the death through our game manager
            Player killer = player.getKiller();
            String deathMessage;
            
            // Use the modern deathMessage() method instead of deprecated getDeathMessage()
            if (event.deathMessage() != null) {
                deathMessage = LegacyComponentSerializer.legacySection().serialize(event.deathMessage());
            } else {
                deathMessage = player.getName() + " died";
            }
            
            gameManager.handlePlayerDeath(player, killer, deathMessage);
            
            // Set the player to spectator mode immediately
            player.setGameMode(org.bukkit.GameMode.SPECTATOR);
            
            // Clear inventory and prevent drops
            event.getDrops().clear();
            event.setKeepInventory(false);
            
            hgLogger.info("Prevented respawn for " + player.getName() + " and set to spectator mode");
        }
    }
    
    /**
     * Handle projectile hit events for explosive arrows
     */
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!initialized || gameManager == null) {
            return;
        }
        
        // Check if this is an arrow
        if (!(event.getEntity() instanceof Arrow)) {
            return;
        }
        
        Arrow arrow = (Arrow) event.getEntity();
        
        // Check if the arrow was shot by a player
        if (!(arrow.getShooter() instanceof Player)) {
            return;
        }
        
        Player shooter = (Player) arrow.getShooter();
        
        // Check if the shooter has the Archer Pro kit
        if (kitManager.getPlayerKit(shooter.getUniqueId()) instanceof com.api_d.hungerGames.kits.premium.ArcherProKit) {
            // Create explosion at arrow location
            Location hitLocation = arrow.getLocation();
            
            // Remove the arrow first
            arrow.remove();
            
            // Create explosion that deals damage to players and mobs
            // Parameters: location, power, setFire, breakBlocks, source
            arrow.getWorld().createExplosion(hitLocation, 4f, true, true, shooter);
            
            // Play explosion sound
            arrow.getWorld().playSound(hitLocation, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
            
            // Add explosion particles for visual effect
            arrow.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION, hitLocation, 1);
            
            // Ensure nearby entities take damage from the explosion
            // This is a backup method to ensure damage is applied
            double explosionRadius = 4.0;
            arrow.getWorld().getNearbyEntities(hitLocation, explosionRadius, explosionRadius, explosionRadius)
                .stream()
                .filter(entity -> entity instanceof LivingEntity)
                .filter(entity -> entity != shooter) // Don't damage the shooter
                .map(entity -> (LivingEntity) entity)
                .forEach(entity -> {
                    double distance = entity.getLocation().distance(hitLocation);
                    if (distance <= explosionRadius) {
                        // Calculate damage based on distance (more damage closer to center)
                        double damageMultiplier = 1.0 - (distance / explosionRadius);
                        double damage = Math.max(1.0, 8.0 * damageMultiplier); // 8 damage at center, decreasing with distance
                        
                        // Apply damage
                        entity.damage(damage, shooter);
                        
                        // Apply knockback
                        if (entity instanceof Player) {
                            Player targetPlayer = (Player) entity;
                            if (targetPlayer.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                                // Apply knockback effect
                                org.bukkit.util.Vector knockback = entity.getLocation().toVector()
                                    .subtract(hitLocation.toVector())
                                    .normalize()
                                    .multiply(2.0); // Knockback strength
                                entity.setVelocity(knockback);
                            }
                        }
                    }
                });
            
            hgLogger.info("Explosive arrow hit by " + shooter.getName() + " at " + hitLocation + " - explosion radius: " + explosionRadius);
        }
    }
    
    /**
     * Handle player respawn events to prevent respawning during games
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!initialized || gameManager == null) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check if this player was in an active game and is now dead
        if (gameManager.getDeadPlayers().contains(player.getUniqueId())) {
            // Cancel the respawn and keep them as spectator
            event.setRespawnLocation(player.getLocation());
            
            // Schedule a task to ensure they stay in spectator mode
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (player.isOnline()) {
                    player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                    hgLogger.info("Forced " + player.getName() + " to stay in spectator mode after respawn attempt");
                }
            }, 1L);
        }
    }
    
    /**
     * Handle game mode change events to prevent dead players from changing game mode
     */
    @EventHandler
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        if (!initialized || gameManager == null) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check if this player is dead and trying to change from spectator mode
        if (gameManager.getDeadPlayers().contains(player.getUniqueId()) && 
            event.getNewGameMode() != org.bukkit.GameMode.SPECTATOR) {
            
            // Cancel the game mode change
            event.setCancelled(true);
            
            // Force them back to spectator mode
            player.setGameMode(org.bukkit.GameMode.SPECTATOR);
            
            player.sendMessage("§c§l[Game] §7You cannot change your game mode while dead!");
            hgLogger.info("Prevented " + player.getName() + " from changing game mode while dead");
        }
    }
    
    // Getters for managers (useful for commands and other classes)
    
    public GameConfig getGameConfig() {
        return config;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public PlayerManager getPlayerManager() {
        return playerManager;
    }
    
    public KitManager getKitManager() {
        return kitManager;
    }
    
    public GameManager getGameManager() {
        return gameManager;
    }
    
    public PlatformGenerator getPlatformGenerator() {
        return platformGenerator;
    }
    
    /**
     * Get the plugin instance (singleton pattern)
     */
    public static HungerGames getInstance() {
        return (HungerGames) Bukkit.getPluginManager().getPlugin("HungerGames");
    }
}
