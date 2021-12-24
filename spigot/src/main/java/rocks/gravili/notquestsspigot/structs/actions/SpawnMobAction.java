/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021 Alessio Gravili
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

package rocks.gravili.notquestsspigot.structs.actions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.bukkit.parsers.WorldArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import rocks.gravili.notquestsspigot.NotQuests;
import rocks.gravili.notquestsspigot.commands.arguments.EntityTypeSelector;

public class SpawnMobAction extends Action {

    private String mobToSpawnType = "";
    private boolean usePlayerLocation = false;
    private Location spawnLocation;
    private int spawnAmount = 1;

    public SpawnMobAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ActionFor rewardFor) {
        manager.command(builder.literal("SpawnMob")
                .argument(EntityTypeSelector.of("entityType", main), ArgumentDescription.of("Type of Entity which should be spawned."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of mobs which should be spawned"))
                .literal("PlayerLocation", ArgumentDescription.of("Takes the location the player currently is in (when executing the action). So, this is a dynamic location."))
                .meta(CommandMeta.DESCRIPTION, "Creates a new SpawnMob Action")
                .handler((context) -> {
                    final String entityType = context.get("entityType");
                    final int amountToSpawn = context.get("amount");

                    SpawnMobAction spawnMobAction = new SpawnMobAction(main);
                    spawnMobAction.setMobToSpawnType(entityType);
                    spawnMobAction.setSpawnAmount(amountToSpawn);
                    spawnMobAction.setUsePlayerLocation(true);

                    main.getActionManager().addAction(spawnMobAction, context);
                }));

        manager.command(builder.literal("SpawnMob")
                .argument(EntityTypeSelector.of("entityType", main), ArgumentDescription.of("Type of Entity which should be spawned."))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of mobs which should be spawned"))
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
                 )*/ //Commented out, because this somehow breaks flags
                .argument(IntegerArgument.newBuilder("x"), ArgumentDescription.of("X coordinate"))
                .argument(IntegerArgument.newBuilder("y"), ArgumentDescription.of("Y coordinate"))
                .argument(IntegerArgument.newBuilder("z"), ArgumentDescription.of("Z coordinate"))
                .meta(CommandMeta.DESCRIPTION, "Creates a new SpawnMob Action")
                .handler((context) -> {
                    final String entityType = context.get("entityType");
                    final int amountToSpawn = context.get("amount");

                    SpawnMobAction spawnMobAction = new SpawnMobAction(main);
                    spawnMobAction.setMobToSpawnType(entityType);
                    spawnMobAction.setSpawnAmount(amountToSpawn);
                    spawnMobAction.setUsePlayerLocation(false);

                    final World world = context.get("world");
                    final Vector coordinates = new Vector(context.get("x"), context.get("y"), context.get("z"));
                    final Location location = coordinates.toLocation(world);

                    spawnMobAction.setSpawnLocation(location);

                    main.getActionManager().addAction(spawnMobAction, context);
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
    public void execute(final Player player, Object... objects) {
        if (!Bukkit.isPrimaryThread()) { //Can only be run in main thread (at least for bukkit entities) :(
            Bukkit.getScheduler().runTask(main.getMain(), () -> {
                execute2(player, objects);
            });
        } else {
            execute2(player, objects);
        }


    }

    public void execute2(final Player player, Object... objects) {
        try {
            EntityType entityType = EntityType.valueOf(getMobToSpawnType());

            if (isUsePlayerLocation()) {
                for (int i = 0; i < getSpawnAmount(); i++) {
                    player.getWorld().spawnEntity(player.getLocation().add(new Vector(0, 1, 0)), entityType);
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
                for (int i = 0; i < getSpawnAmount(); i++) {
                    getSpawnLocation().getWorld().spawnEntity(getSpawnLocation().add(new Vector(0, 1, 0)), entityType);
                }
            }
        } catch (IllegalArgumentException e) {
            if (main.getIntegrationsManager().isMythicMobsEnabled()) {
                if (isUsePlayerLocation()) {
                    main.getIntegrationsManager().getMythicMobsManager().spawnMob(getMobToSpawnType(), player.getLocation(), getSpawnAmount());
                } else {
                    main.getIntegrationsManager().getMythicMobsManager().spawnMob(getMobToSpawnType(), getSpawnLocation(), getSpawnAmount());
                }
            }
        }
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.mobToSpawn", getMobToSpawnType());
        configuration.set(initialPath + ".specifics.spawnLocation", getSpawnLocation());
        configuration.set(initialPath + ".specifics.usePlayerLocation", isUsePlayerLocation());
        configuration.set(initialPath + ".specifics.amount", getSpawnAmount());

    }

    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        this.mobToSpawnType = configuration.getString(initialPath + ".specifics.mobToSpawn", "");
        this.spawnLocation = configuration.getLocation(initialPath + ".specifics.spawnLocation");
        this.usePlayerLocation = configuration.getBoolean(initialPath + ".specifics.usePlayerLocation");
        this.spawnAmount = configuration.getInt(initialPath + ".specifics.amount", 1);
    }


    @Override
    public String getActionDescription() {
        return "Spawns Mob: " + getMobToSpawnType();
    }
}
