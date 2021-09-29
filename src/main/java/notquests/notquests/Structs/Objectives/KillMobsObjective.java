package notquests.notquests.Structs.Objectives;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Quest;

public class KillMobsObjective extends Objective {

    private final NotQuests main;
    private final String mobToKillType;
    private final int amountToKill;

    public KillMobsObjective(NotQuests main, final Quest quest, final int objectiveID, String mobToKill, int amountToKill) {
        super(main, quest, objectiveID, ObjectiveType.KillMobs, amountToKill);
        this.main = main;
        this.amountToKill = amountToKill;
        this.mobToKillType = mobToKill;
    }

    public final String getMobToKill() {
        return mobToKillType;
    }

    public final int getAmountToKill() {
        return amountToKill;
    }


}
