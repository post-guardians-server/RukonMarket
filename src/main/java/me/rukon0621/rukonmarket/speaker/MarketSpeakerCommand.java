package me.rukon0621.rukonmarket.speaker;

import me.rukon0621.guardians.data.PlayerData;
import me.rukon0621.guardians.helper.InvClass;
import me.rukon0621.guardians.helper.ItemClass;
import me.rukon0621.guardians.helper.Msg;
import me.rukon0621.guardians.offlineMessage.OfflineMessageManager;
import me.rukon0621.pay.PaymentData;
import me.rukon0621.rukonmarket.RukonMarket;
import me.rukon0621.rukonmarket.market.MarketItem;
import me.rukon0621.rukonmarket.market.MarketManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static me.rukon0621.guardians.main.pfix;


public class MarketSpeakerCommand implements CommandExecutor {

    public MarketSpeakerCommand() {
        RukonMarket.inst().getCommand("장터글").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

        Player player = (Player) commandSender;
        new MarketSpeakerManager().openMyItems(player);
        player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1.5f);

        return false;
    }

}
