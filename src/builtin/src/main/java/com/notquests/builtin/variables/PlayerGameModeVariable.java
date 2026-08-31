package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

import java.util.List;

public final class PlayerGameModeVariable {
    private PlayerGameModeVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .stringVariable("GameMode")
                .displayName("Game Mode")
                .description("Reads or changes the target player's game mode.")
                .singular("GameMode")
                .plural("GameMode")
                .get((questPlayer, objects) -> questPlayer == null ? "" : questPlayer.gameMode())
                .set((newValue, questPlayer, objects) -> questPlayer != null && questPlayer.setGameMode(newValue))
                .possibleValues((questPlayer, objects) ->
                        questPlayer == null ? List.of() : questPlayer.availableGameModes())
                .register();
    }
}
