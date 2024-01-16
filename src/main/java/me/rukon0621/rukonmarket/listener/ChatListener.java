package me.rukon0621.rukonmarket.listener;

import me.rukon0621.guardians.helper.Msg;
import me.rukon0621.rukonmarket.RukonMarket;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ChatListener implements Listener {
    private String answer = null;
    private final Player player;
    private final CountDownLatch latch;

    /**
     * @param player player
     * @param latch AsyncLatch
     * @param expireSecond 만료 시간
     */
    public ChatListener(Player player, CountDownLatch latch, int expireSecond) {
        this.latch = latch;
        this.player = player;
        RukonMarket.inst().getServer().getPluginManager().registerEvents(this, RukonMarket.inst());
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    latch.await(expireSecond,  TimeUnit.SECONDS);
                    if(answer==null) {
                        disable();
                        latch.countDown();
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                Msg.warn(player, "입력 시간이 만료되었습니다.");
                            }
                        }.runTask(RukonMarket.inst());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(RukonMarket.inst());
    }

    private void disable() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if(!e.getPlayer().equals(player)) return;
        e.setCancelled(true);
        if(e.getMessage().contains("'") || e.getMessage().contains("#") || e.getMessage().contains("--") || e.getMessage().contains("*")) {
            Msg.warn(player, "기호를 사용할 수 없습니다.");
        }
        else answer = Msg.uncolor(e.getMessage());
        latch.countDown();
        disable();
    }

    @Nullable
    public String getAnswer() {
        return answer;
    }

}
