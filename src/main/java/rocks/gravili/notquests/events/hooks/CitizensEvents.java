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

package rocks.gravili.notquests.events.hooks;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.CitizensEnableEvent;
import net.citizensnpcs.api.event.CitizensReloadEvent;
import net.citizensnpcs.api.event.NPCDeathEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.trait.FollowTrait;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.structs.ActiveObjective;
import rocks.gravili.notquests.structs.ActiveQuest;
import rocks.gravili.notquests.structs.QuestPlayer;
import rocks.gravili.notquests.structs.objectives.DeliverItemsObjective;
import rocks.gravili.notquests.structs.objectives.EscortNPCObjective;
import rocks.gravili.notquests.structs.objectives.TalkToNPCObjective;
import rocks.gravili.notquests.structs.triggers.ActiveTrigger;
import rocks.gravili.notquests.structs.triggers.types.NPCDeathTrigger;

import java.util.Locale;

public class CitizensEvents implements Listener {
    private final NotQuests main;

    public CitizensEvents(NotQuests main) {
        this.main = main;
    }


    @EventHandler
    private void onNPCDeathEvent(NPCDeathEvent event) {
        final NPC npc = event.getNPC();

        for (final QuestPlayer questPlayer : main.getQuestPlayerManager().getQuestPlayers()) {
            if (questPlayer.getActiveQuests().size() > 0) {
                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                    for (final ActiveTrigger activeTrigger : activeQuest.getActiveTriggers()) {
                        if (activeTrigger.getTrigger().getTriggerType().equals("NPCDEATH")) {
                            if (((NPCDeathTrigger) activeTrigger.getTrigger()).getNpcToDieID() == npc.getId()) {
                                if (activeTrigger.getTrigger().getApplyOn() == 0) { //Quest and not Objective

                                    if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                                        activeTrigger.addAndCheckTrigger(activeQuest);
                                    } else {
                                        final Player player = Bukkit.getPlayer(questPlayer.getUUID());
                                        if (player != null && player.getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
                                            activeTrigger.addAndCheckTrigger(activeQuest);
                                        }
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
            }
        }
    }

    @EventHandler
    private void onNPCClickEvent(NPCRightClickEvent event) { //Disconnect objectives
        final NPC npc = event.getNPC();
        final Player player = event.getClicker();
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
        if (questPlayer != null) {
            Audience audience = main.adventure().player(player);
            if (questPlayer.getActiveQuests().size() > 0) {
                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                    for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                        if (activeObjective.isUnlocked()) {
                            if (activeObjective.getObjective() instanceof final DeliverItemsObjective deliverItemsObjective) {
                                if (deliverItemsObjective.getRecipientNPCID() == npc.getId()) {
                                    for (final ItemStack itemStack : player.getInventory().getContents()) {
                                        if (itemStack != null) {

                                            if (deliverItemsObjective.getItemToDeliver().getType().equals(itemStack.getType())) {
                                                if (deliverItemsObjective.getItemToDeliver().getItemMeta() != null && !deliverItemsObjective.getItemToDeliver().getItemMeta().equals(itemStack.getItemMeta())) {
                                                    continue;
                                                }

                                                final long progressLeft = activeObjective.getProgressNeeded() - activeObjective.getCurrentProgress();

                                                if (progressLeft == 0) {
                                                    continue;
                                                }

                                                if (progressLeft < itemStack.getAmount()) { //We can finish it with this itemStack
                                                    itemStack.setAmount((itemStack.getAmount() - (int) progressLeft));
                                                    activeObjective.addProgress(progressLeft, npc.getId());
                                                    audience.sendMessage(MiniMessage.miniMessage().parse(
                                                            "<GREEN>You have delivered <AQUA>" + progressLeft + "</AQUA> items to <AQUA>" + npc.getName()
                                                    ));
                                                    break;
                                                } else {
                                                    player.getInventory().removeItem(itemStack);
                                                    activeObjective.addProgress(itemStack.getAmount(), npc.getId());
                                                    audience.sendMessage(MiniMessage.miniMessage().parse(
                                                            "<GREEN>You have delivered <AQUA>" + itemStack.getAmount() + "</AQUA> items to <AQUA>" + npc.getName()
                                                    ));
                                                }
                                            }
                                        }

                                    }

                                }
                            } else if (activeObjective.getObjective() instanceof final TalkToNPCObjective talkToNPCObjective) {
                                if (talkToNPCObjective.getNPCtoTalkID() != -1 && talkToNPCObjective.getNPCtoTalkID() == npc.getId()) {
                                    activeObjective.addProgress(1, npc.getId());
                                    audience.sendMessage(MiniMessage.miniMessage().parse(
                                            "<GREEN>You talked to <AQUA>" + npc.getName()
                                    ));
                                }
                            } else if (activeObjective.getObjective() instanceof final EscortNPCObjective escortNPCObjective) {
                                if (escortNPCObjective.getNpcToEscortToID() == npc.getId()) {
                                    final NPC npcToEscort = CitizensAPI.getNPCRegistry().getById(escortNPCObjective.getNpcToEscortID());
                                    if (npcToEscort != null) {
                                        if (npcToEscort.isSpawned() && (npcToEscort.getEntity().getLocation().distance(player.getLocation()) < 6)) {
                                            activeObjective.addProgress(1, npc.getId());
                                            audience.sendMessage(MiniMessage.miniMessage().parse(
                                                    "<GREEN>You have successfully delivered the NPC <AQUA>" + npcToEscort.getName()
                                            ));

                                            FollowTrait followerTrait = null;
                                            for (final Trait trait : npcToEscort.getTraits()) {
                                                if (trait.getName().toLowerCase(Locale.ROOT).contains("follow")) {
                                                    followerTrait = (FollowTrait) trait;
                                                }
                                            }
                                            if (followerTrait != null) {
                                                npc.removeTrait(followerTrait.getClass());
                                            }

                                            npcToEscort.despawn();
                                        } else {
                                            audience.sendMessage(MiniMessage.miniMessage().parse(
                                                    "<RED>The NPC you have to escort is not close enough to you!"
                                            ));
                                        }
                                    }


                                }
                            }
                            //Eventually trigger CompletionNPC Objective Completion if the objective is not set to complete automatically (so, if getCompletionNPCID() is not -1)
                            if (activeObjective.getObjective().getCompletionNPCID() != -1) {
                                activeObjective.addProgress(0, npc.getId());
                            }
                        }

                    }
                    activeQuest.removeCompletedObjectives(true);
                }
                questPlayer.removeCompletedQuests();
            }
        }

    }

    @EventHandler
    private void onCitizensEnable(CitizensEnableEvent e) {
        main.getLogManager().info("Processing Citizens Enable Event...");
        main.getIntegrationsManager().getCitizensManager().registerQuestGiverTrait();


    }

    @EventHandler
    private void onCitizensReload(CitizensReloadEvent e) {
        main.getLogManager().info("Processing Citizens Reload Event...");

        main.getIntegrationsManager().getCitizensManager().registerQuestGiverTrait();

    }
}
