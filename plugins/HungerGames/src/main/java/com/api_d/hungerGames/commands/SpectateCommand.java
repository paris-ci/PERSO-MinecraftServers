package com.api_d.hungerGames.commands;

import com.api_d.hungerGames.HungerGames;
import com.api_d.hungerGames.game.GameManager;
import com.api_d.hungerGames.game.SpectatorManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Command to teleport to another player (spectators only)
 */
public class SpectateCommand extends BaseCommand implements TabCompleter {
    
    public SpectateCommand(HungerGames plugin) {
        super(plugin);
    }
    
    @Override
    protected boolean execute(CommandSender sender, Command command, String label, String[] args) {
        Player player = getPlayer(sender);
        if (player == null) return true;
        
        if (!checkPermission(sender, "hungergames.spectate")) return true;
        
        // Check if game is running
        GameManager gameManager = plugin.getGameManager();
        if (gameManager == null || !gameManager.isGameRunning()) {
            player.sendMessage("§cNo game is currently running!");
            return true;
        }
        
        // Check if player is a spectator
        SpectatorManager spectatorManager = gameManager.getSpectatorManager();
        if (spectatorManager == null || !spectatorManager.isSpectator(player.getUniqueId())) {
            player.sendMessage("§cYou must be a spectator to use this command!");
            return true;
        }
        
        if (args.length < 1) {
            showUsage(player);
            return true;
        }
        
        String targetName = args[0];
        Player targetPlayer = Bukkit.getPlayer(targetName);
        
        if (targetPlayer == null) {
            player.sendMessage("§cPlayer not found: §e" + targetName);
            return true;
        }
        
        // Check if target is alive
        if (targetPlayer.isDead() || !gameManager.getAlivePlayers().contains(targetPlayer.getUniqueId())) {
            player.sendMessage("§cCannot spectate dead players!");
            return true;
        }
        
        // Teleport to the target player
        player.teleport(targetPlayer.getLocation());
        player.sendMessage("§aTeleported to §e" + targetPlayer.getName());
        
        return true;
    }
    
    private void showUsage(Player player) {
        player.sendMessage("§6Spectate Command Usage:");
        player.sendMessage("§7/spectate <player>");
        player.sendMessage("§7Teleport to another player's location");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Tab complete player names (only alive players)
            String partial = args[0].toLowerCase();
            GameManager gameManager = plugin.getGameManager();
            
            if (gameManager != null && gameManager.isGameRunning()) {
                List<String> alivePlayerNames = gameManager.getAlivePlayers().stream()
                    .map(uuid -> Bukkit.getPlayer(uuid))
                    .filter(Objects::nonNull)
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
                completions.addAll(alivePlayerNames);
            }
        }
        
        return completions;
    }
}
