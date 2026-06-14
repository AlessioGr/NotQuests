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

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.commands.framework.NQFlag;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.Map;

import static rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableArgument.numberVariableArgument;

public class InteractObjective extends Objective {

    private Location locationToInteract;
    private boolean leftClick = false;
    private boolean rightClick = false;
    private int maxDistance = 1;
    private boolean cancelInteraction = false;

    public InteractObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            NQCommandManager manager,
            NQCommandBuilder addObjectiveBuilder,
            final int level) {
        manager.command(addObjectiveBuilder
                .required("amount", numberVariableArgument("amount", null), NQDescription.of("Amount of interactions needed"))
                .required("world", NQArguments.worldArgument(), NQDescription.of("World name"))
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
                .required("x", NQArguments.integerArgument(), NQDescription.of("X coordinate"))
                .required("y", NQArguments.integerArgument(), NQDescription.of("Y coordinate"))
                .required("z", NQArguments.integerArgument(), NQDescription.of("Z coordinate"))
                .flag(NQFlag.builder("leftClick").withDescription(NQDescription.of("Count left-clicks of the location.")).build())
                .flag(NQFlag.builder("rightClick").withDescription(NQDescription.of("Count right-clicks of the location.")).build())
                .flag(NQFlag.builder("cancelInteraction").withDescription(NQDescription.of("Makes it so the interaction will be cancelled while this objective is active")).build())
                .flag(main.getCommandManager().maxDistance)
                .handler(
                        (context) -> {
                            final String amountExpression = context.get("amount");

                            final World world = context.get("world");
                            final Vector coordinates =
                                    new Vector(context.get("x"), context.get("y"), context.get("z"));
                            final Location location = coordinates.toLocation(world);

                            final boolean leftClick = context.flags().isPresent("leftClick");
                            final boolean rightClick = context.flags().isPresent("rightClick");
                            final int maxDistance =
                                    context.flags().getValue(main.getCommandManager().maxDistance, 1);
                            final boolean cancelInteraction = context.flags().isPresent("cancelInteraction");

                            InteractObjective interactObjective = new InteractObjective(main);
                            interactObjective.setLocationToInteract(location);
                            interactObjective.setLeftClick(leftClick);
                            interactObjective.setRightClick(rightClick);
                            interactObjective.setMaxDistance(maxDistance);
                            interactObjective.setCancelInteraction(cancelInteraction);
                            interactObjective.setProgressNeededExpression(amountExpression);

                            main.getObjectiveManager().addObjective(interactObjective, context, level);
                        }));
    }

    @Override
    public String getTaskDescriptionInternal(
            final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
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

        return main.getLanguageManager()
                .getString(
                        "chat.objectives.taskDescription.interact.base",
                        questPlayer,
                        activeObjective,
                        Map.of(
                                "%INTERACTTYPE%", interactType,
                                "%COORDINATES%",
                                "X: "
                                        + getLocationToInteract().getX()
                                        + " Y: "
                                        + getLocationToInteract().getY()
                                        + " Z: "
                                        + getLocationToInteract().getZ(),
                                "%WORLDNAME%", worldName));
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.locationToInteract", getLocationToInteract());
        configuration.set(initialPath + ".specifics.leftClick", isLeftClick());
        configuration.set(initialPath + ".specifics.rightClick", isRightClick());

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
        maxDistance = configuration.getInt(initialPath + ".specifics.maxDistance", 1);
        cancelInteraction =
                configuration.getBoolean(initialPath + ".specifics.cancelInteraction", false);
    }

    @Override
    public void onObjectiveUnlock(
            final ActiveObjective activeObjective,
            final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
    }

    @Override
    public void onObjectiveCompleteOrLock(
            final ActiveObjective activeObjective,
            final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
            final boolean completed) {
    }

    public final Location getLocationToInteract() {
        return locationToInteract;
    }

    public void setLocationToInteract(final Location locationToInteract) {
        this.locationToInteract = locationToInteract;
    }

    public final boolean isLeftClick() {
        return leftClick;
    }

    public void setLeftClick(final boolean leftClick) {
        this.leftClick = leftClick;
    }

    public final boolean isRightClick() {
        return rightClick;
    }

    public void setRightClick(final boolean rightClick) {
        this.rightClick = rightClick;
    }

    public final int getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(final int maxDistance) {
        this.maxDistance = maxDistance;
    }

    public final boolean isCancelInteraction() {
        return cancelInteraction;
    }

    public void setCancelInteraction(final boolean cancelInteraction) {
        this.cancelInteraction = cancelInteraction;
    }
}
