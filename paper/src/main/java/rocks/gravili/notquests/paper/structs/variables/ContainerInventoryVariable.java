/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.paper.structs.variables;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.standard.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

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
    public ItemStack[] getValue(QuestPlayer questPlayer, Object... objects) {
        String worldName = getRequiredStringValue("world");
        World world = Bukkit.getWorld(worldName);
        double x = getRequiredNumberValue("x", questPlayer);
        double y = getRequiredNumberValue("y", questPlayer);
        double z = getRequiredNumberValue("z", questPlayer);
        if (world == null) {
            main.getLogManager().warn("Error: cannot get value of chest inventory variable, because the world " + worldName + " does not exist.");
            return null;
        }

        Location location = new Location(world, x, y, z);
        Block block = location.getBlock();

        if (block.getState() instanceof Container container) {
            return container.getInventory().getStorageContents();

        }else{
            main.getLogManager().warn("Error: cannot get value of chest inventory variable, because the location does not have a container block. Real type: " + block.getType().name());
            return new ItemStack[0];
        }

    }

    @Override
    public boolean setValueInternally(ItemStack[] newValue, QuestPlayer questPlayer, Object... objects) {
        String worldName = getRequiredStringValue("world");
        World world = Bukkit.getWorld(worldName);
        double x = getRequiredNumberValue("x", questPlayer);
        double y = getRequiredNumberValue("y", questPlayer);
        double z = getRequiredNumberValue("z", questPlayer);
        if (world == null) {
            main.getLogManager().warn("Error: cannot set value of chest inventory variable, because the world " + worldName + " does not exist.");
            return false;
        }

        Location location = new Location(world, x, y, z);
        Block block = location.getBlock();

        if (block.getState() instanceof Container container) {
            if (getRequiredBooleanValue("add", questPlayer)) {

                HashMap<Integer, ItemStack> left = container.getInventory().addItem(newValue);
                if (!getRequiredBooleanValue("skipItemIfInventoryFull", questPlayer)) {
                    for (ItemStack leftItemStack : left.values()) {
                        world.dropItem(location, leftItemStack);
                    }
                }
            } else if (getRequiredBooleanValue("remove", questPlayer)) {
                container.getInventory().removeItemAnySlot(newValue);
            } else {
                container.getInventory().setContents(newValue);
            }
        }else{
            main.getLogManager().warn("Error: cannot set value of chest inventory variable, because the location does not have a container block. Real type: " + block.getType().name());
            return false;
        }

        return true;
    }


    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
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
