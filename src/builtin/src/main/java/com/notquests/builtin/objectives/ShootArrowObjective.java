package com.notquests.builtin.objectives;

import com.notquests.core.platform.LocationRegion;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.NotQuestsAdapter;

import java.util.Map;

public final class ShootArrowObjective {
    private static final String TARGET_LOCATION = "targetLocation";
    private static final String TARGET_REGION_MIN = "targetRegionMin";
    private static final String TARGET_REGION_MAX = "targetRegionMax";
    private static final String RADIUS = "radius";

    private ShootArrowObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.objectives().objective("ShootArrow")
                .displayName("Shoot Arrow")
                .description("Counts arrows the player lands inside a configured target region.")
                .field(
                        "amount",
                        adapter.fields().numberExpression(false).progressNeeded(),
                        "Number of arrows that must land inside the target region.")
                .field(
                        TARGET_LOCATION,
                        adapter.fields().storedLocation().config("specifics.targetLocation"),
                        "Center of the target arrow region.")
                .field(
                        TARGET_REGION_MIN,
                        adapter.fields().storedLocation().config("specifics.targetRegionMin"),
                        "Minimum corner of the target region when using a cuboid.")
                .field(
                        TARGET_REGION_MAX,
                        adapter.fields().storedLocation().config("specifics.targetRegionMax"),
                        "Maximum corner of the target region when using a cuboid.")
                .field(
                        RADIUS,
                        adapter.fields().storedNumber(1).config("specifics.radius"),
                        "Radius around the target center where arrows count.")
                .taskDescription((objective, questPlayer, activeObjective) -> {
                    final NQLocation location = objective.location(TARGET_LOCATION);
                    return adapter.objectiveTaskText(
                            "chat.objectives.taskDescription.shootArrow.base",
                            questPlayer,
                            activeObjective,
                            Map.of(
                                    "%COORDINATES%",
                                    location == null ? "???" : location.blockDescription(),
                                    "%WORLDNAME%",
                                    location == null ? "???" : location.worldName(),
                                    "%RADIUS%",
                                    "" + objective.number(RADIUS, 1)));
                })
                .onPlayerShootProjectileHit((event, objective) -> {
                    if (countsArrowLocation(
                            objective.location(TARGET_LOCATION),
                            objective.location(TARGET_REGION_MIN),
                            objective.location(TARGET_REGION_MAX),
                            objective.number(RADIUS, 1),
                            event.location())) {
                        objective.addProgress(1);
                    }
                })
                .register();
    }

    private static boolean countsArrowLocation(
            final NQLocation targetLocation,
            final NQLocation targetRegionMin,
            final NQLocation targetRegionMax,
            final double radius,
            final NQLocation arrowLocation) {
        if (targetRegionMin != null && targetRegionMax != null) {
            return new LocationRegion(targetRegionMin, targetRegionMax).contains(arrowLocation);
        }
        return LocationRegion.withinRadius(targetLocation, radius, arrowLocation);
    }
}
