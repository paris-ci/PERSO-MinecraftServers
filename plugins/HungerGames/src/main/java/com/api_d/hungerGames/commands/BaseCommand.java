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
        try {
            return execute(sender, command, label, args);
        } catch (Exception e) {
            HGLogger hgLogger = new HGLogger(plugin);
            hgLogger.severe("Error executing command '" + label + "': " + e.getMessage());
            sender.sendMessage(Component.text("An error occurred while executing the command.", NamedTextColor.RED));
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
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
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
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return false;
        }
        return true;
    }
    
    /**
     * Send a formatted message with the plugin prefix
     */
    protected void sendMessage(CommandSender sender, String message) {
        String prefixedMessage = plugin.getGameConfig().getPrefix() + message;
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(prefixedMessage);
        sender.sendMessage(component);
    }
    
    /**
     * Send usage information
     */
    protected void sendUsage(CommandSender sender, String usage) {
        sender.sendMessage(Component.text("Usage: " + usage, NamedTextColor.RED));
    }
}