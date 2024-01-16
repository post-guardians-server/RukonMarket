package me.rukon0621.rukonmarket.listener;

import me.rukon0621.rukonmarket.RukonMarket;
import me.rukon0621.rukonmarket.market.MarketWindow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class LoginListener implements Listener {
    public LoginListener() {
        RukonMarket.inst().getServer().getPluginManager().registerEvents(this, RukonMarket.inst());
    }

    //데이터 베이스에 플레이어의 정보가 없으면 초기화
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        MarketWindow.resetPlayerUsingMarket(e.getPlayer());
    }
}
