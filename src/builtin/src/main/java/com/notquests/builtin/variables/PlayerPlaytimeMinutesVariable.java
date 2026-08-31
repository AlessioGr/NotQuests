package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

public final class PlayerPlaytimeMinutesVariable {
    private PlayerPlaytimeMinutesVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("PlaytimeMinutes")
                .displayName("Playtime Minutes")
                .description("Reads or changes the target player's playtime in minutes.")
                .singular("Playtime in minute")
                .plural("Playtime in minutes")
                .get((questPlayer, objects) -> questPlayer == null ? 0 : questPlayer.playtimeTicks() / 1200d)
                .set((newValue, questPlayer, objects) -> {
                    final double minutes = newValue.doubleValue();
                    return questPlayer != null
                            && Double.isFinite(minutes)
                            && questPlayer.setPlaytimeTicks(
                                    PlayerPlaytimeTicksVariable.ticks((long) minutes, 1_200.0d));
                })
                .register();
    }
}
