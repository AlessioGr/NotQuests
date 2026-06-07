/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
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

package rocks.gravili.notquests.paper.structs.objectives;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.Command;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.Map;

import static rocks.gravili.notquests.paper.commands.arguments.ItemStackSelectionParser.itemStackSelectionParser;
import static rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueParser.numberVariableParser;

public class PlaceBlocksObjective extends Objective {

    private ItemStackSelection itemStackSelection;

    private boolean deductIfBlockIsBroken = true;

    public PlaceBlocksObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            PaperCommandManager<CommandSender> manager,
            Command.Builder<CommandSender> addObjectiveBuilder,
            final int level) {
        manager.command(addObjectiveBuilder
                .required("materials", itemStackSelectionParser(main), Description.of("Material of the block which needs to be placed"))
                .required("amount", numberVariableParser("amount", null), Description.of("Amount of blocks which need to be placed"))
                .flag(manager.flagBuilder("doNotDeductIfBlockIsBroken").withDescription(Description.of("Makes it so Quest progress is not removed if the block is broken")))
                .handler((context) -> {
                    final String amountExpression = context.get("amount");
                    final boolean deductIfBlockIsBroken =
                            !context.flags().isPresent("doNotDeductIfBlockIsBroken");

                    final ItemStackSelection itemStackSelection = context.get("materials");

                    PlaceBlocksObjective placeBlocksObjective = new PlaceBlocksObjective(main);
                    placeBlocksObjective.setItemStackSelection(itemStackSelection);

                    placeBlocksObjective.setDeductIfBlockIsBroken(deductIfBlockIsBroken);
                    placeBlocksObjective.setProgressNeededExpression(amountExpression);

                    main.getObjectiveManager().addObjective(placeBlocksObjective, context, level);
                }));
    }

    public final ItemStackSelection getItemStackSelection() {
        return itemStackSelection;
    }

    public void setItemStackSelection(final ItemStackSelection itemStackSelection) {
        this.itemStackSelection = itemStackSelection;
    }

    @Override
    public String getTaskDescriptionInternal(
            final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
        return main.getLanguageManager().getString(
                "chat.objectives.taskDescription.placeBlocks.base",
                questPlayer,
                activeObjective,
                Map.of("%BLOCKTOPLACE%", getItemStackSelection().getAllMaterialsListedTranslated("main")));
    }

    public void setDeductIfBlockIsBroken(final boolean deductIfBlockIsBroken) {
        this.deductIfBlockIsBroken = deductIfBlockIsBroken;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        getItemStackSelection()
                .saveToFileConfiguration(configuration, initialPath + ".specifics.itemStackSelection");

        configuration.set(initialPath + ".specifics.deductIfBlockBroken", isDeductIfBlockBroken());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.itemStackSelection = new ItemStackSelection(main);
        itemStackSelection.loadFromFileConfiguration(
                configuration, initialPath + ".specifics.itemStackSelection");

        // Convert old to new
        if (configuration.contains(initialPath + ".specifics.nqitem")
                || configuration.contains(initialPath + ".specifics.blockToPlace.material")) {
            main.getLogManager().info("Converting old PlaceBlocksObjective to new one...");
            final String nqItemName = configuration.getString(initialPath + ".specifics.nqitem", "");

            if (nqItemName.isBlank()) {
                itemStackSelection.addMaterialName(
                        configuration.getString(initialPath + ".specifics.blockToPlace.material", ""));
            } else {
                itemStackSelection.addNqItemName(nqItemName);
            }
            itemStackSelection.saveToFileConfiguration(
                    configuration, initialPath + ".specifics.itemStackSelection");
            configuration.set(initialPath + ".specifics.nqitem", null);
            configuration.set(initialPath + ".specifics.blockToPlace.material", null);
            // Let's hope it saves somewhere, else conversion will happen again...
        }

        deductIfBlockIsBroken =
                configuration.getBoolean(initialPath + ".specifics.deductIfBlockBroken", true);
    }

    @Override
    public void onObjectiveUnlock(
            final ActiveObjective activeObjective,
            final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
    }

    @Override
    public void onObjectiveCompleteOrLock(
            final ActiveObjective activeObjective,
            final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
            final boolean completed) {
    }

    public final boolean isDeductIfBlockBroken() {
        return deductIfBlockIsBroken;
    }
}
