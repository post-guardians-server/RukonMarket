package me.rukon0621.rukonmarket.speaker;

import me.rukon0621.guardians.helper.InvClass;
import me.rukon0621.rukonmarket.RukonMarket;
import me.rukon0621.rukonmarket.market.MarketItem;
import me.rukon0621.rukonmarket.market.MarketManager;
import me.rukon0621.rukonmarket.util.MarketTimer;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MarketSpeakerManager {

    private static final MarketManager manager = RukonMarket.inst().getMarketManager();
    private static final String myItemSpeakerGUITitle = "&f&l\uF000\uF018";
    private boolean disabled = false;
    private final List<MarketItem> myItems;
    private InvClass inv;

    public MarketSpeakerManager() {
        myItems = new ArrayList<>();
    }

    public void openMyItems(Player player) {
        this.inv = new InvClass(3, myItemSpeakerGUITitle);

        try {
            manager.getMyItems(player, myItems);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        int slot = 0;

        for (MarketItem item : myItems) {
            inv.setslot(slot, item.getSpeakerMyItem(MarketTimer.getInstance().isCoolDown(player)));
            slot++;
        }
        if (!disabled) player.openInventory(inv.getInv());

    }

}
