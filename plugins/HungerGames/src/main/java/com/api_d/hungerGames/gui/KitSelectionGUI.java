package com.api_d.hungerGames.gui;

import com.api_d.hungerGames.HungerGames;
import com.api_d.hungerGames.events.KitSelectionEvent;
import com.api_d.hungerGames.kits.Kit;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.util.*;

/**
 * GUI for kit selection
 */
public class KitSelectionGUI implements Listener {
    
    private final HungerGames plugin;
    private final Map<UUID, Inventory> openInventories;
    private final Map<UUID, Kit> pendingConfirmations;
    
    public KitSelectionGUI(HungerGames plugin) {
        this.plugin = plugin;
        this.openInventories = new HashMap<>();
        this.pendingConfirmations = new HashMap<>();
        // Event registration moved to separate method to avoid this-escape
    }
    
    /**
     * Initialize event listeners after construction to avoid this-escape
     */
    public void initialize() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Open the kit selection GUI for a player
     */
    public void openKitSelection(Player player) {
        // Check if game allows kit selection
        if (!plugin.getGameManager().getCurrentState().canSelectKits()) {
            player.sendMessage(Component.text("You cannot select kits at this time!", NamedTextColor.RED));
            return;
        }
        
        // Create inventory
        Inventory inventory = createKitInventory(player);
        openInventories.put(player.getUniqueId(), inventory);
        
        // Open the inventory
        player.openInventory(inventory);
    }
    
    /**
     * Create the kit selection inventory
     */
    private Inventory createKitInventory(Player player) {
        Collection<Kit> allKits = plugin.getKitManager().getAllKits();
        int size = Math.min(54, ((allKits.size() - 1) / 9 + 1) * 9);
        if (size < 18) size = 18; // Minimum size
        
        String title = "§8Kit Selection";
        Inventory inventory = Bukkit.createInventory(null, size, Component.text(title));
        
        // Get current kit and player credits
        Kit currentKit = plugin.getKitManager().getPlayerKit(player);
        int playerCredits = plugin.getPlayerManager().getPlayerCredits(player);
        
        // Fill inventory with kits
        int slot = 0;
        for (Kit kit : allKits) {
            if (slot >= size) break;
            
            ItemStack displayItem = createKitDisplayItem(kit, player, playerCredits, kit.equals(currentKit));
            inventory.setItem(slot, displayItem);
            slot++;
        }
        
        // Add info item at the bottom
        if (size >= 9) {
            ItemStack infoItem = createInfoItem(playerCredits);
            inventory.setItem(size - 5, infoItem);
        }
        
        return inventory;
    }
    
