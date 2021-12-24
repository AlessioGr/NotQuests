/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021 Alessio Gravili
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

package rocks.gravili.notquestsspigot.objectives.hooks.jobsreborn;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobProgression;
import com.gamingmesh.jobs.container.JobsPlayer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquestsspigot.NotQuests;
import rocks.gravili.notquestsspigot.commands.NotQuestColors;
import rocks.gravili.notquestsspigot.objectives.Objective;
import rocks.gravili.notquestsspigot.structs.ActiveObjective;

import java.util.ArrayList;
import java.util.List;

public class JobsRebornReachJobLevel extends Objective {

    private boolean countPreviousLevels = true;
    private String jobName;

    public JobsRebornReachJobLevel(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        if (!main.getIntegrationsManager().isJobsRebornEnabled()) {
            return;
        }

        manager.command(addObjectiveBuilder.literal("JobsRebornReachJobLevel")
                .argument(StringArgument.<CommandSender>newBuilder("Job Name").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            final Audience audience = main.adventure().sender(context.getSender());
                            main.getUtilManager().sendFancyCommandCompletion(audience, allArgs.toArray(new String[0]), "[Job Name]", "");

                            ArrayList<String> completions = new ArrayList<>();
                            for (Job job : Jobs.getJobs()) {
                                completions.add(job.getName());
                            }
                            return completions;
                        }
                ).single().build(), ArgumentDescription.of("Name of the job"))
                .argument(IntegerArgument.<CommandSender>newBuilder("level").withMin(1), ArgumentDescription.of("Job Level which needs to be reached"))
                .flag(
                        manager.flagBuilder("doNotCountPreviousLevels")
                                .withDescription(ArgumentDescription.of("Makes it so only additional levels gained from the time of unlocking this Objective will count (and previous/existing counts will not count, so it starts from zero)"))
                )
                .meta(CommandMeta.DESCRIPTION, "Adds a new JobsRebornReachJobLevel Objective to a quest")
                .handler((context) -> {
                    Audience audience = main.adventure().sender(context.getSender());
                    int amount = context.get("level");
                    final boolean countPreviousLevels = !context.flags().isPresent("doNotCountPreviousLevels");
                    final String jobName = context.get("Job Name");

                    if (Jobs.getJob(jobName) == null) {
                        audience.sendMessage(MiniMessage.miniMessage().parse(
                                NotQuestColors.errorGradient + "Error: The Job with the name " + NotQuestColors.highlightGradient + jobName + "</GRADIENT> was not found!"
                        ));
                        return;
                    }

                    JobsRebornReachJobLevel jobsRebornReachJobLevel = new JobsRebornReachJobLevel(main);
                    jobsRebornReachJobLevel.setProgressNeeded(amount);
                    jobsRebornReachJobLevel.setCountPreviousLevels(countPreviousLevels);
                    jobsRebornReachJobLevel.setJobName(jobName);

                    main.getObjectiveManager().addObjective(jobsRebornReachJobLevel, context);
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

    public final long getLevelToReach() {
        return getProgressNeeded();
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.jobsRebornReachJobLevel.base", player)
                .replace("%EVENTUALCOLOR%", eventualColor)
                .replace("%AMOUNT%", "" + getLevelToReach())
                .replace("%JOB%", "" + getJobName());
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.countPreviousLevels", isCountPreviousLevels());
        configuration.set(initialPath + ".specifics.jobName", getJobName());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        countPreviousLevels = configuration.getBoolean(initialPath + ".specifics.countPreviousTowns");
        jobName = configuration.getString(initialPath + ".specifics.jobName");

        //Warn
        final Job job = Jobs.getJob(getJobName());
        if (job == null) {
            main.getLogManager().warn("The job <AQUA>" + getJobName() + "</AQUA> does not exist.");
        }
    }


    @Override
    public void onObjectiveUnlock(ActiveObjective activeObjective) {
        activeObjective.addProgress(1); //Job levels start at 1 and not 0
        if (!main.getIntegrationsManager().isJobsRebornEnabled() || !isCountPreviousLevels()) {
            return;
        }

        final Job job = Jobs.getJob(getJobName());
        if (job == null) {
            main.getLogManager().warn("The job <AQUA>" + getJobName() + "</AQUA> does not exist.");
            return;
        }


        final JobsPlayer jobsPlayer = Jobs.getPlayerManager().getJobsPlayer(activeObjective.getQuestPlayer().getUUID());
        if (jobsPlayer == null) {
            return;
        }

        JobProgression jobProgression = jobsPlayer.getJobProgression(job);

        if (jobProgression == null) {
            return;
        }

        activeObjective.addProgress(jobProgression.getLevel());
    }
}
