package rocks.gravili.notquests.Events.hooks;

import com.palmergames.bukkit.towny.event.TownAddResidentEvent;
import com.palmergames.bukkit.towny.event.TownRemoveResidentEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.ActiveObjective;
import rocks.gravili.notquests.Structs.ActiveQuest;
import rocks.gravili.notquests.Structs.Objectives.hooks.TownyReachResidentCountObjective;
import rocks.gravili.notquests.Structs.QuestPlayer;

public class TownyEvents implements Listener {
    private final NotQuests main;

    public TownyEvents(NotQuests main) {
        this.main = main;
    }

    @EventHandler
    public void onResidentAdd(TownAddResidentEvent e) {
        final Player player = e.getResident().getPlayer();
        if(player != null){
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                if (questPlayer.getActiveQuests().size() > 0) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                            if (activeObjective.isUnlocked()) {
                                if (activeObjective.getObjective() instanceof TownyReachResidentCountObjective) {
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
    @EventHandler
    public void onResidentRemove(TownRemoveResidentEvent e) {
        final Player player = e.getResident().getPlayer();
        if(player != null){
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                if (questPlayer.getActiveQuests().size() > 0) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                            if (activeObjective.isUnlocked()) {
                                if (activeObjective.getObjective() instanceof TownyReachResidentCountObjective) {
                                    activeObjective.removeProgress(-1, true);
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
}
