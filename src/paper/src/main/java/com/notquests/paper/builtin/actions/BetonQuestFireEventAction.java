package com.notquests.paper.builtin.actions;

import org.betonquest.betonquest.api.QuestException;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.paper.NotQuests;
import com.notquests.paper.PaperNotQuestsAdapter;
import com.notquests.paper.integrations.betonquest.BetonQuestIntegration;

public final class BetonQuestFireEventAction {
    private static final String PACKAGE = "package";
    private static final String ACTION = "action";

    private BetonQuestFireEventAction() {}

    public static void register(final NotQuests main, final NotQuestsAdapter adapter) {
        adapter.actions().action("BetonQuestFireEvent")
                .displayName("BetonQuest Fire Event")
                .description("Runs a named BetonQuest action from a BetonQuest package.")
                .field(PACKAGE, adapter.fields().text().config("specifics.packageName"), "BetonQuest package containing the action to run.")
                .field(ACTION, adapter.fields().text().config("specifics.eventName"), "BetonQuest action name to run.")
                .singleLine((action, arguments) -> {
                    if (arguments.size() >= 2) {
                        action.setValue(PACKAGE, arguments.get(0));
                        action.setValue(ACTION, arguments.get(1));
                    }
                })
                .execute((action, questPlayer, objects) -> {
                    final String packageName = action.text(PACKAGE);
                    final String actionName = action.text(ACTION);
                    main.getCorePlugin().executeBetonQuestNamedAction(
                            questPlayer,
                            packageName,
                            actionName,
                            target -> {
                                try {
                                    betonQuest(main).runAction(
                                            PaperNotQuestsAdapter.asPaperPlayer(target),
                                            packageName,
                                            actionName);
                                } catch (final QuestException exception) {
                                    throw new IllegalStateException(exception);
                                }
                            });
                })
                .actionDescription((action, questPlayer, objects) ->
                        "Executes BetonQuest action: " + action.text(PACKAGE) + "." + action.text(ACTION))
                .register();
    }

    private static BetonQuestIntegration betonQuest(final NotQuests main) {
        return main.integrations().betonQuest();
    }
}
