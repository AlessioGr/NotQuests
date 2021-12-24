/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021 Alessio Gravili
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

package rocks.gravili.notquests.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.events.notquests.*;
import rocks.gravili.notquests.structs.ActiveObjective;
import rocks.gravili.notquests.structs.ActiveQuest;
import rocks.gravili.notquests.structs.QuestPlayer;
import rocks.gravili.notquests.structs.triggers.ActiveTrigger;

public class TriggerEvents implements Listener {
    private final NotQuests main;

    public TriggerEvents(NotQuests main) {
        this.main = main;
    }


    @EventHandler(priority = EventPriority.MONITOR)
    private void onQuestFinishAccept(QuestFinishAcceptEvent e) {
        if (e.isTriggerAcceptQuestTrigger()) {
            for (final ActiveTrigger activeTrigger : e.getActiveQuest().getActiveTriggers()) {
                if (activeTrigger.getTrigger().getTriggerType().equals("BEGIN")) { //Start the quest
                    if (activeTrigger.getTrigger().getApplyOn() == 0) { //Quest and not objective

                        if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                            activeTrigger.addAndCheckTrigger(e.getActiveQuest());
                        } else {
                            final Player player = e.getQuestPlayer().getPlayer();
                            if (player != null && player.getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
                                activeTrigger.addAndCheckTrigger(e.getActiveQuest());
                            }
                        }
                    }
                }
            }
        }
    }


    @EventHandler(priority = EventPriority.MONITOR)
    private void onQuestComplete(QuestCompletedEvent e) {
        for (final ActiveTrigger activeTrigger : e.getActiveQuest().getActiveTriggers()) {
            if (activeTrigger.getTrigger().getTriggerType().equals("COMPLETE")) { //Complete the quest
                if (activeTrigger.getTrigger().getApplyOn() == 0) { //Quest and not objective

                    if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                        activeTrigger.addAndCheckTrigger(e.getActiveQuest());
                    } else {
                        final Player player = e.getQuestPlayer().getPlayer();
                        if (player != null && player.getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
                            activeTrigger.addAndCheckTrigger(e.getActiveQuest());
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onObjectiveComplete(ObjectiveCompleteEvent e) {

        for (final ActiveTrigger activeTrigger : e.getActiveQuest().getActiveTriggers()) {
            if (activeTrigger.getTrigger().getTriggerType().equals("COMPLETE")) { //Complete the quest
                if (activeTrigger.getTrigger().getApplyOn() >= 1) { //Objective and not Quest
                    if (e.getActiveObjective().getObjectiveID() == activeTrigger.getTrigger().getApplyOn()) {

                        if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                            activeTrigger.addAndCheckTrigger(e.getActiveQuest());
                        } else {
                            final Player player = Bukkit.getPlayer(e.getQuestPlayer().getUUID());
                            if (player != null && player.getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
                                activeTrigger.addAndCheckTrigger(e.getActiveQuest());
                            }
                        }

                    }
                }
            }
        }
    }


    @EventHandler(priority = EventPriority.MONITOR)
    private void onQuestFail(QuestFailEvent e) {
        final QuestPlayer questPlayer = e.getQuestPlayer();
        //final ArrayList<ActiveQuest> activeQuestsCopy = new ArrayList<>(questPlayer.getActiveQuests());

        //So, this is not a for loop anymore but instead it just uses the current activequest. That's because we had this error: "When miner (Gold Madness) & 3rdlife 2, this also fails 3rdlife2 if I fail miner (Gold Madness) for some reason"
        //So, this fixes it. Because the Trigger type FAIL should only apply for the current quest anyways. Other Active Quests dont matter for the FAIL trigger for the current quest
        //TODO: Add an option in the todo trigger creation to make it apply to a different quest (enter different quest name). If not, use current quest.

        // for(final ActiveQuest activeQuest : activeQuestsCopy){
        final ActiveQuest activeQuest = e.getActiveQuest();
        for (final ActiveTrigger activeTrigger : activeQuest.getActiveTriggers()) {
            if (activeTrigger.getTrigger().getTriggerType().equals("FAIL")) {
                if (activeTrigger.getTrigger().getApplyOn() == 0) { //Quest and not Objective
                    if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                        activeTrigger.addAndCheckTrigger(activeQuest);

                    } else {
                        final Player player = Bukkit.getPlayer(questPlayer.getUUID());
                        if (player != null && player.getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
                            activeTrigger.addAndCheckTrigger(activeQuest);
                        }
                    }


                } else if (activeTrigger.getTrigger().getApplyOn() >= 1) { //Objective and not Quest

                    final ActiveObjective activeObjective = activeQuest.getActiveObjectiveFromID(activeTrigger.getTrigger().getApplyOn());
                    if (activeObjective != null && activeObjective.isUnlocked()) {


                        if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                            activeTrigger.addAndCheckTrigger(activeQuest);
                        } else {
                            final Player player = Bukkit.getPlayer(questPlayer.getUUID());
                            if (player != null && player.getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
                                activeTrigger.addAndCheckTrigger(activeQuest);
                            }
                        }

                    }
                }

            }
        }


    }


    @EventHandler(priority = EventPriority.MONITOR)
    private void onObjectiveUnlock(ObjectiveUnlockEvent e) {
        if (e.isTriggerAcceptQuestTrigger()) {
            for (final ActiveTrigger activeTrigger : e.getActiveQuest().getActiveTriggers()) {
                if (activeTrigger.getTrigger().getTriggerType().equals("BEGIN")) { //Start the quest
                    if (activeTrigger.getTrigger().getApplyOn() >= 1) { //Objective and not Quest
                        if (e.getActiveObjective().getObjectiveID() == activeTrigger.getTrigger().getApplyOn()) {
                            if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                                activeTrigger.addAndCheckTrigger(e.getActiveQuest());
                            } else {
                                final Player player = Bukkit.getPlayer(e.getQuestPlayer().getUUID());
                                if (player != null && player.getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
                                    activeTrigger.addAndCheckTrigger(e.getActiveQuest());
                                }
                            }


                        }
                    }
                }
            }
        }
    }


}
