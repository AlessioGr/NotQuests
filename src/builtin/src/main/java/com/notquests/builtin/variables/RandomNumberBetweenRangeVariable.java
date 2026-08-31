package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomNumberBetweenRangeVariable {
    private RandomNumberBetweenRangeVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("RandomNumberBetweenRange")
                .displayName("Random Number Between Range")
                .description("Returns a random whole number between the configured minimum and maximum.")
                .singular("Random Number")
                .plural("Random Numbers")
                .field("min", adapter.fields().doubleNumber(0), "Lowest possible returned number.")
                .field("max", adapter.fields().doubleNumber(0), "Highest possible returned number.")
                .get(context -> {
                    final int first = (int) Math.round(context.number("min", 0));
                    final int second = (int) Math.round(context.number("max", 0));
                    final int min = Math.min(first, second);
                    final int max = Math.max(first, second);
                    return min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
                })
                .possibleValues(context -> List.of("0", "1", "5", "10", "25", "100"))
                .register();
    }
}
