package com.api_d.hungerGames;

import com.api_d.hungerGames.commands.CreditsCommand;
import com.api_d.hungerGames.commands.KitCommand;
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
import org.bukkit.plugin.java.JavaPlugin;

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
        kitManager = new KitManager();
        
        // Initialize platform generator
        platformGenerator = new PlatformGenerator(config, hgLogger.getBukkitLogger());
        
        // Initialize game manager
        gameManager = new GameManager(this, config, databaseManager, playerManager, kitManager);
        
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
        getCommand("kit").setExecutor(new KitCommand(this));
        
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
            
            // Set custom join message
            event.setJoinMessage(config.getPrefix() + "§e" + event.getPlayer().getName() + " joined the Hunger Games!");
            
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
            
            // Set custom quit message
            event.setQuitMessage(config.getPrefix() + "§7" + event.getPlayer().getName() + " left the Hunger Games!");
            
        } catch (Exception e) {
            hgLogger.log(Level.SEVERE, "Error handling player quit: " + event.getPlayer().getName(), e);
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
