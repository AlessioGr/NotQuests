package notquests.notquests.Structs.Objectives;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Quest;
import org.bukkit.entity.EntityType;

public class KillMobsObjective extends Objective {

    private final NotQuests main;
    private final EntityType mobToKill;
    private final int amountToKill;

    public KillMobsObjective(NotQuests main, final Quest quest, final int objectiveID, EntityType mobToKill, int amountToKill) {
        super(main, quest, objectiveID, ObjectiveType.KillMobs, amountToKill);
        this.main = main;
        this.amountToKill = amountToKill;
        this.mobToKill = mobToKill;
    }

    public final EntityType getMobToKill() {
        return mobToKill;
    }

    public final int getAmountToKill() {
        return amountToKill;
    }


}
