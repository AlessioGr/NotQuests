/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
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

package rocks.gravili.notquests.paper.structs.conditions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class WorldTimeCondition extends Condition {

  private int minTime, maxTime;

  public WorldTimeCondition(NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      NotQuests main,
      PaperCommandManager<CommandSender> manager,
      Command.Builder<CommandSender> builder,
      ConditionFor conditionFor) {
    manager.command(
        builder
            .argument(
                IntegerArgument.<CommandSender>newBuilder("minTime").withMin(0).withMax(24),
                ArgumentDescription.of("Minimum world time (24-hour clock)"))
            .argument(
                IntegerArgument.<CommandSender>newBuilder("maxTime").withMin(0).withMax(24),
                ArgumentDescription.of("Maximum world time (24-hour clock)"))
            .handler(
                (context) -> {
                  final int minTime = context.get("minTime");
                  final int maxTime = context.get("maxTime");

                  WorldTimeCondition worldTimeCondition = new WorldTimeCondition(main);
                  worldTimeCondition.setMinTime(minTime);
                  worldTimeCondition.setMaxTime(maxTime);

                  main.getConditionsManager().addCondition(worldTimeCondition, context, conditionFor);
                }));
  }

  public final int getMinTime() {
    return minTime;
  }

  public void setMinTime(final int minTime) {
    this.minTime = minTime;
  }

  public final int getMaxTime() {
    return maxTime;
  }

  public void setMaxTime(final int maxTime) {
    this.maxTime = maxTime;
  }

  @Override
  public String checkInternally(final QuestPlayer questPlayer) {
    long currentTime = questPlayer.getPlayer().getWorld().getTime();

    if (currentTime >= 18000) {

      currentTime = currentTime / 1000 - 18;
    } else {

      currentTime = currentTime / 1000 + 6;
    }

    if (getMaxTime() >= getMinTime()) {
      if (currentTime <= getMaxTime() && currentTime >= getMinTime()) {
        return "";
      } else {
        return "<YELLOW>Come back between <highlight>"
            + getMinTime()
            + "</highlight> and <highlight>"
            + getMaxTime()
            + "</highlight> (It's now "
            + currentTime
            + ")";
      }
    } else { // Maxtime is the next day
      if (currentTime <= getMinTime()) { // Chec for next day
        if (currentTime <= getMaxTime()) {
          return "";
        } else {
          return "<YELLOW>Come back between <highlight>"
              + getMinTime()
              + "</highlight> and <highlight>"
              + getMaxTime()
              + "</highlight> (It's now "
              + currentTime
              + ")";
        }
      } else { // Check for current day
        if (currentTime >= getMinTime() && currentTime <= 24) {
          return "";
        } else {
          return "<YELLOW>Come back between <highlight>"
              + getMinTime()
              + "</highlight> and <highlight>"
              + getMaxTime()
              + "</highlight> (It's now "
              + currentTime
              + ")";
        }
      }
    }
  }

  @Override
  public String getConditionDescriptionInternally(QuestPlayer questPlayer, Object... objects) {
    return "<GRAY>-- World time: " + getMinTime() + " - " + getMaxTime();
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.minTime", getMinTime());
    configuration.set(initialPath + ".specifics.maxTime", getMaxTime());
  }

  @Override
  public void load(FileConfiguration configuration, String initialPath) {
    minTime = configuration.getInt(initialPath + ".specifics.minTime");
    maxTime = configuration.getInt(initialPath + ".specifics.maxTime");
  }

  @Override
  public void deserializeFromSingleLineString(ArrayList<String> arguments) {
    minTime = Integer.parseInt(arguments.get(0));
    maxTime = Integer.parseInt(arguments.get(1));
  }
}
