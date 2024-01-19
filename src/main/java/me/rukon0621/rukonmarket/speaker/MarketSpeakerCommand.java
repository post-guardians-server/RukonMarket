package me.rukon0621.rukonmarket.speaker;

import me.rukon0621.guardians.helper.Msg;
import me.rukon0621.rukonmarket.RukonMarket;
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
        long cool = MarketTimer.getInstance().getCoolDown(player);
        if(cool > 0) {
            Msg.warn(player, String.format("&f다시 홍보글을 사용하려면 &b%d초&f를 기다려야 합니다.", cool), pfix);
            return true;
        }
        RukonMarket.inst().getSpeakerManager().openSelectingWindow(player);
        player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1.5f);
        return true;
    }

}
