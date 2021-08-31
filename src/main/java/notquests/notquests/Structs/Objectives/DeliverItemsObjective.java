package notquests.notquests.Structs.Objectives;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Quest;
import org.bukkit.inventory.ItemStack;

public class DeliverItemsObjective extends Objective {

    private final NotQuests main;
    private final ItemStack itemToCollect;
    private final int amountToCollect;
    private final int recipientNPCID;

    public DeliverItemsObjective(NotQuests main, final Quest quest, final int objectiveID, final ItemStack itemToCollect, final int amountToCollect, final int recipientNPCID) {
        super(main, quest, objectiveID, ObjectiveType.DeliverItems, amountToCollect);
        this.main = main;
        this.itemToCollect = itemToCollect;
        this.amountToCollect = amountToCollect;
        this.recipientNPCID = recipientNPCID;
    }

    public final ItemStack getItemToCollect() {
        return itemToCollect;
    }

    public final int getAmountToCollect() {
        return amountToCollect;
    }

    public final int getRecipientNPCID() {
        return recipientNPCID;
    }


}