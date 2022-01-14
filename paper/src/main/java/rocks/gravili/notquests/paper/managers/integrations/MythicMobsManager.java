package rocks.gravili.notquests.paper.managers.integrations;

import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.api.exceptions.InvalidMobTypeException;
import io.lumine.xikage.mythicmobs.mobs.MythicMob;
import org.bukkit.Location;
import rocks.gravili.notquests.paper.NotQuests;

import java.util.Collection;

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

    public final Collection<String> getMobNames() {
        return mythicMobs.getMobManager().getMobNames();
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


        try {
            for (int i = 0; i < amount; i++) {
                mythicMobs.getAPIHelper().spawnMythicMob(foundMythicMob, location, 1);
            }
        } catch (InvalidMobTypeException e) {
            main.getLogManager().warn("Tried to spawn mythic mob, but the mythic mob " + mobToSpawnType + " is invalid.");
        }


    }

    public final boolean isMythicMob(final String mobToSpawnType) {
        return mythicMobs.getMobManager().getMythicMob(mobToSpawnType) != null;
    }
}
