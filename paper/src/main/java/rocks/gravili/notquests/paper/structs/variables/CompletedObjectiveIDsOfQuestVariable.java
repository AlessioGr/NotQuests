/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
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

package rocks.gravili.notquests.paper.structs.variables;

import cloud.commandframework.arguments.standard.StringArgument;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class CompletedObjectiveIDsOfQuestVariable extends Variable<String[]> {
  public CompletedObjectiveIDsOfQuestVariable(NotQuests main) {
    super(main);
    setCanSetValue(true);

    addRequiredString(
        StringArgument.<CommandSender>newBuilder("QuestName")
            .withSuggestionsProvider(
                (context, lastString) -> {
                  final List<String> allArgs = context.getRawInput();
                  main.getUtilManager()
                      .sendFancyCommandCompletion(
                          context.getSender(),
                          allArgs.toArray(new String[0]),
                          "[Quest Name]",
                          "[...]");

                  ArrayList<String> suggestions = new ArrayList<>();
                  for (Quest quest : main.getQuestManager().getAllQuests()) {
                    suggestions.add(quest.getIdentifier() );
                  }
                  return suggestions;
                })
            .single()
            .build());
  }

  @Override
  public String[] getValueInternally(QuestPlayer questPlayer, Object... objects) {
    final String questName = getRequiredStringValue("QuestName");

    ActiveQuest foundQuest = null;
    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
      if (activeQuest.getQuestIdentifier().equalsIgnoreCase(questName)) {
        foundQuest = activeQuest;
      }
    }
    final ArrayList<String> completedObjectivesOfQuestNames = new ArrayList<>();

    if (foundQuest == null) {
      return completedObjectivesOfQuestNames.toArray(new String[0]);
    }

    for (final ActiveObjective activeObjective : foundQuest.getCompletedObjectives()) {
      completedObjectivesOfQuestNames.add("" + activeObjective.getObjective().getObjectiveID());
    }

    return completedObjectivesOfQuestNames.toArray(new String[0]);
  }

  @Override
  public boolean setValueInternally(String[] newValue, QuestPlayer questPlayer, Object... objects) {
    if (questPlayer == null) {
      return false;
    }
    final String questName = getRequiredStringValue("QuestName");

    ActiveQuest foundQuest = null;
    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
      if (activeQuest.getQuestIdentifier().equalsIgnoreCase(questName)) {
        foundQuest = activeQuest;
      }
    }
    if (foundQuest == null) {
      return false;
    }

    final List<String> newValues = Arrays.asList(newValue);

    for (ActiveObjective activeObjective : foundQuest.getActiveObjectives()) {
      if (newValues.contains("" + activeObjective.getObjectiveID())) {
        activeObjective.addProgress(
            (activeObjective.getProgressNeeded() - activeObjective.getCurrentProgress()));
      }
    }
    foundQuest.removeCompletedObjectives(true);
    questPlayer.removeCompletedQuests();

    return true;
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    return null;
  }

  @Override
  public String getPlural() {
    return "Completed Objective IDs of Quest";
  }

  @Override
  public String getSingular() {
    return "Completed Objective ID of Quest";
  }
}
