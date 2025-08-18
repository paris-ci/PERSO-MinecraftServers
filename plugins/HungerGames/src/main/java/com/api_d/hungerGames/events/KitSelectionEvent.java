package com.api_d.hungerGames.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

/**
 * Event fired when a player selects a kit
 */
public class KitSelectionEvent extends GameEvent implements Cancellable {
    
    private final Player player;
    private final String kitId;
    private boolean cancelled = false;
    private String cancelReason;
    
    public KitSelectionEvent(Player player, String kitId) {
        super();
        this.player = player;
        this.kitId = kitId;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public String getKitId() {
        return kitId;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    public String getCancelReason() {
        return cancelReason;
    }
    
    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }
}