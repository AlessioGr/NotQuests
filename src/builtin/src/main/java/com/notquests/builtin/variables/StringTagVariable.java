package com.notquests.builtin.variables;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.managers.tags.TagType;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Variables.Context;
import com.notquests.core.registry.NotQuestsRegistry.Variables;

public final class StringTagVariable {
    private StringTagVariable() {}

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.variables()
                .stringVariable("TagString")
                .displayName("String Tag")
                .description("Reads or changes a text NotQuests tag on the target player.")
                .singular("Tag")
                .plural("Tags")
                .field("TagName", adapter.fields().text(() -> plugin.tagNames(TagType.STRING)), "String tag identifier.")
                .get(context -> {
                    final Object value = plugin.playerTagValue(context.questPlayer(), tagName(context), TagType.STRING);
                    return value instanceof String stringValue ? stringValue : "";
                })
                .set((newValue, context) ->
                        plugin.setPlayerTagValue(context.questPlayer(), tagName(context), TagType.STRING, newValue))
                .possibleValues(context -> plugin.tagNames(TagType.STRING))
                .register();
    }

    private static String tagName(final Variables.Context context) {
        return context.text("TagName");
    }
}
