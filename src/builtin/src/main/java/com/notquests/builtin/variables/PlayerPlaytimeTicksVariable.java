package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

public final class PlayerPlaytimeTicksVariable {
    private PlayerPlaytimeTicksVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("PlaytimeTicks")
                .displayName("Playtime Ticks")
                .description("Reads or changes the target player's playtime in server ticks.")
                .singular("Playtime in tick")
                .plural("Playtime in ticks")
                .get((questPlayer, objects) -> questPlayer == null ? 0 : questPlayer.playtimeTicks())
                .set((newValue, questPlayer, objects) -> {
                    final double ticks = newValue.doubleValue();
                    return questPlayer != null
                            && Double.isFinite(ticks)
                            && questPlayer.setPlaytimeTicks(ticks(ticks, 1.0d));
                })
                .register();
    }

    static int ticks(final double value, final double multiplier) {
        return (int) Math.max(0.0d, Math.min(Integer.MAX_VALUE, value * multiplier));
    }
}
