package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

public final class PlayerSneakingVariable {
    private PlayerSneakingVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables().booleanVariable("Sneaking")
                .displayName("Sneaking")
                .description("Reads or changes whether the target player is sneaking.")
                .singular("Sneaking")
                .plural("Sneaking")
                .get((questPlayer, objects) -> questPlayer != null && questPlayer.isSneaking())
                .set((newValue, questPlayer, objects) ->
                        questPlayer != null && questPlayer.setSneaking(Boolean.TRUE.equals(newValue)))
                .register();
    }
}
