package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Context;
import com.notquests.core.registry.NotQuestsRegistry.Variables;

import java.util.List;

public final class PermissionVariable {
    private PermissionVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        if (!adapter.supportsArbitraryPermissionChecks()) {
            return;
        }
        final var permission = adapter.variables()
                .booleanVariable("Permission")
                .displayName("Permission")
                .description("Checks whether the target player has a permission node.")
                .singular("Permission")
                .plural("Permissions")
                .field("Permission", adapter.fields().text(() -> List.of("<Enter permission node>")), "Permission node to check.")
                .get(context -> adapter.hasPermission(context.questPlayer(), permission(context)));
        if (adapter.supportsPermissionMutation()) {
            permission.set((newValue, context) -> adapter.setPermission(context.questPlayer(), permission(context), newValue));
        }
        permission.register();
    }

    private static String permission(final Variables.Context context) {
        return context.text("Permission");
    }
}
