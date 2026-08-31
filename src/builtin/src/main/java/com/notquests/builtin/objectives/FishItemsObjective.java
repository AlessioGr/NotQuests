package com.notquests.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;

public final class FishItemsObjective {
    private FishItemsObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        ItemCountObjective.itemObjective(adapter, "FishItems", "Fish Items", "Counts matching items caught by fishing.", "chat.objectives.taskDescription.fishItems.base", "%ITEMTOFISHTYPE%")
                .onPlayerFishItem((event, objective) -> {
                    if (event.matches(objective.itemSelection("materials"))) {
                        objective.addProgress(event.amount());
                    }
                })
                .register();
    }
}
