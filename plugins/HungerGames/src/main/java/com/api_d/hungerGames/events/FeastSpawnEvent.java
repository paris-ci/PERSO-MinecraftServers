package com.api_d.hungerGames.events;

import org.bukkit.Location;

/**
 * Event fired when the feast spawns
 */
public class FeastSpawnEvent extends GameEvent {
    
    private final Location feastLocation;
    
    public FeastSpawnEvent(Location feastLocation) {
        super();
        this.feastLocation = feastLocation;
    }
    
    public Location getFeastLocation() {
        return feastLocation;
    }
}