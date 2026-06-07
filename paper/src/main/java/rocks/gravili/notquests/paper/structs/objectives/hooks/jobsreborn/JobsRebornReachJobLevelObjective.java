/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.paper.structs.objectives.hooks.jobsreborn;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobProgression;
import com.gamingmesh.jobs.container.JobsPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.Command;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.suggestion.Suggestion;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.Objective;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.incendo.cloud.parser.standard.StringParser.stringParser;
import static rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueParser.numberVariableParser;

public class JobsRebornReachJobLevelObjective extends Objective {

    private boolean countPreviousLevels = true;
    private String jobName;

    public JobsRebornReachJobLevelObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            PaperCommandManager<CommandSender> manager,
            Command.Builder<CommandSender> addObjectiveBuilder,
            final int level) {
        if (!main.getIntegrationsManager().isJobsRebornEnabled()) {
            return;
        }

        manager.command(addObjectiveBuilder
                .required("Job Name", stringParser(), Description.of("Name of the job"), (context, lastString) -> {
                    main.getUtilManager().sendFancyCommandCompletion(context.sender(), lastString.input().split(" "), "[Job Name]", "");
                    ArrayList<Suggestion> completions = new ArrayList<>();
                    for (Job job : Jobs.getJobs()) {
                        completions.add(Suggestion.suggestion(job.getName()));
                    }
                    return CompletableFuture.completedFuture(completions);
                })
                .required("level", numberVariableParser("level", null), Description.of("Job level which needs to be reached"))
                .flag(manager.flagBuilder("doNotCountPreviousLevels").withDescription(Description.of("Makes it so only additional levels gained from the time of unlocking this Objective will count (and previous/existing counts will not count, so it starts from zero)")))
                .handler((context) -> {
                    final String amountExpression = context.get("level");
                    final boolean countPreviousLevels = !context.flags().isPresent("doNotCountPreviousLevels");
                    final String jobName = context.get("Job Name");

                    if (Jobs.getJob(jobName) == null) {
                        context.sender().sendMessage(main.parse("<error>Error: The Job with the name <highlight>" + jobName + "</highlight> was not found!"));
                        return;
                    }

                    JobsRebornReachJobLevelObjective jobsRebornReachJobLevel = new JobsRebornReachJobLevelObjective(main);
                    jobsRebornReachJobLevel.setProgressNeededExpression(amountExpression);
                    jobsRebornReachJobLevel.setCountPreviousLevels(countPreviousLevels);
                    jobsRebornReachJobLevel.setJobName(jobName);

                    main.getObjectiveManager().addObjective(jobsRebornReachJobLevel, context, level);
                }));
    }

    public final String getJobName() {
        return jobName;
    }

    public void setJobName(final String jobName) {
        this.jobName = jobName;
    }

    public final boolean isCountPreviousLevels() {
        return countPreviousLevels;
    }

    public void setCountPreviousLevels(final boolean countPreviousLevels) {
        this.countPreviousLevels = countPreviousLevels;
    }

    @Override
    public String getTaskDescriptionInternal(
            final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
        return main.getLanguageManager()
                .getString(
                        "chat.objectives.taskDescription.jobsRebornReachJobLevel.base",
                        questPlayer,
                        Map.of(
                                "%AMOUNT%",
                                ""
                                        + (activeObjective != null
                                        ? activeObjective.getProgressNeeded()
                                        : getProgressNeededExpression()),
                                "%JOB%",
                                getJobName()));
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.countPreviousLevels", isCountPreviousLevels());
        configuration.set(initialPath + ".specifics.jobName", getJobName());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        countPreviousLevels = configuration.getBoolean(initialPath + ".specifics.countPreviousLevels");
        jobName = configuration.getString(initialPath + ".specifics.jobName");

        // Warn
        final Job job = Jobs.getJob(getJobName());
        if (job == null) {
            main.getLogManager()
                    .warn("The job <highlight>" + getJobName() + "</highlight> does not exist.");
        }
    }

    @Override
    public void onObjectiveUnlock(
            final ActiveObjective activeObjective,
            final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
        if (unlockedDuringPluginStartupQuestLoadingProcess) {
            return;
        }
        if (isCountPreviousLevels()) {
            // "Reach job level N": progress simply mirrors the player's current job level, and is kept
            // in sync afterwards (JobsRebornEvents handles level-ups + a periodic re-sync). This also
            // completes the objective immediately if the player is already at/above the target level.
            updateProgressToCurrentLevel(activeObjective);
        } else if (activeObjective.getCurrentProgress() == 0) {
            // "Gain N levels from now": start counting from here (incremented on each level-up).
            activeObjective.addProgress(1); // Job levels start at 1 and not 0
        }
    }

    /**
     * Re-syncs this objective's progress to the player's <b>current</b> job level (only in the
     * count-previous-levels "reach level" mode). Safe to call repeatedly: {@link
     * ActiveObjective#setProgress(double, boolean)} is a no-op when the value is unchanged, so this
     * can be driven from both the Jobs level-up event and a periodic task without double-counting.
     * This is what makes the objective track the real level no matter how it changed (including admin
     * commands like {@code /jobs level add}, which do not fire a level-up event).
     */
    public void updateProgressToCurrentLevel(final ActiveObjective activeObjective) {
        if (!isCountPreviousLevels() || !main.getIntegrationsManager().isJobsRebornEnabled()) {
            return;
        }
        final Job job = Jobs.getJob(getJobName());
        if (job == null) {
            return;
        }
        final JobsPlayer jobsPlayer =
                Jobs.getPlayerManager().getJobsPlayer(activeObjective.getQuestPlayer().getUniqueId());
        if (jobsPlayer == null) {
            return;
        }
        final JobProgression jobProgression = jobsPlayer.getJobProgression(job);
        final int level = (jobProgression == null) ? 0 : jobProgression.getLevel();
        activeObjective.setProgress(level, true);
    }

    @Override
    public void onObjectiveCompleteOrLock(
            final ActiveObjective activeObjective,
            final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
            final boolean completed) {
    }
}
