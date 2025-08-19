package com.api_d.hungerGames.events;

import com.api_d.hungerGames.game.GameState;

/**
 * Event fired when the game state changes
 */
public class GameStateChangeEvent extends GameEvent {
    
    private final GameState previousState;
    private final GameState newState;
    
    public GameStateChangeEvent(GameState previousState, GameState newState) {
        super();
        this.previousState = previousState;
        this.newState = newState;
    }
    
    public GameState getPreviousState() {
        return previousState;
    }
    
    public GameState getNewState() {
        return newState;
    }
}