package com.api_d.hungerGames.commands;

import com.api_d.hungerGames.HungerGames;
import com.api_d.hungerGames.events.KitSelectionEvent;
import com.api_d.hungerGames.kits.Kit;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Command for managing kit selection
 */
public class KitCommand extends BaseCommand {
    
    public KitCommand(HungerGames plugin) {
        super(plugin);
    }
    
    @Override
    protected boolean execute(CommandSender sender, Command command, String label, String[] args) {
        Player player = getPlayer(sender);
        if (player == null) return true;
        
        if (!checkPermission(sender, "hungergames.kit")) return true;
        
        if (args.length == 0) {
            // Show current kit and available kits
            showKitInformation(player);
            return true;
        }
        
        if (args.length == 1) {
            // Select a kit
            if (!checkPermission(sender, "hungergames.kit.select")) return true;
            
            String kitId = args[0].toLowerCase();
            Kit kit = plugin.getKitManager().getKit(kitId);
            
            if (kit == null) {
                sender.sendMessage(Component.text("Unknown kit: " + args[0], NamedTextColor.RED));
                showAvailableKits(player);
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
            
            // Deduct credits if premium kit
            if (kit.isPremium()) {
                plugin.getPlayerManager().deductCredits(player.getUniqueId(), kit.getCost(), "Purchased " + kit.getDisplayName() + " kit");
            }
            
            // Select the kit
            plugin.getKitManager().setPlayerKit(player, kitId);
            
            sendMessage(sender, plugin.getGameConfig().getMessage("kit_selected", "kit", kit.getDisplayName()));
            
            return true;
        }
        
        sendUsage(sender, "/kit [kit_name]");
        return true;
    }
    
    /**
     * Show current kit and kit information
     */
    private void showKitInformation(Player player) {
        Kit currentKit = plugin.getKitManager().getPlayerKit(player);
        int playerCredits = plugin.getPlayerManager().getPlayerCredits(player);
        
        sendMessage(player, "&eYour current kit: &a" + currentKit.getDisplayName());
        sendMessage(player, "&eYour credits: &a" + playerCredits);
        sendMessage(player, "");
        sendMessage(player, "&eAvailable kits:");
        
        showAvailableKits(player);
    }
    
    /**
     * Show list of available kits
     */
    private void showAvailableKits(Player player) {
        int playerCredits = plugin.getPlayerManager().getPlayerCredits(player);
        
        sendMessage(player, "&6Default Kits (Free):");
        for (Kit kit : plugin.getKitManager().getDefaultKits()) {
            sendMessage(player, "  &a" + kit.getId() + " &7- " + kit.getDescription());
        }
        
        sendMessage(player, "");
        sendMessage(player, "&6Premium Kits:");
        for (Kit kit : plugin.getKitManager().getPremiumKits()) {
            boolean canAfford = kit.canPlayerUse(player, playerCredits);
            NamedTextColor color = canAfford ? NamedTextColor.GREEN : NamedTextColor.RED;
            
            sendMessage(player, "  " + "&" + (canAfford ? "a" : "c") + kit.getId() + " &7(" + kit.getCost() + " credits) - " + kit.getDescription());
        }
        
        sendMessage(player, "");
        sendMessage(player, "&eUse &a/kit <kit_name> &eto select a kit!");
    }
}