package com.notquests.builtin.objectives;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.platform.NotQuestsAdapter;

public final class ConditionObjective {
    private static final String CONDITION = "condition";
    private static final String CHECK_ONLY_WHEN_VARIABLE_CHANGED = "checkOnlyWhenCorrespondingVariableValueChanged";

    private ConditionObjective() {}

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.objectives().objective("Condition")
                .displayName("Condition")
                .description("Completes when a saved NotQuests condition is fulfilled.")
                .field(
                        CONDITION,
                        adapter.fields().conditionName().config("specifics.condition"),
                        "Saved condition that must become fulfilled.")
                .flag(
                        CHECK_ONLY_WHEN_VARIABLE_CHANGED,
                        adapter.fields()
                                .presenceFlag()
                                .config("specifics.checkOnlyWhenCorrespondingVariableValueChanged"),
                        "Only re-check this objective when the corresponding variable value changes.")
                .taskDescription((objective, questPlayer, activeObjective) ->
                        plugin.savedConditionDescription(questPlayer, objective.text(CONDITION), activeObjective))
                .onRefresh((refresh, objective) -> {
                    if (objective.flag(CHECK_ONLY_WHEN_VARIABLE_CHANGED) && !refresh.variableValueChanged()) {
                        return;
                    }
                    if (plugin.savedConditionFulfilled(objective.questPlayer(), objective.text(CONDITION))) {
                        objective.addProgress(1);
                    }
                })
                .register();
    }
}
