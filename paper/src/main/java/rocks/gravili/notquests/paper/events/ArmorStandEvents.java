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

package rocks.gravili.notquests.paper.events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import rocks.gravili.notquests.paper.conversation.Conversation;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.DeliverItemsObjective;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.TalkToNPCObjective;

public class ArmorStandEvents implements Listener {
    private final NotQuests main;

    public ArmorStandEvents(final NotQuests main){
        this.main = main;
    }

    @EventHandler
    private void onArmorStandClick(PlayerInteractAtEntityEvent event){

        final Player player = event.getPlayer();
        if(event.getRightClicked().getType() == EntityType.ARMOR_STAND) {

            final ArmorStand armorStand = (ArmorStand) event.getRightClicked();
            final ItemStack heldItem = event.getPlayer().getInventory().getItemInMainHand();

            if (player.hasPermission("notquests.admin.armorstandeditingitems") && heldItem.getType() != Material.AIR && heldItem.getItemMeta() != null) {
                final PersistentDataContainer container = heldItem.getItemMeta().getPersistentDataContainer();

                final NamespacedKey specialItemKey = new NamespacedKey(main.getMain(), "notquests-item");


                if (container.has(specialItemKey, PersistentDataType.INTEGER)) {

                    int id = container.get(specialItemKey, PersistentDataType.INTEGER); //Not null, because we check for it in container.has()

                    final NamespacedKey questsKey = new NamespacedKey(main.getMain(), "notquests-questname");
                    final String questName = container.get(questsKey, PersistentDataType.STRING);

                    final NamespacedKey objectiveIDKey = new NamespacedKey(main.getMain(), "notquests-objectiveid");
                    int objectiveID = -1;
                    if (container.has(objectiveIDKey, PersistentDataType.INTEGER)) {
                        objectiveID = container.get(objectiveIDKey, PersistentDataType.INTEGER);
                    }

                    if (questName == null && ((id >= 0 && id <= 3) || id == 5 || id == 6 || id == 7)) {
                        player.sendMessage(main.parse(
                                "<RED>Error: Your item has no valid quest attached to it."
                        ));
                        return;
                    }

                    if (id == 0 || id == 1) { //Add
                        PersistentDataContainer armorStandPDB = armorStand.getPersistentDataContainer();
                        final NamespacedKey attachedQuestsKey;
                        if (id == 0) { //showing
                            attachedQuestsKey = main.getArmorStandManager().getAttachedQuestsShowingKey();
                        } else {
                            attachedQuestsKey = main.getArmorStandManager().getAttachedQuestsNonShowingKey();
                        }
                        if (armorStandPDB.has(attachedQuestsKey, PersistentDataType.STRING)) {
                            String existingAttachedQuests = armorStandPDB.get(attachedQuestsKey, PersistentDataType.STRING);


                            if (existingAttachedQuests != null) {
                                // boolean condition2 =  (existingAttachedQuests.contains("°"+questName) && (   ( (existingAttachedQuests.indexOf("°" + questName)-1) + ("°"+questName).length())  == existingAttachedQuests.length()-1  )  ) ;
                                //boolean condition3 =  (existingAttachedQuests.contains(questName + "°") &&  existingAttachedQuests.indexOf(questName+"°")  == 0 )   ;

                                //player.sendMessage("" + ( (existingAttachedQuests.indexOf("°" + questName)-1) + ("°"+questName).length()));
                                //player.sendMessage("" + (existingAttachedQuests.length()-1) );


                                if( existingAttachedQuests.equals(questName) || existingAttachedQuests.contains("°" + questName+"°") ) {
                                    player.sendMessage(main.parse(
                                            "<RED>Error: That armor stand already has the Quest <highlight>" + questName + "</highlight> attached to it!\n"
                                                    + "<RED>Attached Quests: <highlight>" + existingAttachedQuests
                                    ));
                                    return;
                                }

                            }

                            if(existingAttachedQuests != null && existingAttachedQuests.length() >= 1){
                                if(   existingAttachedQuests.charAt(existingAttachedQuests.length()-1) == '°' ){
                                    existingAttachedQuests += (questName+"°") ;
                                } else {
                                    existingAttachedQuests += "°" + questName + "°";
                                }

                            } else {
                                existingAttachedQuests += "°" + questName + "°";
                            }

                            armorStandPDB.set(attachedQuestsKey, PersistentDataType.STRING, existingAttachedQuests);

                            player.sendMessage(main.parse(
                                    "<GREEN>Quest with the name <highlight>" + questName + "</highlight> was added to this poor little armorstand!"
                                            + " <DARK_GREEN>Attached Quests: <highlight>" + existingAttachedQuests
                            ));

                        }else {
                            armorStandPDB.set(attachedQuestsKey, PersistentDataType.STRING, "°" + questName + "°");
                            player.sendMessage(main.parse(
                                    "<GREEN>Quest with the name <highlight>" + questName + "</highlight> was added to this poor little armorstand!"
                                            + " <DARK_GREEN>Attached Quests: <highlight>" + "°" + questName + "°"
                            ));

                            //Since this is the first Quest added to it:
                            main.getArmorStandManager().addArmorStandWithQuestsOrConversationAttachedToThem(armorStand);

                        }


                    }else if(id == 2 || id == 3){ //Remove
                        PersistentDataContainer armorstandPDB = armorStand.getPersistentDataContainer();

                        final NamespacedKey attachedQuestsKey;

                        if(id == 2){ //showing
                            attachedQuestsKey  = main.getArmorStandManager().getAttachedQuestsShowingKey();
                        }else{
                            attachedQuestsKey  = main.getArmorStandManager().getAttachedQuestsNonShowingKey();
                        }

                        if(armorstandPDB.has(attachedQuestsKey, PersistentDataType.STRING)){
                            String existingAttachedQuests = armorstandPDB.get(attachedQuestsKey, PersistentDataType.STRING);
                            if(existingAttachedQuests != null && existingAttachedQuests.contains("°"+questName+"°")){


                                existingAttachedQuests = existingAttachedQuests.replace("°" + questName + "°", "°");

                                //So it can go fully empty again
                                boolean foundNonSeparator = false;
                                for (int i = 0; i < existingAttachedQuests.length(); i++){
                                    char c = existingAttachedQuests.charAt(i);
                                    if (c != '°') {
                                        foundNonSeparator = true;
                                        break;
                                    }
                                }

                                if (!foundNonSeparator) {
                                    //It consists only of separators - no quests. Thus, we set this to "" and remove the PDB
                                    existingAttachedQuests = "";
                                    armorstandPDB.remove(attachedQuestsKey);
                                    main.getArmorStandManager().removeArmorStandWithQuestsOrConversationAttachedToThem(armorStand);
                                } else {
                                    armorstandPDB.set(attachedQuestsKey, PersistentDataType.STRING, existingAttachedQuests);
                                }

                                player.sendMessage(main.parse(
                                        "<DARK_GREEN>Quest with the name <highlight>" + questName + "</highlight> was removed from this armor stand!\n" +
                                                "<DARK_GREEN>Attached Quests: <highlight>" + existingAttachedQuests
                                ));

                            } else {
                                player.sendMessage(main.parse(
                                        "<RED>Error: That armor stand does not have the Quest <highlight>" + questName + "</highlight> attached to it!\n" +
                                                "<DARK_GREEN>Attached Quests: <highlight>" + existingAttachedQuests
                                ));
                            }
                        } else {
                            player.sendMessage(main.parse(
                                    "<RED>This armor stand has no quests attached to it!"
                            ));
                        }


                    } else if (id == 4) { //Check
                        player.sendMessage(main.parse(
                                "<GRAY>Armor Stand Entity ID: <WHITE>" + armorStand.getUniqueId()
                        ));


                        //Get all Quests attached to this armor stand:
                        PersistentDataContainer armorstandPDB = armorStand.getPersistentDataContainer();

                        final ArrayList<String> showingQuests = new ArrayList<>();
                        final ArrayList<String> nonShowingQuests = new ArrayList<>();

                        boolean hasShowingQuestsPDBKey = false;
                        boolean hasNonShowingQuestsPDBKey = false;


                        if(armorstandPDB.has(main.getArmorStandManager().getAttachedQuestsShowingKey(), PersistentDataType.STRING)){
                            String existingAttachedQuests = armorstandPDB.get(main.getArmorStandManager().getAttachedQuestsShowingKey(), PersistentDataType.STRING);
                            hasShowingQuestsPDBKey = true;
                            if(existingAttachedQuests != null && existingAttachedQuests.length() >= 1){
                                showingQuests.addAll(Arrays.asList(existingAttachedQuests.split("°")));
                            }
                        }
                        if(armorstandPDB.has(main.getArmorStandManager().getAttachedQuestsNonShowingKey(), PersistentDataType.STRING)){
                            String existingAttachedQuests = armorstandPDB.get(main.getArmorStandManager().getAttachedQuestsNonShowingKey(), PersistentDataType.STRING);
                            hasNonShowingQuestsPDBKey = true;
                            if(existingAttachedQuests != null && existingAttachedQuests.length() >= 1){
                                nonShowingQuests.addAll(Arrays.asList(existingAttachedQuests.split("°")));
                            }
                        }

                        if(showingQuests.size() == 0){
                            if(hasShowingQuestsPDBKey) {
                                player.sendMessage(main.parse(
                                        "<BLUE>All attached showing Quests: <GRAY>Empty"
                                ));
                            }else {
                                player.sendMessage(main.parse(
                                        "<BLUE>All attached showing Quests: <GRAY>None"
                                ));
                            }
                        }else {
                            player.sendMessage(main.parse(
                                    "<BLUE>All " + showingQuests.size() + " attached showing Quests:"
                            ));
                            int counter = 0;
                            for (final String questNameInList : showingQuests) {
                                if (!questNameInList.isBlank()) { //empty or null or only whitespaces
                                    counter++;
                                    player.sendMessage(main.parse(
                                            "<GRAY>" + counter + ". <YELLOW>" + questNameInList
                                    ));
                                }

                            }
                        }

                        if(nonShowingQuests.size() == 0){
                            if(hasNonShowingQuestsPDBKey) {
                                player.sendMessage(main.parse(
                                        "<BLUE>All attached non-showing Quests: <GRAY>Empty"
                                ));
                            }else {
                                player.sendMessage(main.parse(
                                        "<BLUE>All attached non-showing Quests: <GRAY>None"
                                ));
                            }

                        }else {
                            player.sendMessage(main.parse(
                                    "<BLUE>All " + nonShowingQuests.size() + " attached non-showing Quests:"
                            ));
                            int counter = 0;
                            for (final String questNameInList : nonShowingQuests) {
                                if (!questNameInList.isBlank()) { //empty or null or only whitespaces
                                    counter++;
                                    player.sendMessage(main.parse(
                                            "<GRAY>" + counter + ". <YELLOW>" + questNameInList
                                    ));
                                }

                            }
                        }

                    } else if (id == 5) { //Add Objective TalkToNPC

                        final Quest quest = main.getQuestManager().getQuest(questName);
                        if (quest != null) {
                            TalkToNPCObjective talkToNPCObjective = new TalkToNPCObjective(main);
                            talkToNPCObjective.setQuest(quest);
                            talkToNPCObjective.setObjectiveID(quest.getFreeObjectiveID());
                            talkToNPCObjective.setArmorStandUUID(armorStand.getUniqueId());
                            quest.addObjective(talkToNPCObjective, true);
                            player.sendMessage(main.parse(
                                    "<GREEN>Objective successfully added to quest <highlight>" + quest.getQuestName() + "</highlight>!"
                            ));

                        } else {
                            player.sendMessage(main.parse(
                                    "<error>Error: Quest <highlight>" + questName + "</highlight> does not exist."
                            ));
                        }

                    } else if (id == 6) { //Set as completionNPC to an objective of a Quest
                        final Quest quest = main.getQuestManager().getQuest(questName);
                        if (quest != null) {
                            final Objective objective = quest.getObjectiveFromID(objectiveID);
                            if (objective != null) {
                                objective.setCompletionArmorStandUUID(armorStand.getUniqueId(), true);
                                player.sendMessage(main.parse(
                                        "<success>The completionArmorStandUUID of the objective with the ID <highlight>" + objectiveID + "</highlight> has been set to the Armor Stand with the UUID <highlight2>" + armorStand.getUniqueId() + "</highlight2> and name <highlight2>" + main.getArmorStandManager().getArmorStandName(armorStand) + "</highlight2>!"
                                ));
                            } else {
                                player.sendMessage(main.parse(
                                        "<error>Error: Objective with the ID <highlight>"+ objectiveID + "</highlight> was not found for quest <highlight2>" + quest.getQuestName() + "</highlight2>!"
                                ));
                            }
                        } else {
                            player.sendMessage(main.parse(
                                    "<error>Error: Quest <highlight>" + questName + "</highlight> does not exist."
                            ));
                        }


                    } else if (id == 7) { //Add Objective DeliverItems


                        final Quest quest = main.getQuestManager().getQuest(questName);
                        if (quest != null) {
                            final NamespacedKey amountToDeliverKey = new NamespacedKey(main.getMain(), "notquests-itemstackamount");
                            final String amountToDeliverExpression = container.get(amountToDeliverKey, PersistentDataType.STRING);

                            final NamespacedKey itemStackCacheKey = new NamespacedKey(main.getMain(), "notquests-itemstackcache");
                            final int itemStackCache = container.get(itemStackCacheKey, PersistentDataType.INTEGER);


                            final ItemStackSelection itemToDeliverSelection = main.getDataManager().getItemStackSelectionCache().get(itemStackCache);

                            if (itemToDeliverSelection != null) {
                                DeliverItemsObjective deliverItemsObjective = new DeliverItemsObjective(main);
                                deliverItemsObjective.setItemStackSelection(itemToDeliverSelection);

                                deliverItemsObjective.setProgressNeededExpression(amountToDeliverExpression);
                                deliverItemsObjective.setRecipientArmorStandUUID(armorStand.getUniqueId());
                                deliverItemsObjective.setQuest(quest);
                                deliverItemsObjective.setObjectiveID(quest.getFreeObjectiveID());

                                quest.addObjective(deliverItemsObjective, true);

                                player.sendMessage(main.parse(
                                        "<success>DeliverItems Objective successfully added to Quest <highlight>"
                                                + quest.getQuestName() + "</highlight>!"
                                ));

                            } else {
                                player.sendMessage(main.parse(
                                        "<error>ItemStack is not cached anymore! This item won't work after a server restart."
                                ));
                            }


                        } else {
                            player.sendMessage(main.parse(
                                    "<error>Error: Quest <highlight>" + questName + "</highlight> does not exist."
                            ));
                        }

                    } else if (id == 8) { //Add conversation to armorstand


                        NamespacedKey conversationIdentifierKey = new NamespacedKey(main.getMain(), "notquests-conversation");

                        final String conversationIdentifier = container.get(conversationIdentifierKey, PersistentDataType.STRING);
                        if (conversationIdentifier != null && !conversationIdentifier.isBlank()) {
                            final Conversation conversation = main.getConversationManager().getConversation(conversationIdentifier);
                            if (conversation != null) {


                                //Add conversation to armorStandPDB

                                PersistentDataContainer armorStandPDB = armorStand.getPersistentDataContainer();
                                final NamespacedKey attachedConversationKey = main.getArmorStandManager().getAttachedConversationKey();

                                if (armorStandPDB.has(attachedConversationKey, PersistentDataType.STRING)) {
                                    String existingAttachedConversation = armorStandPDB.get(attachedConversationKey, PersistentDataType.STRING);


                                    if (existingAttachedConversation != null) {
                                        player.sendMessage(main.parse(
                                                "<RED>Error: That armor stand already has the Conversation <highlight>" + existingAttachedConversation + "</highlight> attached to it!"
                                        ));
                                    }

                                } else {
                                    armorStandPDB.set(attachedConversationKey, PersistentDataType.STRING, conversation.getIdentifier());
                                    player.sendMessage(main.parse(
                                            "<GREEN>Conversation with the name <highlight>" + conversation.getIdentifier() + "</highlight> was added to this poor little armorstand!"
                                    ));

                                    //Since this is the first Quest added to it:
                                    main.getArmorStandManager().addArmorStandWithQuestsOrConversationAttachedToThem(armorStand);

                                }


                            } else {
                                player.sendMessage(main.parse(
                                        "<error>Error: Conversation <highlight>" + conversationIdentifier + "</highlight> does not exist."
                                ));
                            }


                        } else {
                            player.sendMessage(main.parse(
                                    "<error>Error: this item has no valid conversation."
                            ));
                        }


                    } else if (id == 9) { //Remove conversation from armorstand

                        PersistentDataContainer armorStandPDB = armorStand.getPersistentDataContainer();
                        final NamespacedKey attachedConversationKey = main.getArmorStandManager().getAttachedConversationKey();

                        if (armorStandPDB.has(attachedConversationKey, PersistentDataType.STRING)) {
                            armorStandPDB.remove(attachedConversationKey);
                            main.getArmorStandManager().removeArmorStandWithQuestsOrConversationAttachedToThem(armorStand);
                            player.sendMessage(main.parse(
                                    "<GREEN>All conversations were removed from this armorStand!"
                            ));

                        } else {
                            player.sendMessage(main.parse(
                                    "<RED>This armorstand doesn't have the conversation attached to it."
                            ));
                        }


                    }
                }else {
                    showQuestOrHandleObjectivesOfArmorStands(player, armorStand, event);

                }
            } else {
                showQuestOrHandleObjectivesOfArmorStands(player, armorStand, event);
            }


        }
    }


