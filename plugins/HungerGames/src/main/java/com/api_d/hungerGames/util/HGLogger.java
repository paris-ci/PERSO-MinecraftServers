package com.api_d.hungerGames.util;

import org.bukkit.plugin.Plugin;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Custom logger wrapper that automatically prefixes all messages with [HG]
 */
public class HGLogger {
    private final Logger logger;
    private static final String PREFIX = "[HG] ";
    
    public HGLogger(Plugin plugin) {
        this.logger = plugin.getLogger();
    }
    
    public HGLogger(Logger logger) {
        this.logger = logger;
    }
    
    public void info(String message) {
        logger.info(PREFIX + message);
    }
    
    public void warning(String message) {
        logger.warning(PREFIX + message);
    }
    
    public void severe(String message) {
        logger.severe(PREFIX + message);
    }
    
    public void fine(String message) {
        logger.fine(PREFIX + message);
    }
    
    public void finer(String message) {
        logger.finer(PREFIX + message);
    }
    
    public void finest(String message) {
        logger.finest(PREFIX + message);
    }
    
    public void log(Level level, String message) {
        logger.log(level, PREFIX + message);
    }
    
    public void log(Level level, String message, Throwable thrown) {
        logger.log(level, PREFIX + message, thrown);
    }
    
    public void log(Level level, String message, Object... params) {
        logger.log(level, PREFIX + message, params);
    }
    
    /**
     * Get the underlying Bukkit logger (for cases where direct access is needed)
     */
    public Logger getBukkitLogger() {
        return logger;
    }
}
