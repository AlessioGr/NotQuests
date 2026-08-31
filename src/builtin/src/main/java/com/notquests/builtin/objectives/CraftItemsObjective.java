package com.notquests.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;

public final class CraftItemsObjective {
    private CraftItemsObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        ItemCountObjective.itemObjective(adapter, "CraftItems", "Craft Items", "Counts matching items crafted by the player.", "chat.objectives.taskDescription.craftItems.base", "%ITEMTOCRAFTTYPE%")
                .onPlayerCraftItem((event, objective) -> {
                    if (event.matches(objective.itemSelection("materials"))) {
                        objective.addProgress(event.amount());
                    }
                })
                .register();
    }
}
