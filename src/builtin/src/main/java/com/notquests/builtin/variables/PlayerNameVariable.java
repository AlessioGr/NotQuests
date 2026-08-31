package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

public final class PlayerNameVariable {
    private PlayerNameVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables().stringVariable("Name")
                .displayName("Name")
                .description("Reads or changes the target player's display name.")
                .singular("Name")
                .plural("Names")
                .get((questPlayer, objects) -> questPlayer == null ? "" : questPlayer.displayName())
                .set((newValue, questPlayer, objects) -> questPlayer != null && questPlayer.setDisplayName(newValue))
                .possibleValues((questPlayer, objects) -> adapter.onlinePlayerNames())
                .register();
    }
}
