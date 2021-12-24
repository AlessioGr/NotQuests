package rocks.gravili.notquestsspigot.events.hooks;

import com.projectkorra.projectkorra.event.AbilityStartEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import rocks.gravili.notquestsspigot.NotQuests;
import rocks.gravili.notquestsspigot.objectives.hooks.projectkorra.ProjectKorraUseAbilityObjective;
import rocks.gravili.notquestsspigot.structs.ActiveObjective;
import rocks.gravili.notquestsspigot.structs.ActiveQuest;
import rocks.gravili.notquestsspigot.structs.QuestPlayer;

public class ProjectKorraEvents implements Listener {
    private final NotQuests main;

    public ProjectKorraEvents(NotQuests main) {
        this.main = main;
    }

    @EventHandler
    public void onAbilityStart(AbilityStartEvent e) {
        if (!e.isCancelled()) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(e.getAbility().getPlayer().getUniqueId());
            if (questPlayer != null) {
                if (questPlayer.getActiveQuests().size() > 0) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                            if (activeObjective.isUnlocked()) {
                                if (activeObjective.getObjective() instanceof ProjectKorraUseAbilityObjective projectKorraUseAbilityObjective) {
                                    if (!e.getAbility().getName().equalsIgnoreCase(projectKorraUseAbilityObjective.getAbilityName())) {
                                        continue;
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

}
