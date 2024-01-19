package me.rukon0621.rukonmarket;

import me.rukon0621.rukonmarket.listener.LoginListener;
import me.rukon0621.rukonmarket.market.MarketManager;
import me.rukon0621.rukonmarket.speaker.MarketSpeakerCommand;
import me.rukon0621.rukonmarket.speaker.MarketSpeakerManager;
import org.bukkit.plugin.java.JavaPlugin;

public class RukonMarket extends JavaPlugin {

    private static RukonMarket rukonMarket;
    private MarketManager marketManager;
    private MarketSpeakerManager speakerManager;

    public static RukonMarket inst() {
        return rukonMarket;
    }

    @Override
    public void onEnable() {
        rukonMarket = this;
        marketManager = new MarketManager();
        new LoginListener();
        new MarketCommand();
        new MarketSpeakerCommand();
        speakerManager = new MarketSpeakerManager();
    }

    @Override
    public void onDisable() {

    }

    public MarketSpeakerManager getSpeakerManager() {
        return speakerManager;
    }

    public MarketManager getMarketManager() {
        return marketManager;
    }

}
