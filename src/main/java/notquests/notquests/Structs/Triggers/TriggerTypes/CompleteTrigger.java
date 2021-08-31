package notquests.notquests.Structs.Triggers.TriggerTypes;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Triggers.Action;
import notquests.notquests.Structs.Triggers.Trigger;

public class CompleteTrigger extends Trigger {

    private final NotQuests main;

    public CompleteTrigger(final NotQuests main, Action action, int applyOn, String worldName) {
        super(action, TriggerType.COMPLETE, applyOn, worldName, 1);
        this.main = main;
    }




    /*@Override
    public void isCompleted(){

    }*/


}