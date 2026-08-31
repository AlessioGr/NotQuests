package com.notquests.paper.builtin.variables.hooks;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.paper.NotQuests;
import com.notquests.paper.PaperPlayer;

import java.util.UUID;

public final class FloodgateIsFloodgatePlayerVariable {
    private FloodgateIsFloodgatePlayerVariable() {}

    public static void register(final NotQuests main, final NotQuestsAdapter adapter) {
        adapter.variables()
                .booleanVariable("FloodgateIsFloodgatePlayer")
                .displayName("Floodgate Player")
                .description("Checks whether the target player joined through Floodgate.")
                .singular("Connected using Floodgate")
                .plural("Connected using Floodgate")
                .get(context -> isFloodgatePlayer(main, context.questPlayer()))
                .register();
    }

    private static boolean isFloodgatePlayer(final NotQuests main, final PlatformPlayer questPlayer) {
        if (questPlayer == null || questPlayer.playerIdentifier() == null || questPlayer.playerIdentifier().isBlank()) {
            return false;
        }
        try {
            return main.integrations()
                    .floodgate()
                    .isPlayerOnFloodgate(UUID.fromString(questPlayer.playerIdentifier()));
        } catch (final RuntimeException ignored) {
            return false;
        }
    }
}
