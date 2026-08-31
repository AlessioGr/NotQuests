package com.notquests.builtin.variables;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Context;
import com.notquests.core.registry.NotQuestsRegistry.Variables;

public final class QuestReachedMaxCompletionsVariable {
    private QuestReachedMaxCompletionsVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        register(NotQuestsPlugin.create(), adapter);
    }

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.variables()
                .booleanVariable("QuestReachedMaxCompletions")
                .displayName("Quest Reached Max Completions")
                .description("Checks whether the target player has reached the selected quest's maximum completion limit.")
                .singular("Quest reached max completions")
                .plural("Quest reached max completions")
                .field("Quest", adapter.fields().text(plugin::questNames), "Quest identifier to check.")
                .get(context -> plugin.questReachedMaxCompletions(context.questPlayer(), questName(context)))
                .register();
    }

    private static String questName(final Variables.Context context) {
        return context.text("Quest");
    }
}
