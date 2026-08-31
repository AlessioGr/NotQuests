package com.notquests.builtin.variables;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.platform.NotQuestsAdapter;

public final class ActiveQuestsVariable {
    private ActiveQuestsVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        register(NotQuestsPlugin.create(), adapter);
    }

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.variables()
                .listVariable("ActiveQuests")
                .displayName("Active Quests")
                .description("Reads or changes the quests currently active for the target player.")
                .singular("Active Quest")
                .plural("Active Quests")
                .get((questPlayer, objects) -> plugin.activeQuestNames(questPlayer))
                .set((newValue, questPlayer, objects) -> plugin.setActiveQuestNames(questPlayer, newValue))
                .possibleValues((questPlayer, objects) -> plugin.questNames())
                .register();
    }
}
