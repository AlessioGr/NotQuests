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

import com.projectkorra.projectkorra.event.AbilityStartEvent;
import com.projectkorra.projectkorra.event.EntityBendingDeathEvent;
import java.util.Locale;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.KillMobsObjective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.projectkorra.ProjectKorraUseAbilityObjective;

public class ProjectKorraEvents implements Listener {
    private final NotQuests main;

    public ProjectKorraEvents(NotQuests main) {
        this.main = main;
    }

    @EventHandler
    public void onAbilityStart(AbilityStartEvent e) {
        if (!e.isCancelled()) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(e.getAbility().getPlayer().getUniqueId());
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

    @EventHandler
    public void onEntityKilled(EntityBendingDeathEvent e) {
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(e.getAttacker().getUniqueId());
        if (questPlayer != null) {
            if (questPlayer.getActiveQuests().size() > 0) {
                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                    for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                        if (activeObjective.getObjective() instanceof KillMobsObjective killMobsObjective) {
                            if (activeObjective.isUnlocked()) {
                                if(main.getIntegrationsManager().isProjectKorraEnabled() && killMobsObjective.getProjectKorraAbility().isBlank()){
                                    continue;
                                }
                                if(!killMobsObjective.getProjectKorraAbility().equalsIgnoreCase("any") && !killMobsObjective.getProjectKorraAbility().equalsIgnoreCase(e.getAbility().getName()) ){
                                    continue;
                                }
                                final EntityType killedMob = e.getEntity().getType();
                                if (killMobsObjective.getMobToKill().equalsIgnoreCase("any") || killMobsObjective.getMobToKill().equalsIgnoreCase(killedMob.toString())) {
                                    if (e.getEntity() != e.getAttacker()) { //Suicide prevention

                                        //Extra Flags
                                        if (!killMobsObjective.getNameTagContainsAny().isBlank()) {
                                            if (e.getEntity().getCustomName() == null || e.getEntity().getCustomName().isBlank()) {
                                                continue;
                                            }
                                            boolean foundOneNotFitting = false;
                                            for (final String namePart : killMobsObjective.getNameTagContainsAny().toLowerCase(Locale.ROOT).split(" ")) {
                                                if (!e.getEntity().getCustomName().toLowerCase(Locale.ROOT).contains(namePart)) {
                                                    foundOneNotFitting = true;
                                                }
                                            }
                                            if (foundOneNotFitting) {
                                                continue;
                                            }
                                        }
                                        if (!killMobsObjective.getNameTagEquals().isBlank()) {
                                            if (e.getEntity().getCustomName() == null || e.getEntity().getCustomName().isBlank() || !e.getEntity().getCustomName().equalsIgnoreCase(killMobsObjective.getNameTagEquals())) {
                                                continue;
                                            }
                                        }

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
