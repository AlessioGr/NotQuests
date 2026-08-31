package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

public final class PlayerCurrentPositionYVariable {
    private PlayerCurrentPositionYVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("CurrentPositionY")
                .displayName("Current Y Position")
                .description("Reads or changes the target player's current Y coordinate.")
                .singular("Y Position")
                .plural("Y Position")
                .get((questPlayer, objects) -> questPlayer == null ? 0 : questPlayer.positionY())
                .set((newValue, questPlayer, objects) ->
                        questPlayer != null && questPlayer.setPositionY(newValue.doubleValue()))
                .register();
    }
}
