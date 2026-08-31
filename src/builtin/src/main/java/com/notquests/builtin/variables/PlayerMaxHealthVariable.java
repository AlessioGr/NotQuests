package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

public final class PlayerMaxHealthVariable {
    private PlayerMaxHealthVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("MaxHealth")
                .displayName("Maximum Health")
                .description("Reads or changes the target player's maximum health.")
                .singular("Maximum Health")
                .plural("Maximum Health")
                .get((questPlayer, objects) -> questPlayer == null ? 20 : questPlayer.maxHealth())
                .set((newValue, questPlayer, objects) -> {
                    final double maxHealth = newValue.doubleValue();
                    return questPlayer != null
                            && Double.isFinite(maxHealth)
                            && questPlayer.setMaxHealth(Math.max(1.0d, maxHealth));
                })
                .register();
    }
}
