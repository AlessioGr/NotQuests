package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

import java.util.List;

public final class DistanceToLocationVariable {
    private DistanceToLocationVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("DistanceToLocation")
                .displayName("Distance to Location")
                .description("Measures the player's distance in blocks to a configured world location.")
                .singular("Distance")
                .plural("Distances")
                .field("world", adapter.fields().text(adapter::worldNames), "World that contains the target location.")
                .field("x", adapter.fields().doubleNumber(0), "Target X coordinate.")
                .field("y", adapter.fields().doubleNumber(0), "Target Y coordinate.")
                .field("z", adapter.fields().doubleNumber(0), "Target Z coordinate.")
                .get(context -> {
                    if (context.questPlayer() == null) {
                        return Double.MAX_VALUE;
                    }
                    return context.questPlayer().distanceTo(adapter.location(
                            context.text("world"),
                            context.number("x", 0),
                            context.number("y", 0),
                            context.number("z", 0)));
                })
                .possibleValues(context -> List.of("0", "5", "10", "25", "50", "100"))
                .register();
    }
}
