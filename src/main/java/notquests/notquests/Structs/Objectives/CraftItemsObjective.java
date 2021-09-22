package notquests.notquests.Structs.Objectives;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Quest;
import org.bukkit.inventory.ItemStack;

public class CraftItemsObjective extends Objective {

    private final NotQuests main;
    private final ItemStack itemToCraft;
    private final int amountToCraft;

    public CraftItemsObjective(NotQuests main, final Quest quest, final int objectiveID, ItemStack itemToCraft, int amountToCraft) {
        super(main, quest, objectiveID, ObjectiveType.CraftItems, amountToCraft);
        this.main = main;
        this.itemToCraft = itemToCraft;
        this.amountToCraft = amountToCraft;
    }

    public final ItemStack getItemToCraft() {
        return itemToCraft;
    }

    public final int getAmountToCraft() {
        return amountToCraft;
    }


}
