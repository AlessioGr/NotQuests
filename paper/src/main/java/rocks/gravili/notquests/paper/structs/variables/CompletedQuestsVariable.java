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

import java.util.List;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.CompletedQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class CompletedQuestsVariable extends Variable<String[]> {
  public CompletedQuestsVariable(NotQuests main) {
    super(main);
    setCanSetValue(true);
  }

  @Override
  public String[] getValueInternally(QuestPlayer questPlayer, Object... objects) {
    String[] completedQuests;
    if (questPlayer == null) {
      return null;
    }

    completedQuests =
        questPlayer.getCompletedQuests().stream()
            .map(CompletedQuest::getQuestIdentifier)
            .toArray(String[]::new);

    return completedQuests;
  }

  @Override
  public boolean setValueInternally(String[] newValue, QuestPlayer questPlayer, Object... objects) {
    if (questPlayer == null) {
      return false;
    }

    for (CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
      boolean foundQuest = false;
      for (int i = 0; i < newValue.length; i++) {
        if (newValue[i].equalsIgnoreCase(completedQuest.getQuestIdentifier() )) {
          foundQuest = true;
          break;
        }
      }
      if (!foundQuest) {
        questPlayer.getCompletedQuests().remove(completedQuest);
      }
    }

    for (int i = 0; i < newValue.length; i++) {
      Quest quest = main.getQuestManager().getQuest(newValue[i]);
      if (quest != null && !questPlayer.hasCompletedQuest(quest)) {
        questPlayer.getCompletedQuests().add(new CompletedQuest(quest, questPlayer));
      }
    }

    return true;
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    return main.getQuestManager().getAllQuests().stream()
        .map(quest -> quest.getIdentifier() )
        .toList();
  }

  @Override
  public String getPlural() {
    return "Completed Quests";
  }

  @Override
  public String getSingular() {
    return "Completed Quest";
  }
}
