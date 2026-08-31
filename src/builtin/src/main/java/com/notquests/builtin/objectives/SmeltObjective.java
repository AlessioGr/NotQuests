package com.notquests.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;

public final class SmeltObjective {
    private SmeltObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        ItemCountObjective.itemObjective(adapter, "SmeltItems", "Smelt Items", "Counts matching items taken from a furnace, blast furnace, or smoker output slot.", "chat.objectives.taskDescription.smeltItems.base", "%ITEMTOSMELTTYPE%")
                .onPlayerTakeSmeltedItem((event, objective) -> {
                    if (event.matches(objective.itemSelection("materials"))) {
                        objective.addProgress(event.amount());
                    }
                })
                .register();
    }
}
