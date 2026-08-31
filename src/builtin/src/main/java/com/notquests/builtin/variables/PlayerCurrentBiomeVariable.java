package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

import java.util.List;

public final class PlayerCurrentBiomeVariable {
    private PlayerCurrentBiomeVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .stringVariable("CurrentBiome")
                .displayName("Current Biome")
                .description("Reads the biome at the target player's current location.")
                .singular("Biome")
                .plural("Biomes")
                .get((questPlayer, objects) -> questPlayer == null ? "" : questPlayer.biomeName())
                .possibleValues((questPlayer, objects) ->
                        questPlayer == null ? List.of() : questPlayer.availableBiomeNames())
                .register();
    }
}
