package notquests.notquests.Structs.Objectives;


import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Quest;

public class TriggerCommandObjective extends Objective {

    private final NotQuests main;
    private final String triggerName;


    public TriggerCommandObjective(NotQuests main, final Quest quest, final int objectiveID, String triggerName, int amountToTrigger) {
        super(main, quest, objectiveID, ObjectiveType.TriggerCommand, amountToTrigger);
        this.triggerName = triggerName;
        this.main = main;
    }

    public final String getTriggerName() {
        return triggerName;
    }


}
