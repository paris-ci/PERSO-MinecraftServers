package com.api_d.hungerGames.commands;

import com.api_d.hungerGames.HungerGames;
import com.api_d.hungerGames.game.CompassTracker;
import com.api_d.hungerGames.game.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Command to change compass tracking mode
 */
public class CompassCommand extends BaseCommand implements TabCompleter {
    
    public CompassCommand(HungerGames plugin) {
        super(plugin);
    }
    
    @Override
    protected boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check if player has permission
        if (!player.hasPermission("hungergames.compass.select")) {
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }
        
        // Check if game is running
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null || !gameManager.isGameRunning()) {
            player.sendMessage("§cNo game is currently running!");
            return true;
        }
        
        // Check if player is alive
        if (!gameManager.getAlivePlayers().contains(player.getUniqueId())) {
            player.sendMessage("§cYou must be alive to change compass tracking mode!");
            return true;
        }
        
        if (args.length < 1) {
            showUsage(player);
            return true;
        }
        
        String mode = args[0].toLowerCase();
        
        // Get compass tracker and change mode
        CompassTracker compassTracker = gameManager.getCompassTracker();
        if (compassTracker != null) {
            boolean success = compassTracker.changeTrackingMode(player, mode);
            if (success) {
                player.sendMessage("§aCompass tracking mode changed to: §e" + mode);
            } else {
                player.sendMessage("§cInvalid tracking mode: §e" + mode);
                showAvailableModes(player);
            }
        } else {
            player.sendMessage("§cCompass tracker not available!");
        }
        
        return true;
    }
    
    private void showUsage(Player player) {
        player.sendMessage("§6Compass Command Usage:");
        player.sendMessage("§7/compass <mode>");
        player.sendMessage("§7Available modes:");
        showAvailableModes(player);
    }
    
    private void showAvailableModes(Player player) {
        player.sendMessage("§8- §7spawn §8- Track spawn location");
        player.sendMessage("§8- §7feast §8- Track feast location (if spawned)");
        player.sendMessage("§8- §7party §8- Track closest party member");
        player.sendMessage("§8- §7enemy §8- Track closest enemy");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Tab complete compass tracking modes
            String partial = args[0].toLowerCase();
            List<String> modes = Arrays.asList("spawn", "feast", "party", "enemy");
            
            for (String mode : modes) {
                if (mode.toLowerCase().startsWith(partial)) {
                    completions.add(mode);
                }
            }
        }
        
        return completions;
    }
}
