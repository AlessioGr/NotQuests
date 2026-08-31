package com.notquests.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;

import java.util.Map;

public final class RunCommandObjective {
    private RunCommandObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.objectives().objective("RunCommand")
                .displayName("Run Command")
                .description("Counts when the player runs a matching command.")
                .field(
                        "amount",
                        adapter.fields().numberExpression().progressNeeded(),
                        "Number of times the player must run the matching command.")
                .field(
                        "command",
                        adapter.fields().commandText().config("specifics.commandToRun"),
                        "Command the player must run. The leading slash is optional.")
                .flag(
                        "ignoreCase",
                        adapter.fields().presenceFlag().config("specifics.ignoreCase"),
                        "Compare the command without caring about letter case.")
                .flag(
                        "cancelCommand",
                        adapter.fields().presenceFlag().config("specifics.cancelCommand"),
                        "Cancel the command after it counts for this objective.")
                .taskDescription((objective, questPlayer, activeObjective) -> adapter.objectiveTaskText(
                        "chat.objectives.taskDescription.runCommand.base",
                        questPlayer,
                        activeObjective,
                        Map.of("%COMMANDTORUN%", objective.text("command"))))
                .onPlayerRunCommand((event, objective) -> {
                    final String expected = normalizeCommand(objective.text("command"));
                    final String actual = normalizeCommand(event.command());
                    final boolean matches = objective.flag("ignoreCase")
                            ? actual.equalsIgnoreCase(expected)
                            : actual.equals(expected);
                    if (!matches) {
                        return;
                    }
                    objective.addProgress(1);
                    if (objective.flag("cancelCommand")) {
                        event.cancel();
                    }
                })
                .register();
    }

    private static String normalizeCommand(final String command) {
        if (command == null || command.isBlank()) {
            return "";
        }
        return command.startsWith("/") ? command : "/" + command;
    }
}
