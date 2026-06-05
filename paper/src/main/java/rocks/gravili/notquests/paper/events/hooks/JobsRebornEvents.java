/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
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

package rocks.gravili.notquests.paper.events.hooks;

import com.gamingmesh.jobs.api.JobsLevelUpEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.hooks.jobsreborn.JobsRebornReachJobLevelObjective;

public class JobsRebornEvents implements Listener {
    private final NotQuests main;

    public JobsRebornEvents(NotQuests main) {
        this.main = main;
        startLevelSyncTask();
    }

    /**
     * "Reach job level" objectives should always reflect the player's real job level, however they
     * reached it. {@link JobsLevelUpEvent} only fires on natural level-ups (admin commands such as
     * {@code /jobs level <player> <job> add} bypass it), so we additionally poll every few seconds
     * and re-sync the progress to the live level. {@code setProgress} is a no-op when nothing changed,
     * so this is cheap and never double-counts.
     */
    private void startLevelSyncTask() {
        Bukkit.getScheduler()
                .runTaskTimer(
                        main.getMain(),
                        () -> {
                            if (main.getDataManager().isDisabled()
                                    || !main.getIntegrationsManager().isJobsRebornEnabled()) {
                                return;
                            }
                            for (final Player player : Bukkit.getOnlinePlayers()) {
                                final QuestPlayer questPlayer =
                                        main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
                                if (questPlayer == null || questPlayer.getActiveQuests().isEmpty()) {
                                    continue;
                                }
                                syncObjectives(questPlayer);
                            }
                        },
                        60L,
                        60L);
    }

    private void syncObjectives(final QuestPlayer questPlayer) {
        for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                if (activeObjective.isUnlocked()
                        && activeObjective.getObjective()
                                instanceof final JobsRebornReachJobLevelObjective jobsObjective) {
                    jobsObjective.updateProgressToCurrentLevel(activeObjective);
                }
            }
            activeQuest.removeCompletedObjectives(true);
        }
        questPlayer.removeCompletedQuests();
    }

    @EventHandler
    public void onJobsLevelUp(JobsLevelUpEvent e) {
        if (e.isCancelled()) {
            return;
        }
        final QuestPlayer questPlayer =
                main.getQuestPlayerManager().getActiveQuestPlayer(e.getPlayer().getUniqueId());
        if (questPlayer == null || questPlayer.getActiveQuests().isEmpty()) {
            return;
        }
        for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                if (activeObjective.isUnlocked()
                        && activeObjective.getObjective()
                                instanceof final JobsRebornReachJobLevelObjective jobsObjective) {
                    if (!e.getJob().getName().equalsIgnoreCase(jobsObjective.getJobName())) {
                        continue;
                    }
                    if (jobsObjective.isCountPreviousLevels()) {
                        jobsObjective.updateProgressToCurrentLevel(activeObjective);
                    } else {
                        activeObjective.addProgress(1);
                    }
                }
            }
            activeQuest.removeCompletedObjectives(true);
        }
        questPlayer.removeCompletedQuests();
    }
}
