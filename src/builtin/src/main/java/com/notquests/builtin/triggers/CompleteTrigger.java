package com.notquests.builtin.triggers;

import com.notquests.core.platform.NotQuestsAdapter;

public final class CompleteTrigger {
    private CompleteTrigger() {}

    public static void register(final NotQuestsAdapter platform) {
        platform.triggers().trigger("COMPLETE")
                .displayName("Complete")
                .description("Runs the selected action when the quest or selected objective is completed.")
                .register();
    }
}
