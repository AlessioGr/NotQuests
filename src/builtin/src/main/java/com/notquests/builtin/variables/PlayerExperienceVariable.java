package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

public final class PlayerExperienceVariable {
    private PlayerExperienceVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("Experience")
                .displayName("Experience")
                .description("Reads or changes the target player's total experience points.")
                .singular("Experience")
                .plural("Experience")
                .get((questPlayer, objects) -> questPlayer == null ? 0 : questPlayer.experiencePoints())
                .set((newValue, questPlayer, objects) -> {
                    final double points = newValue.doubleValue();
                    return questPlayer != null
                            && Double.isFinite(points)
                            && questPlayer.setExperiencePoints(nonNegativeInt(points));
                })
                .register();
    }

    private static int nonNegativeInt(final double value) {
        return (int) Math.max(0.0d, Math.min(Integer.MAX_VALUE, value));
    }
}
