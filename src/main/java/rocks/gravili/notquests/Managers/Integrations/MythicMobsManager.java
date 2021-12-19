package rocks.gravili.notquests.Managers.Integrations;

import io.lumine.xikage.mythicmobs.MythicMobs;
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
}
