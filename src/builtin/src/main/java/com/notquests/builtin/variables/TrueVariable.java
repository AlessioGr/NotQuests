package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

public final class TrueVariable {
    private TrueVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables().booleanVariable("True")
                .displayName("True")
                .description("Always returns true.")
                .singular("True")
                .plural("True")
                .get((questPlayer, objects) -> true)
                .register();
    }
}
