package notquests.notquests.Structs.Objectives;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Quest;
import org.bukkit.inventory.ItemStack;

public class CollectItemsObjective extends Objective {

    private final NotQuests main;
    private final ItemStack itemToCollect;
    private final int amountToCollect;

    public CollectItemsObjective(NotQuests main, final Quest quest, final int objectiveID, ItemStack itemToCollect, int amountToCollect) {
        super(main, quest, objectiveID, ObjectiveType.CollectItems, amountToCollect);
        this.main = main;
        this.itemToCollect = itemToCollect;
        this.amountToCollect = amountToCollect;
    }

    public final ItemStack getItemToCollect() {
        return itemToCollect;
    }

    public final int getAmountToCollect() {
        return amountToCollect;
    }


}
