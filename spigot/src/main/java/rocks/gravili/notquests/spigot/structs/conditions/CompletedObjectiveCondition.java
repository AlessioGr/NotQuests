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

package rocks.gravili.notquests.spigot.structs.conditions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.spigot.NotQuests;
import rocks.gravili.notquests.spigot.structs.ActiveQuest;
import rocks.gravili.notquests.spigot.structs.Quest;
import rocks.gravili.notquests.spigot.structs.QuestPlayer;
import rocks.gravili.notquests.spigot.structs.objectives.Objective;

import java.util.ArrayList;
import java.util.List;

import static rocks.gravili.notquests.spigot.commands.NotQuestColors.errorGradient;


public class CompletedObjectiveCondition extends Condition {

    private int objectiveID;


    public CompletedObjectiveCondition(NotQuests main) {
        super(main);
    }

    public void setObjectiveID(final int objectiveID) {
        this.objectiveID = objectiveID;
    }


    public final int getObjectiveToCompleteID() {
        return objectiveID;
    }

    public final Objective getObjectiveToComplete() {
        return getQuest().getObjectiveFromID(getObjectiveToCompleteID());
    }


    @Override
    public String check(final QuestPlayer questPlayer, final boolean enforce) {
        final Objective objectiveToComplete = getObjectiveToComplete();
        if(objectiveToComplete == null){
            return "<RED>Error: Cannot find objective you have to complete first.";
        }

        final Quest quest = getQuest();
        if(quest == null){
            return "<RED>Error: Cannot find current quest.";
        }

        ActiveQuest activeQuest = questPlayer.getActiveQuest(quest);
        if(activeQuest == null){
            return "<RED>Error: Cannot find current active quest.";
        }

        if(activeQuest.getActiveObjectiveFromID(getObjectiveToCompleteID()) != null){
            return "<YELLOW>Finish the following objective first: <AQUA>" + objectiveToComplete.getObjectiveFinalName();
        }
        return "";

    }


    @Override
    public String getConditionDescription() {
        final Objective otherObjective = getObjectiveToComplete();
        if (otherObjective != null) {
            return "<GRAY>-- Finish Objective first: " + otherObjective.getObjectiveFinalName();
        } else {
            return "<GRAY>-- Finish otherObjective first: " + getObjectiveToCompleteID();
        }

    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ConditionFor conditionFor) {
        if (conditionFor == ConditionFor.OBJECTIVE) {
            manager.command(builder.literal("CompletedObjective")
                    .argument(IntegerArgument.<CommandSender>newBuilder("Depending Objective ID").withMin(1).withSuggestionsProvider(
                                    (context, lastString) -> {
                                        final List<String> allArgs = context.getRawInput();
                                        final Audience audience = main.adventure().sender(context.getSender());
                                        main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Depending Objective ID]", "");

                                        ArrayList<String> completions = new ArrayList<>();

                                        final Quest quest = context.get("quest");
                                        for (final Objective objective : quest.getObjectives()) {
                                            if (objective.getObjectiveID() != (int) context.get("Objective ID")) {
                                                completions.add("" + objective.getObjectiveID());
                                            }
                                        }
                                        return completions;
                                    }
                            ).withParser((context, lastString) -> {
                                final int ID = context.get("Depending Objective ID");
                                if (ID == (int) context.get("Depending Objective ID")) {
                                    return ArgumentParseResult.failure(new IllegalArgumentException("An objective cannot depend on itself!"));
                                }
                                final Quest quest = context.get("quest");
                                final Objective foundObjective = quest.getObjectiveFromID(ID);
                                if (foundObjective == null) {
                                    return ArgumentParseResult.failure(new IllegalArgumentException("Objective with the ID '" + ID + "' does not belong to Quest '" + quest.getQuestName() + "'!"));
                                } else {
                                    return ArgumentParseResult.success(ID);
                                }
                            })
                            .build(), ArgumentDescription.of("Depending Objective ID"))
                    .meta(CommandMeta.DESCRIPTION, "Adds a new OtherQuest Requirement to a quest")
                    .handler((context) -> {
                        final Audience audience = main.adventure().sender(context.getSender());

                        final Quest quest = context.get("quest");

                        final int objectiveID = context.get("Objective ID");
                        final Objective objective = quest.getObjectiveFromID(objectiveID);
                        assert objective != null; //Shouldn't be null

                        final int dependingObjectiveID = context.get("Depending Objective ID");
                        final Objective dependingObjective = quest.getObjectiveFromID(dependingObjectiveID);
                        assert dependingObjective != null; //Shouldn't be null

                        if (dependingObjective != objective) {

                            CompletedObjectiveCondition completedObjectiveCondition = new CompletedObjectiveCondition(main);
                            completedObjectiveCondition.setObjectiveID(dependingObjectiveID);

                            main.getConditionsManager().addCondition(completedObjectiveCondition, context);
                        } else {
                            audience.sendMessage(MiniMessage.miniMessage().deserialize(errorGradient + "Error: You cannot set an objective to depend on itself!"));
                        }

                    }));
        }
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.objectiveID", getObjectiveToCompleteID());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        objectiveID = configuration.getInt(initialPath + ".specifics.objectiveID");
    }
}
