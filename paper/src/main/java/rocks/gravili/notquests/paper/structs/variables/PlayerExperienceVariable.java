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
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class PlayerExperienceVariable extends Variable<Integer> {
  public PlayerExperienceVariable(NotQuests main) {
    super(main);
    setCanSetValue(true);
  }

  @Override
  public Integer getValueInternally(QuestPlayer questPlayer, Object... objects) {
    if (questPlayer != null) {
      return getPlayerExp(questPlayer.getPlayer());
    } else {
      return null;
    }
  }

  @Override
  public boolean setValueInternally(Integer newValue, QuestPlayer questPlayer, Object... objects) {
    if (questPlayer != null) {
      questPlayer.getPlayer().setExp(0);
      questPlayer.getPlayer().setLevel(0);
      questPlayer.getPlayer().giveExp(newValue);
      return true;
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
    return "Experience";
  }

  @Override
  public String getSingular() {
    return "Experience";
  }

  /*
   * Code taken from https://www.spigotmc.org/threads/how-to-get-players-exp-points.239171/
   * by DOGC_Kyle
   */
  public int getExpToLevelUp(int level) {
    if (level <= 15) {
      return 2 * level + 7;
    } else if (level <= 30) {
      return 5 * level - 38;
    } else {
      return 9 * level - 158;
    }
  }

  // Calculate total experience up to a level
  public int getExpAtLevel(int level) {
    if (level <= 16) {
      return (int) (Math.pow(level, 2) + 6 * level);
    } else if (level <= 31) {
      return (int) (2.5 * Math.pow(level, 2) - 40.5 * level + 360.0);
    } else {
      return (int) (4.5 * Math.pow(level, 2) - 162.5 * level + 2220.0);
    }
  }

  // Calculate player's current EXP amount
  public int getPlayerExp(Player player) {
    int exp = 0;
    int level = player.getLevel();

    // Get the amount of XP in past levels
    exp += getExpAtLevel(level);

    // Get amount of XP towards next level
    exp += Math.round(getExpToLevelUp(level) * player.getExp());

    return exp;
  }
}