    /**
     * Create a display item for a kit
     */
    private ItemStack createKitDisplayItem(Kit kit, Player player, int playerCredits, boolean isSelected) {
        ItemStack item = new ItemStack(kit.getIcon());
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Set display name with color coding
            String color;
            if (kit.isPremium()) {
                if (kit.canPlayerUse(player, playerCredits)) {
                    color = "§6"; // Golden yellow for available premium
                } else {
                    color = "§c"; // Red for unavailable premium
                }
            } else {
                color = "§7"; // Grey for free kits
            }
            
            String displayName = color + kit.getDisplayName();
            if (isSelected) {
                displayName = "§a§l✓ " + displayName + " §a§l(SELECTED)";
            }
            
            meta.displayName(Component.text(displayName));
            
            // Create lore
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7" + kit.getDescription()));
            lore.add(Component.text(""));
            
            if (kit.isPremium()) {
                lore.add(Component.text("§6Premium Kit"));
                lore.add(Component.text("§7Cost: §e" + kit.getCost() + " credits"));
                if (!kit.canPlayerUse(player, playerCredits)) {
                    lore.add(Component.text("§cInsufficient credits"));
                    lore.add(Component.text("§7You need §e" + kit.getCost() + " §7credits"));
                }
            } else {
                lore.add(Component.text("§aFree Kit"));
            }
            
            lore.add(Component.text(""));
            lore.add(Component.text("§7Starting Items:"));
            for (ItemStack startingItem : kit.getStartingItems()) {
                if (startingItem != null && startingItem.getItemMeta() != null) {
                    String itemName = startingItem.getItemMeta().displayName() != null ? 
                        startingItem.getItemMeta().displayName().toString() : 
                        startingItem.getType().name().toLowerCase().replace("_", " ");
                    lore.add(Component.text("§8- §7" + itemName));
                }
            }
            
            lore.add(Component.text(""));
            if (isSelected) {
                lore.add(Component.text("§aCurrently selected"));
            } else {
                lore.add(Component.text("§eClick to select"));
            }
            
            meta.lore(lore);
            
            // Add visual indicator for selected kit
            if (isSelected) {
                // Enchantment glow trick without showing enchant details
                item.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.UNBREAKING, 1);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                lore.add(Component.text("§a§l✓ SELECTED"));
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Create info item showing player credits
     */
    private ItemStack createInfoItem(int playerCredits) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.displayName(Component.text("§6Your Credits"));
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§7You have §e" + playerCredits + " §7credits"));
            lore.add(Component.text(""));
            lore.add(Component.text("§7Free kits: §aAvailable"));
            if (playerCredits > 0) {
                lore.add(Component.text("§7Premium kits: §6Check individual costs"));
            } else {
                lore.add(Component.text("§7Premium kits: §cNeed credits"));
            }
            
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Handle inventory clicks
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        Inventory inventory = event.getInventory();
        plugin.getLogger().fine("Inventory click event for player: " + player.getName() + " on inventory: " + inventory.getType());
        plugin.getLogger().fine("Inventory hash: " + inventory.hashCode() + ", Player UUID: " + player.getUniqueId());
        plugin.getLogger().fine("Open inventories count: " + openInventories.size() + ", Contains this inventory: " + openInventories.containsValue(inventory));
        
        // Debug: print all tracked inventories
        for (Map.Entry<UUID, Inventory> entry : openInventories.entrySet()) {
            plugin.getLogger().fine("Tracked inventory - UUID: " + entry.getKey() + ", Hash: " + entry.getValue().hashCode());
        }
        
        if (!openInventories.containsValue(inventory)) {
            plugin.getLogger().fine("Inventory not tracked, ignoring click for player: " + player.getName());
            return;
        }
        
        plugin.getLogger().fine("Inventory tracked, processing click for player: " + player.getName());
        event.setCancelled(true);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        plugin.getLogger().fine("Kit selection click - Item type: " + clickedItem.getType() + " for player: " + player.getName());
        
        // Check if this is a confirmation inventory
        String title = event.getView().title().toString();
        plugin.getLogger().fine("Inventory title: '" + title + "' for player: " + player.getName());
        
        // Check if this is a confirmation inventory by checking if player has pending confirmation
        if (pendingConfirmations.containsKey(player.getUniqueId())) {
            plugin.getLogger().fine("Player has pending confirmation, handling confirmation click for player: " + player.getName());
            handleConfirmationClick(event, player);
            return;
        }
        
        // Check if it's the info item
        if (clickedItem.getType() == Material.GOLD_INGOT) {
            plugin.getLogger().fine("Info item clicked, ignoring");
            return; // Info item, do nothing
        }
        
        // Find the kit for this item
        Kit clickedKit = findKitByItem(clickedItem);
        if (clickedKit == null) {
            plugin.getLogger().warning("No kit found for clicked item: " + clickedItem.getType() + " for player: " + player.getName());
            return;
        }
        
        plugin.getLogger().fine("Found kit: " + clickedKit.getId() + " for player: " + player.getName());
        
        // Check if player can use the kit: unlocked or affordable
        int playerCredits = plugin.getPlayerManager().getPlayerCredits(player);
        boolean hasUnlocked = plugin.getPlayerManager().hasUnlockedKit(player.getUniqueId(), clickedKit.getId());
        plugin.getLogger().fine("Player credits: " + playerCredits + ", Kit cost: " + clickedKit.getCost() + ", Unlocked: " + hasUnlocked);
        
        if (!hasUnlocked && !clickedKit.canPlayerUse(player, playerCredits)) {
            plugin.getLogger().fine("Player cannot afford kit: " + clickedKit.getId());
            player.sendMessage(Component.text("You cannot use this kit yet! You need " + clickedKit.getCost() + " credits.", NamedTextColor.RED));
            return;
        }
        
        // Show admin bypass message if applicable
        if (clickedKit.isPremium() && !clickedKit.shouldPlayerPay(player)) {
            plugin.getLogger().fine("Admin bypass active for kit: " + clickedKit.getId());
            player.sendMessage(Component.text("§a§l[Admin] §7Kit cost bypassed due to admin permissions!", NamedTextColor.GREEN));
        }
        
        // Check if it's already selected
        Kit currentKit = plugin.getKitManager().getPlayerKit(player);
        if (clickedKit.equals(currentKit)) {
            plugin.getLogger().fine("Kit already selected: " + clickedKit.getId());
            player.sendMessage(Component.text("This kit is already selected!", NamedTextColor.YELLOW));
            return;
        }
        
        // If it's a premium kit and not already unlocked, show confirmation
        if (clickedKit.isPremium() && !hasUnlocked) {
            plugin.getLogger().info("Showing confirmation dialog for premium kit: " + clickedKit.getId() + " to player: " + player.getName());
            showConfirmationDialog(player, clickedKit);
        } else {
            plugin.getLogger().info("Selecting free kit immediately: " + clickedKit.getId() + " for player: " + player.getName());
            // Free kit, select immediately
            selectKit(player, clickedKit);
        }
    }
    
