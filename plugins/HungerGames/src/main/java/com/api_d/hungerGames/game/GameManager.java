package com.api_d.hungerGames.game;

import com.api_d.hungerGames.config.GameConfig;
import com.api_d.hungerGames.database.DatabaseManager;
import com.api_d.hungerGames.database.models.Game;
import com.api_d.hungerGames.database.models.GameParty;
import com.api_d.hungerGames.events.*;
import com.api_d.hungerGames.kits.KitManager;
import com.api_d.hungerGames.player.PlayerManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private final Logger logger;
    
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
        this.logger = plugin.getLogger();
        this.stateMachine = new GameStateMachine(plugin, config.shouldLogStateChanges());
        
        // Register event listeners
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Initialize a new game
     */
    public void initializeGame() {
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
        
        // Set world border
        WorldBorder border = world.getWorldBorder();
        border.setCenter(spawnLocation);
        border.setSize(config.getWorldBorderInitialSize());
        
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
        // Schedule game start if we have enough players or time runs out
        gameStartTask = new BukkitRunnable() {
            int timeLeft = config.getMaxWaitTime();
            
            @Override
            public void run() {
                if (shouldStartGame() || timeLeft <= 0) {
                    startGame();
                    this.cancel();
                    return;
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
        
        // Start if server is full or we have at least 2 players and someone is ready
        return onlinePlayers >= maxPlayers || (onlinePlayers >= 2 && allPlayersReady());
    }
    
    /**
     * Check if all online players have selected kits
     */
    private boolean allPlayersReady() {
        return Bukkit.getOnlinePlayers().stream()
            .allMatch(kitManager::hasPlayerSelectedKit);
    }
    
    /**
     * Start the game
     */
    public void startGame() {
        if (!stateMachine.transitionTo(GameState.STARTING, "Starting game")) {
            return;
        }
        
        logger.info("Starting Hunger Games with " + Bukkit.getOnlinePlayers().size() + " players");
        
        // Cancel waiting task
        if (gameStartTask != null) {
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
            // Add to alive players
            alivePlayers.add(player.getUniqueId());
            
            // Create or assign party
            assignPlayerToParty(player);
            
            // Set survival start time
            playerSurvivalTimes.put(player.getUniqueId(), gameStartTime);
            
            // Teleport to spawn platform
            teleportToSpawn(player);
            
            // Apply kit
            kitManager.applyKitToPlayer(player);
            
            // Set game mode and effects
            player.setGameMode(GameMode.SURVIVAL);
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 10)); // Invincibility
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 128)); // No jumping
            player.setWalkSpeed(0f); // Can't move
            
            // Save kit selection to database
            String kitId = kitManager.getPlayerKit(player).getId();
            playerManager.setPlayerLastKit(player.getUniqueId(), kitId);
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
                    player.sendTitle("§c" + countdown, "§eGet ready to fight!", 0, 20, 0);
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
        if (!stateMachine.transitionTo(GameState.ACTIVE, "Countdown finished")) {
            return;
        }
        
        logger.info("Game is now active!");
        
        // Remove movement restrictions
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.removePotionEffect(PotionEffectType.RESISTANCE);
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
            player.setWalkSpeed(0.2f); // Normal speed
            
            // Give compass
            // TODO: Implement compass tracking system
        }
        
        broadcastMessage("§aThe Hunger Games have begun! Good luck!");
        
        // Schedule PvP enable
        schedulePvpEnable();
        
        // Schedule feast spawn
        scheduleFeastSpawn();
        
        // Start survival credit task
        startSurvivalCredits();
    }
    
    /**
     * Schedule PvP to be enabled
     */
    private void schedulePvpEnable() {
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
        // TODO: Implement feast spawning
        feastSpawned = true;
        stateMachine.transitionTo(GameState.FEAST, "Feast spawned");
        
        logger.info("Feast has spawned!");
        broadcastMessage(config.getMessage("feast_spawned", "x", "0", "z", "0")); // Placeholder coordinates
    }
    
    /**
     * Start awarding survival credits
     */
    private void startSurvivalCredits() {
        survivalTask = new BukkitRunnable() {
            @Override
            public void run() {
                int credits = config.getSurvivedOneMinuteCredits();
                for (UUID playerId : alivePlayers) {
                    playerManager.awardCredits(playerId, credits, "Survived one minute");
                }
            }
        }.runTaskTimer(plugin, 1200, 1200); // Every minute
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
        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage(config.getMessage("spectator_mode"));
        
        // Clear inventory
        player.getInventory().clear();
        
        // Remove potion effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        
        // TODO: Give spectator compass and items
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
    }
    
    /**
     * Broadcast a message to all players
     */
    private void broadcastMessage(String message) {
        String prefixedMessage = config.getPrefix() + message;
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', prefixedMessage));
    }
    
    // Event handlers
    @EventHandler
    public void onGameStateChange(GameStateChangeEvent event) {
        logger.info("Game state changed: " + event.getPreviousState() + " -> " + event.getNewState());
    }
    
    // Getters
    public GameState getCurrentState() {
        return stateMachine.getCurrentState();
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
}