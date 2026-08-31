package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class ChanceVariable {
    private ChanceVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .booleanVariable("Chance")
                .displayName("Chance")
                .description("Randomly returns true based on the configured percent chance.")
                .singular("Chance")
                .plural("Chances")
                .field("chance", adapter.fields().doubleNumber(0), "Percent chance from 0 to 100 that this variable returns true.")
                .get(context -> ThreadLocalRandom.current().nextDouble(100.0) < context.number("chance", 0))
                .possibleValues(context -> List.of("true", "false"))
                .register();
    }
}
