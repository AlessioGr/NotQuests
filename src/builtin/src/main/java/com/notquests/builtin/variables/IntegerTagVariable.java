package com.notquests.builtin.variables;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.managers.tags.TagType;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Context;
import com.notquests.core.registry.NotQuestsRegistry.Variables;

public final class IntegerTagVariable {
    private IntegerTagVariable() {}

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.variables()
                .numberVariable("TagInteger")
                .displayName("Integer Tag")
                .description("Reads or changes an integer NotQuests tag on the target player.")
                .singular("Tag")
                .plural("Tags")
                .field("TagName", adapter.fields().text(() -> plugin.tagNames(TagType.INTEGER)), "Integer tag identifier.")
                .get(context -> number(plugin.playerTagValue(context.questPlayer(), tagName(context), TagType.INTEGER)))
                .set((newValue, context) -> plugin.setPlayerTagValue(
                        context.questPlayer(), tagName(context), TagType.INTEGER, newValue.intValue()))
                .possibleValues(context -> plugin.tagNames(TagType.INTEGER))
                .register();
    }

    private static Number number(final Object value) {
        return value instanceof Number number ? number : 0;
    }

    private static String tagName(final Variables.Context context) {
        return context.text("TagName");
    }
}
