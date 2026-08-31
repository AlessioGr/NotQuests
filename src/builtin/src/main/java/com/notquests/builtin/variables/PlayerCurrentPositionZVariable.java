package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

public final class PlayerCurrentPositionZVariable {
    private PlayerCurrentPositionZVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("CurrentPositionZ")
                .displayName("Current Z Position")
                .description("Reads or changes the target player's current Z coordinate.")
                .singular("Z Position")
                .plural("Z Position")
                .get((questPlayer, objects) -> questPlayer == null ? 0 : questPlayer.positionZ())
                .set((newValue, questPlayer, objects) ->
                        questPlayer != null && questPlayer.setPositionZ(newValue.doubleValue()))
                .register();
    }
}
