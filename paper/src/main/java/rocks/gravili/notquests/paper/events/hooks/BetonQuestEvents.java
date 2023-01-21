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

import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.api.ConversationOptionEvent;
import org.betonquest.betonquest.api.PlayerObjectiveChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.hooks.betonquest.BetonQuestObjectiveStateChangeObjective;

public class BetonQuestEvents implements Listener {
    private final NotQuests main;

    public BetonQuestEvents(NotQuests main) {
        this.main = main;
    }

    @EventHandler
    public void onBetonQuestObjectiveStateChange(final PlayerObjectiveChangeEvent e) {
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getActiveQuestPlayer(e.getProfile().getProfileUUID());
        if (questPlayer == null || questPlayer.getActiveQuests().isEmpty()) {
            return;
        }
        questPlayer.queueObjectiveCheck(activeObjective -> {
            if (activeObjective.getObjective() instanceof final BetonQuestObjectiveStateChangeObjective betonQuestObjectiveStateChangeObjective) {
                if (activeObjective.isUnlocked()) {
                    if(e.getState() == betonQuestObjectiveStateChangeObjective.getObjectiveState()){
                        if(e.getObjectiveID().getFullID().equalsIgnoreCase(betonQuestObjectiveStateChangeObjective.getObjectiveFullID())){
                            activeObjective.addProgress(1);
                        }
                    }
                }

            }
        });
        questPlayer.checkQueuedObjectives();
    }


    @EventHandler
    public void onBetonQuestObjectiveStateChange(final ConversationOptionEvent e) {
        final BetonQuest betonQuest = main.getIntegrationsManager().getBetonQuestManager().getBetonQuest();

        if(e.getConversation().getInterceptor() != null && e.getConversation().getInterceptor().getClass() == betonQuest.getInterceptor("notquests") && main.getConversationManager() != null){
            if(e.getProfile().getPlayer().isOnline()){
                main.getConversationManager().removeOldMessages(
                        e.getProfile().getPlayer().getPlayer()
                );

            }
        }
    }



}
