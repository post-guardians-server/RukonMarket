package me.rukon0621.rukonmarket.market;

import me.rukon0621.guardians.data.PlayerData;
import me.rukon0621.guardians.data.TypeData;
import me.rukon0621.guardians.helper.DataBase;
import me.rukon0621.guardians.helper.Msg;
import me.rukon0621.guardians.helper.Pair;
import me.rukon0621.guardians.offlineMessage.OfflineMessageManager;
import me.rukon0621.rukonmarket.RukonMarket;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MarketManager {
    private static final Set<Long> idOnProcess = new HashSet<>();

    public boolean isOnPrecess(long timeID) {
        return idOnProcess.contains(timeID);
    }

    public void setOnProcess(long timeID, boolean bool) {
        if(bool) idOnProcess.add(timeID);
        else idOnProcess.remove(timeID);
    }

    public MarketManager() {
        DataBase db = new DataBase("guardians");
        db.execute("CREATE TABLE IF NOT EXISTS marketData(timeID BIGINT PRIMARY KEY, uuid varchar(36), items blob, price INT, isBought BOOLEAN, name TEXT, level INT, type varchar(36))");
    }

    /**
     * @param timeID 마켓 아이템의 고유 ID
     * @return 장터에 해당 ID의 템이 없으면 NULL, 있으면 그 객체를 반환
     */
    @Nullable
    public MarketItem getMarketItemFromDB(long timeID) throws SQLException {
        DataBase db = new DataBase("guardians");
        ResultSet set = db.executeQuery("SELECT * FROM marketData WHERE timeID = " + timeID);
        MarketItem item = null;
        if(set.next()) item = new MarketItem(set);
        set.close();
        db.close();
        return item;
    }

    public void search(List<MarketItem> items, MarketWindow.SearchType searchType, boolean isReversed, String nameFilter , String typeFilter, @Nullable Pair levelFilter, int page) {
        items.clear();
        boolean useNameFilter = nameFilter != null;
        boolean useTypeFilter = typeFilter != null;
        boolean useLevelFilter = levelFilter != null;
        String realTypeFilter = null;
        if(typeFilter!=null) {
            if(typeFilter.endsWith("-")) realTypeFilter = typeFilter.replaceAll("-", "");
        }
        //필터링 및 탐색
        try {
            StringBuilder s = new StringBuilder("SELECT * FROM marketData WHERE isBought = FALSE AND TimeID > " + System.currentTimeMillis());
            if(useNameFilter) s.append(" AND " + "name LIKE CONCAT('%', '").append(nameFilter).append("'").append(",'%')");
            if(useTypeFilter) {
                if(realTypeFilter==null) {
                    TypeData typeData = TypeData.getType(typeFilter);
                    s.append(" AND type IN(");
                    s.append("'").append(typeFilter).append("'");
                    for(String t : typeData.getChild()) {
                        s.append(",'").append(t).append("'");
                    }
                    s.append(")");
                }
                else {
                    s.append(" AND type = '").append(realTypeFilter).append("'");
                }
            }
            if(useLevelFilter) s.append(" AND level BETWEEN ").append(levelFilter.getFirst()).append(" AND ").append(levelFilter.getSecond());
            int id = (page - 1) * 42;
            s.append(" ORDER BY ");
            s.append(searchType.dbFieldName);
            if(isReversed) s.append(" DESC");
            s.append(" LIMIT ").append(id).append(", ").append(42);

            String sql = s.toString();
            if(sql.contains(";") || sql.contains("\\") || sql.contains("'--") || sql.contains("'#")) return;

            DataBase db = new DataBase("guardians");
            ResultSet set = db.executeQuery(sql);
            while(set.next()) {
                items.add(new MarketItem(set));
            }
            set.close();
            db.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void registerNewItem(Player player, MarketItem item) {
        try {
            setOnProcess(item.getTimeID(), true);
            DataBase db = new DataBase("guardians");
            PreparedStatement statement = db.getConnection().prepareStatement("INSERT INTO marketData VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            statement.setLong(1, item.getTimeID());
            statement.setString(2, player.getUniqueId().toString());
            statement.setBytes(3, item.getItem().serializeAsBytes());
            statement.setInt(4, item.getPrice());
            statement.setBoolean(5, false);
            statement.setString(6, item.getName());
            statement.setInt(7, item.getLevel());
            statement.setString(8, item.getType());
            statement.executeUpdate();
            statement.close();
            db.close();
            setOnProcess(item.getTimeID(), false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void unregisterItem(MarketItem item) {
        setOnProcess(item.getTimeID(), true);
        try {
//            String name = Msg.color(item.getItem().getItemMeta().getDisplayName());
//            OfflinePlayer ofp = Bukkit.getOfflinePlayer(UUID.fromString(item.getUuid()));
//            ArrayList<String> st = OfflineMessageManager.getMessages(ofp);
//            Iterator<String> itr = st.iterator();
//            while(itr.hasNext()) {
//                if(st.contains(name)) {
//                    itr.remove();
//                    break;
//                }
//            }
//            OfflineMessageManager.setMessages(ofp, st);
            DataBase db = new DataBase("guardians");
            PreparedStatement statement = db.getConnection().prepareStatement("DELETE FROM marketData WHERE timeId = ? AND uuid = '" + item.getUuid() + "'");
            statement.setLong(1, item.getTimeID());
            statement.executeUpdate();
            statement.close();
            db.close();
            setOnProcess(item.getTimeID(), false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        setOnProcess(item.getTimeID(), false);
    }

    public boolean buyItem(Player player, long timeID) {
        try {
            MarketItem item = getMarketItemFromDB(timeID);
            if(item==null) {
                Msg.warn(player, "이 아이템은 존재하지 않는 아이템입니다.");
                return false;
            }
            if(item.isBought()) {
                Msg.warn(player, "이 아이템은 이미 판매된 아이템입니다.");
                return false;
            }
            if(item.isOver()) {
                Msg.warn(player, "이 아이템은 판매 기간이 만료된 아이템입니다.");
                return false;
            }
            setOnProcess(timeID, true);
            DataBase db = new DataBase("guardians");
            PreparedStatement statement = db.getConnection().prepareStatement("UPDATE marketData SET isBought = ? WHERE timeID = ? AND uuid = '" + item.getUuid() + "'");
            statement.setBoolean(1, true);
            statement.setLong(2, item.getTimeID());
            statement.executeUpdate();
            statement.close();
            setOnProcess(item.getTimeID(), false);
            db.close();
            PlayerData pdc = new PlayerData(player);
            pdc.setMoney(pdc.getMoney() - item.getPrice());
            new BukkitRunnable() {
                @Override
                public void run() {
                    ItemMeta itemMeta = item.getItem().getItemMeta();
                    String name = Msg.color(itemMeta.getDisplayName());
                    OfflineMessageManager.sendOfflineMessage(item.getUuid(), String.format("&c[ &f%s &c] &6장터에서 아이템이 판매되었습니다. 장터 -> 내 아이템 관리에 들어가서 돈을 받아가세요!",name));
                }
            }.runTask(RukonMarket.inst());
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void getMyItems(Player player, List<MarketItem> items) throws SQLException {
        getMyItems(player.getUniqueId().toString(), items);
    }
    public void getMyItems(String uuid, List<MarketItem> items) throws SQLException {
        items.clear();
        DataBase db = new DataBase("guardians");
        ResultSet set = db.executeQuery(String.format("SELECT * FROM marketData WHERE uuid = '%s'", uuid));
        while (set.next()) {
            items.add(new MarketItem(set));
        }
        set.close();
        db.close();
    }

    public void openMarket(Player player) {
        if(MarketWindow.isPlayerUsingMarket(player)) {
            Msg.warn(player, "이미 장터를 사용중입니다.");
            return;
        }
        new MarketWindow(player);
    }
}
