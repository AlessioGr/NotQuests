package notquests.notquests.Structs.Triggers.TriggerTypes;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Triggers.Action;
import notquests.notquests.Structs.Triggers.Trigger;

public class WorldEnterTrigger extends Trigger {

    private final NotQuests main;
    private final String worldToEnterName;

    public WorldEnterTrigger(final NotQuests main, Action action, int applyOn, String worldName, long amountNeeded, String worldToEnterName) {
        super(main, action, TriggerType.WORLDENTER, applyOn, worldName, amountNeeded);
        this.main = main;
        this.worldToEnterName = worldToEnterName;

    }

    public final String getWorldToEnterName() {
        return worldToEnterName;
    }


}
