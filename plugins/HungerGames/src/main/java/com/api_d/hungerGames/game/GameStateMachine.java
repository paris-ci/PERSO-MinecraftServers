package com.api_d.hungerGames.game;

import com.api_d.hungerGames.events.GameStateChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Manages game state transitions and validates state changes
 */
public class GameStateMachine {
    
    private final Plugin plugin;
    private final Logger logger;
    private GameState currentState;
    private final boolean logStateChanges;
    
    // Define valid state transitions
    private static final Set<StateTransition> VALID_TRANSITIONS = EnumSet.of(
        // From WAITING
        new StateTransition(GameState.WAITING, GameState.STARTING),
        new StateTransition(GameState.WAITING, GameState.FINISHED), // Server shutdown
        
        // From STARTING
        new StateTransition(GameState.STARTING, GameState.ACTIVE),
        new StateTransition(GameState.STARTING, GameState.FINISHED), // Server shutdown
        
        // From ACTIVE
        new StateTransition(GameState.ACTIVE, GameState.FEAST),
        new StateTransition(GameState.ACTIVE, GameState.BORDER_SHRINKING),
        new StateTransition(GameState.ACTIVE, GameState.FINAL_FIGHT),
        new StateTransition(GameState.ACTIVE, GameState.ENDING),
        new StateTransition(GameState.ACTIVE, GameState.FINISHED), // Server shutdown
        
        // From FEAST
        new StateTransition(GameState.FEAST, GameState.BORDER_SHRINKING),
        new StateTransition(GameState.FEAST, GameState.FINAL_FIGHT),
        new StateTransition(GameState.FEAST, GameState.ENDING),
        new StateTransition(GameState.FEAST, GameState.FINISHED), // Server shutdown
        
        // From BORDER_SHRINKING
        new StateTransition(GameState.BORDER_SHRINKING, GameState.FINAL_FIGHT),
        new StateTransition(GameState.BORDER_SHRINKING, GameState.ENDING),
        new StateTransition(GameState.BORDER_SHRINKING, GameState.FINISHED), // Server shutdown
        
        // From FINAL_FIGHT
        new StateTransition(GameState.FINAL_FIGHT, GameState.ENDING),
        new StateTransition(GameState.FINAL_FIGHT, GameState.FINISHED), // Server shutdown
        
        // From ENDING
        new StateTransition(GameState.ENDING, GameState.FINISHED)
    );
    
    public GameStateMachine(Plugin plugin, boolean logStateChanges) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.logStateChanges = logStateChanges;
        this.currentState = GameState.WAITING;
        
        if (logStateChanges) {
            logger.info("Game state machine initialized in state: " + currentState.getDisplayName());
        }
    }
    
    /**
     * Get the current game state
     */
    public GameState getCurrentState() {
        return currentState;
    }
    
    /**
     * Attempt to transition to a new state
     * @param newState The state to transition to
     * @return true if the transition was successful, false otherwise
     */
    public boolean transitionTo(GameState newState) {
        return transitionTo(newState, null);
    }
    
    /**
     * Attempt to transition to a new state with a reason
     * @param newState The state to transition to
     * @param reason Optional reason for the transition
     * @return true if the transition was successful, false otherwise
     */
    public boolean transitionTo(GameState newState, String reason) {
        if (currentState == newState) {
            // Already in the target state
            return true;
        }
        
        StateTransition transition = new StateTransition(currentState, newState);
        if (!VALID_TRANSITIONS.contains(transition)) {
            logger.warning("Invalid state transition attempted: " + currentState + " -> " + newState);
            return false;
        }
        
        GameState previousState = currentState;
        currentState = newState;
        
        // Fire event
        GameStateChangeEvent event = new GameStateChangeEvent(previousState, newState);
        Bukkit.getPluginManager().callEvent(event);
        
        // Log the transition
        if (logStateChanges) {
            String message = "Game state changed: " + previousState.getDisplayName() + " -> " + newState.getDisplayName();
            if (reason != null) {
                message += " (" + reason + ")";
            }
            logger.info(message);
        }
        
        return true;
    }
    
    /**
     * Check if a transition to the given state is valid
     */
    public boolean canTransitionTo(GameState newState) {
        if (currentState == newState) {
            return true;
        }
        
        StateTransition transition = new StateTransition(currentState, newState);
        return VALID_TRANSITIONS.contains(transition);
    }
    
    /**
     * Force a state transition (bypasses validation)
     * Use with caution - only for emergency situations
     */
    public void forceTransitionTo(GameState newState, String reason) {
        GameState previousState = currentState;
        currentState = newState;
        
        logger.warning("FORCED state transition: " + previousState + " -> " + newState + 
                      (reason != null ? " (" + reason + ")" : ""));
        
        // Still fire the event
        GameStateChangeEvent event = new GameStateChangeEvent(previousState, newState);
        Bukkit.getPluginManager().callEvent(event);
    }
    
    /**
     * Helper class to represent state transitions
     */
    private static class StateTransition {
        private final GameState from;
        private final GameState to;
        
        public StateTransition(GameState from, GameState to) {
            this.from = from;
            this.to = to;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            StateTransition that = (StateTransition) obj;
            return from == that.from && to == that.to;
        }
        
        @Override
        public int hashCode() {
            return from.hashCode() * 31 + to.hashCode();
        }
        
        @Override
        public String toString() {
            return from + " -> " + to;
        }
    }
}