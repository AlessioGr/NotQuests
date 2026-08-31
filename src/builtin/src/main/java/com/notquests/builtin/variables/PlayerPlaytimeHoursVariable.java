package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

public final class PlayerPlaytimeHoursVariable {
    private PlayerPlaytimeHoursVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("PlaytimeHours")
                .displayName("Playtime Hours")
                .description("Reads or changes the target player's playtime in hours.")
                .singular("Playtime in hour")
                .plural("Playtime in hours")
                .get((questPlayer, objects) -> questPlayer == null ? 0 : questPlayer.playtimeTicks() / 72000d)
                .set((newValue, questPlayer, objects) -> {
                    final double hours = newValue.doubleValue();
                    return questPlayer != null
                            && Double.isFinite(hours)
                            && questPlayer.setPlaytimeTicks(
                                    PlayerPlaytimeTicksVariable.ticks((long) hours, 72_000.0d));
                })
                .register();
    }
}
