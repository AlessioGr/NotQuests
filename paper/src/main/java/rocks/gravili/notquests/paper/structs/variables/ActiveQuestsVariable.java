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
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class ActiveQuestsVariable extends Variable<String[]> {
  public ActiveQuestsVariable(NotQuests main) {
    super(main);
    setCanSetValue(true);
  }

  @Override
  public String[] getValueInternally(QuestPlayer questPlayer, Object... objects) {
    String[] activeQuests;
    if (questPlayer == null) {
      return null;
    }

    activeQuests =
        questPlayer.getActiveQuests().stream()
            .map(ActiveQuest::getQuestIdentifier)
            .toArray(String[]::new);

    return activeQuests;
  }

  @Override
  public boolean setValueInternally(String[] newValue, QuestPlayer questPlayer, Object... objects) {
    if (questPlayer == null) {
      return false;
    }

    for (final ActiveQuest acceptedQuest : questPlayer.getActiveQuests()) {
      boolean foundQuest = false;
      for (int i = 0; i < newValue.length; i++) {
        if (newValue[i].equalsIgnoreCase(acceptedQuest.getQuestIdentifier())) {
          foundQuest = true;
          break;
        }
      }
      if (!foundQuest) {
        questPlayer.failQuest(acceptedQuest);
      }
    }

    for (int i = 0; i < newValue.length; i++) {
      Quest quest = main.getQuestManager().getQuest(newValue[i]);
      if (quest != null && !questPlayer.hasAcceptedQuest(quest)) {
        main.getQuestPlayerManager().forceAcceptQuestSilent(questPlayer.getUniqueId(), quest);
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
    return "Active Quests";
  }

  @Override
  public String getSingular() {
    return "Active Quest";
  }
}