    /**
     * Find a kit by its display item
     */
    private Kit findKitByItem(ItemStack item) {
        if (item == null) {
            plugin.getLogger().warning("findKitByItem called with null item");
            return null;
        }
        
        Collection<Kit> allKits = plugin.getKitManager().getAllKits();
        plugin.getLogger().fine("Searching for kit with item type: " + item.getType() + " (total kits: " + allKits.size() + ")");
        
        // First, try to find by exact icon match
        List<Kit> matchingKits = new ArrayList<>();
        for (Kit kit : allKits) {
            plugin.getLogger().fine("Checking kit: " + kit.getId() + " with icon: " + kit.getIcon());
            if (kit.getIcon() == item.getType()) {
                matchingKits.add(kit);
                plugin.getLogger().fine("Found kit with matching icon: " + kit.getId());
            }
        }
        
        if (matchingKits.size() == 1) {
            // Only one kit with this icon, return it
            Kit kit = matchingKits.get(0);
            plugin.getLogger().fine("Returning single matching kit: " + kit.getId());
            return kit;
        } else if (matchingKits.size() > 1) {
            // Multiple kits with same icon, need to differentiate by other means
            plugin.getLogger().warning("Multiple kits found with same icon " + item.getType() + ": " + 
                matchingKits.stream().map(Kit::getId).collect(java.util.stream.Collectors.joining(", ")));
            
            // For now, return the first one (this is a temporary fix)
            // TODO: Implement better kit identification (e.g., by lore, custom data, etc.)
            Kit kit = matchingKits.get(0);
            plugin.getLogger().warning("Returning first matching kit: " + kit.getId() + " (temporary fix)");
            return kit;
        }
        
        plugin.getLogger().warning("No kit found for item type: " + item.getType());
        return null;
    }
    
