package com.notquests.builtin.variables;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.managers.tags.TagType;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Context;
import com.notquests.core.registry.NotQuestsRegistry.Variables;

public final class DoubleTagVariable {
    private DoubleTagVariable() {}

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("TagDouble")
                .displayName("Double Tag")
                .description("Reads or changes a double NotQuests tag on the target player.")
                .singular("Tag")
                .plural("Tags")
                .field("TagName", adapter.fields().text(() -> plugin.tagNames(TagType.DOUBLE)), "Double tag identifier.")
                .get(context -> number(plugin.playerTagValue(context.questPlayer(), tagName(context), TagType.DOUBLE)))
                .set((newValue, context) -> plugin.setPlayerTagValue(
                        context.questPlayer(), tagName(context), TagType.DOUBLE, newValue.doubleValue()))
                .possibleValues(context -> plugin.tagNames(TagType.DOUBLE))
                .register();
    }

    private static Number number(final Object value) {
        return value instanceof Number number ? number : 0;
    }

    private static String tagName(final Variables.Context context) {
        return context.text("TagName");
    }
}
