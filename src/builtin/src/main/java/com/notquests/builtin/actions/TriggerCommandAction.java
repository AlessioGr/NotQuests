package com.notquests.builtin.actions;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.platform.NotQuestsAdapter;

public final class TriggerCommandAction {
    private static final String TRIGGER_NAME = "triggerName";

    private TriggerCommandAction() {}

    public static void register(final NotQuestsAdapter adapter) {
        register(NotQuestsPlugin.create(), adapter);
    }

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.actions()
                .action("TriggerCommand")
                .displayName("Trigger Command")
                .description("Adds progress to active TriggerCommand objectives with the matching trigger name.")
                .field(
                        TRIGGER_NAME,
                        adapter.fields().text(plugin::triggerCommandNames).config("specifics.triggerName"),
                        "Trigger name configured on one or more TriggerCommand objectives.")
                .singleLine((action, arguments) -> action.setValue(TRIGGER_NAME, arguments.get(0)))
                .execute((action, questPlayer, objects) ->
                        plugin.triggerCommandObjectiveProgress(questPlayer, action.text(TRIGGER_NAME)))
                .actionDescription((action, questPlayer, objects) ->
                        "Triggers command objective: " + action.text(TRIGGER_NAME))
                .register();
    }
}
