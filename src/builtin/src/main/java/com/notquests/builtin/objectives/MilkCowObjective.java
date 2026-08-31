package com.notquests.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;

import java.util.Map;

public final class MilkCowObjective {
    private MilkCowObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.objectives().objective("MilkCow")
                .displayName("Milk Cow")
                .description("Counts cows milked by the player.")
                .field(
                        "amount",
                        adapter.fields().numberExpression().progressNeeded(),
                        "Number of cows the player must milk.")
                .flag(
                        "cancelMilking",
                        adapter.fields().presenceFlag().config("specifics.cancelMilking"),
                        "Cancels the vanilla milking interaction while this objective is active.")
                .taskDescription((objective, questPlayer, activeObjective) -> adapter.objectiveTaskText(
                        "chat.objectives.taskDescription.milkCow.base",
                        questPlayer,
                        activeObjective,
                        Map.of(
                                "%AMOUNTOFCOWS%",
                                ""
                                        + (activeObjective != null
                                                ? activeObjective.progressNeeded()
                                                : objective.text("amount")))))
                .onPlayerMilkCow((event, objective) -> {
                    objective.addProgress(1);
                    if (objective.flag("cancelMilking")) {
                        event.cancel();
                    }
                })
                .register();
    }
}
