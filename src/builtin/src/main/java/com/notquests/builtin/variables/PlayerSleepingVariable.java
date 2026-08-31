package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

public final class PlayerSleepingVariable {
    private PlayerSleepingVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .booleanVariable("Sleeping")
                .displayName("Sleeping")
                .description("Checks whether the target player is currently sleeping.")
                .singular("Sleeping")
                .plural("Sleeping")
                .get((questPlayer, objects) -> questPlayer != null && questPlayer.isSleeping())
                .register();
    }
}
