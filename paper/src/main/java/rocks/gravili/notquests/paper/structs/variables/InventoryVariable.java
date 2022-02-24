package rocks.gravili.notquests.paper.structs.variables;

import cloud.commandframework.ArgumentDescription;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.HashMap;
import java.util.List;

public class InventoryVariable extends Variable<ItemStack[]>{
    public InventoryVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);
        addRequiredBooleanFlag(
                main.getCommandManager().getPaperCommandManager().flagBuilder("skipItemIfInventoryFull")
                        .withDescription(ArgumentDescription.of("Does not drop the item if inventory full if flag set")).build()
        );
    }

    @Override
    public ItemStack[] getValue(QuestPlayer questPlayer, Object... objects) {
        return questPlayer.getPlayer().getInventory().getContents();
    }

    @Override
    public boolean setValueInternally(ItemStack[] newValue, QuestPlayer questPlayer, Object... objects) {
        if (getRequiredBooleanValue("add", questPlayer)) {

            HashMap<Integer, ItemStack> left = questPlayer.getPlayer().getInventory().addItem(newValue);
            if (!getRequiredBooleanValue("skipItemIfInventoryFull", questPlayer)) {
                for (ItemStack leftItemStack : left.values()) {
                    questPlayer.getPlayer().getWorld().dropItem(questPlayer.getPlayer().getLocation(), leftItemStack);
                }
            }
        } else if (getRequiredBooleanValue("remove", questPlayer)) {

            questPlayer.getPlayer().getInventory().removeItemAnySlot(newValue);
        } else {
            questPlayer.getPlayer().getInventory().setContents(newValue);
        }
        return true;
    }


    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "Inventory";
    }

    @Override
    public String getSingular() {
        return "Inventory";
    }
}
