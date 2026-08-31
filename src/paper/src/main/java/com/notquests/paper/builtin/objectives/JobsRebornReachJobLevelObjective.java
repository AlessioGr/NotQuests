package com.notquests.paper.builtin.objectives;

import com.gamingmesh.jobs.Jobs;

import com.notquests.core.platform.NotQuestsAdapter;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.structs.ActiveObjective;
import com.notquests.paper.NotQuests;
import com.notquests.paper.events.hooks.JobsRebornEvents;

public final class JobsRebornReachJobLevelObjective {
    public static final String TYPE = "JobsRebornReachJobLevel";
    private static final String JOB_NAME = "jobName";
    private static final String LEVEL = "level";
    private static final String DO_NOT_COUNT_PREVIOUS_LEVELS = "doNotCountPreviousLevels";

    private JobsRebornReachJobLevelObjective() {}

    public static void register(final NotQuests main, final NotQuestsAdapter adapter) {
        if (!main.getCorePlugin().integrationEnabled("Jobs")) {
            return;
        }
        adapter.objectives().objective(TYPE)
                .displayName("Reach Jobs Reborn Level")
                .description("Counts when the player reaches a target level in a Jobs Reborn job.")
                .field(
                        JOB_NAME,
                        adapter.fields().text().config("specifics.jobName"),
                        "Jobs Reborn job the player must level.")
                .field(
                        LEVEL,
                        adapter.fields().numberExpression(false).progressNeeded(),
                        "Target job level the player must reach.")
                .flag(
                        DO_NOT_COUNT_PREVIOUS_LEVELS,
                        adapter.fields().presenceFlag().invertedBooleanConfig("specifics.countPreviousLevels"),
                        "Only count job levels gained after this objective unlocks; existing levels do not count.")
                .taskDescription((objective, questPlayer, activeObjective) ->
                        main.getCorePlugin().jobsTaskDescription(questPlayer, objective, activeObjective))
                .onUnlock((objective, questPlayer, loading) -> main.getCorePlugin().jobsObjectiveUnlocked(
                        objective,
                        loading,
                        JobsRebornEvents.currentLevel(
                                questPlayer == null ? "" : questPlayer.playerIdentifier(),
                                objective.text(JOB_NAME))))
                .afterLoad(objective -> main.getCorePlugin().jobsObjectiveLoaded(
                        objective,
                        Jobs.getJob(objective.text(JOB_NAME)) != null))
                .register();
    }

}
