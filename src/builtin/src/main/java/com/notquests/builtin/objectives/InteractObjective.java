package com.notquests.builtin.objectives;

import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.NotQuestsAdapter;

import java.util.Map;

public final class InteractObjective {
    private static final String LOCATION = "locationToInteract";
    private static final String LEFT_CLICK = "leftClick";
    private static final String RIGHT_CLICK = "rightClick";
    private static final String MAX_DISTANCE = "maxDistance";
    private static final String CANCEL_INTERACTION = "cancelInteraction";

    private InteractObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.objectives().objective("Interact")
                .displayName("Interact")
                .description("Counts clicks on a configured block or location.")
                .field(
                        "amount",
                        adapter.fields().numberExpression(false).progressNeeded(),
                        "Number of matching interactions required.")
                .field(
                        LOCATION,
                        adapter.fields().storedLocation().config("specifics.locationToInteract"),
                        "Block or location the player must interact with.")
                .flag(
                        LEFT_CLICK,
                        adapter.fields().presenceFlag().config("specifics.leftClick"),
                        "Whether left-clicks count.")
                .flag(
                        RIGHT_CLICK,
                        adapter.fields().presenceFlag().config("specifics.rightClick"),
                        "Whether right-clicks count.")
                .flag(
                        MAX_DISTANCE,
                        adapter.fields().storedInteger(1).config("specifics.maxDistance"),
                        "Maximum distance in blocks from the configured location.")
                .flag(
                        CANCEL_INTERACTION,
                        adapter.fields().presenceFlag().config("specifics.cancelInteraction"),
                        "Whether matching interactions are cancelled while this objective is active.")
                .taskDescription((objective, questPlayer, activeObjective) -> {
                    final NQLocation location = objective.location(LOCATION);
                    final String worldName = location == null ? "???" : location.worldName();
                    final String coordinates = location == null ? "???" : location.blockDescription();
                    return adapter.objectiveTaskText(
                            "chat.objectives.taskDescription.interact.base",
                            questPlayer,
                            activeObjective,
                            Map.of(
                                    "%INTERACTTYPE%",
                                    interactType(objective.flag(LEFT_CLICK), objective.flag(RIGHT_CLICK)),
                                    "%COORDINATES%",
                                    coordinates,
                                    "%WORLDNAME%",
                                    worldName));
                })
                .onPlayerInteractBlock((event, objective) -> {
                    final NQLocation target = objective.location(LOCATION);
                    final NQLocation clicked = event.location();
                    if (target == null || clicked == null || !target.sameWorld(clicked)) {
                        return;
                    }
                    if (!countsClick(event.leftClick(), event.rightClick(), objective.flag(LEFT_CLICK), objective.flag(RIGHT_CLICK))) {
                        return;
                    }
                    final int maxDistance = objective.integer(MAX_DISTANCE, 1);
                    if (target.distanceSquared(clicked) > maxDistance * maxDistance) {
                        return;
                    }
                    objective.addProgress(1);
                    if (objective.flag(CANCEL_INTERACTION)) {
                        event.cancel();
                    }
                })
                .register();
    }

    private static String interactType(final boolean leftClick, final boolean rightClick) {
        if (leftClick && rightClick) {
            return "Left/Right-Click";
        }
        if (leftClick) {
            return "Left-Click";
        }
        if (rightClick) {
            return "Right-Click";
        }
        return "Interact with";
    }

    private static boolean countsClick(
            final boolean eventLeftClick,
            final boolean eventRightClick,
            final boolean countLeftClick,
            final boolean countRightClick) {
        if (!countLeftClick && !countRightClick) {
            return eventLeftClick || eventRightClick;
        }
        return (eventLeftClick && countLeftClick) || (eventRightClick && countRightClick);
    }
}
