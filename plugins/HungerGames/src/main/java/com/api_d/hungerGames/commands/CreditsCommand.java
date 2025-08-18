package com.api_d.hungerGames.commands;

import com.api_d.hungerGames.HungerGames;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command for managing player credits
 */
public class CreditsCommand extends BaseCommand {
    
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
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
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
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                return true;
            }
            
            try {
                int amount = Integer.parseInt(args[2]);
                
                plugin.getPlayerManager().awardCredits(target.getUniqueId(), amount, "Admin award from " + sender.getName());
                
                sendMessage(sender, "Gave " + amount + " credits to " + target.getName());
                sendMessage(target, "You received " + amount + " credits from " + sender.getName() + "!");
                
                return true;
                
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[2]);
                return true;
            }
        }
        
        // Show usage
        sendUsage(sender, "/credits [player] | /credits give <player> <amount>");
        return true;
    }
}