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
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.*;

/**
 * This variable is true if the amount of times the player has previously accepted this Quest is
 * equal or higher than the Quests max accepts
 */
public class QuestReachedMaxAcceptsVariable extends Variable<Boolean> {
  public QuestReachedMaxAcceptsVariable(NotQuests main) {
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

    if (quest == null || questPlayer == null) {
      return false;
    }

    if (quest.getMaxAccepts() <= -1) {
      return false;
    } else if (quest.getMaxAccepts() == 0) {
      return true;
    }

    int acceptedAmount = 0; // only needed for maxAccepts

    for (final CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
      if (completedQuest.getQuest().equals(quest)) {
        acceptedAmount += 1;
      }
    }

    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
      if (activeQuest.getQuest().equals(quest)) {
        acceptedAmount += 1;
      }
    }
    for (final FailedQuest failedQuest : questPlayer.getFailedQuests()) {
      if (failedQuest.getQuest().equals(quest)) {
        acceptedAmount += 1;
      }
    }

    return acceptedAmount >= quest.getMaxAccepts();
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
    return "Quest reached max accepts";
  }

  @Override
  public String getSingular() {
    return "Quest reached max accepts";
  }
}
