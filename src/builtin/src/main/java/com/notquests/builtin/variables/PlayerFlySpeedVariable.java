package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

public final class PlayerFlySpeedVariable {
    private PlayerFlySpeedVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("FlySpeed")
                .displayName("Fly Speed")
                .description("Reads or changes the target player's flying speed.")
                .singular("Fly speed")
                .plural("Fly speed")
                .get((questPlayer, objects) -> questPlayer == null ? 0 : questPlayer.flySpeed())
                .set((newValue, questPlayer, objects) -> {
                    final double speed = newValue.doubleValue();
                    return questPlayer != null
                            && Double.isFinite(speed)
                            && questPlayer.setFlySpeed(Math.max(-1.0d, Math.min(1.0d, speed)));
                })
                .register();
    }
}
