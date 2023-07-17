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
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.bukkit.parsers.WorldArgument;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.EntityTypeSelector;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class SpawnMobAction extends Action {

  private String mobToSpawnType = "";
  private boolean usePlayerLocation = false;
  private Location spawnLocation;
  private int spawnAmount = 1;
  private int spawnRadiusX = 0;
  private int spawnRadiusY = 0;
  private int spawnRadiusZ = 0;

  public SpawnMobAction(final NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      NotQuests main,
      PaperCommandManager<CommandSender> manager,
      Command.Builder<CommandSender> builder,
      ActionFor actionFor) {
    CommandFlag<Integer> spawnRadiusX =
        CommandFlag.newBuilder("spawnRadiusX")
            .withArgument(
                IntegerArgument.<CommandSender>newBuilder("spawnRadiusX").withMin(0).build())
            .build();
    CommandFlag<Integer> spawnRadiusY =
        CommandFlag.newBuilder("spawnRadiusY")
            .withArgument(
                IntegerArgument.<CommandSender>newBuilder("spawnRadiusY").withMin(0).build())
            .build();
    CommandFlag<Integer> spawnRadiusZ =
        CommandFlag.newBuilder("spawnRadiusZ")
            .withArgument(
                IntegerArgument.<CommandSender>newBuilder("spawnRadiusZ").withMin(0).build())
            .build();

    Command.Builder<CommandSender> commonBuilder =
        builder
            .argument(
                EntityTypeSelector.of("entityType", main, false),
                ArgumentDescription.of("Type of Entity which should be spawned."))
            .argument(
                IntegerArgument.<CommandSender>newBuilder("amount").withMin(1),
                ArgumentDescription.of("Amount of mobs which should be spawned"))
            .flag(spawnRadiusX)
            .flag(spawnRadiusY)
            .flag(spawnRadiusZ);

    manager.command(
        commonBuilder
            .literal(
                "PlayerLocation",
                ArgumentDescription.of(
                    "Takes the location the player currently is in (when executing the action). So, this is a dynamic location."))
            .handler(
                (context) -> {
                  final String entityType = context.get("entityType");
                  final int amountToSpawn = context.get("amount");

                  SpawnMobAction spawnMobAction = new SpawnMobAction(main);
                  spawnMobAction.setMobToSpawnType(entityType);
                  spawnMobAction.setSpawnAmount(amountToSpawn);
                  spawnMobAction.setUsePlayerLocation(true);

                  final int spawnRadiusXValue = context.flags().getValue(spawnRadiusX, 0);
                  final int spawnRadiusYValue = context.flags().getValue(spawnRadiusY, 0);
                  final int spawnRadiusZValue = context.flags().getValue(spawnRadiusZ, 0);
                  spawnMobAction.setSpawnRadiusX(spawnRadiusXValue);
                  spawnMobAction.setSpawnRadiusY(spawnRadiusYValue);
                  spawnMobAction.setSpawnRadiusZ(spawnRadiusZValue);

                  main.getActionManager().addAction(spawnMobAction, context, actionFor);
                }));

    manager.command(
        commonBuilder
            .literal("Location", ArgumentDescription.of("Takes the location you enter"))
            .argument(WorldArgument.of("world"), ArgumentDescription.of("World name"))
            /* .argumentTriplet(
                    "coords",
                    TypeToken.get(Vector.class),
                    Triplet.of("x", "y", "z"),
                    Triplet.of(Integer.class, Integer.class, Integer.class),
                    (sender, triplet) -> new Vector(triplet.getFirst(), triplet.getSecond(),
                            triplet.getThird()
                    ),
                    ArgumentDescription.of("Coordinates")
            )*/
            // Commented out, because this somehow breaks flags
            .argument(IntegerArgument.newBuilder("x"), ArgumentDescription.of("X coordinate"))
            .argument(IntegerArgument.newBuilder("y"), ArgumentDescription.of("Y coordinate"))
            .argument(IntegerArgument.newBuilder("z"), ArgumentDescription.of("Z coordinate"))
            .handler(
                (context) -> {
                  final String entityType = context.get("entityType");
                  final int amountToSpawn = context.get("amount");

                  SpawnMobAction spawnMobAction = new SpawnMobAction(main);
                  spawnMobAction.setMobToSpawnType(entityType);
                  spawnMobAction.setSpawnAmount(amountToSpawn);
                  spawnMobAction.setUsePlayerLocation(false);

                  final int spawnRadiusXValue = context.flags().getValue(spawnRadiusX, 0);
                  final int spawnRadiusYValue = context.flags().getValue(spawnRadiusY, 0);
                  final int spawnRadiusZValue = context.flags().getValue(spawnRadiusZ, 0);
                  spawnMobAction.setSpawnRadiusX(spawnRadiusXValue);
                  spawnMobAction.setSpawnRadiusY(spawnRadiusYValue);
                  spawnMobAction.setSpawnRadiusZ(spawnRadiusZValue);

                  final World world = context.get("world");
                  final Vector coordinates =
                      new Vector(context.get("x"), context.get("y"), context.get("z"));
                  final Location location = coordinates.toLocation(world);

                  spawnMobAction.setSpawnLocation(location);

                  main.getActionManager().addAction(spawnMobAction, context, actionFor);
                }));
  }

  public final String getMobToSpawnType() {
    return mobToSpawnType;
  }

  public void setMobToSpawnType(final String mobToSpawnType) {
    this.mobToSpawnType = mobToSpawnType;
  }

  public final Location getSpawnLocation() {
    return spawnLocation;
  }

  public void setSpawnLocation(final Location spawnLocation) {
    this.spawnLocation = spawnLocation;
  }

  public final boolean isUsePlayerLocation() {
    return usePlayerLocation;
  }

  public void setUsePlayerLocation(final boolean usePlayerLocation) {
    this.usePlayerLocation = usePlayerLocation;
  }

  public final int getSpawnAmount() {
    return spawnAmount;
  }

  public void setSpawnAmount(final int spawnAmount) {
    this.spawnAmount = spawnAmount;
  }

  @Override
  public void executeInternally(final QuestPlayer questPlayer, Object... objects) {
    if (!Bukkit
        .isPrimaryThread()) { // Can only be run in main thread (at least for bukkit entities) :(
      Bukkit.getScheduler()
          .runTask(
              main.getMain(),
              () -> {
                execute2(questPlayer.getPlayer(), objects);
              });
    } else {
      execute2(questPlayer.getPlayer(), objects);
    }
  }

  public final Location getRandomLocationWithRadius(final Location baseLocation) {
    if (getSpawnRadiusX() == 0 && getSpawnRadiusY() == 0 && getSpawnRadiusZ() == 0) {
      return baseLocation;
    }
    int xToAdd;
    int yToAdd;
    int zToAdd;
    final Random r = new Random();

    xToAdd =
        getSpawnRadiusX() != 0
            ? ((-getSpawnRadiusX() == getSpawnRadiusX())
                ? -getSpawnRadiusX()
                : r.nextInt(getSpawnRadiusX() + 1 + getSpawnRadiusX()) - getSpawnRadiusX())
            : 0;
    yToAdd =
        getSpawnRadiusY() != 0
            ? ((-getSpawnRadiusY() == getSpawnRadiusY())
                ? -getSpawnRadiusY()
                : r.nextInt(getSpawnRadiusY() + 1 + getSpawnRadiusY()) - getSpawnRadiusY())
            : 0;
    zToAdd =
        getSpawnRadiusZ() != 0
            ? ((-getSpawnRadiusZ() == getSpawnRadiusZ())
                ? -getSpawnRadiusZ()
                : r.nextInt(getSpawnRadiusZ() + 1 + getSpawnRadiusZ()) - getSpawnRadiusZ())
            : 0;

    return baseLocation.clone().add(xToAdd, yToAdd, zToAdd);
  }

  public void execute2(final Player player, Object... objects) {
    try {
      EntityType entityType = EntityType.valueOf(getMobToSpawnType().toUpperCase(Locale.ROOT));

      if (isUsePlayerLocation()) {
        final Location location = player.getLocation().clone().add(new Vector(0, 1, 0));

        for (int i = 0; i < getSpawnAmount(); i++) {

          player.getWorld().spawnEntity(getRandomLocationWithRadius(location), entityType);
        }
      } else {
        if (getSpawnLocation() == null) {
          main.getLogManager().warn("Tried to execute SpawnMob action with null location.");
          return;
        }
        if (getSpawnLocation().getWorld() == null) {
          main.getLogManager().warn("Tried to execute SpawnMob action with invalid world.");
          return;
        }
        final Location location = getSpawnLocation().clone().add(new Vector(0, 1, 0));

        for (int i = 0; i < getSpawnAmount(); i++) {
          getSpawnLocation()
              .getWorld()
              .spawnEntity(getRandomLocationWithRadius(location), entityType);
        }
      }
    } catch (IllegalArgumentException e) {
      if (main.getIntegrationsManager().isMythicMobsEnabled()
          && main.getIntegrationsManager()
              .getMythicMobsManager()
              .isMythicMob(getMobToSpawnType())) {
        if (isUsePlayerLocation()) {
          main.getIntegrationsManager()
              .getMythicMobsManager()
              .spawnMob(getMobToSpawnType(), player.getLocation(), getSpawnAmount(), this);
        } else {
          main.getIntegrationsManager()
              .getMythicMobsManager()
              .spawnMob(getMobToSpawnType(), getSpawnLocation(), getSpawnAmount(), this);
        }
      } else if (main.getIntegrationsManager().isEcoBossesEnabled()
          && main.getIntegrationsManager().getEcoBossesManager().isEcoBoss(getMobToSpawnType())) {
        if (isUsePlayerLocation()) {
          main.getIntegrationsManager()
              .getEcoBossesManager()
              .spawnMob(getMobToSpawnType(), player.getLocation(), getSpawnAmount(), this);
        } else {
          main.getIntegrationsManager()
              .getEcoBossesManager()
              .spawnMob(getMobToSpawnType(), getSpawnLocation(), getSpawnAmount(), this);
        }
      } else {
        main.getLogManager()
            .warn(
                "Tried to execute SpawnMob with an either invalid mob, or a mythic mob while the mythic mobs plugin is not installed.");
      }
    }
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.mobToSpawn", getMobToSpawnType());
    configuration.set(initialPath + ".specifics.spawnLocation", getSpawnLocation());
    configuration.set(initialPath + ".specifics.usePlayerLocation", isUsePlayerLocation());
    configuration.set(initialPath + ".specifics.amount", getSpawnAmount());
    configuration.set(initialPath + ".specifics.spawnRadiusX", getSpawnRadiusX());
    configuration.set(initialPath + ".specifics.spawnRadiusY", getSpawnRadiusY());
    configuration.set(initialPath + ".specifics.spawnRadiusZ", getSpawnRadiusZ());
  }

  @Override
  public void load(final FileConfiguration configuration, String initialPath) {
    this.mobToSpawnType = configuration.getString(initialPath + ".specifics.mobToSpawn", "");
    this.spawnLocation = configuration.getLocation(initialPath + ".specifics.spawnLocation");
    this.usePlayerLocation = configuration.getBoolean(initialPath + ".specifics.usePlayerLocation");
    this.spawnAmount = configuration.getInt(initialPath + ".specifics.amount", 1);
    this.spawnRadiusX = configuration.getInt(initialPath + ".specifics.spawnRadiusX", 0);
    this.spawnRadiusY = configuration.getInt(initialPath + ".specifics.spawnRadiusY", 0);
    this.spawnRadiusZ = configuration.getInt(initialPath + ".specifics.spawnRadiusZ", 0);
  }

  @Override
  public void deserializeFromSingleLineString(ArrayList<String> arguments) {
    this.mobToSpawnType = arguments.get(0);
    this.spawnAmount = Integer.parseInt(arguments.get(1));

    this.usePlayerLocation = (arguments.size() < 3);

    if (!isUsePlayerLocation()) {
      final World world = Bukkit.getWorld(arguments.get(2));
      final Vector coordinates =
          new Vector(
              Integer.parseInt(arguments.get(3)),
              Integer.parseInt(arguments.get(4)),
              Integer.parseInt(arguments.get(5)));
      final Location location = coordinates.toLocation(world);

      this.spawnLocation = location;
    }
  }

  @Override
  public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
    return "Spawns Mob: " + getMobToSpawnType();
  }

  public int getSpawnRadiusX() {
    return spawnRadiusX;
  }

  public void setSpawnRadiusX(int spawnRadiusX) {
    this.spawnRadiusX = spawnRadiusX;
  }

  public int getSpawnRadiusY() {
    return spawnRadiusY;
  }

  public void setSpawnRadiusY(int spawnRadiusY) {
    this.spawnRadiusY = spawnRadiusY;
  }

  public int getSpawnRadiusZ() {
    return spawnRadiusZ;
  }

  public void setSpawnRadiusZ(int spawnRadiusZ) {
    this.spawnRadiusZ = spawnRadiusZ;
  }
}