    /**
     * Show confirmation dialog for premium kit purchase
     */
    private void showConfirmationDialog(Player player, Kit kit) {
        pendingConfirmations.put(player.getUniqueId(), kit);
        
        Inventory confirmInventory = Bukkit.createInventory(null, 27, Component.text("§8Confirm Kit Purchase"));
        
        // Fill with glass panes
        ItemStack glassPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glassPane.getItemMeta();
        glassMeta.displayName(Component.text(""));
        glassPane.setItemMeta(glassMeta);
        
        for (int i = 0; i < 27; i++) {
            confirmInventory.setItem(i, glassPane);
        }
        
        // Kit info in center
        ItemStack kitInfo = createKitDisplayItem(kit, player, plugin.getPlayerManager().getPlayerCredits(player), false);
        confirmInventory.setItem(13, kitInfo);
        
        // Confirm button
        ItemStack confirmItem = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.displayName(Component.text("§a§lCONFIRM PURCHASE"));
        
        List<Component> confirmLore = new ArrayList<>();
        confirmLore.add(Component.text("§7Click to purchase"));
        
        // Check if admin bypass is active
        if (!kit.shouldPlayerPay(player)) {
            confirmLore.add(Component.text("§a§l[ADMIN BYPASS] §7Cost: §e" + kit.getCost() + " §7credits §7(§aFREE§7)"));
        } else {
            confirmLore.add(Component.text("§7" + kit.getDisplayName() + " §7for §e" + kit.getCost() + " §7credits"));
        }
        
        // Show effective cost
        int effectiveCost = kit.getEffectiveCost(player);
        if (effectiveCost == 0) {
            confirmLore.add(Component.text("§a§l[FREE] §7No credits will be deducted"));
        } else {
            confirmLore.add(Component.text("§7You will pay: §e" + effectiveCost + " §7credits"));
        }
        
        confirmMeta.lore(confirmLore);
        confirmItem.setItemMeta(confirmMeta);
        confirmInventory.setItem(11, confirmItem);
        
        // Cancel button
        ItemStack cancelItem = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.displayName(Component.text("§c§lCANCEL"));
        cancelMeta.lore(List.of(Component.text("§7Click to cancel")));
        confirmInventory.setItem(15, cancelItem);
        
        // Track this inventory and open it
        openInventories.put(player.getUniqueId(), confirmInventory);
        plugin.getLogger().fine("Confirmation inventory tracked for player: " + player.getName() + " with UUID: " + player.getUniqueId());
        plugin.getLogger().fine("Total tracked inventories: " + openInventories.size() + ", Pending confirmations: " + pendingConfirmations.size());
        plugin.getLogger().fine("Confirmation inventory hash: " + confirmInventory.hashCode() + ", Player UUID: " + player.getUniqueId());
        player.openInventory(confirmInventory);
    }
    
    /**
     * Handle confirmation dialog clicks
     */
    private void handleConfirmationClick(InventoryClickEvent event, Player player) {
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        plugin.getLogger().fine("Confirmation click - Item type: " + clickedItem.getType() + " for player: " + player.getName());
        
        Kit pendingKit = pendingConfirmations.get(player.getUniqueId());
        if (pendingKit == null) {
            plugin.getLogger().warning("No pending kit found for player: " + player.getName());
            return;
        }
        
        plugin.getLogger().fine("Pending kit: " + pendingKit.getId() + " for player: " + player.getName());
        
        if (clickedItem.getType() == Material.LIME_WOOL) {
            plugin.getLogger().info("Confirm purchase clicked for kit: " + pendingKit.getId() + " by player: " + player.getName());
            // Confirm purchase
            selectKit(player, pendingKit);
            pendingConfirmations.remove(player.getUniqueId());
            player.closeInventory();
        } else if (clickedItem.getType() == Material.RED_WOOL) {
            plugin.getLogger().info("Cancel purchase clicked for kit: " + pendingKit.getId() + " by player: " + player.getName());
            // Cancel purchase
            pendingConfirmations.remove(player.getUniqueId());
            player.closeInventory();
            // Reopen kit selection
            openKitSelection(player);
        } else {
            plugin.getLogger().fine("Unknown item clicked in confirmation: " + clickedItem.getType());
        }
    }
    
    /**
     * Select a kit for a player
     */
    private void selectKit(Player player, Kit kit) {
        plugin.getLogger().info("Starting kit selection for player: " + player.getName() + " with kit: " + kit.getId());
        
        // Fire kit selection event
        KitSelectionEvent event = new KitSelectionEvent(player, kit.getId());
        Bukkit.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) {
            String reason = event.getCancelReason();
            plugin.getLogger().warning("Kit selection event cancelled for player: " + player.getName() + " with reason: " + reason);
            if (reason != null) {
                player.sendMessage(Component.text(reason, NamedTextColor.RED));
            }
            // Clean up tracking
            openInventories.remove(player.getUniqueId());
            pendingConfirmations.remove(player.getUniqueId());
            return;
        }
        
        plugin.getLogger().fine("Kit selection event not cancelled for player: " + player.getName());
        
