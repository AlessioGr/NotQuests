package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

public final class PlayerInWaterVariable {
    private PlayerInWaterVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .booleanVariable("InWater")
                .displayName("In Water")
                .description("Checks whether the target player is currently in water.")
                .singular("In water")
                .plural("In water")
                .get((questPlayer, objects) -> questPlayer != null && questPlayer.isInWater())
                .register();
    }
}
