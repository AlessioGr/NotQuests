package com.notquests.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;

import java.util.LinkedHashMap;
import java.util.Map;

final class ItemCountObjective {
    private ItemCountObjective() {}

    static Objectives.Builder itemObjective(
            final NotQuestsAdapter adapter,
            final String id,
            final String displayName,
            final String description,
            final String taskKey,
            final String itemPlaceholder) {
        return adapter.objectives().objective(id)
                .displayName(displayName)
                .description(description)
                .field(
                        "materials",
                        adapter.fields().itemSelection().config("specifics.itemStackSelection"),
                        "Items or NotQuests custom items that count. Supports one value or a comma-separated list.")
                .field(
                        "amount",
                        adapter.fields().numberExpression().progressNeeded(),
                        "Number of matching items required. Supports math and NotQuests number variables.")
                .taskDescription((objective, questPlayer, activeObjective) -> {
                    final Map<String, String> replacements = new LinkedHashMap<>();
                    replacements.put(itemPlaceholder, objective.itemSelection("materials").listedMaterials("main"));
                    replacements.put(itemNamePlaceholder(itemPlaceholder), "");
                    replacements.put("%(%", "");
                    replacements.put("%)%", "");
                    return adapter.objectiveTaskText(taskKey, questPlayer, activeObjective, replacements);
                });
    }

    private static String itemNamePlaceholder(final String typePlaceholder) {
        return typePlaceholder.replace("TYPE%", "NAME%");
    }
}
