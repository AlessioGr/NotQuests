package com.notquests.builtin.actions;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.platform.NotQuestsAdapter;

public final class FailQuestAction {
    private static final String QUEST = "quest";

    private FailQuestAction() {}

    public static void register(final NotQuestsAdapter adapter) {
        register(NotQuestsPlugin.create(), adapter);
    }

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.actions()
                .action("FailQuest")
                .displayName("Fail Quest")
                .description("Fails another active quest for the target player.")
                .field(
                        QUEST,
                        adapter.fields().text(plugin::questNames).config("specifics.quest"),
                        "Active quest that should be failed for the target player.")
                .singleLine((action, arguments) -> action.setValue(QUEST, arguments.get(0)))
                .execute((action, questPlayer, objects) -> plugin.failQuest(questPlayer, action.text(QUEST), adapter::warn))
                .actionDescription((action, questPlayer, objects) -> "Fails quest: " + action.text(QUEST))
                .register();
    }
}
