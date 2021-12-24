package rocks.gravili.notquests.paper.managers.integrations;

import me.ulrich.clans.packets.interfaces.UClans;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

public class UltimateClansManager {
    private final NotQuests main;
    private final UClans api;

    public UltimateClansManager(final NotQuests main) {
        this.main = main;
        api = (UClans) Bukkit.getPluginManager().getPlugin("UltimateClans");
    }

    public final UClans getApi() {
        return api;
    }

    public final boolean isInClanWithMinLevel(final Player player, final long minLevel) {
        if (api.getPlayerAPI().getPlayerClan(player.getUniqueId()) != null && api.getPlayerAPI().getPlayerClan(player.getUniqueId()).getLevel() >= minLevel) {
            return true;
        }
        return false;
    }
}
