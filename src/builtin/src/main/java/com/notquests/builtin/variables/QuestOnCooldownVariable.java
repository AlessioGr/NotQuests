package com.notquests.builtin.variables;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Context;
import com.notquests.core.registry.NotQuestsRegistry.Variables;

public final class QuestOnCooldownVariable {
    private QuestOnCooldownVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        register(NotQuestsPlugin.create(), adapter);
    }

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.variables()
                .booleanVariable("QuestOnCooldown")
                .displayName("Quest On Cooldown")
                .description("Checks whether the selected quest is currently on cooldown for the target player.")
                .singular("Quest on cooldown")
                .plural("Quest on cooldown")
                .field("Quest", adapter.fields().text(plugin::questNames), "Quest identifier to check.")
                .get(context -> plugin.questOnCooldown(context.questPlayer(), questName(context)))
                .register();
    }

    private static String questName(final Variables.Context context) {
        return context.text("Quest");
    }
}
