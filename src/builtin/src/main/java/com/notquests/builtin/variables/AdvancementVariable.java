package com.notquests.builtin.variables;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Context;
import com.notquests.core.registry.NotQuestsRegistry.Variables;

import java.util.List;

public final class AdvancementVariable {
    private AdvancementVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .booleanVariable("Advancement")
                .displayName("Advancement")
                .description("Checks or changes whether the target player has completed a Minecraft advancement.")
                .singular("Advancement")
                .plural("Advancements")
                .field("Advancement", adapter.fields().text(adapter::advancementIds), "Minecraft advancement key to check, such as minecraft:story/mine_stone.")
                .get(context -> adapter.hasAdvancement(context.questPlayer(), advancement(context)))
                .set((newValue, context) -> adapter.setAdvancement(context.questPlayer(), advancement(context), newValue))
                .register();
    }

    private static String advancement(final Variables.Context context) {
        return context.text("Advancement");
    }
}
