package me.rukon0621.rukonmarket;

import me.rukon0621.guardians.offlineMessage.OfflineMessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MarketCommand implements CommandExecutor {

    public MarketCommand() {
        RukonMarket.inst().getCommand("marketCommand").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        for(Player p : Bukkit.getServer().getOnlinePlayers()) {
            OfflineMessageManager.sendOfflineMessage(p, String.format("&c[ &f와! &c] &6오프라인 메세지 난사"));
        }

        /*
        DataBase db = new DataBase("guardians");
        ResultSet set = db.executeQuery("SELECT * FROM marketData");
        Map<String, List<ItemStack>> map = new HashMap<>();
        try {
            while(set.next()) {
                MarketItem item = new MarketItem(set);
                if(!map.containsKey(item.getUuid())) map.put(item.getUuid(), new ArrayList<>());
                if(item.isBought()) {
                    map.get(item.getUuid()).add(RukonPayment.inst().getPaymentManager().getMoneyItem(item.getPrice()));
                }
                else map.get(item.getUuid()).add(item.getItem());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        for(String uuid : map.keySet()) {
            OfflinePlayer ofp = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
            System.out.println(ofp + " : 전송 완료");
            MailBoxManager.sendAll(ofp, map.get(uuid));
        }

        System.out.println("MARKET DATA FIN");
         */

        return true;
    }
}
