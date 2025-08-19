package com.api_d.hungerGames.game;

import com.api_d.hungerGames.HungerGames;
import com.api_d.hungerGames.config.GameConfig;
import com.api_d.hungerGames.database.DatabaseManager;
import com.api_d.hungerGames.database.models.Game;
import com.api_d.hungerGames.database.models.GameParty;
import com.api_d.hungerGames.events.*;
import com.api_d.hungerGames.kits.KitManager;
import com.api_d.hungerGames.player.PlayerManager;
import com.api_d.hungerGames.world.PlatformGenerator;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import com.api_d.hungerGames.util.HGLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import com.api_d.hungerGames.kits.Kit;

/**
 * Main game manager that coordinates the entire Hunger Games flow
 */
public class GameManager implements Listener {
    
    private final Plugin plugin;
    private final GameConfig config;
    private final DatabaseManager databaseManager;
    private final PlayerManager playerManager;
    private final KitManager kitManager;
    private final GameStateMachine stateMachine;
    private final HGLogger logger;
    
    // Game managers
    private final PlatformGenerator platformGenerator;
    private final CompassTracker compassTracker;
    private final FeastManager feastManager;
    private final BorderManager borderManager;
    private final FinalFightManager finalFightManager;
    private final SpectatorManager spectatorManager;
    private GameProtectionManager protectionManager;
    
    // Game state
    private Game currentGame;
    private final Map<UUID, GameParty> playerParties = new ConcurrentHashMap<>();
    private final Set<UUID> alivePlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> deadPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> playerSurvivalTimes = new ConcurrentHashMap<>();
    
    // Game timing
    private BukkitTask gameStartTask;
    private BukkitTask pvpEnableTask;
    private BukkitTask feastSpawnTask;
    private BukkitTask borderShrinkTask;
    private BukkitTask finalFightTask;
    private BukkitTask survivalTask;
    
    // Game mechanics
    private Location spawnLocation;
    private Location feastLocation;
    private boolean pvpEnabled = false;
    private boolean feastSpawned = false;
    private long gameStartTime;
    
    public GameManager(Plugin plugin, GameConfig config, DatabaseManager databaseManager, 
                      PlayerManager playerManager, KitManager kitManager) {
        this.plugin = plugin;
        this.config = config;
        this.databaseManager = databaseManager;
        this.playerManager = playerManager;
        this.kitManager = kitManager;
        this.logger = new HGLogger(plugin);
        this.stateMachine = new GameStateMachine(config.shouldLogStateChanges());
        
        // Initialize game managers
        this.platformGenerator = new PlatformGenerator(config, plugin.getLogger());
        this.compassTracker = CompassTracker.create(plugin, playerParties);
        this.feastManager = new FeastManager(plugin, config, platformGenerator);
        this.borderManager = new BorderManager(plugin, config);
        this.finalFightManager = new FinalFightManager(plugin, alivePlayers);
        this.spectatorManager = new SpectatorManager((HungerGames) plugin, config, kitManager);
        // Initialize protection manager after construction to avoid this-escape
        this.protectionManager = null;
    }
    
    /**
     * Create and initialize a new GameManager
     */
    public static GameManager create(Plugin plugin, GameConfig config, DatabaseManager databaseManager, 
                                   PlayerManager playerManager, KitManager kitManager) {
        GameManager manager = new GameManager(plugin, config, databaseManager, playerManager, kitManager);
        manager.initializeEventListeners();
        manager.initializeProtectionManager();
        return manager;
    }
    
