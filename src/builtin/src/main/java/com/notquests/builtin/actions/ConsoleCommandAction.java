package com.notquests.builtin.actions;

import com.notquests.core.platform.NotQuestsAdapter;

public final class ConsoleCommandAction {
    private static final String COMMAND = "command";

    private ConsoleCommandAction() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.actions().action("ConsoleCommand")
                .displayName("Console Command")
                .description("Runs a server command from the console.")
                .field(
                        COMMAND,
                        adapter.fields().commandText().config("specifics.consoleCommand"),
                        "Command executed by the server console. The leading slash is optional.")
                .singleLine((action, arguments) -> action.setValue(COMMAND, normalizeCommand(String.join(" ", arguments))))
                .execute((action, questPlayer, objects) -> {
                    final String command = action.text(COMMAND);
                    if (questPlayer == null || !questPlayer.hasPlayer() || command.isBlank()) {
                        adapter.warn("Tried to execute ConsoleCommand action without a target player or command.");
                        return;
                    }
                    adapter.dispatchConsoleCommand(adapter.resolveActionText(action, questPlayer, command, objects));
                })
                .actionDescription((action, questPlayer, objects) -> "Console command: " + action.text(COMMAND))
                .register();
    }

    private static String normalizeCommand(final String command) {
        if (command == null || command.isBlank()) {
            return "";
        }
        return command.startsWith("/") ? command : "/" + command;
    }
}
