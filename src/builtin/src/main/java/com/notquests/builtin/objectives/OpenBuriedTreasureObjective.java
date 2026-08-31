package com.notquests.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;

import java.util.Map;

public final class OpenBuriedTreasureObjective {
    private OpenBuriedTreasureObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.objectives().objective("OpenBuriedTreasure")
                .displayName("Open Buried Treasure")
                .description("Counts unopened buried-treasure chests opened by the player.")
                .field(
                        "amount",
                        adapter.fields().numberExpression().progressNeeded(),
                        "Number of buried treasure chests the player must open.")
                .taskDescription((objective, questPlayer, activeObjective) -> adapter.objectiveTaskText(
                        "chat.objectives.taskDescription.openBuriedTreasure.base",
                        questPlayer,
                        activeObjective,
                        Map.of()))
                .onPlayerOpenBuriedTreasure(objective -> objective.addProgress(1))
                .register();
    }
}
