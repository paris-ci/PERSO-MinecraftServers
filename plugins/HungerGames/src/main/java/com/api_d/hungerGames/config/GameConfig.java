package com.api_d.hungerGames.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import com.api_d.hungerGames.util.HGLogger;

import java.util.List;

/**
 * Manages plugin configuration settings
 */
public class GameConfig {
    
    private final Plugin plugin;
    private final FileConfiguration config;
    private final HGLogger logger;
    
    public GameConfig(Plugin plugin) {
        this.plugin = plugin;
        this.logger = new HGLogger(plugin);
        
        // Save default config if it doesn't exist
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
        
        logger.info("Configuration loaded successfully");
    }
    
    // Database settings
    public String getDatabaseHost() {
        return config.getString("database.host", "postgres");
    }
    
    public int getDatabasePort() {
        return config.getInt("database.port", 5432);
    }
    
    public String getDatabaseName() {
        return config.getString("database.database", "minecraft");
    }
    
    public String getDatabaseUsername() {
        return config.getString("database.username", "postgres");
    }
    
    public String getDatabasePassword() {
        return config.getString("database.password", "postgres");
    }
    
    // Server settings
    public String getServerId() {
        return config.getString("server.server_id", "hungergames-1");
    }
    
    // Timing settings
    public int getMaxWaitTime() {
        return config.getInt("timing.max_wait_time", 300);
    }
    
    public int getSpawnTeleportDelay() {
        return config.getInt("timing.spawn_teleport_delay", 10);
    }
    
    public int getPvpDelay() {
        return config.getInt("timing.pvp_delay", 30);
    }
    
    public int getFeastAppearsAfter() {
        return config.getInt("timing.feast_appears_after", 600);
    }
    
    public int getMaxGameTime() {
        return config.getInt("timing.max_game_time", 45);
    }
    
    // World settings
    public int getSpawnRadius() {
        return config.getInt("world.spawn_radius", 15);
    }
    
    public int getWorldBorderInitialSize() {
        return config.getInt("world.world_border_initial_size", 1000);
    }
    
    public int getWorldBorderMinimumSize() {
        return config.getInt("world.world_border_minimum_size", 30);
    }
    
    public double getWorldBorderShrinkSpeed() {
        return config.getDouble("world.world_border_shrink_speed", 0.5);
    }
    
    // Protection settings
    public boolean isForcedDaytimeEnabled() {
        return config.getBoolean("protection.forced_daytime", true);
    }
    
    public boolean isWorldInteractionBlocked() {
        return config.getBoolean("protection.block_world_interaction", true);
    }
    
    public boolean isMobTargetingBlocked() {
        return config.getBoolean("protection.block_mob_targeting", true);
    }
    
    public boolean isFlightEnabledWhenWaiting() {
        return config.getBoolean("protection.enable_flight_waiting", true);
    }
    
    // Feast settings
    public boolean isFeastEnabled() {
        return config.getBoolean("feast.enabled", true);
    }
    
    public int getFeastRadius() {
        return config.getInt("feast.radius", 10);
    }
    
    public int getFeastBorderDistance() {
        return config.getInt("feast.border_distance", 50);
    }
    
    // Party settings
    public int getMaximumPartySize() {
        return config.getInt("party.maximum_party_size", 4);
    }
    
    public boolean isTeamsEnabled() {
        return config.getBoolean("party.teams_enabled", true);
    }
    
    // Platform settings
    public boolean useSchematics() {
        return config.getBoolean("platforms.use_schematics", false);
    }
    
    public String getSpawnSchematicFile() {
        return config.getString("platforms.spawn_schematic_file", "schematics/spawn.schem");
    }
    
    public String getFeastSchematicFile() {
        return config.getString("platforms.feast_schematic_file", "schematics/feast.schem");
    }
    
    // Loot settings
    public List<String> getSpawnItems() {
        return config.getStringList("loot.spawn_items");
    }
    
    public List<String> getFeastItems() {
        return config.getStringList("loot.feast_items");
    }
    
    // Credit settings
    public int getGameStartedCredits() {
        return config.getInt("credits.game_started", 3);
    }
    
    public int getSurvivedOneMinuteCredits() {
        return config.getInt("credits.survived_one_minute", 1);
    }
    
    public int getKillPlayerCredits() {
        return config.getInt("credits.kill_player", 50);
    }
    
    public int getPartyMemberKillCredits() {
        return config.getInt("credits.party_member_kill", 25);
    }
    
    public int getGameWonLargeCredits() {
        return config.getInt("credits.game_won_large", 500);
    }
    
    public int getGameWonSmallCredits() {
        return config.getInt("credits.game_won_small", 100);
    }
    
    public int getAfkPenalty() {
        return config.getInt("credits.afk_penalty", -30);
    }
    
    public int getPoisonDamagePenalty() {
        return config.getInt("credits.poison_damage_penalty", -50);
    }
    
    // Kit costs
    public int getKitCost(String kitName) {
        return config.getInt("kit_costs." + kitName.toLowerCase(), 0);
    }
    
    // Messages
    public String getPrefix() {
        return config.getString("messages.prefix", "§8[§cHG§8] ");
    }
    
    public String getMessage(String key) {
        return config.getString("messages." + key, "Message not found: " + key);
    }
    
    public String getMessage(String key, String... replacements) {
        String message = getMessage(key);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }
        return message;
    }
    
    // Debug settings
    public boolean isVerboseLogging() {
        return config.getBoolean("debug.verbose_logging", false);
    }
    
    public boolean shouldLogStateChanges() {
        return config.getBoolean("debug.log_state_changes", true);
    }
    
    public boolean shouldLogDatabaseOperations() {
        return config.getBoolean("debug.log_database_operations", false);
    }
    
    /**
     * Reload the configuration from disk
     */
    public void reload() {
        plugin.reloadConfig();
        logger.info("Configuration reloaded");
    }
}