package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;

public final class FalseVariable {
    private FalseVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables().booleanVariable("False")
                .displayName("False")
                .description("Always returns false.")
                .singular("False")
                .plural("False")
                .get((questPlayer, objects) -> false)
                .register();
    }
}
