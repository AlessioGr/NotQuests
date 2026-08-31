package com.notquests.builtin.variables;

import com.notquests.core.platform.NQLocation;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Context;
import com.notquests.core.registry.NotQuestsRegistry.Variables;

public final class BlockVariable {
    private BlockVariable() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.variables()
                .stringVariable("Block")
                .displayName("Block")
                .description("Reads or changes the block material at a configured world location.")
                .singular("Block")
                .plural("Blocks")
                .field("world", adapter.fields().text(adapter::worldNames), "World containing the target block.")
                .field("x", adapter.fields().doubleNumber(0), "Target block X coordinate.")
                .field("y", adapter.fields().doubleNumber(0), "Target block Y coordinate.")
                .field("z", adapter.fields().doubleNumber(0), "Target block Z coordinate.")
                .get(context -> adapter.blockMaterial(location(adapter, context)))
                .set((newValue, context) -> adapter.setBlockMaterial(
                        context.questPlayer(),
                        location(adapter, context),
                        newValue))
                .possibleValues(context -> adapter.blockMaterialOptions())
                .register();
    }

    private static NQLocation location(final NotQuestsAdapter adapter, final Variables.Context context) {
        return adapter.location(
                context.text("world"),
                context.number("x", 0),
                context.number("y", 0),
                context.number("z", 0));
    }
}
