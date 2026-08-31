package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

public final class PlayerClimbingVariable {
    private PlayerClimbingVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .booleanVariable("Climbing")
                .displayName("Climbing")
                .description("Checks whether the target player is currently climbing.")
                .singular("Climbing")
                .plural("Climbing")
                .get((questPlayer, objects) -> questPlayer != null && questPlayer.isClimbing())
                .register();
    }
}
