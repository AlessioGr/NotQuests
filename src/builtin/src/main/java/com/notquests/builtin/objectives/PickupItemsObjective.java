package com.notquests.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;

import java.util.Map;

public final class PickupItemsObjective {
    private PickupItemsObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.objectives().objective("PickupItems")
                .displayName("Pick Up Items")
                .description("Counts matching items picked up by the player.")
                .field(
                        "materials",
                        adapter.fields().itemSelection().config("specifics.itemStackSelection"),
                        "Items or NotQuests custom items that count when picked up. Supports one value or a comma-separated list.")
                .field(
                        "amount",
                        adapter.fields().numberExpression().progressNeeded(),
                        "Number of matching items the player must pick up. Supports math and NotQuests number variables.")
                .flag(
                        "doNotDeductIfItemIsDropped",
                        adapter.fields().presenceFlag().invertedBooleanConfig("specifics.deductIfItemDropped"),
                        "Stops NotQuests from removing progress when the player drops a matching item.")
                .flag(
                        "doNotDeductIfItemIsPlaced",
                        adapter.fields().presenceFlag().invertedBooleanConfig("specifics.deductIfItemPlaced"),
                        "Stops NotQuests from removing progress when the player places a matching block item.")
                .taskDescription((objective, questPlayer, activeObjective) -> adapter.objectiveTaskText(
                        "chat.objectives.taskDescription.pickupItems.base",
                        questPlayer,
                        activeObjective,
                        Map.of(
                                "%ITEMTOPICKUPTYPE%",
                                objective.itemSelection("materials").listedMaterials("main"),
                                "%ITEMTOPICKUPNAME%",
                                "",
                                "%(%",
                                "",
                                "%)%",
                                "")))
                .onPlayerPickupItem((event, objective) -> {
                    if (event.matches(objective.itemSelection("materials"))) {
                        objective.addProgress(event.amount());
                    }
                })
                .onPlayerDropItem((event, objective) -> {
                    if (event.matches(objective.itemSelection("materials"))
                            && !objective.flag("doNotDeductIfItemIsDropped")) {
                        objective.removeProgress(event.amount(), false);
                    }
                })
                .onPlayerPlaceBlock((event, objective) -> {
                    if (event.matches(objective.itemSelection("materials"))
                            && !objective.flag("doNotDeductIfItemIsPlaced")) {
                        objective.removeProgress(1, false);
                    }
                })
                .register();
    }
}
