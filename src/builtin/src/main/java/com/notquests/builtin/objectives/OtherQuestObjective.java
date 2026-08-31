package com.notquests.builtin.objectives;

import com.notquests.core.NotQuestsPlugin;
import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Objectives.Kinds;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;

import java.util.Map;

public final class OtherQuestObjective {
    private OtherQuestObjective() {}

    public static void register(final NotQuestsAdapter adapter) {
        register(NotQuestsPlugin.create(), adapter);
    }

    public static void register(final NotQuestsPlugin plugin, final NotQuestsAdapter adapter) {
        adapter.objectives().objective(Kinds.OTHER_QUEST)
                .displayName("Other Quest")
                .description("Counts completions of another quest.")
                .field(
                        Kinds.OTHER_QUEST_TARGET,
                        adapter.fields().text(plugin::questNames).config("specifics.otherQuestName"),
                        "Quest that must be completed.")
                .field(
                        "amount",
                        adapter.fields().numberExpression().progressNeeded(),
                        "Number of completions required.")
                .flag(
                        "countPreviouslyCompletedQuests",
                        adapter.fields().presenceFlag().config("specifics.countPreviousCompletions"),
                        "Counts matching completions that already existed when this objective unlocked.")
                .taskDescription((objective, questPlayer, activeObjective) -> adapter.objectiveTaskText(
                        "chat.objectives.taskDescription.otherQuest.base",
                        questPlayer,
                        activeObjective,
                        Map.of(
                                "%OTHERQUESTNAME%",
                                objective.text(Kinds.OTHER_QUEST_TARGET))))
                .onUnlock((objective, questPlayer, loading) -> {
                    if (!loading && objective.flag("countPreviouslyCompletedQuests")) {
                        objective.addProgress(plugin.completedQuestCount(
                                questPlayer,
                                objective.text(Kinds.OTHER_QUEST_TARGET)));
                    }
                })
                .onRefresh((refresh, objective) -> {
                    if (refresh.questCompleted()
                            && refresh.matchesCompletedQuest(objective.text(Kinds.OTHER_QUEST_TARGET))) {
                        objective.addProgress(1);
                    }
                })
                .register();
    }
}
