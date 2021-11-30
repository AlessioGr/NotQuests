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
import rocks.gravili.notquests.Structs.Quest;

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
        blockToBreak = Material.valueOf(main.getDataManager().getQuestsConfig().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.blockToBreak.material"));
        deductIfBlockIsPlaced = main.getDataManager().getQuestsConfig().getBoolean("quests." + questName + ".objectives." + objectiveNumber + ".specifics.deductIfBlockPlaced", true);
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.breakBlocks.base", player)
                .replace("%EVENTUALCOLOR%", eventualColor)
                .replace("%BLOCKTOBREAK%", getBlockToBreak().toString());
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.blockToBreak.material", getBlockToBreak().toString());
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.deductIfBlockPlaced", isDeductIfBlockPlaced());

    }


    public final Material getBlockToBreak() {
        return blockToBreak;
    }

    public final long getAmountToBreak() {
        return super.getProgressNeeded();
    }

    public final boolean isDeductIfBlockPlaced() {
        return deductIfBlockIsPlaced;
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("BreakBlocks")
                .argument(MaterialArgument.of("material"), ArgumentDescription.of("Material of the block which needs to be broken."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of blocks which need to be broken"))
                .flag(
                        manager.flagBuilder("doNotDeductIfBlockIsPlaced")
                                .withDescription(ArgumentDescription.of("Makes it so Quest progress is not removed if the block is placed"))
                )
                .meta(CommandMeta.DESCRIPTION, "Adds a new BreakBlocks Objective to a quest")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final Material material = context.get("material");
                    final int amount = context.get("amount");
                    final boolean deductIfBlockIsPlaced = !context.flags().isPresent("doNotDeductIfBlockIsPlaced");

                    BreakBlocksObjective breakBlocksObjective = new BreakBlocksObjective(main, quest, quest.getObjectives().size() + 1, material, amount, deductIfBlockIsPlaced);
                    quest.addObjective(breakBlocksObjective, true);
                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "BreakBlocks Objective successfully added to Quest " + NotQuestColors.highlightGradient
                                    + quest.getQuestName() + "</gradient>!</gradient>"
                    ));

                }));
    }
}
