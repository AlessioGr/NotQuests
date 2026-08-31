package com.notquests.builtin.triggers;

import com.notquests.core.platform.NotQuestsAdapter;

public final class FailTrigger {
    private FailTrigger() {}

    public static void register(final NotQuestsAdapter platform) {
        platform.triggers().trigger("FAIL")
                .displayName("Fail")
                .description("Runs the selected action when the quest fails.")
                .register();
    }
}
