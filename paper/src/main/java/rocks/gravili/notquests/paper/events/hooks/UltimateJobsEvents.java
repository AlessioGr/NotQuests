package rocks.gravili.notquests.paper.events.hooks;

import de.warsteiner.jobs.UltimateJobs;
import de.warsteiner.jobs.utils.cevents.PlayerJobExpEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.hooks.ultimatejobs.UltimateJobsReachJobLevelObjective;

public class UltimateJobsEvents implements Listener {
    private final NotQuests main;

    public UltimateJobsEvents(NotQuests main) {
        this.main = main;
    }

    @EventHandler
    public void onJobsLevelUp(PlayerJobExpEvent e) {
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(e.getPlayer().getUniqueId());
        if (questPlayer != null) {
            if (questPlayer.getActiveQuests().size() > 0) {
                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                    for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                        if (activeObjective.isUnlocked()) {
                            if (activeObjective.getObjective() instanceof UltimateJobsReachJobLevelObjective ultimateJobsReachJobLevelObjective) {
                                if (!UltimateJobs.getPlugin().getJobAPI().getID(e.getJob()).equalsIgnoreCase(ultimateJobsReachJobLevelObjective.getJobID())) {
                                    return;
                                }
                                activeObjective.addProgress(1);
                            }
                        }
                    }
                    activeQuest.removeCompletedObjectives(true);
                }
                questPlayer.removeCompletedQuests();
            }
        }
    }


}
