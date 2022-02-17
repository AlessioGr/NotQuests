package rocks.gravili.notquests.paper.structs.variables;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.standard.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ContainerInventoryVariable extends Variable<ItemStack[]>{
    public ContainerInventoryVariable(NotQuests main) {
        super(main);
        setCanSetValue(true);

        addRequiredString(
                StringArgument.<CommandSender>newBuilder("world").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[World Name]", "[...]");

                            ArrayList<String> suggestions = new ArrayList<>();
                            for(World world : Bukkit.getWorlds()){
                                suggestions.add(world.getName());
                            }
                            return suggestions;

                        }
                ).single().build()
        );
        addRequiredNumber(
                NumberVariableValueArgument.<CommandSender>newBuilder("x", main, null).build()
        );
        addRequiredNumber(
                NumberVariableValueArgument.<CommandSender>newBuilder("y", main, null).build()
        );
        addRequiredNumber(
                NumberVariableValueArgument.<CommandSender>newBuilder("z", main, null).build()
        );

        addRequiredBooleanFlag(
                main.getCommandManager().getPaperCommandManager().flagBuilder("skipItemIfInventoryFull")
                        .withDescription(ArgumentDescription.of("Does not drop the item if inventory full if flag set")).build()
        );
    }

    @Override
    public ItemStack[] getValue(Player player, Object... objects) {
        String worldName = getRequiredStringValue("world");
        World world = Bukkit.getWorld(worldName);
        double x = getRequiredNumberValue("x", player);
        double y = getRequiredNumberValue("y", player);
        double z = getRequiredNumberValue("z", player);
        if(world == null){
            main.getLogManager().warn("Error: cannot get value of chest inventory variable, because the world " + worldName + " does not exist.");
            return null;
        }

        Location location = new Location(world, x, y, z);
        Block block = location.getBlock();

        if(block.getState() instanceof Container container){
            return container.getInventory().getStorageContents();

        }else{
            main.getLogManager().warn("Error: cannot get value of chest inventory variable, because the location does not have a container block. Real type: " + block.getType().name());
            return new ItemStack[0];
        }

    }

    @Override
    public boolean setValueInternally(ItemStack[] newValue, Player player, Object... objects) {
        String worldName = getRequiredStringValue("world");
        World world = Bukkit.getWorld(worldName);
        double x = getRequiredNumberValue("x", player);
        double y = getRequiredNumberValue("y", player);
        double z = getRequiredNumberValue("z", player);
        if(world == null){
            main.getLogManager().warn("Error: cannot set value of chest inventory variable, because the world " + worldName + " does not exist.");
            return false;
        }

        Location location = new Location(world, x, y, z);
        Block block = location.getBlock();

        if(block.getState() instanceof Container container){
            if(getRequiredBooleanValue("add", player)){

                HashMap<Integer, ItemStack> left =  container.getInventory().addItem(newValue);
                if(!getRequiredBooleanValue("skipItemIfInventoryFull", player)){
                    for(ItemStack leftItemStack : left.values()){
                        world.dropItem(location, leftItemStack);
                    }
                }
            }else if(getRequiredBooleanValue("remove", player)){
                container.getInventory().removeItemAnySlot(newValue);
            }else{
                container.getInventory().setContents(newValue);
            }
        }else{
            main.getLogManager().warn("Error: cannot set value of chest inventory variable, because the location does not have a container block. Real type: " + block.getType().name());
            return false;
        }

        return true;
    }


    @Override
    public List<String> getPossibleValues(Player player, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "Container Inventory";
    }

    @Override
    public String getSingular() {
        return "Container Inventory";
    }
}
