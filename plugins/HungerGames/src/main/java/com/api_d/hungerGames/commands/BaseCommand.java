package com.api_d.hungerGames.commands;

import com.api_d.hungerGames.HungerGames;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
        try {
            return execute(sender, command, label, args);
        } catch (Exception e) {
            plugin.getLogger().severe("Error executing command '" + label + "': " + e.getMessage());
            sender.sendMessage(ChatColor.RED + "An error occurred while executing the command.");
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
        return sender instanceof Player;
    }
    
    /**
     * Get player from sender with error handling
     */
    protected Player getPlayer(CommandSender sender) {
        if (!isPlayer(sender)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return null;
        }
        return (Player) sender;
    }
    
    /**
     * Check if a player has permission
     */
    protected boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission);
    }
    
    /**
     * Check permission with error message
     */
    protected boolean checkPermission(CommandSender sender, String permission) {
        if (!hasPermission(sender, permission)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return false;
        }
        return true;
    }
    
    /**
     * Send a formatted message with the plugin prefix
     */
    protected void sendMessage(CommandSender sender, String message) {
        String prefixedMessage = plugin.getGameConfig().getPrefix() + message;
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefixedMessage));
    }
    
    /**
     * Send usage information
     */
    protected void sendUsage(CommandSender sender, String usage) {
        sender.sendMessage(ChatColor.RED + "Usage: " + usage);
    }
}