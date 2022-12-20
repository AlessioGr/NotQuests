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

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import java.util.List;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.variables.Variable;

public class TownyTownResidentCountVariable extends Variable<Integer> {
  public TownyTownResidentCountVariable(NotQuests main) {
    super(main);
  }

  @Override
  public Integer getValueInternally(QuestPlayer questPlayer, Object... objects) {
    if (!main.getIntegrationsManager().isTownyEnabled()) {
      return 0;
    }
    if (questPlayer != null) {
      Resident resident = TownyUniverse.getInstance().getResident(questPlayer.getUniqueId());
      if (resident != null && resident.getTownOrNull() != null && resident.hasTown()) {
        Town town = resident.getTownOrNull();
        return town.getNumResidents();
      } else {
        return 0;
      }

    } else {
      return 0;
    }
  }

  @Override
  public boolean setValueInternally(Integer newValue, QuestPlayer questPlayer, Object... objects) {
    return false;
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    return null;
  }

  @Override
  public String getPlural() {
    return "Residents in Town";
  }

  @Override
  public String getSingular() {
    return "Resident in Town";
  }
}
