package com.api_d.hungerGames.database.models;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Represents a player in the database
 */
public class Player {
    private final int id;
    private final UUID uuid;
    private int credits;
    private String lastKitUsed;
    private final Timestamp createdAt;
    private Timestamp updatedAt;
    
    public Player(int id, UUID uuid, int credits, String lastKitUsed, Timestamp createdAt, Timestamp updatedAt) {
        this.id = id;
        this.uuid = uuid;
        this.credits = credits;
        this.lastKitUsed = lastKitUsed;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    public int getId() {
        return id;
    }
    
    public UUID getUuid() {
        return uuid;
    }
    
    public int getCredits() {
        return credits;
    }
    
    public void setCredits(int credits) {
        this.credits = credits;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }
    
    public String getLastKitUsed() {
        return lastKitUsed;
    }
    
    public void setLastKitUsed(String lastKitUsed) {
        this.lastKitUsed = lastKitUsed;
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }
}