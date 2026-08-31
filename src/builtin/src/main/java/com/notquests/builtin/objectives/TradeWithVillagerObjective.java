package com.notquests.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;

public final class TradeWithVillagerObjective {
    private TradeWithVillagerObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        ItemCountObjective.itemObjective(adapter, "TradeWithVillager", "Trade With Villager", "Counts matching result items taken from villager trade windows.", "chat.objectives.taskDescription.tradeWithVillager.base", "%ITEMTOTRADETYPE%")
                .onPlayerTradeItem((event, objective) -> {
                    if (event.matches(objective.itemSelection("materials"))) {
                        objective.addProgress(event.amount());
                    }
                })
                .register();
    }
}
