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
     * Enhanced error logging with stack trace
     */
    public void error(String message, Throwable throwable) {
        logger.severe(PREFIX + "ERROR: " + message);
        if (throwable != null) {
            logger.severe(PREFIX + "Exception: " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
            logger.severe(PREFIX + "Stack trace:");
            for (StackTraceElement element : throwable.getStackTrace()) {
                logger.severe(PREFIX + "  at " + element.toString());
            }
        }
    }
    
    /**
     * Debug logging with current thread and method information
     */
    public void debug(String message) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length > 2) {
            StackTraceElement caller = stackTrace[2];
            String callerInfo = caller.getClassName() + "." + caller.getMethodName() + ":" + caller.getLineNumber();
            logger.fine(PREFIX + "[DEBUG] " + callerInfo + " - " + message);
        } else {
            logger.fine(PREFIX + "[DEBUG] " + message);
        }
    }
    
    /**
     * Log method entry with parameters
     */
    public void enter(String methodName, Object... params) {
        StringBuilder paramStr = new StringBuilder();
        if (params != null && params.length > 0) {
            for (int i = 0; i < params.length; i += 2) {
                if (i + 1 < params.length) {
                    if (paramStr.length() > 0) paramStr.append(", ");
                    paramStr.append(params[i]).append("=").append(params[i + 1]);
                }
            }
        }
        logger.fine(PREFIX + "ENTER: " + methodName + "(" + paramStr.toString() + ")");
    }
    
    /**
     * Log method exit with return value
     */
    public void exit(String methodName, Object returnValue) {
        logger.fine(PREFIX + "EXIT: " + methodName + " -> " + returnValue);
    }
    
    /**
     * Log method exit without return value
     */
    public void exit(String methodName) {
        logger.fine(PREFIX + "EXIT: " + methodName);
    }
    
    /**
     * Get the underlying Bukkit logger (for cases where direct access is needed)
     */
    public Logger getBukkitLogger() {
        return logger;
    }
}
