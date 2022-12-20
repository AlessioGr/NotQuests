package rocks.gravili.notquests.paper.structs.variables;

import cloud.commandframework.ArgumentDescription;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class EnderChestVariable extends Variable<ItemStack[]>{
    public EnderChestVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);

        addRequiredBooleanFlag(
                main.getCommandManager().getPaperCommandManager().flagBuilder("addToInventoryIfEnderChestFull")
                        .withDescription(ArgumentDescription.of("Puts the item in the player's inventory if their enderchest is full")).build()
        );
        addRequiredBooleanFlag(
                main.getCommandManager().getPaperCommandManager().flagBuilder("skipItemIfEnderChestFull")
                        .withDescription(ArgumentDescription.of("Does not drop the item if enderchest full if flag set")).build()
        );
    }

    @Override
    public ItemStack[] getValueInternally(QuestPlayer questPlayer, Object... objects) {
        return questPlayer.getPlayer().getEnderChest().getContents();
    }

    @Override
    public boolean setValueInternally(ItemStack[] newValue, QuestPlayer questPlayer, Object... objects) {
        if (getRequiredBooleanValue("add", questPlayer)) {

            HashMap<Integer, ItemStack> left = questPlayer.getPlayer().getEnderChest().addItem(newValue);
            if(!left.isEmpty()) {

                if(!getRequiredBooleanValue("addToInventoryIfEnderChestFull", questPlayer)){
                    if (!getRequiredBooleanValue("skipItemIfEnderChestFull", questPlayer)) {
                        for (ItemStack leftItemStack : left.values()) {
                            questPlayer
                                    .getPlayer()
                                    .getWorld()
                                    .dropItem(questPlayer.getPlayer().getLocation(), leftItemStack);
                        }
                    }
                }else {
                    left = questPlayer.getPlayer().getInventory().addItem(left.values().toArray(new ItemStack[0]));
                    if(!left.isEmpty()) {
                        if (!getRequiredBooleanValue("skipItemIfEnderChestFull", questPlayer)) {
                            for (ItemStack leftItemStack : left.values()) {
                                questPlayer
                                        .getPlayer()
                                        .getWorld()
                                        .dropItem(questPlayer.getPlayer().getLocation(), leftItemStack);
                            }
                        }
                    }
                }


            }

        } else if (getRequiredBooleanValue("remove", questPlayer)) {
            questPlayer.getPlayer().getEnderChest().removeItemAnySlot(newValue);
        } else {
            questPlayer.getPlayer().getEnderChest().setContents(newValue);
        }


        return true;
    }


    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "EnderChest Inventory";
    }

    @Override
    public String getSingular() {
        return "EnderChest Inventory";
    }
}