package notquests.notquests.Managers;

import notquests.notquests.NotQuests;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class UtilManager {
    private final NotQuests main;

    public UtilManager( NotQuests main) {
        this.main = main;
    }

    public final OfflinePlayer getOfflinePlayer(final String playerName) {
        return Bukkit.getOfflinePlayer(playerName);
    }
}
