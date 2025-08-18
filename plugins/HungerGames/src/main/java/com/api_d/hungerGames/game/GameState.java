package com.api_d.hungerGames.game;

/**
 * Represents the different states of a Hunger Games match
 */
public enum GameState {
    
    /**
     * Game is waiting for players to join and select kits
     */
    WAITING("Waiting for players"),
    
    /**
     * Game is starting, players are being teleported and countdown is active
     */
    STARTING("Game starting"),
    
    /**
     * Game is active, PvP may or may not be enabled yet
     */
    ACTIVE("Game active"),
    
    /**
     * Feast has spawned and is available
     */
    FEAST("Feast active"),
    
    /**
     * Border is shrinking to force final confrontation
     */
    BORDER_SHRINKING("Border shrinking"),
    
    /**
     * Final fight with poison effects
     */
    FINAL_FIGHT("Final fight"),
    
    /**
     * Game has ended, cleanup in progress
     */
    ENDING("Game ending"),
    
    /**
     * Game is completely finished
     */
    FINISHED("Game finished");
    
    private final String displayName;
    
    GameState(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if the game is in an active playing state
     */
    public boolean isGameActive() {
        return this == ACTIVE || this == FEAST || this == BORDER_SHRINKING || this == FINAL_FIGHT;
    }
    
    /**
     * Check if players can join the game
     */
    public boolean canPlayersJoin() {
        return this == WAITING;
    }
    
    /**
     * Check if players can select kits
     */
    public boolean canSelectKits() {
        return this == WAITING;
    }
    
    /**
     * Check if the game has finished
     */
    public boolean isFinished() {
        return this == FINISHED;
    }
}