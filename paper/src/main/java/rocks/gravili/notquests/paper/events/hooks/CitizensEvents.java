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

package rocks.gravili.notquests.paper.events.hooks;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.CitizensEnableEvent;
import net.citizensnpcs.api.event.CitizensReloadEvent;
import net.citizensnpcs.api.event.NPCDeathEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.trait.FollowTrait;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.conversation.Conversation;
import rocks.gravili.notquests.paper.conversation.ConversationManager;
import rocks.gravili.notquests.paper.managers.npc.ConversationFocus;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.managers.npc.NQNPCID;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.DeliverItemsObjective;
import rocks.gravili.notquests.paper.structs.objectives.TalkToNPCObjective;
import rocks.gravili.notquests.paper.structs.objectives.hooks.citizens.EscortNPCObjective;
import rocks.gravili.notquests.paper.structs.triggers.ActiveTrigger;
import rocks.gravili.notquests.paper.structs.triggers.types.NPCDeathTrigger;

public class CitizensEvents implements Listener {
    private final NotQuests main;

    public CitizensEvents(final NotQuests main) {
        this.main = main;
        main.getLogManager().info("Initialized CitizensEvents");
    }


    @EventHandler
    private void onNPCDeathEvent(NPCDeathEvent event) {
        final NPC npc = event.getNPC();

        for (final QuestPlayer questPlayer : main.getQuestPlayerManager().getActiveQuestPlayers()) {
            if (!questPlayer.getActiveQuests().isEmpty()) {
                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                    for (final ActiveTrigger activeTrigger : activeQuest.getActiveTriggers()) {
                        if (activeTrigger.getTrigger().getTriggerType().equals("NPCDEATH")) {
                            if (((NPCDeathTrigger) activeTrigger.getTrigger()).getNpcToDieID() == npc.getId()) {
                                if (activeTrigger.getTrigger().getApplyOn() == 0) { //Quest and not Objective

                                    if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                                        activeTrigger.addAndCheckTrigger(activeQuest);
                                    } else {
                                        final Player player = Bukkit.getPlayer(questPlayer.getUniqueId());
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
                                        final Player player = Bukkit.getPlayer(questPlayer.getUniqueId());
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

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onNPCClickEvent(NPCRightClickEvent event) { //Disconnect objectives
        final NPC npc = event.getNPC();
        final NQNPC nqNPC = main.getNPCManager().getOrCreateNQNpc("Citizens", NQNPCID.fromInteger(npc.getId()));
        final Player player = event.getClicker();
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());

        if(nqNPC == null){
            questPlayer.sendDebugMessage("Error: NQNpc is null");
            return;
        }


        questPlayer.sendDebugMessage("Clicked on Citizens NPC!");
        //Handle special items first
        final ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (player.hasPermission("notquests.admin.armorstandeditingitems") && heldItem.getType() != Material.AIR && heldItem.getItemMeta() != null) {
            final PersistentDataContainer container = heldItem.getItemMeta().getPersistentDataContainer();

            final NamespacedKey specialActionItemKey = new NamespacedKey(main.getMain(), "notquests-nqnpc-selector-with-action");

            if (container.has(specialActionItemKey, PersistentDataType.INTEGER)) {
                int id = container.get(specialActionItemKey, PersistentDataType.INTEGER); //Not null, because we check for it in container.has()

                main.getNPCManager().executeNPCSelectionAction(nqNPC, id);
            }
        }




        final AtomicBoolean handledObjective = new AtomicBoolean(false);
        questPlayer.sendDebugMessage("Right-clicked NPC event: " + npc.getId() + "." );

        questPlayer.queueObjectiveCheck(activeObjective -> {
            if (activeObjective.getObjective() instanceof final DeliverItemsObjective deliverItemsObjective) {
                if (nqNPC.equals(deliverItemsObjective.getRecipientNPC())) {
                    for (final ItemStack itemStack : player.getInventory().getContents()) {
                        if (itemStack != null) {
                            if(!deliverItemsObjective.getItemStackSelection().checkIfIsIncluded(itemStack)){
                                continue;
                            }

                            final double progressLeft = activeObjective.getProgressNeeded() - activeObjective.getCurrentProgress();

                            if (progressLeft == 0) {
                                continue;
                            }

                            handledObjective.set(true);

                            final String mmNpcName = main.getMiniMessage().serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(npc.getName().replace("§","&")));

                            if (progressLeft < itemStack.getAmount()) { //We can finish it with this itemStack
                                itemStack.setAmount((itemStack.getAmount() - (int) progressLeft));
                                activeObjective.addProgress(progressLeft, nqNPC);



                                player.sendMessage(main.parse(
                                    "<GREEN>You have delivered <highlight>" + progressLeft + "</highlight> items to <highlight>" + mmNpcName
                                ));
                                break;
                            } else {
                                questPlayer.sendDebugMessage("Calling player.getInventory().removeItemAnySlot with amount " + itemStack.getAmount() + "...");
                                player.getInventory().removeItemAnySlot(itemStack);
                                activeObjective.addProgress(itemStack.getAmount(), nqNPC);
                                player.sendMessage(main.parse(
                                    "<GREEN>You have delivered <highlight>" + itemStack.getAmount() + "</highlight> items to <highlight>" + mmNpcName
                                ));
                            }
                        }

                    }
                    player.updateInventory();
                }
            }
        });
        questPlayer.queueObjectiveCheck(activeObjective -> {
            if (activeObjective.getObjective() instanceof final TalkToNPCObjective talkToNPCObjective) {
                if (nqNPC.equals(talkToNPCObjective.getNPCtoTalkTo())) {
                    activeObjective.addProgress(1, nqNPC);
                    final String mmNpcName = main.getMiniMessage().serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(npc.getName().replace("§","&")));

                    player.sendMessage(main.parse(
                        "<GREEN>You talked to <highlight>" +mmNpcName
                    ));
                    handledObjective.set(true);
                }
            }
        });
        questPlayer.queueObjectiveCheck(activeObjective -> {
            if (activeObjective.getObjective() instanceof final EscortNPCObjective escortNPCObjective) {
                if (escortNPCObjective.getNpcToEscortToID() == npc.getId()) {
                    final NPC npcToEscort = CitizensAPI.getNPCRegistry().getById(escortNPCObjective.getNpcToEscortID());
                    if (npcToEscort != null) {
                        if (npcToEscort.isSpawned() && (npcToEscort.getEntity().getLocation().distance(player.getLocation()) < 6)) {
                            activeObjective.addProgress(1, nqNPC);
                            final String mmNpcName = main.getMiniMessage().serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(npcToEscort.getName()));

                            player.sendMessage(main.parse(
                                "<GREEN>You have successfully delivered the NPC <highlight>" + mmNpcName
                            ));
                            handledObjective.set(true);
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
                            player.sendMessage(main.parse(
                                "<RED>The NPC you have to escort is not close enough to you!"
                            ));
                        }
                    }


                }
            }
        });
        questPlayer.queueObjectiveCheck(activeObjective -> {
            //Eventually trigger CompletionNPC Objective Completion if the objective is not set to complete automatically (so, if getCompletionNPCID() is not -1)
            if (activeObjective.getObjective().getCompletionNPC() != null) {
                activeObjective.addProgress(0, nqNPC);
            }
        });
        questPlayer.checkQueuedObjectives();


        //Return if another action already happened
        if (handledObjective.get()) {
            questPlayer.sendDebugMessage("Returning because of handled objective");
            return;
        }

        //Quest Preview
        main.getQuestManager().sendQuestsPreviewOfQuestShownNPCs(nqNPC, questPlayer);

        //Conversations
        ConversationManager manager = main.getConversationManager();
        if(manager != null){
            final Conversation foundConversation = main.getConversationManager().getConversationForNPC(nqNPC);
            if (foundConversation != null) {
                // Cancel NPC's movement
                npc.getNavigator().cancelNavigation();
                npc.getNavigator().setPaused(true);
                manager.getActiveConversationsOfNPCWithPlayerCache().putIfAbsent(npc.getId(), new ArrayList<>());
                manager.getActiveConversationsOfNPCWithPlayerCache().get(npc.getId()).add(player.getUniqueId());
                new BukkitRunnable(){
                    public void run() {
                        if (!manager.getActiveConversationsOfNPCWithPlayerCache().containsKey(nqNPC.getID().getIntegerID())) {
                            npc.getNavigator().setPaused(false);
                            this.cancel();
                        }
                    }
                }.runTaskTimer(this.main.getMain(), 0L, 30L);
                // Try to cancel player's movement
                player.getLocation().setDirection(new Vector(0, 0, 0));
                player.setVelocity(new Vector(0, 0, 0));
                manager.playConversation(questPlayer, foundConversation, nqNPC);
                if (main.getDataManager().getConfiguration().isCitizensFocusingEnabled())
                    new ConversationFocus(main, player, npc.getEntity(), foundConversation).runTaskTimer(main.getMain(), 0, 2);
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
