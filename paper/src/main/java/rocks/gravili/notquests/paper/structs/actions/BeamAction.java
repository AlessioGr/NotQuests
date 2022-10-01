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

package rocks.gravili.notquests.paper.structs.actions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.parsers.WorldArgument;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class BeamAction extends Action {

  private String beamName = "";
  private boolean remove = false;
  private Location beamLocation = null;

  public BeamAction(final NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      NotQuests main,
      PaperCommandManager<CommandSender> manager,
      Command.Builder<CommandSender> builder,
      ActionFor actionFor) {
    manager.command(
        builder
            .argument(
                StringArgument.<CommandSender>newBuilder("beamName")
                    .withSuggestionsProvider(
                        (context, lastString) -> {
                          ArrayList<String> completions = new ArrayList<>();

                          completions.add("<Enter beam name>");

                          final List<String> allArgs = context.getRawInput();
                          main.getUtilManager()
                              .sendFancyCommandCompletion(
                                  context.getSender(),
                                  allArgs.toArray(new String[0]),
                                  "[Beam Name]",
                                  "[...]");

                          return completions;
                        })
                    .build(),
                ArgumentDescription.of("Beam Name."))
            .literal("remove")
            .handler(
                (context) -> {
                  String beamName = context.get("beamName");

                  BeamAction beamAction = new BeamAction(main);
                  beamAction.setBeamName(beamName);
                  beamAction.setRemove(true);

                  main.getActionManager().addAction(beamAction, context, actionFor);
                }));

    manager.command(
        builder
            .argument(
                StringArgument.<CommandSender>newBuilder("beamName")
                    .withSuggestionsProvider(
                        (context, lastString) -> {
                          ArrayList<String> completions = new ArrayList<>();

                          completions.add("<Enter beam name>");

                          final List<String> allArgs = context.getRawInput();
                          main.getUtilManager()
                              .sendFancyCommandCompletion(
                                  context.getSender(),
                                  allArgs.toArray(new String[0]),
                                  "[Beam Name]",
                                  "[...]");

                          return completions;
                        })
                    .build(),
                ArgumentDescription.of("Beam Name."))
            .literal("spawn")
            .argument(WorldArgument.of("world"), ArgumentDescription.of("World name"))
            .argument(IntegerArgument.newBuilder("x"), ArgumentDescription.of("X coordinate"))
            .argument(IntegerArgument.newBuilder("y"), ArgumentDescription.of("Y coordinate"))
            .argument(IntegerArgument.newBuilder("z"), ArgumentDescription.of("Z coordinate"))
            .handler(
                (context) -> {
                  String beamName = context.get("beamName");
                  final World world = context.get("world");
                  final Vector coordinates =
                      new Vector(context.get("x"), context.get("y"), context.get("z"));
                  final Location location = coordinates.toLocation(world);

                  BeamAction beamAction = new BeamAction(main);
                  beamAction.setBeamName(beamName);
                  beamAction.setRemove(false);
                  beamAction.setBeamLocation(location);

                  main.getActionManager().addAction(beamAction, context, actionFor);
                }));
  }

  public final String getBeamName() {
    return beamName;
  }

  public void setBeamName(final String beamName) {
    this.beamName = beamName;
  }

  public final boolean isRemove() {
    return remove;
  }

  public void setRemove(final boolean remove) {
    this.remove = remove;
  }

  public final Location getBeamLocation() {
    return beamLocation;
  }

  public void setBeamLocation(final Location beamLocation) {
    this.beamLocation = beamLocation;
  }

  @Override
  public void executeInternally(final QuestPlayer questPlayer, Object... objects) {
    if (isRemove()) {
      if (questPlayer != null) {
        if (questPlayer.getActiveLocationsAndBeacons().containsKey(getBeamName())) {
          questPlayer.clearBeacons();
        }
      }
    } else {
      if (getBeamLocation() == null) {
        return;
      }
      questPlayer.trackBeacon(beamName, getBeamLocation());
    }
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.beamName", getBeamName());
    configuration.set(initialPath + ".specifics.remove", isRemove());
    configuration.set(initialPath + ".specifics.location", getBeamLocation());
  }

  @Override
  public void load(final FileConfiguration configuration, String initialPath) {
    this.beamName = configuration.getString(initialPath + ".specifics.beamName");
    this.remove = configuration.getBoolean(initialPath + ".specifics.remove");
    this.beamLocation = configuration.getLocation(initialPath + ".specifics.location", null);
  }

  @Override
  public void deserializeFromSingleLineString(ArrayList<String> arguments) {
    this.beamName = arguments.get(0);

    this.remove = String.join(" ", arguments).toLowerCase(Locale.ROOT).contains("--remove");

    if (!remove) {
      final World world = Bukkit.getWorld(arguments.get(1));

      if (world != null) {
        final Vector coordinates =
            new Vector(
                Integer.parseInt(arguments.get(2)),
                Integer.parseInt(arguments.get(3)),
                Integer.parseInt(arguments.get(4)));

        this.beamLocation = coordinates.toLocation(world);
      }
    }
  }

  @Override
  public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
    if (remove) {
      return "Despawns beam: " + getBeamName();
    } else {
      return "Spawns beam: " + getBeamName();
    }
  }
}
