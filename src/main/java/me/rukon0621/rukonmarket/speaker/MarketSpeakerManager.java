package me.rukon0621.rukonmarket.speaker;

import me.rukon0621.callback.speaker.Speaker;
import me.rukon0621.callback.speaker.SpeakerListenEvent;
import me.rukon0621.guardians.helper.Msg;
import me.rukon0621.gui.buttons.Button;
import me.rukon0621.gui.windows.Window;
import me.rukon0621.rukonmarket.RukonMarket;
import me.rukon0621.rukonmarket.listener.ChatListener;
import me.rukon0621.rukonmarket.market.MarketItem;
import me.rukon0621.rukonmarket.market.MarketManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static me.rukon0621.guardians.main.pfix;

public class MarketSpeakerManager implements Listener {

    private static final MarketManager manager = RukonMarket.inst().getMarketManager();
    private final Set<UUID> blocker = new HashSet<>();

    public MarketSpeakerManager() {
        Bukkit.getServer().getPluginManager().registerEvents(this, RukonMarket.inst());
    }

    public void openSelectingWindow(Player player) {
        if(!blocker.add(player.getUniqueId())) {
            Msg.warn(player, "잠시 기다리고 다시 시도해주세요.");
            return;
        }
        List<MarketItem> myItems = new ArrayList<>();
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    manager.getMyItems(player, myItems);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        blocker.remove(player.getUniqueId());
                        new ItemWindow(player, myItems);
                    }
                }.runTask(RukonMarket.inst());
            }
        }.runTaskAsynchronously(RukonMarket.inst());
    }

    private class ItemWindow extends Window {

        public ItemWindow(Player player, List<MarketItem> items) {
            super(player, "&f\uF000", 3);
            int i = 0;
            for(MarketItem item : items) {
                map.put(i, new Button() {
                    @Override
                    public void execute(Player player, ClickType clickType) {
                        player.closeInventory();
                        player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1.3f);
                        speak(player, String.valueOf(item.getPrice()), item.getName());
                    }

                    @Override
                    public ItemStack getIcon() {
                        return item.getIcon(false);
                    }
                });
                i++;
            }
            reloadGUI();
            open();
        }

        @Override
        public void close(boolean b) {
            disable();
            if(b) player.closeInventory();
        }
    }

    @EventHandler
    public void onListenSpeakerEvent(SpeakerListenEvent e) {
        if (e.getMainAction().equals("markerSpeaker")) {
            String name = e.getIn().readUTF();
            String itemName = e.getIn().readUTF();
            String itemInfo = Msg.uncolor(Msg.color(e.getIn().readUTF()));
            String price = e.getIn().readUTF();
            Bukkit.getOnlinePlayers().forEach(p -> {
                        Msg.send(p, " ");
                        Msg.send(p, "&e" + name + "님&f이 아이템을 판매합니다! &7(&f" + itemName + " &8| &f" + price + "디나르&7)", pfix);
                        Msg.send(p, itemInfo);
                        Msg.send(p, " ");
                    }
            );
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        blocker.remove(e.getPlayer().getUniqueId());
    }

    public void speak(Player player, String price, String itemName) {
        if(!blocker.add(player.getUniqueId())) {
            Msg.warn(player, "이미 장터 홍보를 진행하고 있습니다.");
            return;
        }
        else if(MarketTimer.getInstance().getCoolDown(player) > 0) {
            Msg.warn(player, "장터 홍보를 다시 하려면 잠시 기다려야합니다.");
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                CountDownLatch latch = new CountDownLatch(1);
                ChatListener listener = new ChatListener(player, latch, 10);
                Msg.send(player, "아이템 설명을 써주세요! 10초 안에 아이템을 써주셔야합니다. (40자 이내)", pfix);
                try {
                    latch.await();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        blocker.remove(player.getUniqueId());
                        String itemInfo = listener.getAnswer();
                        if (itemInfo == null) return;

                        if(itemInfo.length() >  40) {
                            Msg.warn(player, "40자 이내로 작성해주셔야 합니다.");
                            return;
                        }

                        MarketTimer.getInstance().addPlayer(player);
                        try {
                            new Speaker("markerSpeaker", player.getName(), itemName, itemInfo, price);
                            player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1.5f);
                        } catch (Exception e) {
                            Bukkit.getLogger().info(String.valueOf(e));
                        }
                    }
                }.runTask(RukonMarket.inst());
            }
        }.runTaskAsynchronously(RukonMarket.inst());
    }

}
