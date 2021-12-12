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

package rocks.gravili.notquests.Structs.Objectives;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.bukkit.parsers.MaterialArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.ActiveObjective;
import rocks.gravili.notquests.Structs.Quest;

public class PlaceBlocksObjective extends Objective {

    private final NotQuests main;
    private final Material blockToPlace;
    private final boolean deductIfBlockIsBroken;

    public PlaceBlocksObjective(NotQuests main, final Quest quest, final int objectiveID, Material blockToPlace, int amountToPlace, boolean deductIfBlockIsBroken) {
        super(main, quest, objectiveID, amountToPlace);
        this.main = main;
        this.blockToPlace = blockToPlace;
        this.deductIfBlockIsBroken = deductIfBlockIsBroken;
    }

    public PlaceBlocksObjective(NotQuests main, Quest quest, int objectiveNumber, int progressNeeded) {
        super(main, quest, objectiveNumber, progressNeeded);
        final String questName = quest.getQuestName();

        this.main = main;
        blockToPlace = Material.valueOf(main.getDataManager().getQuestsConfig().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.blockToPlace.material"));
        deductIfBlockIsBroken = main.getDataManager().getQuestsConfig().getBoolean("quests." + questName + ".objectives." + objectiveNumber + ".specifics.deductIfBlockBroken", true);
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
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final Material material = context.get("material");
                    final int amount = context.get("amount");
                    final boolean deductIfBlockIsBroken = !context.flags().isPresent("doNotDeductIfBlockIsBroken");

                    PlaceBlocksObjective placeBlocksObjective = new PlaceBlocksObjective(main, quest, quest.getObjectives().size() + 1, material, amount, deductIfBlockIsBroken);
                    quest.addObjective(placeBlocksObjective, true);
                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "PlaceBlocks Objective successfully added to Quest " + NotQuestColors.highlightGradient
                                    + quest.getQuestName() + "</gradient>!</gradient>"
                    ));

                }));
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.placeBlocks.base", player)
                .replace("%EVENTUALCOLOR%", eventualColor)
                .replace("%BLOCKTOPLACE%", getBlockToPlace().toString());
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.blockToPlace.material", getBlockToPlace().toString());
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.deductIfBlockBroken", isDeductIfBlockBroken());

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
