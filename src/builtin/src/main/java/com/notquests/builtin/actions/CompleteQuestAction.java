package com.notquests.builtin.actions;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.platform.NotQuestsAdapter;

public final class CompleteQuestAction {
    private static final String QUEST = "quest";

    private CompleteQuestAction() {}

    public static void register(final NotQuestsAdapter adapter) {
        register(NotQuestsPlugin.create(), adapter);
    }

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.actions()
                .action("CompleteQuest")
                .displayName("Complete Quest")
                .description("Completes another active quest for the target player.")
                .field(
                        QUEST,
                        adapter.fields().text(plugin::questNames).config("specifics.quest"),
                        "Active quest that should be completed for the target player.")
                .singleLine((action, arguments) -> action.setValue(QUEST, arguments.get(0)))
                .execute((action, questPlayer, objects) ->
                        plugin.forceCompleteQuest(questPlayer, action.text(QUEST), adapter::warn))
                .actionDescription((action, questPlayer, objects) -> "Completes quest: " + action.text(QUEST))
                .register();
    }
}
