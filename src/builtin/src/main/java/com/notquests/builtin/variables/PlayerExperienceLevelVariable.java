package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

public final class PlayerExperienceLevelVariable {
    private PlayerExperienceLevelVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("ExperienceLevel")
                .displayName("Experience Level")
                .description("Reads or changes the target player's experience level.")
                .singular("Experience Level")
                .plural("Experience Levels")
                .get((questPlayer, objects) -> questPlayer == null ? 0 : questPlayer.experienceLevel())
                .set((newValue, questPlayer, objects) -> {
                    final double level = newValue.doubleValue();
                    return questPlayer != null
                            && Double.isFinite(level)
                            && questPlayer.setExperienceLevel(nonNegativeInt(level));
                })
                .register();
    }

    private static int nonNegativeInt(final double value) {
        return (int) Math.max(0.0d, Math.min(Integer.MAX_VALUE, value));
    }
}
