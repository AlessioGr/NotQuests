package com.notquests.paper.builtin.actions;

import org.betonquest.betonquest.api.QuestException;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.paper.NotQuests;
import com.notquests.paper.PaperNotQuestsAdapter;
import com.notquests.paper.integrations.betonquest.BetonQuestIntegration;

public final class BetonQuestFireInlineEventAction {
    private static final String ACTION = "action";

    private BetonQuestFireInlineEventAction() {}

    public static void register(final NotQuests main, final NotQuestsAdapter adapter) {
        adapter.actions().action("BetonQuestFireInlineEvent")
                .displayName("BetonQuest Fire Inline Event")
                .description("Runs an inline BetonQuest action instruction.")
                .field(ACTION, adapter.fields().greedyText().config("specifics.event"), "Inline BetonQuest action instruction to run.")
                .singleLine((action, arguments) -> action.setValue(ACTION, String.join(" ", arguments)))
                .execute((action, questPlayer, objects) -> {
                    final String actionInstruction = action.text(ACTION);
                    main.getCorePlugin().executeBetonQuestInlineAction(
                            questPlayer,
                            actionInstruction,
                            target -> {
                                try {
                                    betonQuest(main).runInlineAction(
                                            PaperNotQuestsAdapter.asPaperPlayer(target),
                                            actionInstruction);
                                } catch (final QuestException exception) {
                                    throw new IllegalStateException(exception);
                                }
                            });
                })
                .actionDescription((action, questPlayer, objects) ->
                        "Executes inline BetonQuest action: " + action.text(ACTION))
                .register();
    }

    private static BetonQuestIntegration betonQuest(final NotQuests main) {
        return main.integrations().betonQuest();
    }
}
