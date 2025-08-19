package com.api_d.hungerGames.events;

import com.api_d.hungerGames.database.models.GameLog;
import org.bukkit.entity.Player;

/**
 * Event fired when a player dies in the hunger games
 */
public class PlayerDeathEvent extends GameEvent {
    
    private final Player victim;
    private final Player killer; // Can be null
    private final GameLog.DeathReason deathReason;
    private final String deathMessage;
    
    public PlayerDeathEvent(Player victim, Player killer, GameLog.DeathReason deathReason, String deathMessage) {
        super();
        this.victim = victim;
        this.killer = killer;
        this.deathReason = deathReason;
        this.deathMessage = deathMessage;
    }
    
    public Player getVictim() {
        return victim;
    }
    
    public Player getKiller() {
        return killer;
    }
    
    public GameLog.DeathReason getDeathReason() {
        return deathReason;
    }
    
    public String getDeathMessage() {
        return deathMessage;
    }
}