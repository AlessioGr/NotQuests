package com.notquests.builtin.conditions;

import com.notquests.core.NotQuestsPlugin.CompletedObjectiveCheck;
import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;

public final class CompletedObjectiveCondition {
    private static final String OBJECTIVE_ID = "dependingObjectiveId";

    private CompletedObjectiveCondition() {}

    public static void register(final NotQuestsAdapter adapter) {
        register(NotQuestsPlugin.create(), adapter);
    }

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.conditions().condition("CompletedObjective")
                .displayName("Completed Objective")
                .description("Checks whether another objective in the same quest has already been completed.")
                .field(
                        OBJECTIVE_ID,
                        adapter.fields().integer(-1).config("specifics.objectiveID"),
                        "Objective ID that must already be completed before this condition is fulfilled.")
                .validFor(
                        Conditions.Target.OBJECTIVE_UNLOCK,
                        Conditions.Target.OBJECTIVE_PROGRESS,
                        Conditions.Target.OBJECTIVE_COMPLETE)
                .singleLine((condition, arguments) ->
                        condition.setValue(OBJECTIVE_ID, Integer.parseInt(arguments.get(0))))
                .check((condition, questPlayer) -> check(plugin, condition, questPlayer))
                .conditionDescription((condition, questPlayer, objects) -> description(plugin, condition, questPlayer))
                .register();
    }

    private static String check(
            final NotQuestsPlugin plugin,
            final Conditions.Data condition,
            final PlatformPlayer questPlayer) {
        final int objectiveId = condition.integer(OBJECTIVE_ID, -1);
        final CompletedObjectiveCheck check =
                plugin.completedObjectiveRequirement(condition, questPlayer, objectiveId);
        if (!check.available()) {
            return "<red>Error: " + check.errorMessage();
        }
        if (!check.completed()) {
            return "<yellow>Finish the following objective first: <highlight>" + check.objectiveName();
        }
        return "";
    }

    private static String description(
            final NotQuestsPlugin plugin,
            final Conditions.Data condition,
            final PlatformPlayer questPlayer) {
        final int objectiveId = condition.integer(OBJECTIVE_ID, -1);
        final CompletedObjectiveCheck check =
                plugin.completedObjectiveRequirement(condition, questPlayer, objectiveId);
        final String objectiveName = check.objectiveName() == null || check.objectiveName().isBlank()
                ? String.valueOf(objectiveId)
                : check.objectiveName();
        return "<gray>-- Finish Objective first: " + objectiveName;
    }
}
