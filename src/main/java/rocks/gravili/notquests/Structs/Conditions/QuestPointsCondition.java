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

package rocks.gravili.notquests.Structs.Conditions;

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
import rocks.gravili.notquests.Structs.Objectives.Objective;
import rocks.gravili.notquests.Structs.Quest;
import rocks.gravili.notquests.Structs.QuestPlayer;

public class QuestPointsCondition extends Condition {

    private final NotQuests main;
    private boolean deductQuestPoints = false;


    public QuestPointsCondition(NotQuests main, Object... objects) {
        super(main, objects);
        this.main = main;
    }

    public void setDeductQuestPoints(final boolean deductQuestPoints){
        this.deductQuestPoints = deductQuestPoints;
    }


    public final long getQuestPointRequirement() {
        return getProgressNeeded();
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
    public void save(final String initialPath) {
        main.getDataManager().getQuestsConfig().set(initialPath + ".specifics.deductQuestPoints", isDeductQuestPoints());
    }

    @Override
    public void load(String initialPath) {
        this.deductQuestPoints = main.getDataManager().getQuestsConfig().getBoolean(initialPath + ".specifics.deductQuestPoints");

    }

    @Override
    public String getConditionDescription() {
        String description = "§7-- Quest points needed: " + getQuestPointRequirement() + "\n";
        if (isDeductQuestPoints()) {
            description += "§7--- §cQuest points WILL BE DEDUCTED!";
        } else {
            description += "§7--- Will quest points be deducted?: No";
        }
        return description;
    }


    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addRequirementBuilder, Command.Builder<CommandSender> objectiveAddConditionBuilder) {
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

                    QuestPointsCondition questPointsRequirement = new QuestPointsCondition(main, amount, quest);
                    questPointsRequirement.setDeductQuestPoints(deductQuestPoints);
                    quest.addRequirement(questPointsRequirement);

                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "QuestPoints Requirement successfully added to Quest " + NotQuestColors.highlightGradient
                                    + quest.getQuestName() + "</gradient>!</gradient>"
                    ));

                }));


        manager.command(objectiveAddConditionBuilder.literal("QuestPoints")
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

                    final int objectiveID = context.get("Objective ID");
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    assert objective != null; //Shouldn't be null

                    QuestPointsCondition questPointsCondition = new QuestPointsCondition(main, amount, quest, objective);
                    questPointsCondition.setDeductQuestPoints(deductQuestPoints);


                    objective.addCondition(questPointsCondition, true);

                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "QuestPoints Condition successfully added to Objective " + NotQuestColors.highlightGradient
                                    + objective.getObjectiveFinalName() + "</gradient>!</gradient>"
                    ));

                }));


    }
}
