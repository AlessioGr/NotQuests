package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

public final class PlayerPingVariable {
    private PlayerPingVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("Ping")
                .displayName("Ping")
                .description("Reads the target player's current network ping in milliseconds.")
                .singular("Ping")
                .plural("Ping")
                .get((questPlayer, objects) -> questPlayer == null ? 0 : questPlayer.pingMillis())
                .register();
    }
}
