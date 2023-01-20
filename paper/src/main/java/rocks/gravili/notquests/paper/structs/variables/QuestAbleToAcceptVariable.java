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
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.*;
import rocks.gravili.notquests.paper.structs.conditions.Condition;
import rocks.gravili.notquests.paper.structs.conditions.Condition.ConditionResult;

/**
 * This variable is true if the player is able to take the Quest. That means, they fulfill all Quest
 * conditions, as well as other factors like the Quest cooldown or maxAccepts.
 */
public class QuestAbleToAcceptVariable extends Variable<Boolean> {
  public QuestAbleToAcceptVariable(NotQuests main) {
    super(main);
    addRequiredString(
        StringArgument.<CommandSender>newBuilder("Quest to check")
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
  public Boolean getValueInternally(QuestPlayer questPlayer, Object... objects) {
    final Quest quest = main.getQuestManager().getQuest(getRequiredStringValue("Quest to check"));

    if (quest == null) {
      return true;
    }

    if (questPlayer != null) {

      int completedAmount = 0;
      long mostRecentCompleteTime = 0;

      int failedAmount = 0;
      long mostRecentFailTime = 0;

      int acceptedAmount = 0;
      for (final CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
        if (completedQuest.getQuest().equals(quest)) {
          completedAmount += 1;
          acceptedAmount += 1;
          if (completedQuest.getTimeCompleted() > mostRecentCompleteTime) {
            mostRecentCompleteTime = completedQuest.getTimeCompleted();
          }
        }
      }
      for (final FailedQuest failedQuest : questPlayer.getFailedQuests()) {
        if (failedQuest.getQuest().equals(quest)) {
          failedAmount += 1;
          acceptedAmount += 1;
          if (failedQuest.getTimeFailed() > mostRecentFailTime) {
            mostRecentFailTime = failedQuest.getTimeFailed();
          }
        }
      }
      for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
        if (activeQuest.getQuest().equals(quest)) {
          acceptedAmount += 1;
        }
      }





      final long completeTimeDifference = System.currentTimeMillis() - mostRecentCompleteTime;
      final long completeTimeDifferenceMinutes =
          TimeUnit.MILLISECONDS.toMinutes(completeTimeDifference);

      if (completeTimeDifferenceMinutes < quest.getAcceptCooldownComplete()
          || quest.getMaxCompletions() == 0
          || (quest.getMaxCompletions() > -1 && completedAmount >= quest.getMaxCompletions())
          || (quest.getMaxAccepts() > -1 && acceptedAmount >= quest.getMaxAccepts())
          || (quest.getMaxFails() > -1 && failedAmount >= quest.getMaxFails())
      ) {
        return false;
      }
    } else {
      if (quest.getMaxAccepts() == 0) {
        return false;
      }
    }

    for (final Condition condition : quest.getRequirements()) {
      final ConditionResult check = condition.check(questPlayer);
      if (!check.fulfilled()) {
        return false;
      }
    }

    return true; // Able to accept the Quest
  }

  @Override
  public boolean setValueInternally(Boolean newValue, QuestPlayer questPlayer, Object... objects) {
    return false;
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    return null;
  }

  @Override
  public String getPlural() {
    return "Able to accept Quest";
  }

  @Override
  public String getSingular() {
    return "Able to accept Quest";
  }
}
