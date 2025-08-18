package com.api_d.hungerGames.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Base class for all custom HungerGames events
 */
public abstract class GameEvent extends Event {
    
    private static final HandlerList handlers = new HandlerList();
    
    public GameEvent() {
        super();
    }
    
    public GameEvent(boolean isAsync) {
        super(isAsync);
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}