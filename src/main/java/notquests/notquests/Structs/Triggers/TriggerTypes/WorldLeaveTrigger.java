package notquests.notquests.Structs.Triggers.TriggerTypes;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Triggers.Action;
import notquests.notquests.Structs.Triggers.Trigger;

public class WorldLeaveTrigger extends Trigger {

    private final NotQuests main;
    private final String worldToLeaveName;

    public WorldLeaveTrigger(final NotQuests main, Action action, int applyOn, String worldName, long amountNeeded, String worldToLeaveName) {
        super(main, action, TriggerType.WORLDLEAVE, applyOn, worldName, amountNeeded);
        this.main = main;
        this.worldToLeaveName = worldToLeaveName;

    }

    public final String getWorldToLeaveName() {
        return worldToLeaveName;
    }


}