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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class DayOfWeekVariable extends Variable<String> {
  public DayOfWeekVariable(NotQuests main) {
    super(main);
  }

  @Override
  public String getValueInternally(QuestPlayer questPlayer, Object... objects) {
    DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();

    return dayOfWeek.name().toLowerCase(Locale.ROOT);
  }

  @Override
  public boolean setValueInternally(String newValue, QuestPlayer questPlayer, Object... objects) {
    return false;
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    List<String> possibleValues = new ArrayList<>();
    for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
      possibleValues.add(dayOfWeek.name().toLowerCase(Locale.ROOT));
    }
    return possibleValues;
  }

  @Override
  public String getPlural() {
    return "Day of Week";
  }

  @Override
  public String getSingular() {
    return "Day of Week";
  }
}
