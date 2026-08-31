package com.notquests.builtin.variables;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Context;
import com.notquests.core.registry.NotQuestsRegistry.Variables;

public final class ConditionVariable {
    private ConditionVariable() {}

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.variables()
                .booleanVariable("Condition")
                .displayName("Condition")
                .description("Evaluates a saved NotQuests condition expression for the target player.")
                .singular("Condition")
                .plural("Conditions")
                .field("Conditions", adapter.fields().text(plugin::savedConditionNames), "Saved condition expression to evaluate. Use saved condition names with & or | to combine them.")
                .get(context -> plugin.evaluateSavedConditionExpression(context.questPlayer(), expression(context)))
                .possibleValues(context -> plugin.savedConditionNames())
                .register();
    }

    private static String expression(final Variables.Context context) {
        return context.text("Conditions");
    }
}
