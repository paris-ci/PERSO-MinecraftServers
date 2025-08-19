package com.api_d.hungerGames.database.models;

import java.sql.Timestamp;

/**
 * Represents a game party in the database
 */
public class GameParty {
    private final int id;
    private final int gameId;
    private final String name;
    private final Timestamp createdAt;
    
    public GameParty(int id, int gameId, String name, Timestamp createdAt) {
        this.id = id;
        this.gameId = gameId;
        this.name = name;
        this.createdAt = createdAt;
    }
    
    public int getId() {
        return id;
    }
    
    public int getGameId() {
        return gameId;
    }
    
    public String getName() {
        return name;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
}