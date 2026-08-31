package com.notquests.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;

import java.util.Map;

public final class TriggerCommandObjective {
    public static final String TYPE = "TriggerCommand";
    public static final String TRIGGER_NAME = "triggerName";

    private TriggerCommandObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        adapter.objectives().objective(TYPE)
                .displayName("Trigger Command")
                .description("Counts when a matching NotQuests trigger command is fired.")
                .field(
                        TRIGGER_NAME,
                        adapter.fields().text().config("specifics.triggerName"),
                        "Trigger command name that actions, BetonQuest events, or `/qa trigger` can fire.")
                .field(
                        "amount",
                        adapter.fields().numberExpression().progressNeeded(),
                        "Number of trigger command calls required.")
                .taskDescription((objective, questPlayer, activeObjective) -> adapter.objectiveTaskText(
                        "chat.objectives.taskDescription.triggerCommand.base",
                        questPlayer,
                        activeObjective,
                        Map.of("%TRIGGERNAME%", objective.text(TRIGGER_NAME))))
                .onPlayerRunCommand((event, objective) -> {
                    if (objective.text(TRIGGER_NAME).equalsIgnoreCase(event.command())) {
                        objective.addProgress(1);
                    }
                })
                .register();
    }
}
