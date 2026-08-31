package com.notquests.builtin.actions;

import com.notquests.core.platform.NotQuestsAdapter;

public final class ChatAction {
    private static final String MESSAGE = "message";

    private ChatAction() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.actions().action("Chat")
                .displayName("Chat")
                .description("Makes the target player send a chat message.")
                .field(
                        MESSAGE,
                        adapter.fields().text().config("specifics.chatMessage"),
                        "Chat message sent by the target player. Wrap it in quotes when using spaces in commands.")
                .singleLine((action, arguments) -> action.setValue(MESSAGE, String.join(" ", arguments)))
                .execute((action, questPlayer, objects) -> {
                    final String message = action.text(MESSAGE);
                    if (questPlayer == null || !questPlayer.hasPlayer() || message.isBlank()) {
                        adapter.warn("Tried to execute Chat action without a target player or message.");
                        return;
                    }
                    questPlayer.chat(adapter.resolveActionText(action, questPlayer, message, objects));
                })
                .actionDescription((action, questPlayer, objects) -> "Player chat message: " + action.text(MESSAGE))
                .register();
    }
}