    /**
     * Initialize event listeners after construction
     */
    private void initializeEventListeners() {
        // Register event listeners
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Initialize protection manager after construction to avoid this-escape
     */
    private void initializeProtectionManager() {
        this.protectionManager = new GameProtectionManager((HungerGames) plugin, config, this);
        this.protectionManager.initialize();
    }
    
    /**
     * Initialize a new game
     */
    public void initializeGame() {
        // Prevent starting multiple games
        if (currentGame != null || stateMachine.getCurrentState() != null) {
            logger.warning("Game is already running. Cannot initialize new game.");
            logger.warning("Current game: " + (currentGame != null ? "ID=" + currentGame.getId() : "null"));
            logger.warning("Current state: " + (stateMachine.getCurrentState() != null ? stateMachine.getCurrentState().getDisplayName() : "null"));
            return;
        }
        
        logger.info("Initializing new Hunger Games match...");
        
        try {
            // Create game record in database
            currentGame = createGameRecord();
            
            // Initialize game state
            alivePlayers.clear();
            deadPlayers.clear();
            playerParties.clear();
            playerSurvivalTimes.clear();
            pvpEnabled = false;
            feastSpawned = false;
            
            // Set up world
            setupWorld();
            
            // Start waiting for players
            stateMachine.transitionTo(GameState.WAITING, "Game initialized");
            startWaitingPhase();
            
            logger.info("Game initialized successfully with ID: " + currentGame.getId());
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize game", e);
        }
    }
    
    /**
     * Create a game record in the database
     */
    private Game createGameRecord() throws SQLException {
        String query = "INSERT INTO games (server_id, waiting_at) VALUES (?, CURRENT_TIMESTAMP) RETURNING id, waiting_at, created_at";
        
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            
            statement.setString(1, config.getServerId());
            ResultSet resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                return new Game(
                    resultSet.getInt("id"),
                    config.getServerId(),
                    resultSet.getTimestamp("waiting_at"),
                    null,
                    null,
                    resultSet.getTimestamp("created_at")
                );
            }
            
            throw new SQLException("Failed to create game record");
        }
    }
    
    /**
     * Set up the world for the game
     */
    private void setupWorld() {
        World world = Bukkit.getWorlds().get(0); // Main world
        spawnLocation = world.getSpawnLocation();
        
        // Generate spawn platform
        platformGenerator.generateSpawnPlatform(spawnLocation);
        
        // Initialize world border
        borderManager.initializeBorder(world, spawnLocation);
        
        // Set compass tracker spawn location
        compassTracker.setSpawnLocation(spawnLocation);
        
        // Clear weather
        world.setStorm(false);
        world.setThundering(false);
        world.setTime(6000); // Set to day
        
        logger.info("World setup completed");
    }
    
    /**
     * Start the waiting phase
     */
    private void startWaitingPhase() {
        logger.info("Starting waiting phase. Max wait time: " + config.getMaxWaitTime() + " seconds");
        
        // Schedule game start if we have enough players or time runs out
        gameStartTask = new BukkitRunnable() {
            int timeLeft = config.getMaxWaitTime();
            int lastLogTime = timeLeft; // Track when we last logged the message
            
            @Override
            public void run() {
                int onlinePlayers = Bukkit.getOnlinePlayers().size();
                boolean shouldStart = shouldStartGame();
                
                if (shouldStart || timeLeft <= 0) {
                    if (timeLeft <= 0) {
                        logger.info("Max wait time reached (" + config.getMaxWaitTime() + "s). Checking if we can start game...");
                        // Even if time runs out, we still need at least 2 players
                        if (onlinePlayers >= 2) {
                            logger.info("Starting game with " + onlinePlayers + " players after max wait time.");
                            startGame();
                            this.cancel();
                            return;
                        } else {
                            logger.info("Max wait time reached but only " + onlinePlayers + " players. Waiting for more players...");
                            timeLeft = 30; // Reset timer to wait another 30 seconds
                        }
                    } else {
                        logger.info("Game start conditions met. Starting game with " + onlinePlayers + " players.");
                        startGame();
                        this.cancel();
                        return;
                    }
                }
                
                // Only log "not enough players" every 30 seconds to reduce spam
                if (onlinePlayers < 2 && (timeLeft % 30 == 0) && timeLeft != lastLogTime) {
                    logger.info("Not enough players to start game. Need at least 2, have " + onlinePlayers);
                    lastLogTime = timeLeft;
                }
                
                if (timeLeft % 30 == 0 || timeLeft <= 10) {
                    broadcastMessage(config.getMessage("game_starting_soon", "seconds", String.valueOf(timeLeft)));
                }
                
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0, 20); // Every second
    }
    
    /**
     * Check if the game should start
     */
    private boolean shouldStartGame() {
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        int maxPlayers = Bukkit.getMaxPlayers();
        
        // Don't start if we have less than 2 players
        if (onlinePlayers < 2) {
            logger.info("Not enough players to start game. Need at least 2, have " + onlinePlayers);
            return false;
        }
        
        // Start if server is full or we have at least 2 players and all are ready
        boolean shouldStart = onlinePlayers >= maxPlayers || allPlayersReady();
        logger.info("Game start check: " + onlinePlayers + " players, max: " + maxPlayers + ", all ready: " + allPlayersReady() + ", should start: " + shouldStart);
        return shouldStart;
    }
    
    /**
     * Check if all online players have selected kits
     */
    private boolean allPlayersReady() {
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        
        // If no players are online, they can't be ready
        if (onlinePlayers == 0) {
            return false;
        }
        
        return Bukkit.getOnlinePlayers().stream()
            .allMatch(kitManager::hasPlayerSelectedKit);
    }
    
    /**
     * Force start the game (admin command)
     */
    public void forceStartGame() {
        if (stateMachine.getCurrentState() != GameState.WAITING) {
            logger.warning("Cannot force start game: not in WAITING state");
            return;
        }
        
        logger.info("Admin force starting the game");
        
        // Cancel the waiting task
        if (gameStartTask != null) {
            gameStartTask.cancel();
        }
        
        // Force start the game by directly calling the setup methods
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        logger.info("Force starting Hunger Games with " + onlinePlayers + " players");
        
        // Update game record
        updateGameStartTime();
        
        // Set up players
        setupPlayersForGame();
        
        // Award starting credits
        awardStartingCredits();
        
        // Start countdown
        startCountdown();
    }
    
    /**
     * Start the game
     */
    public void startGame() {
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        
        // Additional safety check - don't start with less than 2 players
        if (onlinePlayers < 2) {
            logger.warning("Attempted to start game with only " + onlinePlayers + " players. Minimum required: 2. Aborting game start.");
            return;
        }
        
        // Additional check to prevent starting if game is already running
        if (currentGame != null && stateMachine.getCurrentState() != null) {
            logger.warning("Game is already running. Cannot start another game.");
            return;
        }
        
        if (!stateMachine.transitionTo(GameState.STARTING, "Starting game")) {
            return;
        }
        
        logger.info("Starting Hunger Games with " + onlinePlayers + " players");
        
        // Cancel waiting task
        if (gameStartTask != null) {
            gameStartTime = System.currentTimeMillis();
            gameStartTask.cancel();
        }
        
        // Update game record
        updateGameStartTime();
        
        // Set up players
        setupPlayersForGame();
        
        // Award starting credits
        awardStartingCredits();
        
        // Start countdown
        startCountdown();
    }
    
    /**
     * Update game start time in database
     */
    private void updateGameStartTime() {
        try {
            String query = "UPDATE games SET started_at = CURRENT_TIMESTAMP WHERE id = ?";
            databaseManager.execute(query, currentGame.getId());
            currentGame.setStartedAt(new Timestamp(System.currentTimeMillis()));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to update game start time", e);
        }
    }
    
    /**
     * Set up all players for the game
     */
    private void setupPlayersForGame() {
        gameStartTime = System.currentTimeMillis();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Clear inventory completely
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            
            // Reset player health and hunger
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(5.0f);
            
            // Remove all potion effects
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            
            // Add to alive players
            alivePlayers.add(player.getUniqueId());
            
            // Create or assign party
            assignPlayerToParty(player);
            
            // Set survival start time
            playerSurvivalTimes.put(player.getUniqueId(), gameStartTime);
            
            // Teleport to spawn platform
            teleportToSpawn(player);
            
            // Apply kit (this will give them their starting items and effects)
            kitManager.applyKitToPlayer(player);
            
            // Set game mode and effects
            player.setGameMode(GameMode.SURVIVAL);
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 10)); // Invincibility
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 128)); // No jumping
            player.setWalkSpeed(0f); // Can't move
            
            // Save kit selection to database
            String kitId = kitManager.getPlayerKit(player).getId();
            playerManager.setPlayerLastKit(player.getUniqueId(), kitId);
            
            logger.info("Set up player " + player.getName() + " with kit " + kitId);
        }
        
        logger.info("Set up " + alivePlayers.size() + " players for the game");
    }
    
    /**
     * Assign a player to a party
     */
    private void assignPlayerToParty(Player player) {
        try {
            // For now, create individual parties (team mode can be added later)
            String partyName = generatePartyName();
            
            String query = "INSERT INTO game_parties (game_id, name) VALUES (?, ?) RETURNING id";
            
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {
                
                statement.setInt(1, currentGame.getId());
                statement.setString(2, partyName);
                
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    GameParty party = new GameParty(
                        resultSet.getInt("id"),
                        currentGame.getId(),
                        partyName,
                        new Timestamp(System.currentTimeMillis())
                    );
                    
                    playerParties.put(player.getUniqueId(), party);
                    logger.info("Created party '" + partyName + "' for player " + player.getName());
                }
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to assign player to party: " + player.getName(), e);
        }
    }
    
    /**
     * Generate a random party name
     */
    private String generatePartyName() {
        String[] blockNames = {"Stone", "Iron", "Gold", "Diamond", "Emerald", "Lapis", "Redstone", "Coal"};
        String[] mobNames = {"Creepers", "Zombies", "Skeletons", "Spiders", "Endermen", "Chickens", "Cows", "Pigs"};
        
        Random random = new Random();
        String blockName = blockNames[random.nextInt(blockNames.length)];
        String mobName = mobNames[random.nextInt(mobNames.length)];
        
        return "The " + blockName + " " + mobName;
    }
    
    /**
     * Teleport a player to the spawn platform
     */
    private void teleportToSpawn(Player player) {
        // Teleport to a random location on the spawn circle
        double angle = Math.random() * Math.PI * 2;
        double radius = config.getSpawnRadius() - 2; // Inside the platform
        
        double x = spawnLocation.getX() + Math.cos(angle) * radius;
        double z = spawnLocation.getZ() + Math.sin(angle) * radius;
        double y = spawnLocation.getY() + 2; // Above the platform
        
        Location teleportLocation = new Location(spawnLocation.getWorld(), x, y, z);
        player.teleport(teleportLocation);
    }
    
    /**
     * Award starting credits to all players
     */
    private void awardStartingCredits() {
        int credits = config.getGameStartedCredits();
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerManager.awardCredits(player.getUniqueId(), credits, "Game started");
        }
    }
    
    /**
     * Start the pre-game countdown
     */
    private void startCountdown() {
        new BukkitRunnable() {
            int countdown = config.getSpawnTeleportDelay();
            
            @Override
            public void run() {
                if (countdown <= 0) {
                    // Game officially starts
                    startActivePhase();
                    this.cancel();
                    return;
                }
                
                // Display countdown
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.showTitle(Title.title(
                        Component.text("§c" + countdown), 
                        Component.text("§eGet ready to fight!")
                    ));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                }
                
                countdown--;
            }
        }.runTaskTimer(plugin, 0, 20); // Every second
    }
    
    /**
     * Start the active game phase
     */
    private void startActivePhase() {
        // First transition to STARTING state
        if (!stateMachine.transitionTo(GameState.STARTING, "Countdown finished")) {
            logger.warning("Failed to transition to STARTING state");
            return;
        }
        
        logger.info("Game is now starting!");
        
        // Remove movement restrictions
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.removePotionEffect(PotionEffectType.RESISTANCE);
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
            player.setWalkSpeed(0.2f); // Normal speed
            
            // Give compass with tracking system
            compassTracker.giveCompass(player);
            
            // Display player info
            displayPlayerInfo(player);
        }
        
        broadcastMessage("§aThe Hunger Games have begun! Good luck!");
        
        // Now transition to ACTIVE state
        if (!stateMachine.transitionTo(GameState.ACTIVE, "Players released")) {
            logger.warning("Failed to transition to ACTIVE state");
            return;
        }
        
        logger.info("Game is now active!");
        
        // Schedule PvP enable
        schedulePvpEnable();
        
        // Schedule feast spawn
        scheduleFeastSpawn();
        
        // Start survival credit task
        startSurvivalCredits();
    }
    
    /**
     * Display player information and kit details
     */
    private void displayPlayerInfo(Player player) {
        Kit kit = kitManager.getPlayerKit(player);
        String kitName = kit != null ? kit.getId() : "Unknown";
        
        // Send personal message
        player.sendMessage("§6=== Your Hunger Games Stats ===");
        player.sendMessage("§eKit: §a" + kitName);
        player.sendMessage("§eHealth: §a" + (int)player.getHealth() + "§7/§a20");
        player.sendMessage("§eHunger: §a" + player.getFoodLevel() + "§7/§a20");
        player.sendMessage("§eArmor: §a" + getArmorDescription(player));
        player.sendMessage("§eInventory: §a" + getInventoryDescription(player));
        player.sendMessage("§6===============================");
    }
    
    /**
     * Get a description of the player's armor
     */
    private String getArmorDescription(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        int armorPieces = 0;
        for (ItemStack piece : armor) {
            if (piece != null && piece.getType() != Material.AIR) {
                armorPieces++;
            }
        }
        return armorPieces + " pieces equipped";
    }
    
    /**
     * Get a description of the player's inventory
     */
    private String getInventoryDescription(Player player) {
        int items = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                items++;
            }
        }
        return items + " items";
    }
    
    /**
     * Schedule PvP to be enabled
     */
    private void schedulePvpEnable() {
        // Cancel any existing PvP enable task
        if (pvpEnableTask != null) {
            pvpEnableTask.cancel();
        }
        
        pvpEnableTask = new BukkitRunnable() {
            @Override
            public void run() {
                enablePvp();
            }
        }.runTaskLater(plugin, config.getPvpDelay() * 20L);
    }
    
    /**
     * Enable PvP
     */
    private void enablePvp() {
        pvpEnabled = true;
        broadcastMessage(config.getMessage("pvp_enabled"));
        
        // Allow all worlds to have PvP
        for (World world : Bukkit.getWorlds()) {
            world.setPVP(true);
        }
        
        logger.info("PvP has been enabled");
    }
    
    /**
     * Schedule feast spawn
     */
    private void scheduleFeastSpawn() {
        if (!config.isFeastEnabled()) {
            return;
        }
        
        // Cancel any existing feast spawn task
        if (feastSpawnTask != null) {
            feastSpawnTask.cancel();
        }
        
        // Start feast reminders
        feastManager.startFeastReminders();
        
        feastSpawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                spawnFeast();
            }
        }.runTaskLater(plugin, (config.getPvpDelay() + config.getFeastAppearsAfter()) * 20L);
    }
    
    /**
     * Spawn the feast
     */
    private void spawnFeast() {
        World world = Bukkit.getWorlds().get(0);
        feastLocation = feastManager.spawnFeast(world, spawnLocation);
        
        if (feastLocation != null) {
            feastSpawned = true;
            stateMachine.transitionTo(GameState.FEAST, "Feast spawned");
            
            // Update compass tracker with feast location
            compassTracker.setFeastLocation(feastLocation);
            
            // Start border shrinking
            borderManager.startBorderShrinking();
            
            // Start final fight timer
            startFinalFightTimer();
            
            logger.info("Feast spawned successfully at " + feastLocation.toString());
        } else {
            logger.warning("Failed to spawn feast");
        }
    }
    
    /**
     * Check if final fight should start (when border reaches minimum size)
     */
    private void checkFinalFightStart() {
        if (borderManager.getCurrentBorderSize() <= config.getWorldBorderMinimumSize() + 10) {
            // Border is close to minimum, start final fight
            if (!finalFightManager.isFinalFightActive()) {
                finalFightManager.startFinalFight();
            }
        }
    }
    
    /**
     * Start awarding survival credits
     */
    private void startSurvivalCredits() {
        // Cancel any existing survival credit task
        if (survivalTask != null) {
            survivalTask.cancel();
        }
        
        survivalTask = new BukkitRunnable() {
            @Override
            public void run() {
                int credits = config.getSurvivedOneMinuteCredits();
                for (UUID playerId : alivePlayers) {
                    playerManager.awardCredits(playerId, credits, "Survived one minute");
                }
                
                // Update compasses periodically
                compassTracker.updateAllCompasses();
                
                // Check if final fight should start
                checkFinalFightStart();
            }
        }.runTaskTimer(plugin, 1200, 1200); // Every minute
    }
    
    /**
     * Start the final fight timer
     */
    private void startFinalFightTimer() {
        // Cancel any existing final fight task
        if (finalFightTask != null) {
            finalFightTask.cancel();
        }
        
        finalFightTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Start final fight phase with poison effects
                finalFightManager.startFinalFight();
            }
        }.runTaskLater(plugin, config.getMaxGameTime() * 60 * 20L); // Convert minutes to ticks
    }
    
    /**
     * Handle player death
     */
    public void handlePlayerDeath(Player victim, Player killer, String deathMessage) {
        UUID victimId = victim.getUniqueId();
        
        if (!alivePlayers.contains(victimId)) {
            return; // Player already dead
        }
        
        // Move from alive to dead
        alivePlayers.remove(victimId);
        deadPlayers.add(victimId);
        
        logger.info("Player died: " + victim.getName() + " (killer: " + (killer != null ? killer.getName() : "none") + ")");
        
        // Award kill credits
        if (killer != null && alivePlayers.contains(killer.getUniqueId())) {
            playerManager.awardCredits(killer.getUniqueId(), config.getKillPlayerCredits(), "Killed " + victim.getName());
            
            // Award party member credits
            GameParty killerParty = playerParties.get(killer.getUniqueId());
            if (killerParty != null) {
                for (Map.Entry<UUID, GameParty> entry : playerParties.entrySet()) {
                    UUID memberId = entry.getKey();
                    GameParty memberParty = entry.getValue();
                    
                    if (!memberId.equals(killer.getUniqueId()) && 
                        memberParty.getId() == killerParty.getId() && 
                        alivePlayers.contains(memberId)) {
                        playerManager.awardCredits(memberId, config.getPartyMemberKillCredits(), 
                                                 "Party member " + killer.getName() + " killed " + victim.getName());
                    }
                }
            }
        }
        
        // Set player to spectator
        setPlayerAsSpectator(victim);
        
        // Check win condition
        checkWinCondition();
        
        // Fire event
        PlayerDeathEvent event = new PlayerDeathEvent(victim, killer, 
            killer != null ? com.api_d.hungerGames.database.models.GameLog.DeathReason.PLAYER : 
                           com.api_d.hungerGames.database.models.GameLog.DeathReason.ENVIRONMENT,
            deathMessage);
        Bukkit.getPluginManager().callEvent(event);
    }
    
    /**
     * Set a player as spectator
     */
    private void setPlayerAsSpectator(Player player) {
        // Use spectator manager to handle all spectator functionality
        spectatorManager.setPlayerAsSpectator(player);
        
        // Remove potion effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }
    
    /**
     * Check if the game should end (win condition)
     */
    private void checkWinCondition() {
        if (alivePlayers.size() <= 1) {
            endGame();
        }
    }
    
    /**
     * Cancel the current game (e.g., due to insufficient players)
     */
    public void cancelGame() {
        logger.info("Cancelling game due to insufficient players...");
        
        // Cancel all tasks
        cancelAllTasks();
        
        // Reset game state
        if (currentGame != null) {
            try {
                // Mark game as cancelled in database
                String query = "UPDATE games SET ended_at = CURRENT_TIMESTAMP WHERE id = ?";
                databaseManager.execute(query, currentGame.getId());
                currentGame.setEndedAt(new Timestamp(System.currentTimeMillis()));
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to update cancelled game in database", e);
            }
        }
        
        // Reset game state
        currentGame = null;
        alivePlayers.clear();
        deadPlayers.clear();
        playerParties.clear();
        playerSurvivalTimes.clear();
        pvpEnabled = false;
        feastSpawned = false;
        
        // Reset managers
        feastManager.reset();
        borderManager.reset();
        finalFightManager.reset();
        spectatorManager.reset();
        compassTracker.clearAllPlayers(); // Clear all players
        
        // Reset state machine
        stateMachine.reset();
        
        logger.info("Game cancelled successfully");
    }
    
    /**
     * End the game
     */
    public void endGame() {
        if (!stateMachine.transitionTo(GameState.ENDING, "Game ended")) {
            return;
        }
        
        logger.info("Game ending...");
        
        // Cancel all tasks
        cancelAllTasks();
        
        // Award winner credits
        if (alivePlayers.size() == 1) {
            UUID winnerId = alivePlayers.iterator().next();
            Player winner = Bukkit.getPlayer(winnerId);
            
            int winCredits = playerParties.size() >= 4 ? config.getGameWonLargeCredits() : config.getGameWonSmallCredits();
            playerManager.awardCredits(winnerId, winCredits, "Won the game");
            
            if (winner != null) {
                broadcastMessage(config.getMessage("game_ended", "winners", winner.getName()));
            }
        } else {
            broadcastMessage(config.getMessage("game_ended", "winners", "Nobody"));
        }
        
        // Update database
        updateGameEndTime();
        
        // Schedule server shutdown
        new BukkitRunnable() {
            @Override
            public void run() {
                stateMachine.transitionTo(GameState.FINISHED, "Cleanup completed");
                Bukkit.shutdown();
            }
        }.runTaskLater(plugin, 200); // 10 seconds delay
    }
    
    /**
     * Update game end time in database
     */
    private void updateGameEndTime() {
        try {
            String query = "UPDATE games SET ended_at = CURRENT_TIMESTAMP WHERE id = ?";
            databaseManager.execute(query, currentGame.getId());
            currentGame.setEndedAt(new Timestamp(System.currentTimeMillis()));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to update game end time", e);
        }
    }
    
    /**
     * Cancel all running tasks
     */
    private void cancelAllTasks() {
        if (gameStartTask != null) gameStartTask.cancel();
        if (pvpEnableTask != null) pvpEnableTask.cancel();
        if (feastSpawnTask != null) feastSpawnTask.cancel();
        if (borderShrinkTask != null) borderShrinkTask.cancel();
        if (finalFightTask != null) finalFightTask.cancel();
        if (survivalTask != null) survivalTask.cancel();
        
        // Clean up managers
        feastManager.cleanup();
        borderManager.cleanup();
        finalFightManager.cleanup();
        spectatorManager.cleanup();
        protectionManager.cleanup();
    }
    
    /**
     * Broadcast a message to all players
     */
    private void broadcastMessage(String message) {
        String prefixedMessage = config.getPrefix() + message;
        Component component = LegacyComponentSerializer.legacySection().deserialize(prefixedMessage);
        Bukkit.broadcast(component);
    }
    
    // Event handlers
    @EventHandler
    public void onGameStateChange(GameStateChangeEvent event) {
        logger.info("Game state changed: " + event.getPreviousState() + " -> " + event.getNewState());
        
        // Handle specific state transitions
        if (event.getNewState() == GameState.STARTING) {
            // Game is starting, ensure all players are in survival mode
            logger.info("Game starting - transitioning players to survival mode");
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.setGameMode(GameMode.SURVIVAL);
                // Disable flight for all players when game starts
                player.setAllowFlight(false);
                player.setFlying(false);
            }
        } else if (event.getNewState() == GameState.ACTIVE) {
            // Game is now active, disable protection features
            logger.info("Game is now active - disabling protection features");
            protectionManager.updateAllPlayersFlight();
            
            // PvP enable is already scheduled in startActivePhase()
            // No need to schedule it again here
            
            // Feast spawn and survival credit task are already started in startActivePhase()
            // No need to start them again here
            
        } else if (event.getNewState() == GameState.FEAST) {
            // Feast state activated
            logger.info("Feast state activated");
            
            // Update compass tracker with feast location
            if (feastLocation != null) {
                compassTracker.setFeastLocation(feastLocation);
            }
            
            // Start border shrinking after feast
            if (borderManager != null && !borderManager.isBorderShrinking()) {
                borderManager.startBorderShrinking();
            }
            
            // Start final fight timer
            startFinalFightTimer();
            
        } else if (event.getNewState() == GameState.BORDER_SHRINKING) {
            // Border shrinking state activated
            logger.info("Border shrinking state activated");
            
            // Ensure border is actually shrinking
            if (borderManager != null && !borderManager.isBorderShrinking()) {
                borderManager.startBorderShrinking();
            }
            
        } else if (event.getNewState() == GameState.FINAL_FIGHT) {
            // Final fight state activated
            logger.info("Final fight state activated");
            
            // Ensure final fight is actually active
            if (finalFightManager != null && !finalFightManager.isFinalFightActive()) {
                finalFightManager.startFinalFight();
            }
            
        } else if (event.getNewState() == GameState.ENDING) {
            // Game ending state activated
            logger.info("Game ending state activated");
            
            // Cancel all ongoing tasks
            cancelAllTasks();
            
            // Announce winner if there's only one player left
            if (alivePlayers.size() == 1) {
                UUID winnerId = alivePlayers.iterator().next();
                Player winner = Bukkit.getPlayer(winnerId);
                if (winner != null) {
                    broadcastMessage("§6§l" + winner.getName() + " §ahas won the Hunger Games!");
                }
            }
            
        } else if (event.getNewState() == GameState.FINISHED) {
            // Game finished state activated
            logger.info("Game finished state activated");
            
            // Clean up and reset for next game
            cancelAllTasks();
            
        } else if (event.getNewState().isGameActive()) {
            // Game is now active, disable protection features
            logger.info("Game is now active - disabling protection features");
            protectionManager.updateAllPlayersFlight();
        } else if (event.getNewState() == GameState.WAITING) {
            // Game is waiting, enable protection features
            logger.info("Game is waiting - enabling protection features");
            protectionManager.updateAllPlayersFlight();
        } else {
            // For other states, update protection as needed
            protectionManager.updateAllPlayersFlight();
        }
    }
    
    /**
     * Handle player disconnection
     */
    public void handlePlayerDisconnect(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (alivePlayers.contains(playerId)) {
            // Player was alive, handle as death
            handlePlayerDeath(player, null, "Disconnected");
            
            // Drop inventory
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            }
            
            // Penalize credits
            playerManager.awardCredits(playerId, config.getAfkPenalty(), "Disconnected");
        }
        
        // Remove from compass tracker
        compassTracker.removePlayer(playerId);
    }
    
    // Getters
    public GameState getCurrentState() {
        return stateMachine.getCurrentState();
    }
    
    public boolean isGameRunning() {
        return currentGame != null && stateMachine.getCurrentState() != null;
    }
    
    public boolean isPvpEnabled() {
        return pvpEnabled;
    }
    
    public boolean isFeastSpawned() {
        return feastSpawned;
    }
    
    public Set<UUID> getAlivePlayers() {
        return new HashSet<>(alivePlayers);
    }
    
    public Set<UUID> getDeadPlayers() {
        return new HashSet<>(deadPlayers);
    }
    
    public Game getCurrentGame() {
        return currentGame;
    }
    
    /**
     * Get the platform generator
     */
    public PlatformGenerator getPlatformGenerator() {
        return platformGenerator;
    }
    
    /**
     * Get the compass tracker
     */
    public CompassTracker getCompassTracker() {
        return compassTracker;
    }
    
    /**
     * Get the spectator manager
     */
    public SpectatorManager getSpectatorManager() {
        return spectatorManager;
    }

    /**
     * Get the protection manager
     */
    public GameProtectionManager getProtectionManager() {
        return protectionManager;
    }
    
    /**
     * Debug method to show current game state
     */
    public String getDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Game Manager Debug Info:\n");
        info.append("- Current Game: ").append(currentGame != null ? "ID=" + currentGame.getId() : "null").append("\n");
        info.append("- Current State: ").append(stateMachine.getCurrentState() != null ? stateMachine.getCurrentState().getDisplayName() : "null").append("\n");
        info.append("- Alive Players: ").append(alivePlayers.size()).append("\n");
        info.append("- Dead Players: ").append(deadPlayers.size()).append("\n");
        info.append("- PvP Enabled: ").append(pvpEnabled).append("\n");
        info.append("- Feast Spawned: ").append(feastSpawned).append("\n");
        info.append("- Online Players: ").append(Bukkit.getOnlinePlayers().size()).append("\n");
        return info.toString();
    }
    
    // Admin force methods
    
    /**
     * Force transition to a specific game state
     */
    public void forceStateTransition(GameState targetState) {
        if (currentGame == null) {
            logger.warning("Cannot force state transition: no game running");
            return;
        }
        
        logger.info("Admin forcing state transition to: " + targetState.getDisplayName());
        
        // Execute the actual game logic for the target state
        switch (targetState) {
            case FEAST:
                // Force spawn feast and transition to FEAST state
                if (!feastSpawned) {
                    forceSpawnFeast();
                } else {
                    // Feast already spawned, just transition state
                    stateMachine.transitionTo(targetState, "Admin forced transition");
                }
                break;
                
            case BORDER_SHRINKING:
                // Force start border shrinking and transition to BORDER_SHRINKING state
                forceStartBorderShrinking();
                break;
                
            case FINAL_FIGHT:
                // Force start final fight and transition to FINAL_FIGHT state
                forceStartFinalFight();
                break;
                
            case ENDING:
                // Force end the game and transition to ENDING state
                forceEndGame();
                break;
                
            case FINISHED:
                // Force end the game and transition to FINISHED state
                forceEndGame();
                stateMachine.transitionTo(targetState, "Admin forced transition");
                break;
                
            default:
                // For other states, just transition without additional logic
                stateMachine.transitionTo(targetState, "Admin forced transition");
                break;
        }
    }
    
    /**
     * Force enable PvP
     */
    public void forceEnablePvp() {
        if (!pvpEnabled) {
            pvpEnabled = true;
            logger.info("Admin force enabled PvP");
            
            // Cancel any existing PvP task
            if (pvpEnableTask != null) {
                pvpEnableTask.cancel();
            }
            
            // Broadcast PvP enabled
            broadcastMessage("§cPvP is now enabled!");
        }
    }
    
    /**
     * Force spawn feast
     */
    public void forceSpawnFeast() {
        if (!feastSpawned) {
            feastSpawned = true;
            logger.info("Admin force spawned feast");
            
            // Cancel any existing feast task
            if (feastSpawnTask != null) {
                feastSpawnTask.cancel();
            }
            
            // Spawn feast immediately
            World world = Bukkit.getWorlds().get(0);
            feastLocation = feastManager.spawnFeast(world, spawnLocation);
            
            // Update compass tracker with feast location
            if (feastLocation != null) {
                compassTracker.setFeastLocation(feastLocation);
            }
            
            // Broadcast feast spawned
            Location feastLoc = feastManager.getFeastLocation();
            if (feastLoc != null) {
                broadcastMessage("§6The feast has spawned at X: " + feastLoc.getBlockX() + ", Z: " + feastLoc.getBlockZ() + "!");
            } else {
                broadcastMessage("§6The feast has spawned!");
            }
            
            // Transition to FEAST state
            stateMachine.transitionTo(GameState.FEAST, "Admin force spawned feast");
        }
    }
    
    /**
     * Force start border shrinking
     */
    public void forceStartBorderShrinking() {
        logger.info("Admin force started border shrinking");
        
        // Cancel any existing border task
        if (borderShrinkTask != null) {
            borderShrinkTask.cancel();
        }
        
        // Start border shrinking regardless of current state
        if (borderManager != null) {
            borderManager.startBorderShrinking();
        }
        
        // Transition to border shrinking state
        stateMachine.transitionTo(GameState.BORDER_SHRINKING, "Admin force started border shrinking");
    }
    
    /**
     * Force start final fight
     */
    public void forceStartFinalFight() {
        logger.info("Admin force started final fight");
        
        // Cancel any existing final fight task
        if (finalFightTask != null) {
            finalFightTask.cancel();
        }
        
        // Start final fight regardless of current state
        if (finalFightManager != null) {
            finalFightManager.startFinalFight();
        }
        
        // Transition to final fight state
        stateMachine.transitionTo(GameState.FINAL_FIGHT, "Admin force started final fight");
    }
    
    /**
     * Force end the game
     */
    public void forceEndGame() {
        if (currentGame != null) {
            logger.info("Admin force ending game");
            
            // Cancel all tasks
            cancelAllTasks();
            
            // Transition to ending state
            stateMachine.transitionTo(GameState.ENDING, "Admin force ended");
        }
    }
}