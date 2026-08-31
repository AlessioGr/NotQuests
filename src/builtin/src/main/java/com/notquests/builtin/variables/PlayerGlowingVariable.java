package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

public final class PlayerGlowingVariable {
    private PlayerGlowingVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .booleanVariable("Glowing")
                .displayName("Glowing")
                .description("Reads or changes whether the target player is glowing.")
                .singular("Glowing")
                .plural("Glowing")
                .get((questPlayer, objects) -> questPlayer != null && questPlayer.isGlowing())
                .set((newValue, questPlayer, objects) ->
                        questPlayer != null && questPlayer.setGlowing(Boolean.TRUE.equals(newValue)))
                .register();
    }
}