        // Deduct credits if premium kit and player should pay and not already unlocked
        if (kit.isPremium() && kit.shouldPlayerPay(player)) {
            boolean hasUnlocked = plugin.getPlayerManager().hasUnlockedKit(player.getUniqueId(), kit.getId());
            if (hasUnlocked) {
                // Already unlocked; complete selection without paying
                Bukkit.getScheduler().runTask(plugin, () -> completeKitSelection(player, kit));
                return;
            }
            plugin.getLogger().info("Deducting credits for premium kit: " + kit.getId() + " for player: " + player.getName() + " (cost: " + kit.getCost() + ")");
            // Handle credits deduction asynchronously
            plugin.getPlayerManager().deductCredits(player.getUniqueId(), kit.getCost(), "Purchased " + kit.getDisplayName() + " kit")
                .thenAccept(success -> {
                    if (success) {
                        plugin.getLogger().info("Credits deducted successfully for kit: " + kit.getId() + " for player: " + player.getName());
                        // Credits deducted successfully, select the kit
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            // Persist unlock
                            plugin.getPlayerManager().unlockKit(player.getUniqueId(), kit.getId());
                            completeKitSelection(player, kit);
                        });
                    } else {
                        plugin.getLogger().warning("Failed to deduct credits for kit: " + kit.getId() + " for player: " + player.getName());
                        // Failed to deduct credits
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage(Component.text("Failed to purchase kit! Please try again.", NamedTextColor.RED));
                            // Clean up tracking
                            openInventories.remove(player.getUniqueId());
                            pendingConfirmations.remove(player.getUniqueId());
                            // Reopen kit selection
                            openKitSelection(player);
                        });
                    }
                })
                .exceptionally(throwable -> {
                    // Handle any errors
                    plugin.getLogger().log(java.util.logging.Level.SEVERE, "Error deducting credits for kit purchase: " + throwable.getMessage(), throwable);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage(Component.text("An error occurred while purchasing the kit. Please try again.", NamedTextColor.RED));
                        // Clean up tracking
                        openInventories.remove(player.getUniqueId());
                        pendingConfirmations.remove(player.getUniqueId());
                        // Reopen kit selection
                        openKitSelection(player);
                    });
                    return null;
                });
            return; // Exit early, kit selection will be handled in the callback
        }
        
        plugin.getLogger().info("No credits to deduct, completing kit selection immediately for player: " + player.getName());
        // For free kits or admin bypass, select immediately
        completeKitSelection(player, kit);
    }
    
    /**
     * Complete the kit selection process (called after credits are deducted)
     */
    private void completeKitSelection(Player player, Kit kit) {
        plugin.getLogger().info("Completing kit selection for player: " + player.getName() + " with kit: " + kit.getId());
        
        // Select the kit
        plugin.getKitManager().setPlayerKit(player, kit.getId());
        plugin.getLogger().fine("Kit set for player: " + player.getName() + " with kit: " + kit.getId());
        
        // Send success message with admin bypass info if applicable
        if (kit.isPremium() && !kit.shouldPlayerPay(player)) {
            plugin.getLogger().fine("Sending admin bypass success message to player: " + player.getName());
            player.sendMessage(Component.text("§a§l[Admin Bypass] §7Kit " + kit.getDisplayName() + " selected for FREE!", NamedTextColor.GREEN));
        } else {
            plugin.getLogger().fine("Sending regular success message to player: " + player.getName());
            player.sendMessage(Component.text("Successfully selected " + kit.getDisplayName() + " kit!", NamedTextColor.GREEN));
        }
        
        // Clean up tracking
        plugin.getLogger().fine("Cleaning up tracking for player: " + player.getName());
        openInventories.remove(player.getUniqueId());
        pendingConfirmations.remove(player.getUniqueId());
        
        // Close inventory
        plugin.getLogger().fine("Closing inventory for player: " + player.getName());
        player.closeInventory();
        
        plugin.getLogger().info("Kit selection completed successfully for player: " + player.getName() + " with kit: " + kit.getId());
    }
    
    /**
     * Handle inventory close
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        plugin.getLogger().fine("Inventory close event for player: " + player.getName());
        
        // Only remove tracking if this is not a confirmation inventory being closed for kit selection
        if (pendingConfirmations.containsKey(player.getUniqueId())) {
            plugin.getLogger().fine("Player has pending confirmation, keeping tracking for kit selection process");
            // Don't remove tracking yet - let the kit selection process handle it
            return;
        }
        
        plugin.getLogger().fine("Removing inventory tracking for player: " + player.getName());
        openInventories.remove(player.getUniqueId());
    }
}
