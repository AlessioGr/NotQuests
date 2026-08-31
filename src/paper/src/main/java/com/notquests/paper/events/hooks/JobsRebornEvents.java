package com.notquests.paper.events.hooks;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.api.JobsLevelUpEvent;
import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobProgression;
import com.gamingmesh.jobs.container.JobsPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.notquests.paper.NotQuests;

import java.util.UUID;

public class JobsRebornEvents implements Listener {
    private final NotQuests main;

    public JobsRebornEvents(NotQuests main) {
        this.main = main;
    }

    @EventHandler
    public void onJobsLevelUp(JobsLevelUpEvent e) {
        if (e.isCancelled()) {
            return;
        }
        main.getCorePlugin().jobsLevelChanged(
                e.getPlayer().getUniqueId().toString(),
                e.getJob().getName(),
                currentLevel(e.getPlayer().getUniqueId().toString(), e.getJob().getName()));
    }

    public static double currentLevel(final String playerIdentifier, final String jobName) {
        final Job job = Jobs.getJob(jobName);
        final JobsPlayer jobsPlayer;
        try {
            jobsPlayer = Jobs.getPlayerManager().getJobsPlayer(
                    UUID.fromString(playerIdentifier));
        } catch (final RuntimeException exception) {
            return Double.NaN;
        }
        if (job == null || jobsPlayer == null) {
            return Double.NaN;
        }
        final JobProgression progression = jobsPlayer.getJobProgression(job);
        return progression == null ? 0 : progression.getLevel();
    }
}
