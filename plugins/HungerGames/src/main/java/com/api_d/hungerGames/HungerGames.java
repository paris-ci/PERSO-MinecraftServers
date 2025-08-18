package com.api_d.hungerGames;

import com.api_d.hungerGames.commands.CreditsCommand;
import com.api_d.hungerGames.commands.KitCommand;
import com.api_d.hungerGames.config.GameConfig;
import com.api_d.hungerGames.database.DatabaseManager;
import com.api_d.hungerGames.game.GameManager;
import com.api_d.hungerGames.kits.KitManager;
import com.api_d.hungerGames.player.PlayerManager;
import com.api_d.hungerGames.world.PlatformGenerator;
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
    
    // Plugin state
    private boolean initialized = false;

    @Override
    public void onEnable() {
        getLogger().info("Starting HungerGames plugin...");
        
        try {
            // Initialize core systems
            initializeConfig();
            initializeDatabase();
            initializeManagers();
            initializeEventListeners();
            initializeCommands();
            
            // Set up the world and start the game
            initializeWorld();
            
            // Start the game
            startGame();
            
            initialized = true;
            getLogger().info("HungerGames plugin enabled successfully!");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize HungerGames plugin", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    /**
     * Initialize configuration system
     */
    private void initializeConfig() {
        getLogger().info("Initializing configuration...");
        config = new GameConfig(this);
        getLogger().info("Configuration initialized");
    }
    
    /**
     * Initialize database connection and schema
     */
    private void initializeDatabase() {
        getLogger().info("Initializing database...");
        
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
        
        getLogger().info("Database initialized successfully");
    }
    
    /**
     * Initialize all manager classes
     */
    private void initializeManagers() {
        getLogger().info("Initializing managers...");
        
        // Initialize player manager
        playerManager = new PlayerManager(databaseManager, getLogger());
        
        // Initialize kit manager
        kitManager = new KitManager();
        
        // Initialize platform generator
        platformGenerator = new PlatformGenerator(config, getLogger());
        
        // Initialize game manager
        gameManager = new GameManager(this, config, databaseManager, playerManager, kitManager);
        
        getLogger().info("All managers initialized");
    }
    
    /**
     * Initialize event listeners
     */
    private void initializeEventListeners() {
        getLogger().info("Registering event listeners...");
        
        // Register this plugin as a listener for basic events
        Bukkit.getPluginManager().registerEvents(this, this);
        
        // Game manager is already registered in its constructor
        
        getLogger().info("Event listeners registered");
    }
    
    /**
     * Initialize command handlers
     */
    private void initializeCommands() {
        getLogger().info("Registering commands...");
        
        // Register commands
        getCommand("credits").setExecutor(new CreditsCommand(this));
        getCommand("kit").setExecutor(new KitCommand(this));
        
        getLogger().info("Commands registered");
    }
    
    /**
     * Initialize the world for the game
     */
    private void initializeWorld() {
        getLogger().info("Setting up world for Hunger Games...");
        
        // Generate spawn platform
        if (Bukkit.getWorlds().size() > 0) {
            platformGenerator.generateSpawnPlatform(Bukkit.getWorlds().get(0).getSpawnLocation());
        }
        
        getLogger().info("World setup completed");
    }
    
    /**
     * Start the hunger games
     */
    private void startGame() {
        getLogger().info("Starting Hunger Games match...");
        gameManager.initializeGame();
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down HungerGames plugin...");
        
        try {
            // Save all player data
            if (playerManager != null) {
                playerManager.saveAllPlayers().join();
            }
            
            // Close database connections
            if (databaseManager != null) {
                databaseManager.shutdown();
            }
            
            getLogger().info("HungerGames plugin disabled successfully");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during plugin shutdown", e);
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
                    getLogger().info("Loaded player data for " + event.getPlayer().getName() + 
                                   " (Credits: " + player.getCredits() + ")");
                } else {
                    getLogger().warning("Failed to load player data for " + event.getPlayer().getName());
                }
            });
            
            // Set custom join message
            event.setJoinMessage(config.getPrefix() + "§e" + event.getPlayer().getName() + " joined the Hunger Games!");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error handling player join: " + event.getPlayer().getName(), e);
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
            
            // Set custom quit message
            event.setQuitMessage(config.getPrefix() + "§7" + event.getPlayer().getName() + " left the Hunger Games!");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error handling player quit: " + event.getPlayer().getName(), e);
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
