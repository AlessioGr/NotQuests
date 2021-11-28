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

package rocks.gravili.notquests.Structs.Objectives;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.bukkit.parsers.WorldArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Quest;


public class InteractObjective extends Objective {

    private final NotQuests main;
    private final Location locationToInteract;
    private final boolean leftClick;
    private final boolean rightClick;
    private final String taskDescription;
    private final int maxDistance;
    private final boolean cancelInteraction;

    public InteractObjective(NotQuests main, final Quest quest, final int objectiveID, int amountToInteract, Location locationToInteract, boolean leftClick, boolean rightClick, int maxDistance, boolean cancelInteraction, String taskDescription) {
        super(main, quest, objectiveID, amountToInteract);
        this.main = main;
        this.locationToInteract = locationToInteract;
        this.leftClick = leftClick;
        this.rightClick = rightClick;
        this.taskDescription = taskDescription;
        this.maxDistance = maxDistance;
        this.cancelInteraction = cancelInteraction;
    }

    public InteractObjective(NotQuests main, Quest quest, int objectiveNumber, int progressNeeded) {
        super(main, quest, objectiveNumber, progressNeeded);
        final String questName = quest.getQuestName();

        this.main = main;
        locationToInteract = main.getDataManager().getQuestsConfig().getLocation("quests." + questName + ".objectives." + objectiveNumber + ".specifics.locationToInteract");
        leftClick = main.getDataManager().getQuestsConfig().getBoolean("quests." + questName + ".objectives." + objectiveNumber + ".specifics.leftClick", false);
        rightClick = main.getDataManager().getQuestsConfig().getBoolean("quests." + questName + ".objectives." + objectiveNumber + ".specifics.rightClick", false);
        taskDescription = main.getDataManager().getQuestsConfig().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.taskDescription", "");
        maxDistance = main.getDataManager().getQuestsConfig().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.maxDistance", 1);
        cancelInteraction = main.getDataManager().getQuestsConfig().getBoolean("quests." + questName + ".objectives." + objectiveNumber + ".specifics.cancelInteraction", false);

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
                    final Audience audience = main.adventure().sender(context.getSender());
                    final Quest quest = context.get("quest");
                    final int amount = context.get("amount");

                    final World world = context.get("world");
                    final Vector coordinates = new Vector(context.get("x"), context.get("y"), context.get("z"));
                    final Location location = coordinates.toLocation(world);

                    final boolean leftClick = context.flags().isPresent("leftClick");
                    final boolean rightClick = context.flags().isPresent("rightClick");
                    final String taskDescription = context.flags().getValue(main.getCommandManager().taskDescription, "");
                    final int maxDistance = context.flags().getValue(main.getCommandManager().maxDistance, 1);
                    final boolean cancelInteraction = context.flags().isPresent("cancelInteraction");

                    InteractObjective interactObjective = new InteractObjective(main, quest, quest.getObjectives().size() + 1, amount, location, leftClick, rightClick, maxDistance, cancelInteraction, taskDescription);
                    quest.addObjective(interactObjective, true);
                    audience.sendMessage(MiniMessage.miniMessage().parse(
                            NotQuestColors.successGradient + "Interact Objective successfully added to Quest " + NotQuestColors.highlightGradient
                                    + quest.getQuestName() + "</gradient>!</gradient>"
                    ));

                }));
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
                    .replaceAll("%EVENTUALCOLOR%", eventualColor)
                    .replaceAll("%INTERACTTYPE%", interactType)
                    .replaceAll("%COORDINATES%", "X: " + getLocationToInteract().getX() + " Y: " + getLocationToInteract().getY() + " Z: " + getLocationToInteract().getZ())
                    .replaceAll("%WORLDNAME%", worldName);
        } else {
            toReturn = main.getLanguageManager().getString("chat.objectives.taskDescription.interact.taskDescriptionProvided", player)
                    .replaceAll("%TASKDESCRIPTION%", getTaskDescription())
                    .replaceAll("%EVENTUALCOLOR%", eventualColor)
                    .replaceAll("%INTERACTTYPE%", interactType)
                    .replaceAll("%COORDINATES%", "X: " + getLocationToInteract().getX() + " Y: " + getLocationToInteract().getY() + " Z: " + getLocationToInteract().getZ())
                    .replaceAll("%WORLDNAME%", worldName);
        }

        return toReturn;
    }

    @Override
    public void save() {
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.locationToInteract", getLocationToInteract());
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.leftClick", isLeftClick());
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.rightClick", isRightClick());
        if (!getTaskDescription().isBlank()) {
            main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.taskDescription", getTaskDescription());
        }
        if (getMaxDistance() > 1) {
            main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.maxDistance", getMaxDistance());
        }
        main.getDataManager().getQuestsConfig().set("quests." + getQuest().getQuestName() + ".objectives." + getObjectiveID() + ".specifics.cancelInteraction", isCancelInteraction());


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
