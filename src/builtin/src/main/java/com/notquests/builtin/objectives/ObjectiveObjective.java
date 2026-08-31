package com.notquests.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Objectives.Kinds;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;

import java.util.Map;

public final class ObjectiveObjective {
    private ObjectiveObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.objectives().objective(Kinds.OBJECTIVE_GROUP)
                .displayName("Objective Group")
                .description("Creates a named parent objective that can contain sub-objectives.")
                .field(
                        Kinds.OBJECTIVE_GROUP_HOLDER_NAME,
                        adapter.fields().greedyText().config("specifics.objectiveHolderName"),
                        "Display name for this objective group.")
                .taskDescription((objective, questPlayer, activeObjective) -> adapter.objectiveTaskText(
                        "chat.objectives.taskDescription.objective.base",
                        questPlayer,
                        activeObjective,
                        Map.of(
                                "%OBJECTIVEHOLDERNAME%",
                                objective.text(Kinds.OBJECTIVE_GROUP_HOLDER_NAME))))
                .onUnlock((objective, questPlayer, loading) -> {
                    if (!loading
                            && objective.childObjectiveCount() == 0
                            && objective.currentProgress() < objective.progressNeeded()) {
                        objective.setProgress(objective.progressNeeded(), false);
                    }
                })
                .register();
    }
}
