package com.notquests.builtin.objectives;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.platform.NotQuestsAdapter;

public final class SlimefunResearchObjective {
    public static final String TYPE = "SlimefunResearch";

    private SlimefunResearchObjective() {}

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        if (!plugin.integrationEnabled("Slimefun")) {
            return;
        }
        adapter.objectives().objective(TYPE)
                .displayName("Slimefun Research")
                .description("Counts Slimefun research cost spent by the player.")
                .field(
                        "amount",
                        adapter.fields().numberExpression().progressNeeded(),
                        "Total Slimefun research cost the player must spend.")
                .taskDescription((objective, questPlayer, activeObjective) ->
                        plugin.slimefunResearchTaskDescription(questPlayer))
                .register();
    }
}
