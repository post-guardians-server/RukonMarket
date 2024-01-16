package me.rukon0621.rukonmarket.speaker;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.rukon0621.callback.ProxyCallBack;
import me.rukon0621.callback.speaker.Speaker;
import me.rukon0621.callback.speaker.SpeakerListenEvent;
import me.rukon0621.guardians.helper.Msg;
import me.rukon0621.rukonmarket.RukonMarket;
import me.rukon0621.rukonmarket.listener.ChatListener;
import me.rukon0621.rukonmarket.util.MarketTimer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.rukon0621.guardians.main.pfix;

public class MarketSpeakerListener implements Listener {

    private static final String myItemSpeakerGUITitle = "&f&l\uF000\uF018";
    private static final RukonMarket plugin = RukonMarket.inst();

    public MarketSpeakerListener() {
        RukonMarket.inst().getServer().getPluginManager().registerEvents(this, RukonMarket.inst());
    }

    @EventHandler
    public void invClickEvent(InventoryClickEvent e) {

        Player p = (Player) e.getWhoClicked();

        if (e.getRawSlot() == -999) {
            p.playSound(p, Sound.UI_BUTTON_CLICK, 1, 1.3f);
            p.closeInventory();
            return;
        }

        if (Msg.recolor(e.getView().getTitle()).equals(myItemSpeakerGUITitle)) {
            e.setCancelled(true);
            if (e.getClick().equals(ClickType.SHIFT_RIGHT)) {

                ItemStack clickedItem = e.getCurrentItem();

                if (clickedItem != null && clickedItem.getType() != Material.AIR) {

                    p.closeInventory();
                    ItemMeta meta = clickedItem.getItemMeta();

                    String itemName = meta.getDisplayName();
                    int price = getItemPrice(meta.getLore());

                    marketSpeaker(p, String.valueOf(price), itemName);

                }

            }
        }

    }

    @EventHandler
    public void markerChat(SpeakerListenEvent e) {

        if (e.getMainAction().equals("markerSpeaker")) {
            String itemName = e.getIn().readUTF();
            String itemInfo = e.getIn().readUTF();
            String price = e.getIn().readUTF();

            Bukkit.getOnlinePlayers().forEach(p ->
                    Msg.send(p, itemName + " : " + itemInfo + " : " + price)
            );
        }

    }

    public void marketSpeaker(Player p, String price, String itemName) {

        new BukkitRunnable() {
            @Override
            public void run() {
                CountDownLatch latch = new CountDownLatch(1);
                ChatListener listener = new ChatListener(p, latch, 15);

                Msg.warn(p, "아이템 설명을 써주세요!", pfix);

                try {
                    latch.await();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }

                String itemInfo = listener.getAnswer();

                if (itemInfo != null) {
                    try {
                        if (checkTimerPlayer(p)) {
                            new Speaker("markerSpeaker", itemName, itemInfo, price);
                        } else {
                            Msg.send(p, "쿨타임이 안됨..", pfix);
                        }
                    } catch (Exception e) {
                        Bukkit.getLogger().info(String.valueOf(e));
                    }
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private boolean checkTimerPlayer(Player p) {
        MarketTimer marketTimer = MarketTimer.getInstance();

        if (marketTimer.isCoolDown(p) == 0L) {
            marketTimer.addPlayer(p);
            return true;
        } else {
            return false;
        }

    }

    private int getItemPrice(List<String> lore) {

        int price = 0;

        for (String line : lore) {
            if (line.contains("가격")) {
                String strippedLine = ChatColor.stripColor(line);
                Matcher matcher = Pattern.compile("\\d+").matcher(strippedLine);
                if (matcher.find()) {
                    price = Integer.parseInt(matcher.group());
                    break;
                }
            }
        }
        return price;
    }
}
