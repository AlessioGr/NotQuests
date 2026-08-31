package com.notquests.builtin.actions;

import com.notquests.core.platform.NotQuestsAdapter;

public final class ShowActionBarAction {
    private static final String MESSAGE = "message";

    private ShowActionBarAction() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.actions().action("ShowActionBar")
                .displayName("Show Action Bar")
                .description("Shows a short action-bar message above the target player's hotbar.")
                .field(
                        MESSAGE,
                        adapter.fields().greedyText().config("specifics.message"),
                        "Action-bar message shown above the target player's hotbar. Supports MiniMessage formatting and NotQuests placeholders.")
                .singleLine((action, arguments) -> action.setValue(MESSAGE, String.join(" ", arguments)))
                .execute((action, questPlayer, objects) -> {
                    final String message = action.text(MESSAGE);
                    if (questPlayer == null || !questPlayer.hasPlayer() || message.isBlank()) {
                        return;
                    }
                    questPlayer.sendActionBar(SendMessageAction.resolve(adapter, action, questPlayer, message, objects));
                })
                .actionDescription((action, questPlayer, objects) -> "Shows action bar: " + action.text(MESSAGE))
                .register();
    }
}
