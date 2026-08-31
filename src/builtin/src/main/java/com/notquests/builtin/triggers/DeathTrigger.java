package com.notquests.builtin.triggers;

import com.notquests.core.platform.NotQuestsAdapter;

public final class DeathTrigger {
    private DeathTrigger() {}

    public static void register(final NotQuestsAdapter platform) {
        platform.triggers().trigger("DEATH")
                .displayName("Death")
                .description("Runs the selected action after the quest player dies the configured number of times.")
                .field(
                        "amount",
                        platform.fields().integer(1).config("amountNeeded"),
                        "Number of player deaths required before this trigger runs.")
                .register();
    }
}
