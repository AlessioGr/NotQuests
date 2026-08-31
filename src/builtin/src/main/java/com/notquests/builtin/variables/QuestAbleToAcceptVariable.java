package com.notquests.builtin.variables;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Context;
import com.notquests.core.registry.NotQuestsRegistry.Variables;

public final class QuestAbleToAcceptVariable {
    private QuestAbleToAcceptVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        register(NotQuestsPlugin.create(), adapter);
    }

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.variables()
                .booleanVariable("QuestAbleToAccept")
                .displayName("Quest Able To Accept")
                .description("Checks whether the target player can currently accept the selected quest.")
                .singular("Able to accept Quest")
                .plural("Able to accept Quest")
                .field("Quest", adapter.fields().text(plugin::questNames), "Quest identifier to check.")
                .get(context -> plugin.canAcceptQuest(context.questPlayer(), questName(context)))
                .register();
    }

    private static String questName(final Variables.Context context) {
        return context.text("Quest");
    }
}
