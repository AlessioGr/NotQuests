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
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;

import java.util.ArrayList;

public class BreakBlocksObjective extends Objective {

    private final NotQuests main;
    private final Material blockToBreak;
    private final boolean deductIfBlockIsPlaced;

    public BreakBlocksObjective(NotQuests main, final Quest quest, final int objectiveID, Material blockToBreak, int amountToBreak, boolean deductIfBlockIsPlaced) {
        super(main, quest, objectiveID, amountToBreak);
        this.main = main;
        this.blockToBreak = blockToBreak;
        this.deductIfBlockIsPlaced = deductIfBlockIsPlaced;
    }

    public BreakBlocksObjective(NotQuests main, Quest quest, int objectiveNumber, int progressNeeded) {
        super(main, quest, objectiveNumber, progressNeeded);
        final String questName = quest.getQuestName();

        this.main = main;
        blockToBreak = Material.valueOf(main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.blockToBreak.material"));
        deductIfBlockIsPlaced = main.getDataManager().getQuestsData().getBoolean("quests." + questName + ".objectives." + objectiveNumber + ".specifics.deductIfBlockPlaced");
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.breakBlocks.base", player)
                .replaceAll("%EVENTUALCOLOR%", eventualColor)
                .replaceAll("%BLOCKTOBREAK%", getBlockToBreak().toString());
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.blockToBreak.material", getBlockToBreak().toString());
        main.getDataManager().getQuestsData().set("quests." + getQuest().getQuestName()  + ".objectives." + getObjectiveID() + ".specifics.deductIfBlockPlaced", willDeductIfBlockPlaced());

    }


    public final Material getBlockToBreak() {
        return blockToBreak;
    }

    public final long getAmountToBreak() {
        return super.getProgressNeeded();
    }

    public final boolean willDeductIfBlockPlaced() {
        return deductIfBlockIsPlaced;
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("BreakBlocks")
                .argument(MaterialArgument.of("material"), ArgumentDescription.of("Material of the block which needs to be broken."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1).withSuggestionsProvider((context, lastString) -> {
                    ArrayList<String> completions = new ArrayList<>();
                    completions.add("<amount>");
                    completions.add("1");
                    completions.add("11");

                    return completions;
                }).build(), ArgumentDescription.of("Amount of blocks which need to be broken"))
                .flag(
                        manager.flagBuilder("deductIfBlockIsPlaced")
                                .withDescription(ArgumentDescription.of("Determines if Quest progress should be removed if a block is placed"))
                )
                .meta(CommandMeta.DESCRIPTION, "Adds a new BreakBlocks Objective to a quest")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                }));
    }
}
