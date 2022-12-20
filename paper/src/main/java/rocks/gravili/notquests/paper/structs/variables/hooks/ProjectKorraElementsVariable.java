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

package rocks.gravili.notquests.paper.structs.variables.hooks;

import java.util.ArrayList;
import java.util.List;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;

public class ProjectKorraElementsVariable extends Variable<String[]> {
  public ProjectKorraElementsVariable(NotQuests main) {
    super(main);
    setCanSetValue(true);
  }

  @Override
  public String[] getValueInternally(QuestPlayer questPlayer, Object... objects) {
    if (!main.getIntegrationsManager().isProjectKorraEnabled()) {
      return null;
    }
    if (questPlayer != null) {
      return main.getIntegrationsManager()
          .getProjectKorraManager()
          .getElements(questPlayer.getPlayer())
          .toArray(new String[0]);
    } else {
      return null;
    }
  }

  @Override
  public boolean setValueInternally(String[] newValue, QuestPlayer questPlayer, Object... objects) {
    if (!main.getIntegrationsManager().isProjectKorraEnabled()) {
      return false;
    }
    if (questPlayer != null) {
      main.getIntegrationsManager()
          .getProjectKorraManager()
          .setElements(questPlayer.getPlayer(), new ArrayList<>(List.of(newValue)));
      return true;

    } else {
      return false;
    }
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    return main.getIntegrationsManager().getProjectKorraManager().getAllElements();
  }

  @Override
  public String getPlural() {
    return "Elements";
  }

  @Override
  public String getSingular() {
    return "Element";
  }
}
