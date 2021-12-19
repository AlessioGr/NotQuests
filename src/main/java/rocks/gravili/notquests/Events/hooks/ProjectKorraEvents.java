package rocks.gravili.notquests.Events.hooks;

import com.projectkorra.projectkorra.event.AbilityStartEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.ActiveObjective;
import rocks.gravili.notquests.Structs.ActiveQuest;
import rocks.gravili.notquests.Structs.Objectives.hooks.ProjectKorra.ProjectKorraUseAbilityObjective;
import rocks.gravili.notquests.Structs.QuestPlayer;

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
