package me.rukon0621.rukonmarket.util;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class MarketTimer {

    public static final int timerSecond = 900;
    private static MarketTimer marketTimer;
    private Map<UUID, Long> coolTime = new HashMap<>();

    private MarketTimer() {
    }

    public static MarketTimer getInstance() {
        if (marketTimer == null)
            marketTimer = new MarketTimer();
        return marketTimer;
    }

    public Long isCoolDown(Player player) {
        if (coolTime.containsKey(player.getUniqueId())) {
            Long interval = System.currentTimeMillis() - coolTime.get(player.getUniqueId());
            if (interval <= 1000 * timerSecond) {
                return (long) (timerSecond - (int) (interval / 1000));
            } else {
                coolTime.remove(player.getUniqueId());
                return 0L;
            }
        }
        return 0L;
    }


    public void addPlayer(Player p) {
        coolTime.put(p.getUniqueId(), System.currentTimeMillis());
    }

}