    public void showQuestOrHandleObjectivesOfArmorStands(final Player player, final ArmorStand armorStand, final PlayerInteractAtEntityEvent event) {
        //Handle Objectives
        final AtomicBoolean handledObjective = new AtomicBoolean(false);

        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());

        if (questPlayer != null) {
            questPlayer.queueObjectiveCheck(activeObjective -> {
                if (activeObjective.getObjective() instanceof final DeliverItemsObjective deliverItemsObjective) {
                    if (deliverItemsObjective.getRecipientNPCID() == -1 && deliverItemsObjective.getRecipientArmorStandUUID().equals(armorStand.getUniqueId())) {
                        for (final ItemStack itemStack : player.getInventory().getContents()) {
                            if (itemStack != null) {
                                if(!deliverItemsObjective.getItemStackSelection().checkIfIsIncluded(itemStack)) {
                                    continue;
                                }
                                final double progressLeft = activeObjective.getProgressNeeded() - activeObjective.getCurrentProgress();

                                if (progressLeft == 0) {
                                    continue;
                                }
                                handledObjective.set(true);

                                if (progressLeft < itemStack.getAmount()) { //We can finish it with this itemStack
                                    itemStack.setAmount((itemStack.getAmount() - (int) progressLeft));
                                    activeObjective.addProgress(progressLeft, armorStand.getUniqueId());
                                    player.sendMessage(main.parse(
                                        "<GREEN>You have delivered <highlight>" + progressLeft + "</highlight> items to <highlight>" + main.getArmorStandManager().getArmorStandName(armorStand)
                                    ));
                                    break;
                                } else {
                                    player.getInventory().removeItemAnySlot(itemStack);
                                    activeObjective.addProgress(itemStack.getAmount(), armorStand.getUniqueId());
                                    player.sendMessage(main.parse(
                                        "<GREEN>You have delivered <highlight>" + itemStack.getAmount() + "</highlight> items to <highlight>" + main.getArmorStandManager().getArmorStandName(armorStand)
                                    ));
                                }
                            }

                        }

                    }
                }
            });
            questPlayer.queueObjectiveCheck(activeObjective -> {
                if (activeObjective.getObjective() instanceof final TalkToNPCObjective talkToNPCObjective) {
                    if (talkToNPCObjective.getNPCtoTalkID() == -1 && (talkToNPCObjective.getArmorStandUUID().equals(armorStand.getUniqueId()))) {
                        activeObjective.addProgress(1, armorStand.getUniqueId());
                        player.sendMessage(main.parse(
                            "<GREEN>You talked to <highlight>" + main.getArmorStandManager().getArmorStandName(armorStand)
                        ));
                        handledObjective.set(true);
                    }
                }
            });
            questPlayer.queueObjectiveCheck(activeObjective -> {
                //Eventually trigger CompletionNPC Objective Completion if the objective is not set to complete automatically (so, if getCompletionArmorStandUUID() is not null)
                if (activeObjective.getObjective().getCompletionArmorStandUUID() != null) {

                    activeObjective.addProgress(0, armorStand.getUniqueId());
                }
            });
            questPlayer.checkQueuedObjectives();


        }


