package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

import java.util.ArrayList;
import java.util.List;

public final class PlayerHealthVariable {
    private PlayerHealthVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("Health")
                .displayName("Health")
                .description("Reads or changes the target player's current health.")
                .singular("Health")
                .plural("Health")
                .get((questPlayer, objects) -> questPlayer == null ? 0 : questPlayer.health())
                .set((newValue, questPlayer, objects) -> {
                    final double health = newValue.doubleValue();
                    return questPlayer != null
                            && Double.isFinite(health)
                            && questPlayer.setHealth(Math.max(0.0d, Math.min(questPlayer.maxHealth(), health)));
                })
                .possibleValues((questPlayer, objects) -> healthValues(questPlayer == null ? 20 : questPlayer.maxHealth()))
                .register();
    }

    private static List<String> healthValues(final double maxHealth) {
        final List<String> values = new ArrayList<>();
        for (double health = 0; health <= maxHealth; health += 0.5d) {
            values.add(String.valueOf(health));
        }
        return values;
    }
}
