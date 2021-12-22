/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021 Alessio Gravili
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

package rocks.gravili.notquests.structs.objectives;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.bukkit.parsers.MaterialArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.structs.ActiveObjective;

public class PlaceBlocksObjective extends Objective {

    private Material blockToPlace;
    private boolean deductIfBlockIsBroken = true;

    public PlaceBlocksObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("PlaceBlocks")
                .argument(MaterialArgument.of("material"), ArgumentDescription.of("Material of the block which needs to be place."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of blocks which need to be placed"))
                .flag(
                        manager.flagBuilder("doNotDeductIfBlockIsBroken")
                                .withDescription(ArgumentDescription.of("Makes it so Quest progress is not removed if the block is broken"))
                )
                .meta(CommandMeta.DESCRIPTION, "Adds a new PlaceBlocks Objective to a quest")
                .handler((context) -> {
                    final Material material = context.get("material");
                    final int amount = context.get("amount");
                    final boolean deductIfBlockIsBroken = !context.flags().isPresent("doNotDeductIfBlockIsBroken");

                    PlaceBlocksObjective placeBlocksObjective = new PlaceBlocksObjective(main);
                    placeBlocksObjective.setBlockToPlace(material);
                    placeBlocksObjective.setDeductIfBlockIsBroken(deductIfBlockIsBroken);
                    placeBlocksObjective.setProgressNeeded(amount);

                    main.getObjectiveManager().addObjective(placeBlocksObjective, context);
                }));
    }

    public void setBlockToPlace(final Material blockToPlace) {
        this.blockToPlace = blockToPlace;
    }

    public void setDeductIfBlockIsBroken(final boolean deductIfBlockIsBroken) {
        this.deductIfBlockIsBroken = deductIfBlockIsBroken;
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.placeBlocks.base", player)
                .replace("%EVENTUALCOLOR%", eventualColor)
                .replace("%BLOCKTOPLACE%", getBlockToPlace().toString());
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.blockToPlace.material", getBlockToPlace().toString());
        configuration.set(initialPath + ".specifics.deductIfBlockBroken", isDeductIfBlockBroken());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        blockToPlace = Material.valueOf(configuration.getString(initialPath + ".specifics.blockToPlace.material"));
        deductIfBlockIsBroken = configuration.getBoolean(initialPath + ".specifics.deductIfBlockBroken", true);
    }


    @Override
    public void onObjectiveUnlock(ActiveObjective activeObjective) {

    }

    public final Material getBlockToPlace() {
        return blockToPlace;
    }

    public final long getAmountToPlace() {
        return super.getProgressNeeded();
    }

    public final boolean isDeductIfBlockBroken() {
        return deductIfBlockIsBroken;
    }
}
