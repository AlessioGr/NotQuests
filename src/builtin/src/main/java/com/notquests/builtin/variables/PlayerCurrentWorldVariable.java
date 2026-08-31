package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

import java.util.List;

public final class PlayerCurrentWorldVariable {
    private PlayerCurrentWorldVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .stringVariable("CurrentWorld")
                .displayName("Current World")
                .description("Reads or changes the world the target player is currently in.")
                .singular("World")
                .plural("Worlds")
                .get((questPlayer, objects) -> questPlayer == null ? "" : questPlayer.worldName())
                .set((newValue, questPlayer, objects) ->
                        questPlayer != null && questPlayer.teleportToWorldSpawn(newValue))
                .possibleValues((questPlayer, objects) ->
                        questPlayer == null ? List.of() : questPlayer.availableWorldNames())
                .register();
    }
}
