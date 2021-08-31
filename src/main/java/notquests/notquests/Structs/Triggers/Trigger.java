package notquests.notquests.Structs.Triggers;


import notquests.notquests.Structs.ActiveQuest;
import notquests.notquests.Structs.Triggers.TriggerTypes.TriggerType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Trigger {
    private final TriggerType triggerType; //Enum
    private final Action action; //Class
    private final int applyOn; // 0 is for the whole quest. Positive numbers = objectives (JUST INTERNALLY HERE, NOT IN THE ADMIN COMMAND)
    private final String worldName;
    private final long amountNeeded; // 0 or 1 means every trigger() triggers it

    public Trigger(Action action, TriggerType triggerType, int applyOn, String worldName, long amountNeeded) {

        this.action = action;
        this.triggerType = triggerType;
        this.applyOn = applyOn;
        this.amountNeeded = amountNeeded;
        this.worldName = worldName;
    }

    public final String getWorldName() {
        return worldName;
    }

    public final TriggerType getTriggerType() {
        return triggerType;
    }

    public final Action getTriggerAction() {
        return action;
    }

    public final int getApplyOn() {
        return applyOn;
    }

    public final long getAmountNeeded() {
        return amountNeeded;
    }

    public void trigger(ActiveQuest activeQuest) { //or void completeTrigger() or finishTrigger()
        //execute action here
        final Player player = Bukkit.getPlayer(activeQuest.getQuestPlayer().getUUID());

        if (player != null) {
            action.execute(player, activeQuest);
        } else {
            System.out.println("§eNotQuests > Tried to execute trigger for offline player - ABORTED!");
        }

    }

}
