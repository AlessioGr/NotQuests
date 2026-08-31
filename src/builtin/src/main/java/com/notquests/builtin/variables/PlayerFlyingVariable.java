package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

public final class PlayerFlyingVariable {
    private PlayerFlyingVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables().booleanVariable("Flying")
                .displayName("Flying")
                .description("Reads or changes whether the target player is flying.")
                .singular("Flying")
                .plural("Flying")
                .get((questPlayer, objects) -> questPlayer != null && questPlayer.isFlying())
                .set((newValue, questPlayer, objects) ->
                        questPlayer != null && questPlayer.setFlying(Boolean.TRUE.equals(newValue)))
                .register();
    }
}
