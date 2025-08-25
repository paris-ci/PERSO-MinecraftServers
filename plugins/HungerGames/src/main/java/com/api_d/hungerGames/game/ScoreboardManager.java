package com.api_d.hungerGames.game;

import com.api_d.hungerGames.kits.Kit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Displays a sidebar scoreboard showing phase, elapsed time, alive players, and kit
 */
public class ScoreboardManager {

    private final Plugin plugin;
    private final GameManager gameManager;
    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private BukkitRunnable task;

    public ScoreboardManager(Plugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    public void start() {
        stop();
        task = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updatePlayerBoard(player);
                }
            }
        };
        task.runTaskTimer(plugin, 0L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        // Clear scoreboards to avoid leaks
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
        playerScoreboards.clear();
    }

    private void updatePlayerBoard(Player player) {
        Scoreboard board = playerScoreboards.computeIfAbsent(player.getUniqueId(), id -> Bukkit.getScoreboardManager().getNewScoreboard());

        Objective obj = board.getObjective("hg");
        if (obj == null) {
            obj = board.registerNewObjective("hg", Criteria.DUMMY, net.kyori.adventure.text.Component.text("§c§lHungerGames"));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            obj.displayName(net.kyori.adventure.text.Component.text("§c§lHungerGames"));
        }

        // Build static layout once
        if (board.getTeam("hg_phase") == null) {
            obj.getScore(" ").setScore(10);
            obj.getScore("§7Phase:").setScore(9);
            obj.getScore("  ").setScore(7);
            obj.getScore("§7Elapsed:").setScore(6);
            obj.getScore("   ").setScore(4);
            obj.getScore("§7Alive:").setScore(3);
            // Register dynamic lines
            registerLine(board, "hg_phase", "§a", 8);
            registerLine(board, "hg_time", "§b", 5);
            registerLine(board, "hg_alive", "§c", 2);
            registerLine(board, "hg_kit", "§d", 1);
            registerLine(board, "hg_credits", "§e", 0);
        }

        // Data
        GameState state = gameManager.getCurrentState();
        String phase = state != null ? state.getDisplayName() : "Initializing";
        int alive = gameManager.getAlivePlayers().size();
        long elapsedSec = 0L;
        if (state != null && state.isGameActive()) {
            elapsedSec = Math.max(0L, (System.currentTimeMillis() - gameManager.getGameStartTime()) / 1000L);
        }
        String time = formatTime(elapsedSec);
        Kit kit = gameManager.getPlayerKitFor(player);
        String kitName = kit != null ? kit.getDisplayName() : "None";

        // Update dynamic values via teams (kit first, then credits)
        int credits = gameManager.getPlayerManager().getPlayerCredits(player);
        setTeamPrefix(board, "hg_phase", "§f " + phase);
        setTeamPrefix(board, "hg_time", "§f " + time);
        setTeamPrefix(board, "hg_alive", "§f " + alive);
        setTeamPrefix(board, "hg_kit", "§7Kit: §f" + kitName);
        setTeamPrefix(board, "hg_credits", "§7Credits: §e" + credits);

        player.setScoreboard(board);
    }

    private void registerLine(Scoreboard board, String teamName, String entry, int score) {
        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }
        if (!team.hasEntry(entry)) {
            team.addEntry(entry);
        }
        Objective obj = board.getObjective("hg");
        if (obj != null) {
            obj.getScore(entry).setScore(score);
        }
    }

    private void setTeamPrefix(Scoreboard board, String teamName, String prefix) {
        Team team = board.getTeam(teamName);
        if (team != null) {
            team.prefix(net.kyori.adventure.text.Component.text(prefix));
        }
    }

    private String formatTime(long seconds) {
        long m = seconds / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }
}


