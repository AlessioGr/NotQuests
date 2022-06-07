/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
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

package rocks.gravili.notquests.spigot.events.hooks;


import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import rocks.gravili.notquests.spigot.NotQuests;
import rocks.gravili.notquests.spigot.structs.ActiveObjective;
import rocks.gravili.notquests.spigot.structs.ActiveQuest;
import rocks.gravili.notquests.spigot.structs.QuestPlayer;
import rocks.gravili.notquests.spigot.structs.objectives.KillMobsObjective;

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
                                    if (killMobsObjective.getMobToKill().equalsIgnoreCase("any") || killMobsObjective.getMobToKill().equals(killedMob.getInternalName())) {
                                        if (event.getEntity() != event.getKiller()) { //Suicide prevention
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

}
