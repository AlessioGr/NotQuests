package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

public final class PlayerInLavaVariable {
    private PlayerInLavaVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .booleanVariable("InLava")
                .displayName("In Lava")
                .description("Checks whether the target player is currently in lava.")
                .singular("In lava")
                .plural("In lava")
                .get((questPlayer, objects) -> questPlayer != null && questPlayer.isInLava())
                .register();
    }
}
