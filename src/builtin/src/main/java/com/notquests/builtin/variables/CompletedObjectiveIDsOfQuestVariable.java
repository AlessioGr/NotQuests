package com.notquests.builtin.variables;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Context;
import com.notquests.core.registry.NotQuestsRegistry.Variables;

public final class CompletedObjectiveIDsOfQuestVariable {
    private CompletedObjectiveIDsOfQuestVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        register(NotQuestsPlugin.create(), adapter);
    }

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.variables()
                .listVariable("CompletedObjectiveIDsOfQuest")
                .displayName("Completed Objective IDs Of Quest")
                .description("Reads or changes the objective IDs completed inside one active quest for the target player.")
                .singular("Completed Objective ID Of Quest")
                .plural("Completed Objective IDs Of Quest")
                .field("QuestName", adapter.fields().text(plugin::questNames), "Quest whose completed objective IDs should be read or changed.")
                .get(context -> plugin.completedObjectiveIds(context.questPlayer(), questName(context)))
                .set((newValue, context) -> plugin.setCompletedObjectiveIds(
                        context.questPlayer(),
                        questName(context),
                        newValue))
                .possibleValues(context -> plugin.completedObjectiveIds(context.questPlayer(), questName(context)))
                .register();
    }

    private static String questName(final Variables.Context context) {
        return context.text("QuestName");
    }
}
