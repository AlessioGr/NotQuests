package com.notquests.builtin.objectives;

import com.notquests.core.platform.LocationRegion;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.NotQuestsAdapter;

import java.util.Map;

public final class ReachLocationObjective {
    private static final String MIN_LOCATION = "minLocation";
    private static final String MAX_LOCATION = "maxLocation";
    private static final String LOCATION_NAME = "locationName";

    private ReachLocationObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.objectives().objective("ReachLocation")
                .displayName("Reach Location")
                .description("Completes when the player enters a configured location region.")
                .field(
                        MIN_LOCATION,
                        adapter.fields().storedLocation().config("specifics.minLocation"),
                        "Minimum corner of the target region.")
                .field(
                        MAX_LOCATION,
                        adapter.fields().storedLocation().config("specifics.maxLocation"),
                        "Maximum corner of the target region.")
                .field(
                        LOCATION_NAME,
                        adapter.fields().greedyText().config("specifics.locationName"),
                        "Location name shown to players in objective task text.")
                .taskDescription((objective, questPlayer, activeObjective) -> adapter.objectiveTaskText(
                        "chat.objectives.taskDescription.reachLocation.base",
                        questPlayer,
                        activeObjective,
                        Map.of("%LOCATIONNAME%", objective.text(LOCATION_NAME))))
                .onPlayerMove((event, objective) -> {
                    if (new LocationRegion(objective.location(MIN_LOCATION), objective.location(MAX_LOCATION))
                            .contains(event.to())) {
                        objective.addProgress(1);
                    }
                })
                .register();
    }
}
