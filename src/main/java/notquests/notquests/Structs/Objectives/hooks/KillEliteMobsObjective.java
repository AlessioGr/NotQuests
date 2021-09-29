package notquests.notquests.Structs.Objectives.hooks;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Objectives.Objective;
import notquests.notquests.Structs.Objectives.ObjectiveType;
import notquests.notquests.Structs.Quest;

public class KillEliteMobsObjective extends Objective {

    private final NotQuests main;
    private final String mobToKillType;
    private final int amountToKill;

    public KillEliteMobsObjective(NotQuests main, final Quest quest, final int objectiveID, String mobToKill, int amountToKill) {
        super(main, quest, objectiveID, ObjectiveType.KillEliteMobs, amountToKill);
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
