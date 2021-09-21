package notquests.notquests.Events;


import notquests.notquests.NotQuests;
import notquests.notquests.Structs.ActiveObjective;
import notquests.notquests.Structs.ActiveQuest;
import notquests.notquests.Structs.Objectives.*;
import notquests.notquests.Structs.Quest;
import notquests.notquests.Structs.QuestPlayer;
import notquests.notquests.Structs.Triggers.ActiveTrigger;
import notquests.notquests.Structs.Triggers.TriggerTypes.TriggerType;
import notquests.notquests.Structs.Triggers.TriggerTypes.WorldEnterTrigger;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;

public class QuestEvents implements Listener {
    private final NotQuests main;

    public QuestEvents(NotQuests main) {
        this.main = main;
    }




    @EventHandler(priority = EventPriority.LOWEST)
    private void onBlockBreak(BlockBreakEvent e) {
        if (!e.isCancelled()) {
            final Player player = e.getPlayer();
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                if (questPlayer.getActiveQuests().size() > 0) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                            if (activeObjective.isUnlocked()) {
                                if (activeObjective.getObjective() instanceof BreakBlocksObjective) {
                                    if (((BreakBlocksObjective) activeObjective.getObjective()).getBlockToBreak().equals(e.getBlock().getType())) {
                                        activeObjective.addProgress(1, -1);
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

    @EventHandler
    private void onBlockPlace(BlockPlaceEvent e) {
        final Player player = e.getPlayer();
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
        if (questPlayer != null) {
            if (questPlayer.getActiveQuests().size() > 0) {
                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                    for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                        if (activeObjective.isUnlocked()) {
                            if (activeObjective.getObjective() instanceof BreakBlocksObjective) {
                                if (((BreakBlocksObjective) activeObjective.getObjective()).getBlockToBreak().equals(e.getBlock().getType())) {
                                    if (((BreakBlocksObjective) activeObjective.getObjective()).willDeductIfBlockPlaced()) {
                                        activeObjective.removeProgress(1, false);
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


    @EventHandler
    private void onPickupItemEvent(EntityPickupItemEvent e) {
        final Entity entity = e.getEntity();
        if (entity instanceof final Player player) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                if (questPlayer.getActiveQuests().size() > 0) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                            if (activeObjective.isUnlocked()) {
                                if (activeObjective.getObjective() instanceof final CollectItemsObjective objective) {
                                    if (objective.getItemToCollect().getType().equals(e.getItem().getItemStack().getType()) && objective.getItemToCollect().getItemMeta().equals(e.getItem().getItemStack().getItemMeta())) {
                                        activeObjective.addProgress(e.getItem().getItemStack().getAmount(), -1);
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

    @EventHandler
    private void onDropItemEvent(EntityDropItemEvent e) { //DEFAULT ENABLED FOR ITEM DROPS UNLIKE FOR BLOCK BREAKS
        final Entity entity = e.getEntity();
        if (entity instanceof final Player player) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                if (questPlayer.getActiveQuests().size() > 0) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                            if (activeObjective.isUnlocked()) {
                                if (activeObjective.getObjective() instanceof final CollectItemsObjective objective) {
                                    if (objective.getItemToCollect().getType().equals(e.getItemDrop().getItemStack().getType()) && objective.getItemToCollect().getItemMeta().equals(e.getItemDrop().getItemStack().getItemMeta())) {
                                        activeObjective.removeProgress(e.getItemDrop().getItemStack().getAmount(), false);
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


    @EventHandler
    private void onEntityDeath(EntityDeathEvent e) { //KillMobs objectives & Death triggers

        //Death Triggers
        if (e.getEntity() instanceof final Player player) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                if (questPlayer.getActiveQuests().size() > 0) {

                    Iterator<ActiveQuest> iter = questPlayer.getActiveQuests().iterator();

                    for (int i = 0; i < questPlayer.getActiveQuests().size(); i++) {
                        final ActiveQuest activeQuest = questPlayer.getActiveQuests().get(i);
                        for (final ActiveTrigger activeTrigger : activeQuest.getActiveTriggers()) {
                            if (activeTrigger.getTrigger().getTriggerType() == TriggerType.DEATH) {
                                if (activeTrigger.getTrigger().getApplyOn() == 0) { //Quest and not Objective
                                    if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                                        activeTrigger.addAndCheckTrigger(activeQuest);
                                    } else {
                                        final Player qPlayer = Bukkit.getPlayer(questPlayer.getUUID());
                                        if (qPlayer != null && qPlayer.getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
                                            activeTrigger.addAndCheckTrigger(activeQuest);
                                        }
                                    }


                                } else if (activeTrigger.getTrigger().getApplyOn() >= 1) { //Objective and not Quest
                                    final ActiveObjective activeObjective = activeQuest.getActiveObjectiveFromID(activeTrigger.getTrigger().getApplyOn());
                                    if (activeObjective != null && activeObjective.isUnlocked()) {

                                        if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                                            activeTrigger.addAndCheckTrigger(activeQuest);
                                        } else {
                                            final Player qPlayer = Bukkit.getPlayer(questPlayer.getUUID());
                                            if (qPlayer != null && qPlayer.getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
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


        //KillMobs objectives
        final Player player = e.getEntity().getKiller();
        if (player != null) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                if (questPlayer.getActiveQuests().size() > 0) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                            if (activeObjective.getObjective() instanceof KillMobsObjective) {
                                if (activeObjective.isUnlocked()) {
                                    final EntityType killedMob = e.getEntity().getType();
                                    if (((KillMobsObjective) activeObjective.getObjective()).getMobToKill().equals(killedMob)) {
                                        if (e.getEntity() != e.getEntity().getKiller()) { //Suicide prevention
                                            activeObjective.addProgress(1, -1);
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

    @EventHandler
    private void onConsumeItemEvent(PlayerItemConsumeEvent e) { //DEFAULT ENABLED FOR ITEM DROPS UNLIKE FOR BLOCK BREAKS
        final Player player = e.getPlayer();

        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
        if (questPlayer != null) {
            if (questPlayer.getActiveQuests().size() > 0) {
                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                    for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                        if (activeObjective.getObjective() instanceof ConsumeItemsObjective) {
                            if (activeObjective.isUnlocked()) {
                                final ConsumeItemsObjective objective = ((ConsumeItemsObjective) activeObjective.getObjective());
                                if (objective.getItemToConsume().getType().equals(e.getItem().getType()) && objective.getItemToConsume().getItemMeta().equals(e.getItem().getItemMeta())) {
                                    activeObjective.addProgress(1, -1);
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


    @EventHandler
    private void onDisconnectEvent(PlayerQuitEvent e) { //Disconnect objectives
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(e.getPlayer().getUniqueId());
        if (questPlayer != null) {
            if (questPlayer.getActiveQuests().size() > 0) {
                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {

                    for (final ActiveTrigger activeTrigger : activeQuest.getActiveTriggers()) {
                        if (activeTrigger.getTrigger().getTriggerType() == TriggerType.DISCONNECT) {
                            if (activeTrigger.getTrigger().getApplyOn() == 0) {//Quest and not Objective

                                if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                                    activeTrigger.addAndCheckTrigger(activeQuest);
                                } else {
                                    final Player qPlayer = Bukkit.getPlayer(questPlayer.getUUID());
                                    if (qPlayer != null && qPlayer.getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
                                        activeTrigger.addAndCheckTrigger(activeQuest);
                                    }
                                }

                            } else if (activeTrigger.getTrigger().getApplyOn() >= 1) { //Objective and not Quest
                                final ActiveObjective activeObjective = activeQuest.getActiveObjectiveFromID(activeTrigger.getTrigger().getApplyOn());
                                if (activeObjective != null && activeObjective.isUnlocked()) {

                                    if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                                        activeTrigger.addAndCheckTrigger(activeQuest);
                                    } else {
                                        final Player qPlayer = Bukkit.getPlayer(questPlayer.getUUID());
                                        if (qPlayer != null && qPlayer.getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
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
    public void playerChangeWorldEvent(PlayerChangedWorldEvent e) {


        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(e.getPlayer().getUniqueId());
        if (questPlayer != null) {
            if (questPlayer.getActiveQuests().size() > 0) {
                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {

                    for (final ActiveTrigger activeTrigger : activeQuest.getActiveTriggers()) {
                        if (activeTrigger.getTrigger().getTriggerType() == TriggerType.WORLDENTER) {
                            if (e.getPlayer().getWorld().getName().equals(((WorldEnterTrigger) activeTrigger.getTrigger()).getWorldToEnterName())) {
                                if (activeTrigger.getTrigger().getApplyOn() == 0) {//Quest and not Objective

                                    if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                                        activeTrigger.addAndCheckTrigger(activeQuest);
                                    } else {
                                        if (e.getFrom().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
                                            activeTrigger.addAndCheckTrigger(activeQuest);
                                        }
                                    }

                                } else if (activeTrigger.getTrigger().getApplyOn() >= 1) { //Objective and not Quest
                                    final ActiveObjective activeObjective = activeQuest.getActiveObjectiveFromID(activeTrigger.getTrigger().getApplyOn());
                                    if (activeObjective != null && activeObjective.isUnlocked()) {

                                        if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                                            activeTrigger.addAndCheckTrigger(activeQuest);
                                        } else {
                                            if (e.getFrom().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
                                                activeTrigger.addAndCheckTrigger(activeQuest);
                                            }
                                        }

                                    }
                                }
                            }

                        } else if (activeTrigger.getTrigger().getTriggerType() == TriggerType.WORLDLEAVE) {
                            if (e.getFrom().getName().equals(((WorldEnterTrigger) activeTrigger.getTrigger()).getWorldToEnterName())) {
                                if (activeTrigger.getTrigger().getApplyOn() == 0) {//Quest and not Objective

                                    if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                                        activeTrigger.addAndCheckTrigger(activeQuest);
                                    } else {
                                        if (e.getPlayer().getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
                                            activeTrigger.addAndCheckTrigger(activeQuest);
                                        }
                                    }

                                } else if (activeTrigger.getTrigger().getApplyOn() >= 1) { //Objective and not Quest
                                    final ActiveObjective activeObjective = activeQuest.getActiveObjectiveFromID(activeTrigger.getTrigger().getApplyOn());
                                    if (activeObjective != null && activeObjective.isUnlocked()) {

                                        if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                                            activeTrigger.addAndCheckTrigger(activeQuest);
                                        } else {
                                            if (e.getPlayer().getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
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
    }


    @EventHandler
    private void onArmorstandClick(PlayerInteractAtEntityEvent event){

        final Player player = event.getPlayer();

        if(event.getRightClicked().getType() == EntityType.ARMOR_STAND) {
            ArmorStand armorstand = (ArmorStand) event.getRightClicked();

            final ItemStack heldItem = event.getPlayer().getInventory().getItemInMainHand();

            if(heldItem != null && heldItem.getItemMeta() != null){
                final PersistentDataContainer container = heldItem.getItemMeta().getPersistentDataContainer();

                final NamespacedKey specialItemKey = new NamespacedKey(main, "notquests-item");

                if(container.has(specialItemKey, PersistentDataType.INTEGER)){
                    int id = container.get(specialItemKey, PersistentDataType.INTEGER);

                    final NamespacedKey questsKey = new NamespacedKey(main, "notquests-questname");

                    final String questName =  container.get(questsKey, PersistentDataType.STRING);

                    if(questName == null && id >= 0 && id <= 3){
                        player.sendMessage("§cError: Your item has no valid quest attached to it.");
                        return;
                    }

                    if(id == 0 || id == 1){ //Add
                        PersistentDataContainer armorstandPDB = armorstand.getPersistentDataContainer();
                        NamespacedKey attachedQuestsKey;
                        if(id == 0){ //showing
                            attachedQuestsKey  = new NamespacedKey(main, "notquests-attachedQuests-showing");
                        }else{
                            attachedQuestsKey  = new NamespacedKey(main, "notquests-attachedQuests-nonshowing");
                        }
                        if(armorstandPDB.has(attachedQuestsKey, PersistentDataType.STRING)){
                            String existingAttachedQuests = armorstandPDB.get(attachedQuestsKey, PersistentDataType.STRING);


                            if(existingAttachedQuests != null ){
                                // boolean condition2 =  (existingAttachedQuests.contains("°"+questName) && (   ( (existingAttachedQuests.indexOf("°" + questName)-1) + ("°"+questName).length())  == existingAttachedQuests.length()-1  )  ) ;
                                //boolean condition3 =  (existingAttachedQuests.contains(questName + "°") &&  existingAttachedQuests.indexOf(questName+"°")  == 0 )   ;

                                //player.sendMessage("" + ( (existingAttachedQuests.indexOf("°" + questName)-1) + ("°"+questName).length()));
                                //player.sendMessage("" + (existingAttachedQuests.length()-1) );


                                if( existingAttachedQuests.equals(questName) || existingAttachedQuests.contains("°" + questName+"°") ){
                                    player.sendMessage("§cError: That armor stand already has the Quest §b" + questName + " §cattached to it!");
                                    player.sendMessage("§cAttached Quests: §b" + existingAttachedQuests);
                                    return;
                                }

                            }

                            if(existingAttachedQuests != null && existingAttachedQuests.length() >= 1){
                                if(   existingAttachedQuests.charAt(existingAttachedQuests.length()-1) == '°' ){
                                    existingAttachedQuests += (questName+"°") ;
                                }else{
                                    existingAttachedQuests += "°"+questName+"°";
                                }

                            }else{
                                existingAttachedQuests += "°"+questName+"°";
                            }

                            armorstandPDB.set(attachedQuestsKey, PersistentDataType.STRING, existingAttachedQuests);

                            player.sendMessage("§aQuest with the name §b" + questName + " §awas added to this poor little armorstand!");
                            player.sendMessage("§2Attached Quests: §b" + existingAttachedQuests);
                            return;

                        }else{
                            armorstandPDB.set(attachedQuestsKey, PersistentDataType.STRING, "°"+questName+"°");
                            player.sendMessage("§aQuest with the name §b" + questName + " §awas added to this poor little armorstand!");
                            player.sendMessage("§2Attached Quests: §b" + "°"+questName+"°");
                            return;
                        }


                    }else if(id == 2 || id == 3){ //Remove
                        PersistentDataContainer armorstandPDB = armorstand.getPersistentDataContainer();

                        NamespacedKey attachedQuestsKey;

                        if(id == 2){ //showing
                            attachedQuestsKey  = new NamespacedKey(main, "notquests-attachedQuests-showing");
                        }else{
                            attachedQuestsKey  = new NamespacedKey(main, "notquests-attachedQuests-nonshowing");
                        }

                        if(armorstandPDB.has(attachedQuestsKey, PersistentDataType.STRING)){
                            String existingAttachedQuests = armorstandPDB.get(attachedQuestsKey, PersistentDataType.STRING);
                            if(existingAttachedQuests != null && existingAttachedQuests.contains("°"+questName+"°")){


                                existingAttachedQuests = existingAttachedQuests.replaceAll("°" + questName+"°", "°");

                                //So it can go fully empty again
                                boolean foundNonSeparator = false;
                                for (int i = 0; i < existingAttachedQuests.length(); i++){
                                    char c = existingAttachedQuests.charAt(i);
                                    if(c != '°'){
                                        foundNonSeparator = true;
                                    }
                                }
                                if(!foundNonSeparator){
                                    existingAttachedQuests = "";
                                }

                                armorstandPDB.set(attachedQuestsKey, PersistentDataType.STRING, existingAttachedQuests);
                                player.sendMessage("§2Quest with the name §b" + questName + " §2was removed from this armor stand!");
                                player.sendMessage("§2Attached Quests: §b" + existingAttachedQuests);
                                return;

                            }else{
                                player.sendMessage("§cError: That armor stand does not have the Quest §b" + questName + " §cattached to it!");
                                player.sendMessage("§2Attached Quests: §b" + existingAttachedQuests);
                                return;
                            }
                        }else{
                            player.sendMessage("§cThis armor stand has no quests attached to it!");
                            return;
                        }


                    }else if(id == 4){ //Check
                        //Get all Quests attached to this armor stand:
                        PersistentDataContainer armorstandPDB = armorstand.getPersistentDataContainer();
                        NamespacedKey attachedQuestsShowingKey = new NamespacedKey(main, "notquests-attachedQuests-showing");
                        NamespacedKey attachedQuestsNonShowingKey = new NamespacedKey(main, "notquests-attachedQuests-nonshowing");

                        final ArrayList<String> showingQuests = new ArrayList<>();
                        final ArrayList<String> nonShowingQuests = new ArrayList<>();


                        if(armorstandPDB.has(attachedQuestsShowingKey, PersistentDataType.STRING)){
                            String existingAttachedQuests = armorstandPDB.get(attachedQuestsShowingKey, PersistentDataType.STRING);
                            if(existingAttachedQuests != null && existingAttachedQuests.length() >= 1){
                                for(String split : existingAttachedQuests.split("°")){
                                    showingQuests.add(split);
                                }
                            }
                        }
                        if(armorstandPDB.has(attachedQuestsNonShowingKey, PersistentDataType.STRING)){
                            String existingAttachedQuests = armorstandPDB.get(attachedQuestsNonShowingKey, PersistentDataType.STRING);
                            if(existingAttachedQuests != null && existingAttachedQuests.length() >= 1){
                                for(String split : existingAttachedQuests.split("°")){
                                    nonShowingQuests.add(split);
                                }
                            }
                        }

                        if(showingQuests.size() == 0){
                            player.sendMessage("§9All attached showing Quests: §7None");
                        }else{
                            player.sendMessage("§9All " + showingQuests.size() + " attached showing Quests:");
                            int counter=0;
                            for(final String questNameInList : showingQuests){
                                if(questNameInList.isBlank()){ //empty or null or only whitespaces
                                    counter++;
                                    player.sendMessage("§7" + counter + ". §e" + questNameInList);
                                }

                            }
                        }

                        if(nonShowingQuests.size() == 0){
                            player.sendMessage("§9All attached non-showing Quests: §7None");
                        }else{
                            player.sendMessage("§9All " + nonShowingQuests.size() + " attached non-showing Quests:");
                            int counter=0;
                            for(final String questNameInList : nonShowingQuests){
                                if(questNameInList.isBlank()){ //empty or null or only whitespaces
                                    counter++;
                                    player.sendMessage("§7" + counter + ". §e" + questNameInList);
                                }

                            }
                        }

                    }else{ //???
                        return;
                    }
                }else{
                    //Show quests
                    main.getQuestManager().sendQuestsPreviewOfQuestShownArmorstands(armorstand, player);
                }
            }else{
                //Show quests
                main.getQuestManager().sendQuestsPreviewOfQuestShownArmorstands(armorstand, player);
            }


        }
    }


}
