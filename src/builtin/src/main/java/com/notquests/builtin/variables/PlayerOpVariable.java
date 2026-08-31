package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

public final class PlayerOpVariable {
    private PlayerOpVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .booleanVariable("Op")
                .displayName("Operator")
                .description("Reads or changes whether the target player is a server operator.")
                .singular("Op")
                .plural("Op")
                .get((questPlayer, objects) -> questPlayer != null && questPlayer.isOperator())
                .set((newValue, questPlayer, objects) ->
                        questPlayer != null && questPlayer.setOperator(Boolean.TRUE.equals(newValue)))
                .register();
    }
}
