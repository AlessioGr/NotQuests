package com.notquests.builtin.actions;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Actions;

public final class SendMessageAction {
    private static final String MESSAGE = "message";

    private SendMessageAction() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.actions().action("SendMessage")
                .displayName("Send Message")
                .description("Sends a private chat message to the target player.")
                .field(
                        MESSAGE,
                        adapter.fields().greedyText().config("specifics.message"),
                        "Message sent privately to the target player. Supports MiniMessage formatting and NotQuests placeholders.")
                .singleLine((action, arguments) -> action.setValue(MESSAGE, String.join(" ", arguments)))
                .execute((action, questPlayer, objects) -> {
                    final String message = action.text(MESSAGE);
                    if (questPlayer == null || !questPlayer.hasPlayer() || message.isBlank()) {
                        adapter.warn("Tried to execute SendMessage action without a target player or message.");
                        return;
                    }
                    questPlayer.sendMessage(resolve(adapter, action, questPlayer, message, objects));
                })
                .actionDescription((action, questPlayer, objects) -> "Sends message: " + action.text(MESSAGE))
                .register();
    }

    static String resolve(
            final NotQuestsAdapter adapter,
            final Actions.Data action,
            final PlatformPlayer questPlayer,
            final String text,
            final Object... objects) {
        return adapter.resolveActionText(action, questPlayer, text, objects);
    }
}
