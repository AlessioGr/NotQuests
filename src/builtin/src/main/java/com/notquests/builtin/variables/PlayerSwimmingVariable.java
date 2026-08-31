package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

public final class PlayerSwimmingVariable {
    private PlayerSwimmingVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables().booleanVariable("Swimming")
                .displayName("Swimming")
                .description("Reads or changes whether the target player is swimming.")
                .singular("Swimming")
                .plural("Swimming")
                .get((questPlayer, objects) -> questPlayer != null && questPlayer.isSwimming())
                .set((newValue, questPlayer, objects) ->
                        questPlayer != null && questPlayer.setSwimming(Boolean.TRUE.equals(newValue)))
                .register();
    }
}
