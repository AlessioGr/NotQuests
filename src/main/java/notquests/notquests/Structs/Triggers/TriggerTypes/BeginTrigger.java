package notquests.notquests.Structs.Triggers.TriggerTypes;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Triggers.Action;
import notquests.notquests.Structs.Triggers.Trigger;

public class BeginTrigger extends Trigger {

    private final NotQuests main;

    public BeginTrigger(final NotQuests main, Action action, int applyOn, String worldName) {
        super(action, TriggerType.BEGIN, applyOn, worldName, 1);
        this.main = main;
    }




    /*@Override
    public void isCompleted(){

    }*/


}