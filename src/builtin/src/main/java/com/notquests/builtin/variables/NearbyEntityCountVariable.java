package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;

import java.util.List;

public final class NearbyEntityCountVariable {
    private NearbyEntityCountVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("NearbyEntityCount")
                .displayName("Nearby Entity Count")
                .description("Counts matching entities near the player inside a configurable radius.")
                .singular("Nearby Entity Count")
                .plural("Nearby Entity Counts")
                .field("entityType", adapter.fields().entityType(), "Entity type to count, or any for all nearby entities.")
                .field("radius", adapter.fields().doubleNumber(0), "Radius in blocks around the player.")
                .get(context -> nearbyEntityCount(
                        context.questPlayer(),
                        context.text("entityType"),
                        Math.max(0, context.number("radius", 0))))
                .possibleValues(context -> List.of("0", "1", "3", "5", "10", "25"))
                .register();
    }

    private static int nearbyEntityCount(
            final PlatformPlayer player,
            final String configuredEntityType,
            final double radius) {
        if (player == null) {
            return 0;
        }
        final String expected = configuredEntityType == null ? "" : configuredEntityType;
        final boolean any = expected.equalsIgnoreCase("any");
        int count = 0;
        for (final String entityType : player.nearbyEntityTypeIds(radius)) {
            final String fullId = entityType == null ? "" : entityType;
            final int namespace = fullId.indexOf(':');
            final String shortId = namespace < 0 ? fullId : fullId.substring(namespace + 1);
            if (any || fullId.equalsIgnoreCase(expected) || shortId.equalsIgnoreCase(expected)) {
                count++;
            }
        }
        return count;
    }
}
