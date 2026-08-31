package com.notquests.builtin.variables;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.platform.NotQuestsAdapter;

public final class QuestPointsVariable {
    private QuestPointsVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        register(NotQuestsPlugin.create(), adapter);
    }

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("QuestPoints")
                .displayName("Quest Points")
                .description("Reads or changes the target player's quest points.")
                .singular("Quest Point")
                .plural("Quest Points")
                .field(
                        "notifyPlayer",
                        adapter.fields().presenceFlag(),
                        "Notifies the player when their quest points are changed or set.")
                .get(context -> plugin.questPoints(context.questPlayer()))
                .set((newValue, context) -> plugin.setQuestPoints(
                        context.questPlayer(),
                        newValue.longValue(),
                        context.bool("notifyPlayer", false)))
                .register();
    }
}
