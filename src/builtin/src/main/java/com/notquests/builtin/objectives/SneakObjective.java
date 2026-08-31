package com.notquests.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;

import java.util.Map;

public final class SneakObjective {
    private SneakObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.objectives().objective("Sneak")
                .displayName("Sneak")
                .description("Counts times the player starts sneaking.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Number of sneaks required.")
                .taskDescription((objective, questPlayer, activeObjective) -> adapter.objectiveTaskText(
                        "chat.objectives.taskDescription.sneak.base",
                        questPlayer,
                        activeObjective,
                        Map.of(
                                "%AMOUNTOFSNEAKS%",
                                ""
                                        + (activeObjective != null
                                                ? activeObjective.progressNeeded()
                                                : objective.text("amount")))))
                .onPlayerStartSneak(objective -> objective.addProgress(1))
                .register();
    }
}
