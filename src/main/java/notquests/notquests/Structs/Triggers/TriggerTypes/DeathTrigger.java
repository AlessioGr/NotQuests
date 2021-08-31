package notquests.notquests.Structs.Triggers.TriggerTypes;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Triggers.Action;
import notquests.notquests.Structs.Triggers.Trigger;

public class DeathTrigger extends Trigger {

    private final NotQuests main;


    public DeathTrigger(final NotQuests main, Action action, int applyOn, String worldName, long amountNeeded) {
        super(main, action, TriggerType.DEATH, applyOn, worldName, amountNeeded);
        this.main = main;
    }





    /*@Override
    public void isCompleted(){

    }*/


}
