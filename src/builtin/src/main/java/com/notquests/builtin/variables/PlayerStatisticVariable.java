package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Context;
import com.notquests.core.registry.NotQuestsRegistry.Variables;

import java.util.List;

public final class PlayerStatisticVariable {
    private PlayerStatisticVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("Statistic")
                .displayName("Statistic")
                .description("Reads or changes one untyped Minecraft statistic for the target player.")
                .singular("Statistic")
                .plural("Statistics")
                .field("Statistic", adapter.fields().text(() -> suggestions(adapter)), "Untyped Minecraft statistic to read or change, such as MOB_KILLS or JUMP.")
                .get(context -> adapter.playerStatistic(context.questPlayer(), statistic(context)))
                .set((newValue, context) -> {
                    final double value = newValue.doubleValue();
                    return Double.isFinite(value)
                            && adapter.setPlayerStatistic(
                                    context.questPlayer(),
                                    statistic(context),
                                    (int) Math.max(0.0d, Math.min(Integer.MAX_VALUE, value)));
                })
                .register();
    }

    private static List<String> suggestions(final NotQuestsAdapter adapter) {
        final List<String> statisticIds = adapter.statisticIds();
        return statisticIds.isEmpty() ? List.of("<Enter Statistic name>") : statisticIds;
    }

    private static String statistic(final Variables.Context context) {
        return context.text("Statistic");
    }
}
