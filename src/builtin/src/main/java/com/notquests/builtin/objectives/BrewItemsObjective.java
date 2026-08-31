package com.notquests.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;

import java.util.Map;

public final class BrewItemsObjective {
    private BrewItemsObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.objectives().objective("BrewItems")
                .displayName("Brew Items")
                .description("Counts matching freshly brewed items taken from a brewing stand.")
                .field(
                        "materials",
                        adapter.fields().itemSelection().config("specifics.itemStackSelection"),
                        "Potion, bottle, or custom brewed item the player must take after brewing completes.")
                .field(
                        "amount",
                        adapter.fields().numberExpression().progressNeeded(),
                        "Number of freshly brewed items the player must collect. Supports math and NotQuests number variables.")
                .taskDescription((objective, questPlayer, activeObjective) -> adapter.objectiveTaskText(
                        "chat.objectives.taskDescription.brewItems.base",
                        questPlayer,
                        activeObjective,
                        Map.of(
                                "%ITEMTOBREWTYPE%",
                                objective.itemSelection("materials").listedMaterials("main"),
                                "%ITEMTOBREWNAME%",
                                "",
                                "%(%",
                                "",
                                "%)%",
                                "")))
                .onPlayerTakeBrewedItem((event, objective) -> {
                    if (event.matches(objective.itemSelection("materials"))) {
                        objective.addProgress(event.amount());
                    }
                })
                .register();
    }
}
