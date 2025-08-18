package com.api_d.hungerGames.database.models;

import java.sql.Timestamp;

/**
 * Represents a game in the database
 */
public class Game {
    private final int id;
    private final String serverId;
    private final Timestamp waitingAt;
    private Timestamp startedAt;
    private Timestamp endedAt;
    private final Timestamp createdAt;
    
    public Game(int id, String serverId, Timestamp waitingAt, Timestamp startedAt, Timestamp endedAt, Timestamp createdAt) {
        this.id = id;
        this.serverId = serverId;
        this.waitingAt = waitingAt;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.createdAt = createdAt;
    }
    
    public int getId() {
        return id;
    }
    
    public String getServerId() {
        return serverId;
    }
    
    public Timestamp getWaitingAt() {
        return waitingAt;
    }
    
    public Timestamp getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(Timestamp startedAt) {
        this.startedAt = startedAt;
    }
    
    public Timestamp getEndedAt() {
        return endedAt;
    }
    
    public void setEndedAt(Timestamp endedAt) {
        this.endedAt = endedAt;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
}