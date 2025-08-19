package com.api_d.hungerGames.commands;

import com.api_d.hungerGames.HungerGames;
import com.api_d.hungerGames.util.HGLogger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Base class for all HungerGames commands
 */
public abstract class BaseCommand implements CommandExecutor {
    
    protected final HungerGames plugin;
    
    public BaseCommand(HungerGames plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        HGLogger hgLogger = new HGLogger(plugin);
        
        try {
            // Debug logging for command execution
            hgLogger.enter("onCommand", "label", label, "args", java.util.Arrays.toString(args));
            hgLogger.info("Command execution started - Command: '" + label + "', Sender: " + 
                         (sender != null ? sender.getName() : "NULL") + 
                         ", Args: " + (args != null ? java.util.Arrays.toString(args) : "NULL"));
            
            // Log sender details
            if (sender != null) {
                hgLogger.info("Sender type: " + sender.getClass().getSimpleName());
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    hgLogger.info("Player details - Name: " + player.getName() + 
                                ", UUID: " + player.getUniqueId() + 
                                ", Online: " + player.isOnline());
                }
            } else {
                hgLogger.warning("Sender is NULL!");
            }
            
            boolean result = execute(sender, command, label, args);
            hgLogger.info("Command execution completed successfully - Result: " + result);
            hgLogger.exit("onCommand", result);
            return result;
            
        } catch (Exception e) {
            hgLogger.error("Error executing command '" + label + "'", e);
            
            // Additional debug info
            hgLogger.severe("Command details - Label: '" + label + "', Args: " + 
                          (args != null ? java.util.Arrays.toString(args) : "NULL"));
            hgLogger.severe("Sender info - Type: " + (sender != null ? sender.getClass().getSimpleName() : "NULL") + 
                          ", Name: " + (sender != null ? sender.getName() : "NULL"));
            
            if (sender instanceof Player) {
                Player player = (Player) sender;
                hgLogger.severe("Player state - Online: " + player.isOnline() + 
                              ", Valid: " + player.isValid() + 
                              ", Dead: " + player.isDead());
            }
            
            sender.sendMessage(Component.text("An error occurred while executing the command.", NamedTextColor.RED));
            hgLogger.exit("onCommand", "ERROR");
            return true;
        }
    }
    
    /**
     * Execute the command logic
     */
    protected abstract boolean execute(CommandSender sender, Command command, String label, String[] args);
    
    /**
     * Check if the sender is a player
     */
    protected boolean isPlayer(CommandSender sender) {
        boolean result = sender instanceof Player;
        HGLogger hgLogger = new HGLogger(plugin);
        hgLogger.fine("isPlayer check - Sender: " + (sender != null ? sender.getClass().getSimpleName() : "NULL") + 
                     ", Result: " + result);
        return result;
    }
    
    /**
     * Get player from sender with error handling
     */
    protected Player getPlayer(CommandSender sender) {
        HGLogger hgLogger = new HGLogger(plugin);
        hgLogger.fine("getPlayer called - Sender: " + (sender != null ? sender.getClass().getSimpleName() : "NULL"));
        
        if (!isPlayer(sender)) {
            hgLogger.warning("getPlayer failed - Sender is not a Player");
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return null;
        }
        
        Player player = (Player) sender;
        hgLogger.fine("getPlayer success - Player: " + player.getName() + ", UUID: " + player.getUniqueId());
        return player;
    }
    
    /**
     * Check if a player has permission
     */
    protected boolean hasPermission(CommandSender sender, String permission) {
        HGLogger hgLogger = new HGLogger(plugin);
        hgLogger.fine("hasPermission check - Sender: " + (sender != null ? sender.getName() : "NULL") + 
                     ", Permission: " + permission);
        
        if (sender == null) {
            hgLogger.severe("hasPermission called with NULL sender!");
            return false;
        }
        
        boolean result = sender.hasPermission(permission);
        hgLogger.fine("hasPermission result: " + result);
        return result;
    }
    
    /**
     * Check permission with error message
     */
    protected boolean checkPermission(CommandSender sender, String permission) {
        HGLogger hgLogger = new HGLogger(plugin);
        hgLogger.fine("checkPermission called - Sender: " + (sender != null ? sender.getName() : "NULL") + 
                     ", Permission: " + permission);
        
        if (sender == null) {
            hgLogger.severe("checkPermission called with NULL sender!");
            return false;
        }
        
        boolean result = hasPermission(sender, permission);
        hgLogger.fine("checkPermission result: " + result);
        
        if (!result) {
            hgLogger.info("Permission denied - Sender: " + sender.getName() + ", Permission: " + permission);
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
        }
        
        return result;
    }
    
    /**
     * Send a formatted message with the plugin prefix
     */
    protected void sendMessage(CommandSender sender, String message) {
        String prefixedMessage = plugin.getGameConfig().getPrefix() + message;
        Component component = LegacyComponentSerializer.legacySection().deserialize(prefixedMessage);
        sender.sendMessage(component);
    }
    
    /**
     * Send usage information
     */
    protected void sendUsage(CommandSender sender, String usage) {
        sender.sendMessage(Component.text("Usage: " + usage, NamedTextColor.RED));
    }
}