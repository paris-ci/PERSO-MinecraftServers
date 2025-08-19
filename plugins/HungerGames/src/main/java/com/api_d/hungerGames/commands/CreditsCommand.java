package com.api_d.hungerGames.commands;

import com.api_d.hungerGames.HungerGames;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command for managing player credits
 */
public class CreditsCommand extends BaseCommand implements TabCompleter {
    
    public CreditsCommand(HungerGames plugin) {
        super(plugin);
    }
    
    @Override
    protected boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Show own credits
            Player player = getPlayer(sender);
            if (player == null) return true;
            
            if (!checkPermission(sender, "hungergames.credits")) return true;
            
            int credits = plugin.getPlayerManager().getPlayerCredits(player);
            sendMessage(sender, plugin.getGameConfig().getMessage("credits_balance", "credits", String.valueOf(credits)));
            return true;
        }
        
        if (args.length == 1) {
            // Show another player's credits
            if (!checkPermission(sender, "hungergames.credits.other")) return true;
            
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + args[0], NamedTextColor.RED));
                return true;
            }
            
            int credits = plugin.getPlayerManager().getPlayerCredits(target);
            sendMessage(sender, target.getName() + " has " + credits + " credits.");
            return true;
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            // Give credits to a player
            if (!checkPermission(sender, "hungergames.credits.give")) return true;
            
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
                return true;
            }
            
            try {
                int amount = Integer.parseInt(args[2]);
                
                plugin.getPlayerManager().awardCredits(target.getUniqueId(), amount, "Admin award from " + sender.getName());
                
                sendMessage(sender, "Gave " + amount + " credits to " + target.getName());
                sendMessage(target, "You received " + amount + " credits from " + sender.getName() + "!");
                
                return true;
                
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid amount: " + args[2], NamedTextColor.RED));
                return true;
            }
        }
        
        // Show usage
        sendUsage(sender, "/credits [player] | /credits give <player> <amount>");
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Tab complete: player names or "give"
            String partial = args[0].toLowerCase();
            if ("give".startsWith(partial)) {
                completions.add("give");
            }
            
            // Add online player names
            List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
            completions.addAll(playerNames);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            // Tab complete player names for give command
            String partial = args[1].toLowerCase();
            List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
            completions.addAll(playerNames);
        }
        
        return completions;
    }
}