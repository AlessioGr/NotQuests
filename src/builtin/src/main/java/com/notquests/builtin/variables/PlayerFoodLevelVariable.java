package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

import java.util.ArrayList;
import java.util.List;

public final class PlayerFoodLevelVariable {
    private PlayerFoodLevelVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("FoodLevel")
                .displayName("Food Level")
                .description("Reads or changes the target player's hunger bar value from 0 to 20.")
                .singular("Food Level")
                .plural("Food Levels")
                .get((questPlayer, objects) -> questPlayer == null ? 0 : questPlayer.foodLevel())
                .set((newValue, questPlayer, objects) -> {
                    final double foodLevel = newValue.doubleValue();
                    return questPlayer != null
                            && Double.isFinite(foodLevel)
                            && questPlayer.setFoodLevel((int) Math.max(0.0d, Math.min(20.0d, foodLevel)));
                })
                .possibleValues((questPlayer, objects) -> wholeNumberValues(0, 20))
                .register();
    }

    private static List<String> wholeNumberValues(final int min, final int max) {
        final List<String> values = new ArrayList<>();
        for (int value = min; value <= max; value++) {
            values.add(String.valueOf(value));
        }
        return values;
    }
}
