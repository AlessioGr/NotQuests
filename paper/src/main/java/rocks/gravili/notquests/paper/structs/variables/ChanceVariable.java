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
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class ChanceVariable extends Variable<Boolean> {

  public ChanceVariable(NotQuests main) {
    super(main);

    addRequiredNumber(
        NumberVariableValueArgument.<CommandSender>newBuilder("chance", main, null).build());
  }

  @Override
  public Boolean getValueInternally(QuestPlayer questPlayer, Object... objects) {
    double chanceToHave = getRequiredNumberValue("chance", questPlayer);

    double random = Math.random() * 100;
    return random < chanceToHave;
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
    return "Chances";
  }

  @Override
  public String getSingular() {
    return "Chance";
  }
}
