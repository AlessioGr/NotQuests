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

package rocks.gravili.notquests.paper.structs.objectives.hooks.ultimatejobs;

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
import de.warsteiner.jobs.UltimateJobs;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.NotQuestColors;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.objectives.Objective;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UltimateJobsReachJobLevelObjective extends Objective {

    private boolean countPreviousLevels = true;
    private String jobID;

    public UltimateJobsReachJobLevelObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        if (!main.getIntegrationsManager().isUltimateJobsEnabled()) {
            return;
        }

        manager.command(addObjectiveBuilder
                .argument(StringArgument.<CommandSender>newBuilder("Job ID").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Job ID]", "");

                            ArrayList<String> completions = new ArrayList<>();
                            for (Job job : Jobs.getJobs()) {
                                completions.add(job.getName());
                            }
                            for(File loadedJob : UltimateJobs.getPlugin().getLoadedJobs()){
                                UltimateJobs.getPlugin().getJobAPI().getID(loadedJob);
                            }
                            return completions;
                        }
                ).single().build(), ArgumentDescription.of("ID of the job"))
                .argument(IntegerArgument.<CommandSender>newBuilder("level").withMin(1), ArgumentDescription.of("Job Level which needs to be reached"))
                .flag(
                        manager.flagBuilder("doNotCountPreviousLevels")
                                .withDescription(ArgumentDescription.of("Makes it so only additional levels gained from the time of unlocking this Objective will count (and previous/existing counts will not count, so it starts from zero)"))
                )
                .handler((context) -> {
                    int amount = context.get("level");
                    final boolean countPreviousLevels = !context.flags().isPresent("doNotCountPreviousLevels");
                    final String jobID = context.get("Job ID");

                    UltimateJobsReachJobLevelObjective ultimateJobsReachJobLevelObjective = new UltimateJobsReachJobLevelObjective(main);
                    ultimateJobsReachJobLevelObjective.setProgressNeeded(amount);
                    ultimateJobsReachJobLevelObjective.setCountPreviousLevels(countPreviousLevels);
                    ultimateJobsReachJobLevelObjective.setJobID(jobID);


                    main.getObjectiveManager().addObjective(ultimateJobsReachJobLevelObjective, context);
                }));
    }

    public final String getJobID() {
        return jobID;
    }

    public void setJobID(final String jobID) {
        this.jobID = jobID;
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
    public String getObjectiveTaskDescription(final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.ultimateJobsRebornReachJobLevel.base", player, Map.of(
                "%AMOUNT%", "" + getLevelToReach(),
                "%JOBID%", getJobID()
        ));
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.countPreviousLevels", isCountPreviousLevels());
        configuration.set(initialPath + ".specifics.jobID", getJobID());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        countPreviousLevels = configuration.getBoolean(initialPath + ".specifics.countPreviousTowns");
        jobID = configuration.getString(initialPath + ".specifics.jobID");

    }
    @Override
    public void onObjectiveUnlock(final ActiveObjective activeObjective, final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
        if(activeObjective.getCurrentProgress() != 0){
            return;
        }

        activeObjective.addProgress(1); //Job levels start at 1 and not 0
        if (!main.getIntegrationsManager().isJobsRebornEnabled() || !isCountPreviousLevels()) {
            return;
        }

        activeObjective.addProgress(UltimateJobs.getPlugin().getPlayerAPI().getJobLevel(activeObjective.getQuestPlayer().getUUID().toString(), jobID));
    }
    @Override
    public void onObjectiveCompleteOrLock(final ActiveObjective activeObjective, final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess, final boolean completed) {
    }

}
