package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

public final class PlayerCurrentPositionXVariable {
    private PlayerCurrentPositionXVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("CurrentPositionX")
                .displayName("Current X Position")
                .description("Reads or changes the target player's current X coordinate.")
                .singular("X Position")
                .plural("X Position")
                .get((questPlayer, objects) -> questPlayer == null ? 0 : questPlayer.positionX())
                .set((newValue, questPlayer, objects) ->
                        questPlayer != null && questPlayer.setPositionX(newValue.doubleValue()))
                .register();
    }
}
