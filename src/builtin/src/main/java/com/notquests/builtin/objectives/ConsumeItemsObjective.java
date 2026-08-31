package com.notquests.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;

public final class ConsumeItemsObjective {
    private ConsumeItemsObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        ItemCountObjective.itemObjective(adapter, "ConsumeItems", "Consume Items", "Counts matching food, potions, or other consumable items used by the player.", "chat.objectives.taskDescription.consumeItems.base", "%ITEMTOCONSUMETYPE%")
                .onPlayerConsumeItem((event, objective) -> {
                    if (event.matches(objective.itemSelection("materials"))) {
                        objective.addProgress(event.amount());
                    }
                })
                .register();
    }
}
