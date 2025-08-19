package com.api_d.hungerGames.commands;

import com.api_d.hungerGames.HungerGames;
import com.api_d.hungerGames.game.GameManager;
import com.api_d.hungerGames.game.GameState;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Admin command for managing Hunger Games
 */
public class AdminCommand extends BaseCommand implements TabCompleter {
    
    public AdminCommand(HungerGames plugin) {
        super(plugin);
    }
    
    @Override
    protected boolean execute(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPermission(sender, "hungergames.admin")) return true;
        
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "start":
                return handleStart(sender, args);
            case "next":
                return handleNext(sender, args);
            case "state":
                return handleState(sender, args);
            case "cancel":
                return handleCancel(sender, args);
            case "reload":
                return handleReload(sender, args);
            case "status":
                return handleStatus(sender, args);
            case "forcepvp":
                return handleForcePvp(sender, args);
            case "forcefeast":
                return handleForceFeast(sender, args);
            case "forceborder":
                return handleForceBorder(sender, args);
            case "forcefinal":
                return handleForceFinal(sender, args);
            case "end":
                return handleEnd(sender, args);
            case "debug":
                return handleDebug(sender, args);
            default:
                showHelp(sender);
                return true;
        }
    }
    
    /**
     * Handle the start command
     */
    private boolean handleStart(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "hungergames.admin.start")) return true;
        
        GameManager gameManager = plugin.getGameManager();
        
        if (gameManager.isGameRunning()) {
            GameState currentState = gameManager.getCurrentState();
            if (currentState == GameState.WAITING) {
                // Game is waiting, force start it
                sendMessage(sender, "§aForce starting the game...");
                gameManager.forceStartGame();
                return true;
            } else {
                sendMessage(sender, "§cA game is already running in state: " + currentState.getDisplayName());
                return true;
            }
        }
        
        // Force start the game
        gameManager.initializeGame();
        sendMessage(sender, "§aGame has been force started!");
        
        // Broadcast to all players
        Bukkit.broadcast(Component.text("§8[§cHG§8] §aAn administrator has started the game!", NamedTextColor.GREEN));
        
        return true;
    }
    
    /**
     * Handle the next command - proceed to next game phase
     */
    private boolean handleNext(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "hungergames.admin.next")) return true;
        
        GameManager gameManager = plugin.getGameManager();
        
        if (!gameManager.isGameRunning()) {
            sendMessage(sender, "§cNo game is currently running!");
            return true;
        }
        
        GameState currentState = gameManager.getCurrentState();
        if (currentState == null) {
            sendMessage(sender, "§cGame state is null!");
            return true;
        }
        
        // Determine next state based on current state
        GameState nextState = getNextState(currentState);
        if (nextState == null) {
            sendMessage(sender, "§cCannot proceed from current state: " + currentState.getDisplayName());
            return true;
        }
        
        // Force transition to next state
        gameManager.forceStateTransition(nextState);
        sendMessage(sender, "§aGame has been forced to proceed to: §e" + nextState.getDisplayName());
        
        // Broadcast to all players
        Bukkit.broadcast(Component.text("§8[§cHG§8] §eGame phase changed to: §a" + nextState.getDisplayName(), NamedTextColor.YELLOW));
        
        return true;
    }
    
    /**
     * Handle the state command - set specific game state
     */
    private boolean handleState(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "hungergames.admin.state")) return true;
        
        if (args.length < 2) {
            sendMessage(sender, "§cUsage: /hgadmin state <state_name>");
            sendMessage(sender, "§eAvailable states: WAITING, STARTING, ACTIVE, FEAST, BORDER_SHRINKING, FINAL_FIGHT, ENDING, FINISHED");
            return true;
        }
        
        GameManager gameManager = plugin.getGameManager();
        
        if (!gameManager.isGameRunning()) {
            sendMessage(sender, "§cNo game is currently running!");
            return true;
        }
        
        String stateName = args[1].toUpperCase();
        GameState targetState;
        
        try {
            targetState = GameState.valueOf(stateName);
        } catch (IllegalArgumentException e) {
            sendMessage(sender, "§cInvalid state: " + stateName);
            sendMessage(sender, "§eAvailable states: WAITING, STARTING, ACTIVE, FEAST, BORDER_SHRINKING, FINAL_FIGHT, ENDING, FINISHED");
            return true;
        }
        
        // Force transition to target state
        gameManager.forceStateTransition(targetState);
        sendMessage(sender, "§aGame state has been forced to: §e" + targetState.getDisplayName());
        
        // Broadcast to all players
        Bukkit.broadcast(Component.text("§8[§cHG§8] §eGame state changed to: §a" + targetState.getDisplayName(), NamedTextColor.YELLOW));
        
        return true;
    }
    
    /**
     * Handle the cancel command
     */
    private boolean handleCancel(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "hungergames.admin.cancel")) return true;
        
        GameManager gameManager = plugin.getGameManager();
        
        if (!gameManager.isGameRunning()) {
            sendMessage(sender, "§cNo game is currently running!");
            return true;
        }
        
        // Cancel the current game
        gameManager.cancelGame();
        sendMessage(sender, "§aGame has been cancelled!");
        
        // Broadcast to all players
        Bukkit.broadcast(Component.text("§8[§cHG§8] §cThe game has been cancelled by an administrator!", NamedTextColor.RED));
        
        return true;
    }
    
    /**
     * Handle the reload command
     */
    private boolean handleReload(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "hungergames.admin.reload")) return true;
        
        try {
            plugin.getGameConfig().reload();
            sendMessage(sender, "§aConfiguration reloaded successfully!");
        } catch (Exception e) {
            sendMessage(sender, "§cFailed to reload configuration: " + e.getMessage());
        }
        
        return true;
    }
    
    /**
     * Handle the status command
     */
    private boolean handleStatus(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "hungergames.admin.status")) return true;
        
        GameManager gameManager = plugin.getGameManager();
        
        sendMessage(sender, "§6=== Hunger Games Status ===");
        sendMessage(sender, "§ePlugin enabled: §a" + plugin.isEnabled());
        sendMessage(sender, "§eGame running: §a" + gameManager.isGameRunning());
        
        if (gameManager.isGameRunning()) {
            GameState currentState = gameManager.getCurrentState();
            sendMessage(sender, "§eCurrent state: §a" + (currentState != null ? currentState.getDisplayName() : "Unknown"));
            sendMessage(sender, "§eAlive players: §a" + gameManager.getAlivePlayers().size());
            sendMessage(sender, "§eDead players: §a" + gameManager.getDeadPlayers().size());
            sendMessage(sender, "§ePvP enabled: §a" + gameManager.isPvpEnabled());
            sendMessage(sender, "§eFeast spawned: §a" + gameManager.isFeastSpawned());
        }
        
        sendMessage(sender, "§eOnline players: §a" + Bukkit.getOnlinePlayers().size());
        sendMessage(sender, "§eTotal worlds: §a" + Bukkit.getWorlds().size());
        
        return true;
    }
    
    /**
     * Handle force PvP command
     */
    private boolean handleForcePvp(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "hungergames.admin.forcepvp")) return true;
        
        GameManager gameManager = plugin.getGameManager();
        
        if (!gameManager.isGameRunning()) {
            sendMessage(sender, "§cNo game is currently running!");
            return true;
        }
        
        // Force enable PvP
        gameManager.forceEnablePvp();
        sendMessage(sender, "§aPvP has been force enabled!");
        
        // Broadcast to all players
        Bukkit.broadcast(Component.text("§8[§cHG§8] §cPvP has been force enabled by an administrator!", NamedTextColor.RED));
        
        return true;
    }
    
    /**
     * Handle force feast command
     */
    private boolean handleForceFeast(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "hungergames.admin.forcefeast")) return true;
        
        GameManager gameManager = plugin.getGameManager();
        
        if (!gameManager.isGameRunning()) {
            sendMessage(sender, "§cNo game is currently running!");
            return true;
        }
        
        // Force spawn feast
        gameManager.forceSpawnFeast();
        sendMessage(sender, "§aFeast has been force spawned!");
        
        // Broadcast to all players
        Bukkit.broadcast(Component.text("§8[§cHG§8] §6The feast has been force spawned by an administrator!", NamedTextColor.GOLD));
        
        return true;
    }
    
    /**
     * Handle force border command
     */
    private boolean handleForceBorder(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "hungergames.admin.forceborder")) return true;
        
        GameManager gameManager = plugin.getGameManager();
        
        if (!gameManager.isGameRunning()) {
            sendMessage(sender, "§cNo game is currently running!");
            return true;
        }
        
        // Force start border shrinking
        gameManager.forceStartBorderShrinking();
        sendMessage(sender, "§aBorder shrinking has been force started!");
        
        // Broadcast to all players
        Bukkit.broadcast(Component.text("§8[§cHG§8] §eThe world border is now force shrinking!", NamedTextColor.YELLOW));
        
        return true;
    }
    
    /**
     * Handle force final fight command
     */
    private boolean handleForceFinal(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "hungergames.admin.forcefinal")) return true;
        
        GameManager gameManager = plugin.getGameManager();
        
        if (!gameManager.isGameRunning()) {
            sendMessage(sender, "§cNo game is currently running!");
            return true;
        }
        
        // Force start final fight
        gameManager.forceStartFinalFight();
        sendMessage(sender, "§aFinal fight has been force started!");
        
        // Broadcast to all players
        Bukkit.broadcast(Component.text("§8[§cHG§8] §cThe final fight has been force started by an administrator!", NamedTextColor.RED));
        
        return true;
    }
    
    /**
     * Handle end command
     */
    private boolean handleEnd(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "hungergames.admin.end")) return true;
        
        GameManager gameManager = plugin.getGameManager();
        
        if (!gameManager.isGameRunning()) {
            sendMessage(sender, "§cNo game is currently running!");
            return true;
        }
        
        // Force end the game
        gameManager.forceEndGame();
        sendMessage(sender, "§aGame has been force ended!");
        
        // Broadcast to all players
        Bukkit.broadcast(Component.text("§8[§cHG§8] §cThe game has been force ended by an administrator!", NamedTextColor.RED));
        
        return true;
    }
    
    /**
     * Handle debug command
     */
    private boolean handleDebug(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "hungergames.admin.debug")) return true;
        
        GameManager gameManager = plugin.getGameManager();
        
        sendMessage(sender, "§6=== Hunger Games Debug Info ===");
        sendMessage(sender, "§eGame running: §a" + gameManager.isGameRunning());
        sendMessage(sender, "§eCurrent state: §a" + (gameManager.getCurrentState() != null ? gameManager.getCurrentState().getDisplayName() : "Unknown"));
        sendMessage(sender, "§eAlive players: §a" + gameManager.getAlivePlayers().size());
        sendMessage(sender, "§eDead players: §a" + gameManager.getDeadPlayers().size());
        sendMessage(sender, "§ePvP enabled: §a" + gameManager.isPvpEnabled());
        sendMessage(sender, "§eFeast spawned: §a" + gameManager.isFeastSpawned());
        sendMessage(sender, "§eOnline players: §a" + Bukkit.getOnlinePlayers().size());
        
        return true;
    }
    
    /**
     * Get the next logical game state
     */
    private GameState getNextState(GameState currentState) {
        switch (currentState) {
            case WAITING:
                return GameState.STARTING;
            case STARTING:
                return GameState.ACTIVE;
            case ACTIVE:
                return GameState.FEAST;
            case FEAST:
                return GameState.BORDER_SHRINKING;
            case BORDER_SHRINKING:
                return GameState.FINAL_FIGHT;
            case FINAL_FIGHT:
                return GameState.ENDING;
            case ENDING:
                return GameState.FINISHED;
            case FINISHED:
                return null; // No next state
            default:
                return null;
        }
    }
    
    /**
     * Show help information
     */
    private void showHelp(CommandSender sender) {
        sendMessage(sender, "§6=== Hunger Games Admin Commands ===");
        sendMessage(sender, "§e/hgadmin start §7- Force start the game");
        sendMessage(sender, "§e/hgadmin next §7- Proceed to next game phase");
        sendMessage(sender, "§e/hgadmin state <state> §7- Set specific game state");
        sendMessage(sender, "§e/hgadmin cancel §7- Cancel the current game");
        sendMessage(sender, "§e/hgadmin reload §7- Reload configuration");
        sendMessage(sender, "§e/hgadmin status §7- Show game status");
        sendMessage(sender, "§e/hgadmin forcepvp §7- Force enable PvP");
        sendMessage(sender, "§e/hgadmin forcefeast §7- Force spawn feast");
        sendMessage(sender, "§e/hgadmin forceborder §7- Force start border shrinking");
        sendMessage(sender, "§e/hgadmin forcefinal §7- Force start final fight");
        sendMessage(sender, "§e/hgadmin end §7- Force end the game");
        sendMessage(sender, "§e/hgadmin debug §7- Show debug information about the game state");
        sendMessage(sender, "");
        sendMessage(sender, "§eAvailable states: WAITING, STARTING, ACTIVE, FEAST, BORDER_SHRINKING, FINAL_FIGHT, ENDING, FINISHED");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Tab complete admin subcommands
            String partial = args[0].toLowerCase();
            List<String> subCommands = Arrays.asList(
                "start", "next", "state", "cancel", "reload", "status",
                "forcepvp", "forcefeast", "forceborder", "forcefinal", "end", "debug"
            );
            
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(partial)) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("state")) {
            // Tab complete game states
            String partial = args[1].toLowerCase();
            List<String> states = Arrays.asList(
                "WAITING", "STARTING", "ACTIVE", "FEAST", "BORDER_SHRINKING", "FINAL_FIGHT", "ENDING", "FINISHED"
            );
            
            for (String state : states) {
                if (state.toLowerCase().startsWith(partial)) {
                    completions.add(state);
                }
            }
        }
        
        return completions;
    }
}