        //Show quests
        if (handledObjective.get()) {
            if (main.getConfiguration().isArmorStandPreventEditing()) {
                event.setCancelled(true);
            }
            return;
        }


        //If Armor Stand has Quests attached to it and it prevent-editing is true in the config
        if (main.getQuestManager().sendQuestsPreviewOfQuestShownArmorstands(armorStand, questPlayer) && main.getConfiguration().isArmorStandPreventEditing()) {
            event.setCancelled(true);
        }

        //Conversations
        final Conversation foundConversation = main.getConversationManager().getConversationAttachedToArmorstand(armorStand);
        if (questPlayer != null && foundConversation != null) {
            main.getConversationManager().playConversation(questPlayer, foundConversation);
            if (main.getConfiguration().isArmorStandPreventEditing()) {
                event.setCancelled(true);
            }
        }

    }


    @EventHandler
    private void onArmorStandLoad(EntitiesLoadEvent event) {
        if (!main.getConfiguration().isArmorStandQuestGiverIndicatorParticleEnabled()) {
            return;
        }
        for(final Entity entity : event.getEntities()){
            if (entity instanceof final ArmorStand armorStand) {
                final PersistentDataContainer armorStandPDB = armorStand.getPersistentDataContainer();

                if (!armorStandPDB.has(main.getArmorStandManager().getAttachedQuestsShowingKey(), PersistentDataType.STRING) && !armorStandPDB.has(main.getArmorStandManager().getAttachedQuestsNonShowingKey(), PersistentDataType.STRING) && !armorStandPDB.has(main.getArmorStandManager().getAttachedConversationKey(), PersistentDataType.STRING)) {
                    return;
                }

                main.getArmorStandManager().addArmorStandWithQuestsOrConversationAttachedToThem(armorStand);

            }
        }

    }

    @EventHandler
    private void onArmorStandUnload(EntitiesUnloadEvent event) {
        if (!main.getConfiguration().isArmorStandQuestGiverIndicatorParticleEnabled()) {
            return;
        }
        for (final Entity entity : event.getEntities()) {
            if (entity instanceof final ArmorStand armorStand) {
                final PersistentDataContainer armorStandPDB = armorStand.getPersistentDataContainer();


                if (!armorStandPDB.has(main.getArmorStandManager().getAttachedQuestsShowingKey(), PersistentDataType.STRING) && !armorStandPDB.has(main.getArmorStandManager().getAttachedQuestsNonShowingKey(), PersistentDataType.STRING) && !armorStandPDB.has(main.getArmorStandManager().getAttachedConversationKey(), PersistentDataType.STRING)) {
                    continue;
                }

                main.getArmorStandManager().removeArmorStandWithQuestsOrConversationAttachedToThem(armorStand);
            }
        }
    }


    //This probably will never happen and is not really needed, because armor stands are not spawned immediately with the PDB - you have to assign it to them first
    @EventHandler
    public void onArmorStandSpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof final ArmorStand armorStand) {
            if (!main.getConfiguration().isArmorStandQuestGiverIndicatorParticleEnabled()) {
                return;
            }
            final PersistentDataContainer armorStandPDB = armorStand.getPersistentDataContainer();

            if (!armorStandPDB.has(main.getArmorStandManager().getAttachedQuestsShowingKey(), PersistentDataType.STRING) && !armorStandPDB.has(main.getArmorStandManager().getAttachedQuestsNonShowingKey(), PersistentDataType.STRING) && !armorStandPDB.has(main.getArmorStandManager().getAttachedConversationKey(), PersistentDataType.STRING)) {
                return;
            }
            main.getArmorStandManager().addArmorStandWithQuestsOrConversationAttachedToThem(armorStand);

        }

    }


    @EventHandler
    public void onArmorStandDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof final ArmorStand armorStand) {
            if (!main.getConfiguration().isArmorStandQuestGiverIndicatorParticleEnabled()) {
                return;
            }
            final PersistentDataContainer armorStandPDB = armorStand.getPersistentDataContainer();


            if (!armorStandPDB.has(main.getArmorStandManager().getAttachedQuestsShowingKey(), PersistentDataType.STRING) && !armorStandPDB.has(main.getArmorStandManager().getAttachedQuestsNonShowingKey(), PersistentDataType.STRING) && !armorStandPDB.has(main.getArmorStandManager().getAttachedConversationKey(), PersistentDataType.STRING)) {
                return;
            }

            main.getArmorStandManager().removeArmorStandWithQuestsOrConversationAttachedToThem(armorStand);

        }

    }


}
