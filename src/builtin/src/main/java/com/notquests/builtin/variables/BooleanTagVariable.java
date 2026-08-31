package com.notquests.builtin.variables;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.managers.tags.TagType;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Context;
import com.notquests.core.registry.NotQuestsRegistry.Variables;

public final class BooleanTagVariable {
    private BooleanTagVariable() {}

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.variables()
                .booleanVariable("TagBoolean")
                .displayName("Boolean Tag")
                .description("Reads or changes a boolean NotQuests tag on the target player.")
                .singular("Tag")
                .plural("Tags")
                .field("TagName", adapter.fields().text(() -> plugin.tagNames(TagType.BOOLEAN)), "Boolean tag identifier.")
                .get(context -> {
                    final Object value = plugin.playerTagValue(context.questPlayer(), tagName(context), TagType.BOOLEAN);
                    return value instanceof Boolean booleanValue && booleanValue;
                })
                .set((newValue, context) -> plugin.setPlayerTagValue(
                        context.questPlayer(), tagName(context), TagType.BOOLEAN, Boolean.TRUE.equals(newValue)))
                .possibleValues(context -> plugin.tagNames(TagType.BOOLEAN))
                .register();
    }

    private static String tagName(final Variables.Context context) {
        return context.text("TagName");
    }
}
