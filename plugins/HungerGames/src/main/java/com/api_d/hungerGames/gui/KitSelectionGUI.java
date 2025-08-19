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
            
            ItemStack displayItem = createKitDisplayItem(kit, playerCredits, kit.equals(currentKit));
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
    private ItemStack createKitDisplayItem(Kit kit, int playerCredits, boolean isSelected) {
        ItemStack item = new ItemStack(kit.getIcon());
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Set display name with color coding
            String color;
            if (kit.isPremium()) {
                if (kit.canPlayerUse(null, playerCredits)) {
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
                if (!kit.canPlayerUse(null, playerCredits)) {
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
            
            // Add visual indicator for selected kit (no enchantment glow due to API compatibility)
            if (isSelected) {
                // Use a different approach - add a special lore line instead
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
        if (!openInventories.containsValue(inventory)) return;
        
        event.setCancelled(true);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        // Check if it's the info item
        if (clickedItem.getType() == Material.GOLD_INGOT) {
            return; // Info item, do nothing
        }
        
        // Find the kit for this item
        Kit clickedKit = findKitByItem(clickedItem);
        if (clickedKit == null) return;
        
        // Check if player can afford the kit
        int playerCredits = plugin.getPlayerManager().getPlayerCredits(player);
        if (!clickedKit.canPlayerUse(player, playerCredits)) {
            player.sendMessage(Component.text("You cannot afford this kit! You need " + clickedKit.getCost() + " credits.", NamedTextColor.RED));
            return;
        }
        
        // Check if it's already selected
        Kit currentKit = plugin.getKitManager().getPlayerKit(player);
        if (clickedKit.equals(currentKit)) {
            player.sendMessage(Component.text("This kit is already selected!", NamedTextColor.YELLOW));
            return;
        }
        
        // If it's a premium kit, show confirmation
        if (clickedKit.isPremium()) {
            showConfirmationDialog(player, clickedKit);
        } else {
            // Free kit, select immediately
            selectKit(player, clickedKit);
        }
    }
    
    /**
     * Find a kit by its display item
     */
    private Kit findKitByItem(ItemStack item) {
        Collection<Kit> allKits = plugin.getKitManager().getAllKits();
        for (Kit kit : allKits) {
            if (kit.getIcon() == item.getType()) {
                return kit;
            }
        }
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
        ItemStack kitInfo = createKitDisplayItem(kit, plugin.getPlayerManager().getPlayerCredits(player), false);
        confirmInventory.setItem(13, kitInfo);
        
        // Confirm button
        ItemStack confirmItem = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.displayName(Component.text("§a§lCONFIRM PURCHASE"));
        
        List<Component> confirmLore = new ArrayList<>();
        confirmLore.add(Component.text("§7Click to purchase"));
        confirmLore.add(Component.text("§7" + kit.getDisplayName() + " §7for §e" + kit.getCost() + " §7credits"));
        confirmMeta.lore(confirmLore);
        confirmItem.setItemMeta(confirmMeta);
        confirmInventory.setItem(11, confirmItem);
        
        // Cancel button
        ItemStack cancelItem = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.displayName(Component.text("§c§lCANCEL"));
        cancelMeta.lore(List.of(Component.text("§7Click to cancel")));
        cancelItem.setItemMeta(cancelMeta);
        confirmInventory.setItem(15, cancelItem);
        
        // Open confirmation inventory
        player.openInventory(confirmInventory);
    }
    
    /**
     * Handle confirmation dialog clicks
     */
    @EventHandler
    public void onConfirmationClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        String title = event.getView().title().toString();
        if (!title.contains("Confirm Kit Purchase")) return;
        
        event.setCancelled(true);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        Kit pendingKit = pendingConfirmations.get(player.getUniqueId());
        if (pendingKit == null) return;
        
        if (clickedItem.getType() == Material.LIME_WOOL) {
            // Confirm purchase
            selectKit(player, pendingKit);
            pendingConfirmations.remove(player.getUniqueId());
            player.closeInventory();
        } else if (clickedItem.getType() == Material.RED_WOOL) {
            // Cancel purchase
            pendingConfirmations.remove(player.getUniqueId());
            player.closeInventory();
            // Reopen kit selection
            openKitSelection(player);
        }
    }
    
    /**
     * Select a kit for a player
     */
    private void selectKit(Player player, Kit kit) {
        // Fire kit selection event
        KitSelectionEvent event = new KitSelectionEvent(player, kit.getId());
        Bukkit.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) {
            String reason = event.getCancelReason();
            if (reason != null) {
                player.sendMessage(Component.text(reason, NamedTextColor.RED));
            }
            return;
        }
        
        // Deduct credits if premium kit
        if (kit.isPremium()) {
            plugin.getPlayerManager().deductCredits(player.getUniqueId(), kit.getCost(), "Purchased " + kit.getDisplayName() + " kit");
        }
        
        // Select the kit
        plugin.getKitManager().setPlayerKit(player, kit.getId());
        
        // Send success message
        player.sendMessage(Component.text("Successfully selected " + kit.getDisplayName() + " kit!", NamedTextColor.GREEN));
        
        // Close inventory
        player.closeInventory();
    }
    
    /**
     * Handle inventory close
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        openInventories.remove(player.getUniqueId());
        pendingConfirmations.remove(player.getUniqueId());
    }
}
