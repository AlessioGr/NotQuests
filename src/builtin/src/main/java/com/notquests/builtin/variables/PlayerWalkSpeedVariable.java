package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

public final class PlayerWalkSpeedVariable {
    private PlayerWalkSpeedVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("WalkSpeed")
                .displayName("Walk Speed")
                .description("Reads or changes the target player's walk speed.")
                .singular("Walk speed")
                .plural("Walk speed")
                .get((questPlayer, objects) -> questPlayer == null ? 0 : questPlayer.walkSpeed())
                .set((newValue, questPlayer, objects) -> {
                    final double speed = newValue.doubleValue();
                    return questPlayer != null
                            && Double.isFinite(speed)
                            && questPlayer.setWalkSpeed(Math.max(-1.0d, Math.min(1.0d, speed)));
                })
                .register();
    }
}
