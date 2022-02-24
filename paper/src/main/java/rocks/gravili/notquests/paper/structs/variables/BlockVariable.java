package rocks.gravili.notquests.paper.structs.variables;

import cloud.commandframework.arguments.standard.StringArgument;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.managers.items.NQItem;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class BlockVariable extends Variable<String>{
    public BlockVariable(NotQuests main) {
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

    }

    @Override
    public String getValue(QuestPlayer questPlayer, Object... objects) {
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

        return block.getType().name().toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean setValueInternally(String newValue, QuestPlayer questPlayer, Object... objects) {
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




        final String materialToBreak;
        if (newValue.equalsIgnoreCase("hand")) { //"hand"
            if (questPlayer != null) {
                materialToBreak = questPlayer.getPlayer().getInventory().getItemInMainHand().getType().name();
            } else {
                return false;
            }
        } else {
            if (!newValue.equalsIgnoreCase("any")) {
                materialToBreak = main.getItemsManager().getMaterial(newValue).name();
            }else{
                int rnd = new Random().nextInt(Material.values().length);
                materialToBreak = Material.values()[rnd].name().toLowerCase(Locale.ROOT);
            }
        }
        block.setType(Material.valueOf(materialToBreak));


        return true;
    }


    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
        final List<String> completions = new ArrayList<>();
        for (Material value : Material.values()) {
            completions.add(value.name().toLowerCase());
        }

        for (NQItem nqItem : main.getItemsManager().getItems()) {
            completions.add(nqItem.getItemName());
        }

        completions.add("hand");
        completions.add("any");

        return completions;
    }

    @Override
    public String getPlural() {
        return "Blocks";
    }

    @Override
    public String getSingular() {
        return "Block";
    }
}
