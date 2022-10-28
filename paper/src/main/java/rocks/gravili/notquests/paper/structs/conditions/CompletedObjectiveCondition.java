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

package rocks.gravili.notquests.paper.structs.conditions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.ObjectiveHolder;

public class CompletedObjectiveCondition extends Condition {

  private int objectiveID;

  public CompletedObjectiveCondition(NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      NotQuests main,
      PaperCommandManager<CommandSender> manager,
      Command.Builder<CommandSender> builder,
      ConditionFor conditionFor) {
    if (conditionFor == ConditionFor.OBJECTIVEUNLOCK || conditionFor == ConditionFor.OBJECTIVEPROGRESS || conditionFor == ConditionFor.OBJECTIVECOMPLETE) {
      manager.command(
          builder
              .argument(
                  IntegerArgument.<CommandSender>newBuilder("Depending Objective ID")
                      .withMin(1)
                      .withSuggestionsProvider(
                          (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager()
                                .sendFancyCommandCompletion(
                                    context.getSender(),
                                    allArgs.toArray(new String[0]),
                                    "[Depending Objective ID]",
                                    "");

                            ArrayList<String> completions = new ArrayList<>();

                            final Quest quest = context.get("quest");
                            for (final Objective objective : quest.getObjectives()) {
                              if (objective.getObjectiveID() != ((Objective)context.get("Objective ID")).getObjectiveID() ) { //TODO: Support nested objectives
                                completions.add("" + objective.getObjectiveID());
                              }
                            }
                            return completions;
                          })
                      .withParser(
                          (context, lastString) -> {
                            final int ID = context.get("Depending Objective ID");
                            if (ID == (int) context.get("Depending Objective ID")) {
                              return ArgumentParseResult.failure(
                                  new IllegalArgumentException(
                                      "An objective cannot depend on itself!"));
                            }
                            final Quest quest = context.get("quest");
                            final Objective foundObjective = quest.getObjectiveFromID(ID);
                            if (foundObjective == null) {
                              return ArgumentParseResult.failure(
                                  new IllegalArgumentException(
                                      "Objective with the ID '"
                                          + ID
                                          + "' does not belong to Quest '"
                                          + quest.getIdentifier() 
                                          + "'!"));
                            } else {
                              return ArgumentParseResult.success(ID);
                            }
                          })
                      .build(),
                  ArgumentDescription.of("Depending Objective ID"))
              .handler(
                  (context) -> {
                    final Quest quest = context.get("quest");

                    final Objective objective = context.get("Objective ID"); //TODO: Support nested objectives

                    final int dependingObjectiveID = context.get("Depending Objective ID");
                    final Objective dependingObjective =
                        quest.getObjectiveFromID(dependingObjectiveID);
                    assert dependingObjective != null; // Shouldn't be null

                    if (dependingObjective != objective) {

                      CompletedObjectiveCondition completedObjectiveCondition =
                          new CompletedObjectiveCondition(main);
                      completedObjectiveCondition.setObjectiveID(dependingObjectiveID);

                      main.getConditionsManager()
                          .addCondition(completedObjectiveCondition, context, conditionFor);
                    } else {
                      context
                          .getSender()
                          .sendMessage(
                              main.parse(
                                  "<error>Error: You cannot set an objective to depend on itself!"));
                    }
                  }));
    }
  }

  public void setObjectiveID(final int objectiveID) {
    this.objectiveID = objectiveID;
  }

  public final int getObjectiveToCompleteID() {
    return objectiveID;
  }

  public final Objective getObjectiveToComplete() {
    return getObjectiveHolder().getObjectiveFromID(getObjectiveToCompleteID());
  }

  @Override
  public String checkInternally(final QuestPlayer questPlayer) {
    final Objective objectiveToComplete = getObjectiveToComplete();
    if (objectiveToComplete == null) {
      return "<RED>Error: Cannot find objective you have to complete first.";
    }

    final ObjectiveHolder objectiveHolder = getObjectiveHolder();
    if (objectiveHolder == null) {
      return "<RED>Error: Cannot find current quest.";
    }

    //TODO: Support nested objectives
    if(objectiveHolder instanceof final Quest quest){
      ActiveQuest activeQuest = questPlayer.getActiveQuest(quest);
      if (activeQuest == null) {
        return "<RED>Error: Cannot find current active quest.";
      }

      if (activeQuest.getActiveObjectiveFromID(getObjectiveToCompleteID()) != null) {
        return "<YELLOW>Finish the following objective first: <highlight>"
            + objectiveToComplete.getDisplayNameOrIdentifier();
      }
    }else {
      return "objectiveHolder is no Quest";
    }

    return "";
  }

  @Override
  public String getConditionDescriptionInternally(QuestPlayer questPlayer, Object... objects) {
    final Objective otherObjective = getObjectiveToComplete();
    if (otherObjective != null) {
      return "<GRAY>-- Finish Objective first: " + otherObjective.getDisplayNameOrIdentifier();
    } else {
      return "<GRAY>-- Finish otherObjective first: " + getObjectiveToCompleteID();
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

  @Override
  public void deserializeFromSingleLineString(ArrayList<String> arguments) {
    objectiveID = Integer.parseInt(arguments.get(0));
  }
}
