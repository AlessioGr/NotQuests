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
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class PlayerMaxHealthVariable extends Variable<Double> {

  public PlayerMaxHealthVariable(NotQuests main) {
    super(main);
    setCanSetValue(true);
  }

  @Override
  public Double getValueInternally(QuestPlayer questPlayer, Object... objects) {
    if (questPlayer != null) {
      final AttributeInstance attributeInstance =
          questPlayer.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH);
      return attributeInstance != null ? attributeInstance.getValue() : 0;
    } else {
      return 0d;
    }
  }

  @Override
  public boolean setValueInternally(Double newValue, QuestPlayer questPlayer, Object... objects) {
    if (questPlayer != null) {
      final AttributeInstance attributeInstance =
          questPlayer.getPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH);
      if (attributeInstance != null) {
        attributeInstance.setBaseValue(newValue);
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    return null;
  }

  @Override
  public String getPlural() {
    return "Maximum Health";
  }

  @Override
  public String getSingular() {
    return "Maximum Health";
  }
}
