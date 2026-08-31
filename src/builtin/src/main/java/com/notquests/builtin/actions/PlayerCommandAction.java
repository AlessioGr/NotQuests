package com.notquests.builtin.actions;

import com.notquests.core.platform.NotQuestsAdapter;

public final class PlayerCommandAction {
    private static final String COMMAND = "command";

    private PlayerCommandAction() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.actions().action("PlayerCommand")
                .displayName("Player Command")
                .description("Runs a command from the target player's perspective.")
                .field(
                        COMMAND,
                        adapter.fields().commandText().config("specifics.playerCommand"),
                        "Command executed by the target player. The leading slash is optional.")
                .singleLine((action, arguments) -> action.setValue(COMMAND, normalizeCommand(String.join(" ", arguments))))
                .execute((action, questPlayer, objects) -> {
                    final String command = action.text(COMMAND);
                    if (questPlayer == null || !questPlayer.hasPlayer() || command.isBlank()) {
                        adapter.warn("Tried to execute PlayerCommand action without a target player or command.");
                        return;
                    }
                    questPlayer.performCommand(adapter.resolveActionText(action, questPlayer, command, objects));
                })
                .actionDescription((action, questPlayer, objects) -> "Player command: " + action.text(COMMAND))
                .register();
    }

    private static String normalizeCommand(final String command) {
        if (command == null || command.isBlank()) {
            return "";
        }
        return command.startsWith("/") ? command : "/" + command;
    }
}
