package com.api_d.hungerGames.commands;

import com.api_d.hungerGames.HungerGames;
import com.api_d.hungerGames.events.KitSelectionEvent;
import com.api_d.hungerGames.gui.KitSelectionGUI;
import com.api_d.hungerGames.kits.Kit;
import com.api_d.hungerGames.util.HGLogger;
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
    private final HGLogger hgLogger;
    
    public KitCommand(HungerGames plugin) {
        super(plugin);
        this.kitSelectionGUI = new KitSelectionGUI(plugin);
        this.kitSelectionGUI.initialize();
        this.hgLogger = new HGLogger(plugin);
    }
    
    @Override
    protected boolean execute(CommandSender sender, Command command, String label, String[] args) {
        hgLogger.enter("execute", "sender", sender != null ? sender.getName() : "NULL", "args", java.util.Arrays.toString(args));
        hgLogger.info("KitCommand.execute() started - Sender: " + (sender != null ? sender.getName() : "NULL"));
        hgLogger.info("Command args: " + (args != null ? java.util.Arrays.toString(args) : "NULL"));
        
        // Step 1: Get player from sender
        hgLogger.info("Step 1: Getting player from sender...");
        Player player = getPlayer(sender);
        hgLogger.info("Player result: " + (player != null ? player.getName() : "NULL"));
        
        if (player == null) {
            hgLogger.warning("Player is null, returning early");
            hgLogger.exit("execute", false);
            return true;
        }
        
        // Step 2: Check basic permission
        hgLogger.info("Step 2: Checking basic permission 'hungergames.kit'...");
        if (!checkPermission(sender, "hungergames.kit")) {
            hgLogger.warning("Basic permission check failed");
            hgLogger.exit("execute", false);
            return true;
        }
        hgLogger.info("Basic permission check passed");
        
        if (args.length == 0) {
            // Open the kit selection GUI
            hgLogger.info("No args provided, opening kit selection GUI");
            kitSelectionGUI.openKitSelection(player);
            hgLogger.exit("execute", true);
            return true;
        }
        
        if (args.length == 1) {
            hgLogger.info("One arg provided: " + args[0]);
            
            // Select a kit
            hgLogger.info("Step 3: Checking kit selection permission 'hungergames.kit.select'...");
            if (!checkPermission(sender, "hungergames.kit.select")) {
                hgLogger.warning("Kit selection permission check failed");
                hgLogger.exit("execute", false);
                return true;
            }
            hgLogger.info("Kit selection permission check passed");
            
            String kitId = args[0].toLowerCase();
            hgLogger.info("Looking up kit with ID: " + kitId);
            Kit kit = plugin.getKitManager().getKit(kitId);
            
            if (kit == null) {
                hgLogger.warning("Kit not found: " + kitId);
                sender.sendMessage(Component.text("Unknown kit: " + args[0], NamedTextColor.RED));
                hgLogger.exit("execute", false);
                return true;
            }
            hgLogger.info("Kit found: " + kit.getDisplayName());
            
            // Check if game allows kit selection
            hgLogger.info("Step 4: Checking if game allows kit selection...");
            if (!plugin.getGameManager().getCurrentState().canSelectKits()) {
                hgLogger.warning("Game does not allow kit selection at this time");
                sender.sendMessage(Component.text("You cannot select kits at this time!", NamedTextColor.RED));
                hgLogger.exit("execute", false);
                return true;
            }
            hgLogger.info("Game allows kit selection");
            
            // Check if player can afford the kit
            hgLogger.info("Step 5: Getting player credits...");
            int playerCredits = plugin.getPlayerManager().getPlayerCredits(player);
            hgLogger.info("Player credits: " + playerCredits);
            
            // Check if player should pay for the kit and has enough credits
            hgLogger.info("Step 6: Checking kit payment requirements...");
            if (kit.shouldPlayerPay(player)) {
                hgLogger.info("Player needs to pay for kit - Cost: " + kit.getCost());
                // Player needs to pay - check if they have enough credits
                if (playerCredits < kit.getCost()) {
                    hgLogger.warning("Insufficient credits - Required: " + kit.getCost() + ", Available: " + playerCredits);
                    sendMessage(sender, "§cInsufficient credits! You need " + kit.getCost() + " credits for this kit. You have " + playerCredits + " credits.");
                    hgLogger.exit("execute", false);
                    return true;
                }
                hgLogger.info("Player has sufficient credits");
            } else {
                hgLogger.info("Player does not need to pay for kit (admin bypass or free kit)");
            }
            
            // Fire kit selection event
            hgLogger.info("Step 7: Firing kit selection event...");
            KitSelectionEvent event = new KitSelectionEvent(player, kitId);
            Bukkit.getPluginManager().callEvent(event);
            
            if (event.isCancelled()) {
                String reason = event.getCancelReason();
                hgLogger.warning("Kit selection event was cancelled - Reason: " + reason);
                if (reason != null) {
                    sender.sendMessage(Component.text(reason, NamedTextColor.RED));
                }
                hgLogger.exit("execute", false);
                return true;
            }
            hgLogger.info("Kit selection event was not cancelled");
            
            // Deduct credits if premium kit and player should pay
            if (kit.isPremium() && kit.shouldPlayerPay(player)) {
                hgLogger.info("Deducting credits for premium kit - Amount: " + kit.getCost());
                // Handle credits deduction asynchronously
                plugin.getPlayerManager().deductCredits(player.getUniqueId(), kit.getCost(), "Purchased " + kit.getDisplayName() + " kit")
                    .thenAccept(success -> {
                        if (success) {
                            // Credits deducted successfully, select the kit
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                completeKitSelection(player, kitId, sender);
                            });
                        } else {
                            // Failed to deduct credits
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                sendMessage(sender, "§cFailed to purchase kit! Please try again.");
                                hgLogger.warning("Failed to deduct credits for kit purchase");
                            });
                        }
                    })
                    .exceptionally(throwable -> {
                        // Handle any errors
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            sendMessage(sender, "§cAn error occurred while purchasing the kit. Please try again.");
                            hgLogger.error("Error deducting credits for kit purchase", throwable);
                        });
                        return null;
                    });
                return true; // Exit early, kit selection will be handled in the callback
            }
            
            // For free kits or admin bypass, select immediately
            completeKitSelection(player, kitId, sender);
            hgLogger.info("KitCommand.execute() completed successfully");
            hgLogger.exit("execute", true);
            return true;
        }
        
        hgLogger.info("Invalid number of arguments, showing usage");
        sendUsage(sender, "/kit [kit_name]");
        hgLogger.exit("execute", false);
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
    
    /**
     * Complete the kit selection process (called after credits are deducted)
     */
    private void completeKitSelection(Player player, String kitId, CommandSender sender) {
        hgLogger.info("Step 8: Setting player kit...");
        plugin.getKitManager().setPlayerKit(player, kitId);
        hgLogger.info("Kit successfully set for player");
        
        // Show success message with admin bypass info if applicable
        Kit kit = plugin.getKitManager().getKit(kitId);
        if (kit != null && kit.isPremium() && !kit.shouldPlayerPay(player)) {
            hgLogger.info("Showing admin bypass success message");
            sendMessage(sender, "§a§l[Admin Bypass] §7Kit " + kit.getDisplayName() + " selected for FREE!");
        } else {
            hgLogger.info("Showing regular success message");
            sendMessage(sender, plugin.getGameConfig().getMessage("kit_selected", "kit", kit.getDisplayName()));
        }
    }

}