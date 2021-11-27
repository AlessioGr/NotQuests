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

package rocks.gravili.notquests.Structs.Requirements;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;
import rocks.gravili.notquests.Structs.QuestPlayer;

public class QuestPointsRequirement extends Requirement {

    private final NotQuests main;
    private final long questPointRequirement;
    private final boolean deductQuestPoints;


    public QuestPointsRequirement(NotQuests main, final Quest quest, final int requirementID, long questPointRequirement) {
        super(main, quest, requirementID, questPointRequirement);
        this.main = main;
        this.questPointRequirement = questPointRequirement;

        this.deductQuestPoints = main.getDataManager().getQuestsConfig().getBoolean("quests." + quest.getQuestName() + ".requirements." + requirementID + ".specifics.deductQuestPoints");
    }

    public QuestPointsRequirement(NotQuests main, final Quest quest, final int requirementID, long questPointRequirement, boolean deductQuestPoints) {
        super(main, quest, requirementID, questPointRequirement);
        this.main = main;
        this.questPointRequirement = questPointRequirement;
        this.deductQuestPoints = deductQuestPoints;
    }


    public final long getQuestPointRequirement() {
        return questPointRequirement;
    }


    public final boolean isDeductQuestPoints() {
        return deductQuestPoints;
    }

    @Override
    public String check(final QuestPlayer questPlayer, final boolean enforce) {
        final long questPointRequirementAmount = getQuestPointRequirement();
        final boolean deductQuestPoints = isDeductQuestPoints();

        if (questPlayer.getQuestPoints() < questPointRequirementAmount) {
            return "\n§eYou need §b" + (questPointRequirementAmount - questPlayer.getQuestPoints()) + " §emore quest points.";
        } else {
            if (enforce && deductQuestPoints && questPointRequirementAmount > 0) {
                questPlayer.removeQuestPoints(questPointRequirementAmount, true);
            }
            return "";
        }
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".requirements." + getRequirementID() + ".specifics.deductQuestPoints", isDeductQuestPoints());
    }

    @Override
    public String getRequirementDescription() {
        String description = "§7-- Quest points needed: " + getQuestPointRequirement() + "\n";
        if (isDeductQuestPoints()) {
            description += "§7--- §cQuest points WILL BE DEDUCTED!";
        } else {
            description += "§7--- Will quest points be deducted?: No";
        }
        return description;
    }


    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addRequirementBuilder) {
        manager.command(addRequirementBuilder.literal("QuestPoints")
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of QuestPoints needed"))
                .flag(
                        manager.flagBuilder("deductQuestPoints")
                                .withDescription(ArgumentDescription.of("Makes it so the required quest points are deducted from the players balance if the Quest is accepted."))
                )
                .meta(CommandMeta.DESCRIPTION, "Adds a new QuestPoints Requirement to a quest")
                .handler((context) -> {
                    final Audience audience = main.adventure().sender(context.getSender());

                    final Quest quest = context.get("quest");

                    final int amount = context.get("amount");
                    final boolean deductQuestPoints = context.flags().isPresent("deductQuestPoints");

                    QuestPointsRequirement questPointsRequirement = new QuestPointsRequirement(main, quest, quest.getRequirements().size() + 1, amount, deductQuestPoints);
                    quest.addRequirement(questPointsRequirement);

                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "QuestPoints Requirement successfully added to Quest " + NotQuestColors.highlightGradient
                                    + quest.getQuestName() + "</gradient>!</gradient>"
                    ));

                }));
    }
}
