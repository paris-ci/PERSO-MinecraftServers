package com.api_d.hungerGames.commands;

import com.api_d.hungerGames.HungerGames;
import com.api_d.hungerGames.events.KitSelectionEvent;
import com.api_d.hungerGames.gui.KitSelectionGUI;
import com.api_d.hungerGames.kits.Kit;
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
 * Command for managing kit selection
 */
public class KitCommand extends BaseCommand implements TabCompleter {
    
    private final KitSelectionGUI kitSelectionGUI;
    
    public KitCommand(HungerGames plugin) {
        super(plugin);
        this.kitSelectionGUI = new KitSelectionGUI(plugin);
        this.kitSelectionGUI.initialize();
    }
    
    @Override
    protected boolean execute(CommandSender sender, Command command, String label, String[] args) {
        Player player = getPlayer(sender);
        if (player == null) return true;
        
        if (!checkPermission(sender, "hungergames.kit")) return true;
        
        if (args.length == 0) {
            // Open the kit selection GUI
            kitSelectionGUI.openKitSelection(player);
            return true;
        }
        
        if (args.length == 1) {
            // Select a kit
            if (!checkPermission(sender, "hungergames.kit.select")) return true;
            
            String kitId = args[0].toLowerCase();
            Kit kit = plugin.getKitManager().getKit(kitId);
            
            if (kit == null) {
                sender.sendMessage(Component.text("Unknown kit: " + args[0], NamedTextColor.RED));
                return true;
            }
            
            // Check if game allows kit selection
            if (!plugin.getGameManager().getCurrentState().canSelectKits()) {
                sender.sendMessage(Component.text("You cannot select kits at this time!", NamedTextColor.RED));
                return true;
            }
            
            // Check if player can afford the kit
            int playerCredits = plugin.getPlayerManager().getPlayerCredits(player);
            if (!kit.canPlayerUse(player, playerCredits)) {
                sendMessage(sender, plugin.getGameConfig().getMessage("insufficient_credits"));
                sendMessage(sender, "You need " + kit.getCost() + " credits for this kit. You have " + playerCredits + " credits.");
                return true;
            }
            
            // Check if player should pay for the kit
            if (kit.shouldPlayerPay(player)) {
                // Player needs to pay
                if (playerCredits < kit.getCost()) {
                    sendMessage(sender, "§cInsufficient credits! You need " + kit.getCost() + " credits for this kit.");
                    return true;
                }
            } else {
                // Admin bypass - no payment needed
                sendMessage(sender, "§a§l[Admin] §7Kit cost bypassed due to admin permissions!");
            }
            
            // Fire kit selection event
            KitSelectionEvent event = new KitSelectionEvent(player, kitId);
            Bukkit.getPluginManager().callEvent(event);
            
            if (event.isCancelled()) {
                String reason = event.getCancelReason();
                if (reason != null) {
                    sender.sendMessage(Component.text(reason, NamedTextColor.RED));
                }
                return true;
            }
            
            // Deduct credits if premium kit and player should pay
            if (kit.isPremium() && kit.shouldPlayerPay(player)) {
                plugin.getPlayerManager().deductCredits(player.getUniqueId(), kit.getCost(), "Purchased " + kit.getDisplayName() + " kit");
            }
            
            // Select the kit
            plugin.getKitManager().setPlayerKit(player, kitId);
            
            // Show success message with admin bypass info if applicable
            if (kit.isPremium() && !kit.shouldPlayerPay(player)) {
                sendMessage(sender, "§a§l[Admin Bypass] §7Kit " + kit.getDisplayName() + " selected for FREE!");
            } else {
                sendMessage(sender, plugin.getGameConfig().getMessage("kit_selected", "kit", kit.getDisplayName()));
            }
            
            return true;
        }
        
        sendUsage(sender, "/kit [kit_name]");
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Tab complete kit names
            String partial = args[0].toLowerCase();
            List<String> kitNames = plugin.getKitManager().getAllKits().stream()
                .map(Kit::getId)
                .filter(id -> id.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
            completions.addAll(kitNames);
        }
        
        return completions;
    }
    

}