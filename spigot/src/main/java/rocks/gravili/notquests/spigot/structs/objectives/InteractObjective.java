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

package rocks.gravili.notquests.spigot.structs.objectives;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.bukkit.parsers.WorldArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import rocks.gravili.notquests.spigot.NotQuests;
import rocks.gravili.notquests.spigot.structs.ActiveObjective;


public class InteractObjective extends Objective {

    private Location locationToInteract;
    private boolean leftClick = false;
    private boolean rightClick = false;
    private String taskDescription = "";
    private int maxDistance = 1;
    private boolean cancelInteraction = false;

    public InteractObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder.literal("Interact")
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of interactions needed."))
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
                .flag(
                        manager.flagBuilder("leftClick")
                                .withDescription(ArgumentDescription.of("Count left-clicks of the location."))
                )
                .flag(
                        manager.flagBuilder("rightClick")
                                .withDescription(ArgumentDescription.of("Count right-clicks of the location."))
                )
                .flag(
                        manager.flagBuilder("cancelInteraction")
                                .withDescription(ArgumentDescription.of("Makes it so the interaction will be cancelled while this objective is active"))
                )
                .flag(main.getCommandManager().taskDescription)
                .flag(main.getCommandManager().maxDistance)
                .meta(CommandMeta.DESCRIPTION, "Adds a new Interact Objective to a quest")
                .handler((context) -> {
                    final int amount = context.get("amount");

                    final World world = context.get("world");
                    final Vector coordinates = new Vector(context.get("x"), context.get("y"), context.get("z"));
                    final Location location = coordinates.toLocation(world);

                    final boolean leftClick = context.flags().isPresent("leftClick");
                    final boolean rightClick = context.flags().isPresent("rightClick");
                    final String taskDescription = context.flags().getValue(main.getCommandManager().taskDescription, "");
                    final int maxDistance = context.flags().getValue(main.getCommandManager().maxDistance, 1);
                    final boolean cancelInteraction = context.flags().isPresent("cancelInteraction");

                    InteractObjective interactObjective = new InteractObjective(main);
                    interactObjective.setLocationToInteract(location);
                    interactObjective.setLeftClick(leftClick);
                    interactObjective.setRightClick(rightClick);
                    interactObjective.setTaskDescription(taskDescription);
                    interactObjective.setMaxDistance(maxDistance);
                    interactObjective.setCancelInteraction(cancelInteraction);
                    interactObjective.setProgressNeeded(amount);

                    main.getObjectiveManager().addObjective(interactObjective, context);
                }));
    }

    public void setLocationToInteract(final Location locationToInteract) {
        this.locationToInteract = locationToInteract;
    }

    public void setLeftClick(final boolean leftClick) {
        this.leftClick = leftClick;
    }

    public void setRightClick(final boolean rightClick) {
        this.rightClick = rightClick;
    }

    public void setTaskDescription(final String taskDescription) {
        this.taskDescription = taskDescription;
    }

    public void setMaxDistance(final int maxDistance) {
        this.maxDistance = maxDistance;
    }

    public void setCancelInteraction(final boolean cancelInteraction) {
        this.cancelInteraction = cancelInteraction;
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        String toReturn;
        String interactType = "";
        if (isLeftClick()) {
            interactType = "Left-Click";
        }
        if (isRightClick()) {
            interactType = "Right-Click";
        }
        if (isLeftClick() && isRightClick()) {
            interactType = "Left/Right-Click";
        }

        String worldName = "???";
        if (getLocationToInteract().getWorld() != null) {
            worldName = getLocationToInteract().getWorld().getName();
        }

        if (taskDescription.isBlank()) {
            toReturn = main.getLanguageManager().getString("chat.objectives.taskDescription.interact.base", player)
                    .replace("%EVENTUALCOLOR%", eventualColor)
                    .replace("%INTERACTTYPE%", interactType)
                    .replace("%COORDINATES%", "X: " + getLocationToInteract().getX() + " Y: " + getLocationToInteract().getY() + " Z: " + getLocationToInteract().getZ())
                    .replace("%WORLDNAME%", worldName);
        } else {
            toReturn = main.getLanguageManager().getString("chat.objectives.taskDescription.interact.taskDescriptionProvided", player)
                    .replace("%TASKDESCRIPTION%", getTaskDescription())
                    .replace("%EVENTUALCOLOR%", eventualColor)
                    .replace("%INTERACTTYPE%", interactType)
                    .replace("%COORDINATES%", "X: " + getLocationToInteract().getX() + " Y: " + getLocationToInteract().getY() + " Z: " + getLocationToInteract().getZ())
                    .replace("%WORLDNAME%", worldName);
        }

        return toReturn;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.locationToInteract", getLocationToInteract());
        configuration.set(initialPath + ".specifics.leftClick", isLeftClick());
        configuration.set(initialPath + ".specifics.rightClick", isRightClick());
        if (!getTaskDescription().isBlank()) {
            configuration.set(initialPath + ".specifics.taskDescription", getTaskDescription());
        }
        if (getMaxDistance() > 1) {
            configuration.set(initialPath + ".specifics.maxDistance", getMaxDistance());
        }
        configuration.set(initialPath + ".specifics.cancelInteraction", isCancelInteraction());

    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        locationToInteract = configuration.getLocation(initialPath + ".specifics.locationToInteract");
        leftClick = configuration.getBoolean(initialPath + ".specifics.leftClick", false);
        rightClick = configuration.getBoolean(initialPath + ".specifics.rightClick", false);
        taskDescription = configuration.getString(initialPath + ".specifics.taskDescription", "");
        maxDistance = configuration.getInt(initialPath + ".specifics.maxDistance", 1);
        cancelInteraction = configuration.getBoolean(initialPath + ".specifics.cancelInteraction", false);

    }


    @Override
    public void onObjectiveUnlock(final ActiveObjective activeObjective) {

    }

    public final Location getLocationToInteract() {
        return locationToInteract;
    }

    public final boolean isLeftClick() {
        return leftClick;
    }

    public final boolean isRightClick() {
        return rightClick;
    }

    public final String getTaskDescription() {
        return taskDescription;
    }

    public final int getMaxDistance() {
        return maxDistance;
    }

    public final boolean isCancelInteraction() {
        return cancelInteraction;
    }


}
