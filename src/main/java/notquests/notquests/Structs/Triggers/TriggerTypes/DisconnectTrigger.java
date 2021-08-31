package notquests.notquests.Structs.Triggers.TriggerTypes;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Triggers.Action;
import notquests.notquests.Structs.Triggers.Trigger;

public class DisconnectTrigger extends Trigger {

    private final NotQuests main;

    public DisconnectTrigger(final NotQuests main, Action action, int applyOn, String worldName) {
        super(action, TriggerType.DISCONNECT, applyOn, worldName, 1);
        this.main = main;
    }




    /*@Override
    public void isCompleted(){

    }*/


}