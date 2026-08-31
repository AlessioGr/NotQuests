package com.notquests.paper.builtin.objectives;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.platform.PlatformPlayer;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.structs.ActiveObjective;
import com.notquests.paper.NotQuests;

import java.util.Map;

public final class BetonQuestObjectiveStateChangeObjective {
    public static final String TYPE = "BetonQuestObjectiveStateChange";
    private static final String PACKAGE_NAME = "packageName";
    private static final String OBJECTIVE_NAME = "objectiveName";
    private static final String OBJECTIVE_STATE = "objectiveState";

    private BetonQuestObjectiveStateChangeObjective() {}

    public static void register(final NotQuests main, final NotQuestsAdapter adapter) {
        if (!main.getCorePlugin().integrationEnabled("BetonQuest")) {
            return;
        }
        adapter.objectives().objective(TYPE)
                .displayName("BetonQuest Objective State Change")
                .description("Counts when a BetonQuest objective changes to a configured state.")
                .field(
                        PACKAGE_NAME,
                        adapter.fields().text().config("specifics.packageName"),
                        "BetonQuest package that contains the objective to watch.")
                .field(
                        OBJECTIVE_NAME,
                        adapter.fields().text().config("specifics.objectiveName"),
                        "BetonQuest objective whose state change should count.")
                .field(
                        OBJECTIVE_STATE,
                        adapter.fields().text().config("specifics.objectiveState"),
                        "BetonQuest objective state that should count as progress.")
                .taskDescription((objective, questPlayer, activeObjective) -> taskDescription(main, objective, questPlayer, activeObjective))
                .register();
    }

    private static String taskDescription(
            final NotQuests main,
            final Objectives.Data objective,
            final PlatformPlayer questPlayer,
            final ActiveObjective activeObjective) {
        return main.getCorePlugin().translate(questPlayer,
                        "chat.objectives.taskDescription.BetonQuestCompleteObjective.base",
                        Map.of("%BETONQUESTOBJECTIVENAME%", objective.text(OBJECTIVE_NAME)),
                        "    <GRAY>Complete BetonQuest objective <WHITE>%BETONQUESTOBJECTIVENAME%</WHITE>.");
    }
}
