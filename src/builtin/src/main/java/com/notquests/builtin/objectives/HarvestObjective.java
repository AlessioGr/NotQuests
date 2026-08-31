package com.notquests.builtin.objectives;

import com.notquests.core.items.ItemSelection;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.structs.ActiveObjectives;

import java.util.Map;

public final class HarvestObjective {
    private HarvestObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.objectives().objective("Harvest")
                .displayName("Harvest")
                .description("Counts fully grown crops, plants, or fruit blocks harvested by the player.")
                .field(
                        "crops",
                        adapter.fields().itemSelection().config("specifics.itemStackSelection"),
                        "Crop, plant, or fruit block that must be harvested after it is fully grown. Crop item names like carrot or potato are accepted as aliases.")
                .field(
                        "amount",
                        adapter.fields().numberExpression().progressNeeded(),
                        "Number of fully grown crops, plants, or fruit blocks the player must harvest.")
                .taskDescription((objective, questPlayer, activeObjective) -> adapter.objectiveTaskText(
                        "chat.objectives.taskDescription.harvest.base",
                        questPlayer,
                        activeObjective,
                        Map.of("%CROPTOHARVEST%", objective.itemSelection("crops").listedMaterials("main"))))
                .onPlayerHarvestBlock((event, objective) -> {
                    if (!event.playerPlaced()
                            && event.fullyGrownHarvestable()
                            && matchesHarvestMaterial(event, objective.itemSelection("crops"))) {
                        objective.addProgress(1);
                    }
                })
                .register();
    }

    private static boolean matchesHarvestMaterial(
            final Objectives.HarvestBlockEvent event,
            final ItemSelection selection) {
        return selection != null
                && (event.matches(selection)
                || ActiveObjectives.harvestedMaterialAliases(event.materialId()).stream()
                        .anyMatch(selection::includesMaterial));
    }
}
