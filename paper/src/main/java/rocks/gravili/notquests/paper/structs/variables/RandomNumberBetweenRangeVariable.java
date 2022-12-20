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
import java.util.Random;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class RandomNumberBetweenRangeVariable extends Variable<Integer> {
  public RandomNumberBetweenRangeVariable(NotQuests main) {
    super(main);

    addRequiredNumber(
        NumberVariableValueArgument.<CommandSender>newBuilder("min", main, null).build());
    addRequiredNumber(
        NumberVariableValueArgument.<CommandSender>newBuilder("max", main, null).build());
  }

  @Override
  public Integer getValueInternally(QuestPlayer questPlayer, Object... objects) {
    final Random r = new Random();

    int min = (int) Math.round(getRequiredNumberValue("min", questPlayer));

    int max = (int) Math.round(getRequiredNumberValue("max", questPlayer));

    return (min == max) ? min : r.nextInt(max + 1 - min) + min;
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
    return "Random numbers";
  }

  @Override
  public String getSingular() {
    return "Random number";
  }
}
