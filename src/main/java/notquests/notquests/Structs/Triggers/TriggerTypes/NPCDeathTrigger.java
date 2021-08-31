package notquests.notquests.Structs.Triggers.TriggerTypes;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Triggers.Action;
import notquests.notquests.Structs.Triggers.Trigger;

public class NPCDeathTrigger extends Trigger {

    private final NotQuests main;
    private final int npcToDieID;

    public NPCDeathTrigger(final NotQuests main, Action action, int applyOn, String worldName, long amountNeeded, int npcToDieID) {
        super(action, TriggerType.NPCDEATH, applyOn, worldName, amountNeeded);
        this.main = main;
        this.npcToDieID = npcToDieID;

    }

    public final int getNpcToDieID() {
        return npcToDieID;
    }




    /*@Override
    public void isCompleted(){

    }*/


}
