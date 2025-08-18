package com.api_d.hungerGames.database.models;

import java.sql.Timestamp;

/**
 * Represents a game log entry in the database
 */
public class GameLog {
    private final int id;
    private final int gameId;
    private final int playerId;
    private final int partyId;
    private Timestamp diedAt;
    private DeathReason deathReason;
    private Integer killerId;
    private String deathMessage;
    private final Timestamp createdAt;
    
    public enum DeathReason {
        PLAYER,
        ENVIRONMENT,
        DISCONNECTED,
        WINNER
    }
    
    public GameLog(int id, int gameId, int playerId, int partyId, Timestamp diedAt, 
                   DeathReason deathReason, Integer killerId, String deathMessage, Timestamp createdAt) {
        this.id = id;
        this.gameId = gameId;
        this.playerId = playerId;
        this.partyId = partyId;
        this.diedAt = diedAt;
        this.deathReason = deathReason;
        this.killerId = killerId;
        this.deathMessage = deathMessage;
        this.createdAt = createdAt;
    }
    
    public int getId() {
        return id;
    }
    
    public int getGameId() {
        return gameId;
    }
    
    public int getPlayerId() {
        return playerId;
    }
    
    public int getPartyId() {
        return partyId;
    }
    
    public Timestamp getDiedAt() {
        return diedAt;
    }
    
    public void setDiedAt(Timestamp diedAt) {
        this.diedAt = diedAt;
    }
    
    public DeathReason getDeathReason() {
        return deathReason;
    }
    
    public void setDeathReason(DeathReason deathReason) {
        this.deathReason = deathReason;
    }
    
    public Integer getKillerId() {
        return killerId;
    }
    
    public void setKillerId(Integer killerId) {
        this.killerId = killerId;
    }
    
    public String getDeathMessage() {
        return deathMessage;
    }
    
    public void setDeathMessage(String deathMessage) {
        this.deathMessage = deathMessage;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
}