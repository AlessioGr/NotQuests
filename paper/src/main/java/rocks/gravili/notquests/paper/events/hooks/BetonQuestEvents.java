package rocks.gravili.notquests.paper.events.hooks;

import io.lumine.xikage.mythicmobs.mobs.MythicMob;
import org.betonquest.betonquest.api.Objective;
import org.betonquest.betonquest.api.PlayerObjectiveChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.hooks.betonquest.BetonQuestObjectiveStateChangeObjective;

public class BetonQuestEvents implements Listener {
    private final NotQuests main;

    public BetonQuestEvents(NotQuests main) {
        this.main = main;
    }

    @EventHandler
    public void onBetonQuestObjectiveStateChange(final PlayerObjectiveChangeEvent e) {
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(e.getPlayer().getUniqueId());
        if (questPlayer != null) {
            if (questPlayer.getActiveQuests().size() > 0) {
                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                    for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                        if (activeObjective.getObjective() instanceof BetonQuestObjectiveStateChangeObjective betonQuestObjectiveStateChangeObjective) {
                            if (activeObjective.isUnlocked()) {
                                if(e.getState() == betonQuestObjectiveStateChangeObjective.getObjectiveState()){
                                    if(e.getObjectiveID().getFullID().equalsIgnoreCase(betonQuestObjectiveStateChangeObjective.getObjectiveFullID())){
                                        activeObjective.addProgress(1);
                                    }
                                }
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
