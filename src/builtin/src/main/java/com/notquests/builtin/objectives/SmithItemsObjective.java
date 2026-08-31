package com.notquests.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;

import java.util.Map;

public final class SmithItemsObjective {
    private SmithItemsObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.objectives().objective("SmithItems")
                .displayName("Smith Items")
                .description("Counts matching result items taken from a smithing table.")
                .field(
                        "materials",
                        adapter.fields().itemSelection().config("specifics.itemStackSelection"),
                        "Smithing result item that should count. Use any if every smithing result should count.")
                .field(
                        "amount",
                        adapter.fields().numberExpression().progressNeeded(),
                        "Number of smithing result items the player must take. Supports math and NotQuests number variables.")
                .taskDescription((objective, questPlayer, activeObjective) -> adapter.objectiveTaskText(
                        "chat.objectives.taskDescription.smithItems.base",
                        questPlayer,
                        activeObjective,
                        Map.of(
                                "%ITEMTOSMITHTYPE%",
                                objective.itemSelection("materials").listedMaterials("main"),
                                "%ITEMTOSMITHNAME%",
                                "",
                                "%(%",
                                "",
                                "%)%",
                                "")))
                .onPlayerTakeSmithingResult((event, objective) -> {
                    if (event.matches(objective.itemSelection("materials"))) {
                        objective.addProgress(event.amount());
                    }
                })
                .register();
    }
}
