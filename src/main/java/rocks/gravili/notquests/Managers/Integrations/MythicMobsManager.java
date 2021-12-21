package rocks.gravili.notquests.Managers.Integrations;

import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.mobs.MythicMob;
import org.bukkit.Location;
import rocks.gravili.notquests.NotQuests;

public class MythicMobsManager {
    private final NotQuests main;
    private MythicMobs mythicMobs;

    public MythicMobsManager(final NotQuests main) {
        this.main = main;
        this.mythicMobs = MythicMobs.inst();
    }

    public MythicMobs getMythicMobs() {
        return mythicMobs;
    }

    public void spawnMob(String mobToSpawnType, Location location, int amount) {
        MythicMob foundMythicMob = mythicMobs.getMobManager().getMythicMob(mobToSpawnType);
        if (foundMythicMob == null) {
            main.getLogManager().warn("Tried to spawn mythic mob, but the mythic mob " + mobToSpawnType + " was not found.");
            return;
        }
        if (location == null) {
            main.getLogManager().warn("Tried to spawn mythic mob, but the spawn location is invalid.");
            return;
        }
        if (location.getWorld() == null) {
            main.getLogManager().warn("Tried to spawn mythic mob, but the spawn location world is invalid.");
            return;
        }

        for (int i = 0; i < amount; i++) {
            mythicMobs.getMobManager().spawnMob(foundMythicMob.getEntityType(), location);
        }


    }
}
