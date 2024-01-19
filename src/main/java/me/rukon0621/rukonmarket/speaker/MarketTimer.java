package me.rukon0621.rukonmarket.speaker;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MarketTimer {

    public static final int timerSecond = 600;
    private static MarketTimer marketTimer;
    private final Map<UUID, Long> coolTime = new HashMap<>();

    private MarketTimer() {
    }

    public static MarketTimer getInstance() {
        if (marketTimer == null)
            marketTimer = new MarketTimer();
        return marketTimer;
    }

    /**
     * @param player player
     * @return return cooldown sec, if player is not on cooldown returns 0.
     */
    public Long getCoolDown(Player player) {
        return Math.max(0, coolTime.getOrDefault(player.getUniqueId(), System.currentTimeMillis()) - System.currentTimeMillis()) / 1000L;
    }

    public void addPlayer(Player p) {
        coolTime.put(p.getUniqueId(), System.currentTimeMillis() + timerSecond * 1000L);
    }
}
