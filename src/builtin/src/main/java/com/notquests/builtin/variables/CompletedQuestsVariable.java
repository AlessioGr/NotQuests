package com.notquests.builtin.variables;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.platform.NotQuestsAdapter;

public final class CompletedQuestsVariable {
    private CompletedQuestsVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        register(NotQuestsPlugin.create(), adapter);
    }

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.variables()
                .listVariable("CompletedQuests")
                .displayName("Completed Quests")
                .description("Reads or changes the quests completed by the target player.")
                .singular("Completed Quest")
                .plural("Completed Quests")
                .get((questPlayer, objects) -> plugin.completedQuestNames(questPlayer))
                .set((newValue, questPlayer, objects) -> plugin.setCompletedQuestNames(questPlayer, newValue))
                .possibleValues((questPlayer, objects) -> plugin.questNames())
                .register();
    }
}
