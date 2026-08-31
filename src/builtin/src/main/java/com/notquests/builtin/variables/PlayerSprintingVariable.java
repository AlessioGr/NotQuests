package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

public final class PlayerSprintingVariable {
    private PlayerSprintingVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables().booleanVariable("Sprinting")
                .displayName("Sprinting")
                .description("Reads or changes whether the target player is sprinting.")
                .singular("Sprinting")
                .plural("Sprinting")
                .get((questPlayer, objects) -> questPlayer != null && questPlayer.isSprinting())
                .set((newValue, questPlayer, objects) ->
                        questPlayer != null && questPlayer.setSprinting(Boolean.TRUE.equals(newValue)))
                .register();
    }
}
