package notquests.notquests.Structs.Objectives;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Quest;
import org.bukkit.inventory.ItemStack;

public class ConsumeItemsObjective extends Objective {

    private final NotQuests main;
    private final ItemStack itemToConsume;
    private final int amountToConsume;

    public ConsumeItemsObjective(NotQuests main, final Quest quest, final int objectiveID, ItemStack itemToConsume, int amountToConsume) {
        super(main, quest, objectiveID, ObjectiveType.ConsumeItems, amountToConsume);
        this.main = main;
        this.itemToConsume = itemToConsume;
        this.amountToConsume = amountToConsume;
    }

    public final ItemStack getItemToConsume() {
        return itemToConsume;
    }

    public final int getAmountToConsume() {
        return amountToConsume;
    }


}
