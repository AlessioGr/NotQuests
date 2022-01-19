package rocks.gravili.notquests.paper.structs.variables;

import cloud.commandframework.ArgumentDescription;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;

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
    public ItemStack[] getValue(Player player, Object... objects) {
        return player.getInventory().getContents();
    }

    @Override
    public boolean setValueInternally(ItemStack[] newValue, Player player, Object... objects) {
        if(getRequiredBooleanValue("add")){

            HashMap<Integer, ItemStack> left =  player.getInventory().addItem(newValue);
            if(!getRequiredBooleanValue("skipItemIfInventoryFull")){
                for(ItemStack leftItemStack : left.values()){
                    player.getWorld().dropItem(player.getLocation(), leftItemStack);
                }
            }
        }else if(getRequiredBooleanValue("remove")){

            player.getInventory().removeItemAnySlot(newValue);
        }else{
            player.getInventory().setContents(newValue);
        }
        return true;
    }


    @Override
    public List<String> getPossibleValues(Player player, Object... objects) {
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
