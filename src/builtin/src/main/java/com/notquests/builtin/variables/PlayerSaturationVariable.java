package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

import java.util.ArrayList;
import java.util.List;

public final class PlayerSaturationVariable {
    private PlayerSaturationVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("Saturation")
                .displayName("Saturation")
                .description("Reads or changes the target player's hidden saturation value from 0 to 20.")
                .singular("Saturation")
                .plural("Saturation Values")
                .get((questPlayer, objects) -> questPlayer == null ? 0 : questPlayer.saturation())
                .set((newValue, questPlayer, objects) -> {
                    final double saturation = newValue.doubleValue();
                    return questPlayer != null
                            && Double.isFinite(saturation)
                            && questPlayer.setSaturation(Math.max(0.0d, Math.min(20.0d, saturation)));
                })
                .possibleValues((questPlayer, objects) -> halfStepValues(0, 20))
                .register();
    }

    private static List<String> halfStepValues(final int min, final int max) {
        final List<String> values = new ArrayList<>();
        for (double value = min; value <= max; value += 0.5d) {
            values.add(String.valueOf(value));
        }
        return values;
    }
}
