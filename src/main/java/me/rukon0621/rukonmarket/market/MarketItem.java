package me.rukon0621.rukonmarket.market;

import me.rukon0621.guardians.helper.DateUtil;
import me.rukon0621.guardians.helper.InvClass;
import me.rukon0621.guardians.helper.ItemClass;
import me.rukon0621.guardians.helper.Msg;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MarketItem {
    private final String uuid;
    private final ItemStack item;
    private final long time; //만료 시기 (고유 번호)
    private final int price;
    private boolean bought;
    private final String type;
    private final String name;
    private final int level;

    public MarketItem(ResultSet set) throws SQLException {
        this.time = set.getLong(1);
        this.uuid = set.getString(2);
        this.item = ItemStack.deserializeBytes(set.getBytes(3));
        this.price = set.getInt(4);
        this.bought = set.getBoolean(5);
        this.name = set.getString(6);
        this.level = set.getInt(7);
        this.type = set.getString(8);
    }

    public MarketItem(long time, String uuid, ItemStack item, int price, boolean bought, String pureName, int level, String type) {
        this.price = price;
        this.uuid = uuid;
        this.item = item;
        this.time = time;
        this.bought = bought;
        this.name = pureName;
        this.level = level;
        this.type = type;
    }

    public ItemStack getIcon(boolean isOp) {
        ItemClass item = new ItemClass(new ItemStack(this.item));
        item.addLore(" ");
        item.addLore(String.format("&6\uE015\uE00C\uE00C가격: %d 디나르", price));
        item.addLore("&c\uE011\uE00C\uE00C등록 만료일까지 " + DateUtil.formatDate((time - System.currentTimeMillis()) / 1000L));

        //if(isOp) {
        //    item.addLore("&e\uE011\uE00C\uE00C등록인: " + Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName());
        //}

        item.addLore(" ");
        item.addLore("&6\uE015\uE00C\uE00C아이템을 구매하려면 쉬프트 우클릭하십시오.");
        return item.getItem();
    }

    public ItemStack getSpeakerMyItem(long seconde) {
        ItemClass item = new ItemClass(new ItemStack(this.item));
        item.addLore(" ");
        item.addLore(String.format("&e가격: %d 디나르", price));
        item.addLore(" ");
        item.addLore("&e※ " + (seconde == 0 ? "지금 사용가능합니다." : "홍보 쿨타임 : " + DateUtil.formatDate(seconde)));
        item.addLore("&c※ 홍보를 진행할려면 &4쉬프트 우클릭&c 하십시오.");
        return item.getItem();
    }

    public ItemStack getIconOnMyPage() {
        ItemClass item = new ItemClass(new ItemStack(this.item));
        item.addLore(" ");
        item.addLore(String.format("&e가격: %d 디나르", price));
        item.addLore(" ");
        if (isBought()) {
            item.addLore("&a※ 아이템 거래가 완료되었습니다! 클릭하여 돈을 지급 받으십시오.");
        } else if (isOver()) {
            item.addLore("&c※ 아이템이 만료되었습니다. &4쉬프트 우클릭&c하여 아이템을 회수 하십시오.");
        } else {
            item.addLore("&e※ 등록 만료일까지 " + DateUtil.formatDate((time - System.currentTimeMillis()) / 1000L));
            item.addLore("&c※ 등록을 철회하려면 &4쉬프트 우클릭&c 하십시오.");
        }
        return item.getItem();
    }

    /**
     * @return 등록 기간이 만료되었는가
     */
    public boolean isOver() {
        return time <= System.currentTimeMillis();
    }

    public void give(Player player) {
        if (!InvClass.giveOrDrop(player, item)) {
            Msg.warn(player, "인벤토리에 공간이 부족하여 아이템이 메일로 전송되었습니다.");
        }
    }

    public void setBought(boolean bool) {
        bought = bool;
    }

    public String getUuid() {
        return uuid;
    }

    public ItemStack getItem() {
        return item;
    }

    public boolean isBought() {
        return bought;
    }

    public long getTimeID() {
        return time;
    }

    public int getPrice() {
        return price;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public String getType() {
        return type;
    }
}
