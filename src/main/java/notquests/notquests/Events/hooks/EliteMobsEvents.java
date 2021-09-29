package notquests.notquests.Events.hooks;

import com.magmaguy.elitemobs.api.EliteMobDeathEvent;
import com.magmaguy.elitemobs.mobconstructor.EliteEntity;
import notquests.notquests.NotQuests;
import notquests.notquests.Structs.ActiveObjective;
import notquests.notquests.Structs.ActiveQuest;
import notquests.notquests.Structs.Objectives.hooks.KillEliteMobsObjective;
import notquests.notquests.Structs.QuestPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Locale;

public class EliteMobsEvents implements Listener {
    private final NotQuests main;

    public EliteMobsEvents(NotQuests main) {
        this.main = main;
    }

    @EventHandler
    public void onEliteMobDeath(EliteMobDeathEvent event) {
        final EliteEntity eliteMob = event.getEliteEntity();

        for (final Player player : eliteMob.getDamagers().keySet()) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                if (questPlayer.getActiveQuests().size() > 0) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                            if (activeObjective.getObjective() instanceof KillEliteMobsObjective killEliteMobsObjective) {
                                if (activeObjective.isUnlocked()) {

                                    //Check conditions

                                    if (!killEliteMobsObjective.getEliteMobToKillContainsName().isBlank() && !eliteMob.getName().toLowerCase(Locale.ROOT).contains(killEliteMobsObjective.getEliteMobToKillContainsName().toLowerCase(Locale.ROOT))) {
                                        continue;
                                    }

                                    if (killEliteMobsObjective.getMinimumLevel() >= 0 && eliteMob.getLevel() < killEliteMobsObjective.getMinimumLevel()) {
                                        continue;
                                    }

                                    if (killEliteMobsObjective.getMaximumLevel() >= 0 && eliteMob.getLevel() > killEliteMobsObjective.getMaximumLevel()) {
                                        continue;
                                    }

                                    if (eliteMob.getDamagers().get(player) < killEliteMobsObjective.getMinimumDamagePercentage()) {
                                        continue;
                                    }

                                    if (!killEliteMobsObjective.getSpawnReason().isBlank() && !eliteMob.getSpawnReason().toString().toLowerCase(Locale.ROOT).equalsIgnoreCase(killEliteMobsObjective.getSpawnReason().toLowerCase(Locale.ROOT))) {
                                        continue;
                                    }

                                    activeObjective.addProgress(1, -1);

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
