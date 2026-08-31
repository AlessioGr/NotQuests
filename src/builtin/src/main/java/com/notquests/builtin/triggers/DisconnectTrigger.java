package com.notquests.builtin.triggers;

import com.notquests.core.platform.NotQuestsAdapter;

public final class DisconnectTrigger {
    private DisconnectTrigger() {}

    public static void register(final NotQuestsAdapter platform) {
        platform.triggers().trigger("DISCONNECT")
                .displayName("Disconnect")
                .description("Runs the selected action after the quest player disconnects the configured number of times.")
                .field(
                        "amount",
                        platform.fields().integer(1).config("amountNeeded"),
                        "Number of disconnects required before this trigger runs.")
                .register();
    }
}
