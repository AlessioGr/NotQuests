package com.notquests.builtin.actions;

import com.notquests.core.platform.NotQuestsAdapter;

public final class BroadcastMessageAction {
    private static final String MESSAGE = "message";

    private BroadcastMessageAction() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.actions().action("BroadcastMessage")
                .displayName("Broadcast Message")
                .description("Broadcasts a chat message to every online player.")
                .field(
                        MESSAGE,
                        adapter.fields().greedyText().config("specifics.message"),
                        "Message broadcast to the whole server. Supports MiniMessage formatting and NotQuests placeholders.")
                .singleLine((action, arguments) -> action.setValue(MESSAGE, String.join(" ", arguments)))
                .execute((action, questPlayer, objects) -> {
                    final String message = action.text(MESSAGE);
                    if (message.isBlank()) {
                        adapter.warn("Tried to execute BroadcastMessage action with an empty message.");
                        return;
                    }
                    adapter.broadcast(adapter.resolveActionText(action, questPlayer, message, objects));
                })
                .actionDescription((action, questPlayer, objects) -> "Broadcasts message: " + action.text(MESSAGE))
                .register();
    }
}
