package notquests.notquests.Events.hooks;

import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent;
import io.lumine.xikage.mythicmobs.mobs.MythicMob;
import notquests.notquests.NotQuests;
import notquests.notquests.Structs.ActiveObjective;
import notquests.notquests.Structs.ActiveQuest;
import notquests.notquests.Structs.Objectives.KillMobsObjective;
import notquests.notquests.Structs.QuestPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MythicMobsEvents implements Listener {
    private final NotQuests main;

    public MythicMobsEvents(NotQuests main) {
        this.main = main;
    }

    @EventHandler
    public void onMythicMobDeath(final MythicMobDeathEvent event) {
        //KillMobs objectives
        if (event.getKiller() instanceof Player player) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                if (questPlayer.getActiveQuests().size() > 0) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                            if (activeObjective.getObjective() instanceof KillMobsObjective killMobsObjective) {
                                if (activeObjective.isUnlocked()) {
                                    final MythicMob killedMob = event.getMobType();
                                    if (killMobsObjective.getMobToKill().equals(killedMob.getInternalName())) {
                                        if (event.getEntity() != event.getKiller()) { //Suicide prevention
                                            activeObjective.addProgress(1, -1);
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

}
