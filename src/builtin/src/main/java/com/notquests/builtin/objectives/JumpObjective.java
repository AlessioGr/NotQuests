package com.notquests.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;

import java.util.Map;

public final class JumpObjective {
    private JumpObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.objectives().objective("Jump")
                .displayName("Jump")
                .description("Counts times the player jumps.")
                .field("amount", adapter.fields().numberExpression().progressNeeded(), "Number of jumps required.")
                .taskDescription((objective, questPlayer, activeObjective) -> adapter.objectiveTaskText(
                        "chat.objectives.taskDescription.jump.base",
                        questPlayer,
                        activeObjective,
                        Map.of(
                                "%AMOUNTOFJUMPS%",
                                ""
                                        + (activeObjective != null
                                                ? activeObjective.progressNeeded()
                                                : objective.text("amount")))))
                .onPlayerJump(objective -> objective.addProgress(1))
                .register();
    }
}
