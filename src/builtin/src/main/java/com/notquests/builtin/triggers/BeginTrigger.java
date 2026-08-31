package com.notquests.builtin.triggers;

import com.notquests.core.platform.NotQuestsAdapter;

public final class BeginTrigger {
    private BeginTrigger() {}

    public static void register(final NotQuestsAdapter platform) {
        platform.triggers().trigger("BEGIN")
                .displayName("Begin")
                .description("Runs the selected action when the quest starts or an objective unlocks.")
                .register();
    }
}
