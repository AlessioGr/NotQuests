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

package rocks.gravili.notquests.paper.structs.objectives;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArrayArgument;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class ReachLocationObjective extends Objective {
  private Location min, max;
  private String locationName;

  public ReachLocationObjective(NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      NotQuests main,
      PaperCommandManager<CommandSender> manager,
      Command.Builder<CommandSender> addObjectiveBuilder,
      final int level) {
    if (!main.getIntegrationsManager().isWorldEditEnabled()) {
      return;
    }

    manager.command(
        addObjectiveBuilder
            .senderType(Player.class)
            .literal("worldeditselection")
            .argument(
                StringArrayArgument.of(
                    "Location Name",
                    (context, lastString) -> {
                      final List<String> allArgs = context.getRawInput();
                      main.getUtilManager()
                          .sendFancyCommandCompletion(
                              context.getSender(),
                              allArgs.toArray(new String[0]),
                              "<Location Name>",
                              "");
                      ArrayList<String> completions = new ArrayList<>();
                      completions.add("<Enter new Location name>");
                      return completions;
                    }),
                ArgumentDescription.of("Location name"))
            .handler(
                (context) -> {
                  final String locationName =
                      String.join(" ", (String[]) context.get("Location Name"));

                  main.getIntegrationsManager()
                      .getWorldEditManager()
                      .handleReachLocationObjectiveCreation(
                          (Player) context.getSender(), locationName, context, level);
                }));
  }

  @Override
  public String getTaskDescriptionInternal(
      final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
    return main.getLanguageManager()
        .getString(
            "chat.objectives.taskDescription.reachLocation.base",
            questPlayer,
            activeObjective,
            Map.of("%LOCATIONNAME%", getLocationName()));
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.minLocation", getMinLocation());
    configuration.set(initialPath + ".specifics.maxLocation", getMaxLocation());
    configuration.set(initialPath + ".specifics.locationName", getLocationName());
  }

  @Override
  public void onObjectiveUnlock(
      final ActiveObjective activeObjective,
      final boolean unlockedDuringPluginStartupQuestLoadingProcess) {}

  @Override
  public void onObjectiveCompleteOrLock(
      final ActiveObjective activeObjective,
      final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
      final boolean completed) {}

  public final Location getMinLocation() {
    return min;
  }

  public void setMinLocation(final Location minLocation) {
    this.min = minLocation;
    if (getLocation() == null) {
      setLocation(minLocation, false);
    }
  }

  public final Location getMaxLocation() {
    return max;
  }

  public void setMaxLocation(final Location maxLocation) {
    this.max = maxLocation;
  }

  public final String getLocationName() {
    return locationName;
  }

  public void setLocationName(final String locationName) {
    this.locationName = locationName;
  }

  @Override
  public void load(FileConfiguration configuration, String initialPath) {
    setMinLocation(configuration.getLocation(initialPath + ".specifics.minLocation"));
    setMaxLocation(configuration.getLocation(initialPath + ".specifics.maxLocation"));
    locationName = configuration.getString(initialPath + ".specifics.locationName");
  }
}
