package notquests.notquests.Events;

import notquests.notquests.NotQuests;
import notquests.notquests.Structs.ActiveObjective;
import notquests.notquests.Structs.ActiveQuest;
import notquests.notquests.Structs.Objectives.TalkToNPCObjective;
import notquests.notquests.Structs.Quest;
import notquests.notquests.Structs.QuestPlayer;
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

import java.util.ArrayList;
import java.util.Arrays;

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

                final NamespacedKey specialItemKey = new NamespacedKey(main, "notquests-item");


                if (container.has(specialItemKey, PersistentDataType.INTEGER)) {

                    int id = container.get(specialItemKey, PersistentDataType.INTEGER); //Not null, because we check for it in container.has()

                    final NamespacedKey questsKey = new NamespacedKey(main, "notquests-questname");

                    final String questName = container.get(questsKey, PersistentDataType.STRING);

                    if (questName == null && ((id >= 0 && id <= 3) || id == 5)) {
                        player.sendMessage("§cError: Your item has no valid quest attached to it.");
                        return;
                    }

                    if(id == 0 || id == 1) { //Add
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

                            armorStandPDB.set(attachedQuestsKey, PersistentDataType.STRING, existingAttachedQuests);

                            player.sendMessage("§aQuest with the name §b" + questName + " §awas added to this poor little armorstand!");
                            player.sendMessage("§2Attached Quests: §b" + existingAttachedQuests);

                        }else {
                            armorStandPDB.set(attachedQuestsKey, PersistentDataType.STRING, "°" + questName + "°");
                            player.sendMessage("§aQuest with the name §b" + questName + " §awas added to this poor little armorstand!");
                            player.sendMessage("§2Attached Quests: §b" + "°" + questName + "°");

                            //Since this is the first Quest added to it:
                            main.getArmorStandManager().addArmorStandWithQuestsAttachedToThem(armorStand);

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


                                existingAttachedQuests = existingAttachedQuests.replaceAll("°" + questName+"°", "°");

                                //So it can go fully empty again
                                boolean foundNonSeparator = false;
                                for (int i = 0; i < existingAttachedQuests.length(); i++){
                                    char c = existingAttachedQuests.charAt(i);
                                    if (c != '°') {
                                        foundNonSeparator = true;
                                        break;
                                    }
                                }

                                if(!foundNonSeparator){
                                    //It consists only of separators - no quests. Thus, we set this to "" and remove the PDB
                                    existingAttachedQuests = "";
                                    armorstandPDB.remove(attachedQuestsKey);
                                    main.getArmorStandManager().removeArmorStandWithQuestsAttachedToThem(armorStand);
                                }else{
                                    armorstandPDB.set(attachedQuestsKey, PersistentDataType.STRING, existingAttachedQuests);
                                }


                                player.sendMessage("§2Quest with the name §b" + questName + " §2was removed from this armor stand!");
                                player.sendMessage("§2Attached Quests: §b" + existingAttachedQuests);

                            } else {
                                player.sendMessage("§cError: That armor stand does not have the Quest §b" + questName + " §cattached to it!");
                                player.sendMessage("§2Attached Quests: §b" + existingAttachedQuests);
                            }
                        } else {
                            player.sendMessage("§cThis armor stand has no quests attached to it!");
                        }


                    } else if (id == 4) { //Check

                        player.sendMessage("§7Armor Stand Entity ID: §f" + armorStand.getUniqueId());


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
                            if(hasShowingQuestsPDBKey){
                                player.sendMessage("§9All attached showing Quests: §7Empty");
                            }else{
                                player.sendMessage("§9All attached showing Quests: §7None");
                            }
                        }else{
                            player.sendMessage("§9All " + showingQuests.size() + " attached showing Quests:");
                            int counter=0;
                            for(final String questNameInList : showingQuests){
                                if(!questNameInList.isBlank()){ //empty or null or only whitespaces
                                    counter++;
                                    player.sendMessage("§7" + counter + ". §e" + questNameInList);
                                }

                            }
                        }

                        if(nonShowingQuests.size() == 0){
                            if(hasNonShowingQuestsPDBKey){
                                player.sendMessage("§9All attached non-showing Quests: §7Empty");
                            }else{
                                player.sendMessage("§9All attached non-showing Quests: §7None");
                            }

                        }else{
                            player.sendMessage("§9All " + nonShowingQuests.size() + " attached non-showing Quests:");
                            int counter = 0;
                            for (final String questNameInList : nonShowingQuests) {
                                if (!questNameInList.isBlank()) { //empty or null or only whitespaces
                                    counter++;
                                    player.sendMessage("§7" + counter + ". §e" + questNameInList);
                                }

                            }
                        }

                    } else if (id == 5) { //Add Objective TalkToNPC


                        final Quest quest = main.getQuestManager().getQuest(questName);
                        if (quest != null) {
                            TalkToNPCObjective talkToNPCObjective = new TalkToNPCObjective(main, quest, quest.getObjectives().size() + 1, armorStand.getUniqueId());
                            quest.addObjective(talkToNPCObjective, true);
                            player.sendMessage("§aObjective successfully added to quest §b" + quest.getQuestName() + "§a!");

                        } else {
                            player.sendMessage("§cError: Quest §b" + questName + " §cdoes not exist.");
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
        final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());

        boolean handledObjective = false;

        if (questPlayer != null) {
            if (questPlayer.getActiveQuests().size() > 0) {
                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                    for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                        if (activeObjective.isUnlocked()) {
                            if (activeObjective.getObjective() instanceof final TalkToNPCObjective objective) {
                                if (objective.getNPCtoTalkID() == -1 && (objective.getArmorStandUUID().equals(armorStand.getUniqueId()))) {
                                    activeObjective.addProgress(1, armorStand.getUniqueId());
                                    player.sendMessage("§aYou talked to §b" + main.getArmorStandManager().getArmorStandName(armorStand));
                                    handledObjective = true;
                                }
                            }
                            //Eventually trigger CompletionNPC Objective Completion if the objective is not set to complete automatically (so, if getCompletionNPCID() is not -1)
                            // if (activeObjective.getObjective().getCompletionNPCID() != -1) {
                            //     activeObjective.addProgress(0, npc.getId());
                            // }
                        }

                    }
                    activeQuest.removeCompletedObjectives(true);
                }
                questPlayer.removeCompletedQuests();
            }
        }


        //Show quests
        if (handledObjective) {
            if (main.getDataManager().getConfiguration().isArmorStandPreventEditing()) {
                event.setCancelled(true);
            }
            return;
        }


        //If Armor Stand has Quests attached to it and it prevent-editing is true in the config
        if (main.getQuestManager().sendQuestsPreviewOfQuestShownArmorstands(armorStand, player) && main.getDataManager().getConfiguration().isArmorStandPreventEditing()) {
            event.setCancelled(true);
        }
    }


    @EventHandler
    private void onArmorStandLoad(EntitiesLoadEvent event) {
        if (!main.getDataManager().getConfiguration().isArmorStandQuestGiverIndicatorParticleEnabled()) {
            return;
        }
        for(final Entity entity : event.getEntities()){
            if(entity instanceof ArmorStand armorStand){
                final PersistentDataContainer armorStandPDB = armorStand.getPersistentDataContainer();

                boolean hasShowingQuestsPDBKey = false;
                boolean hasNonShowingQuestsPDBKey = false;


                if(armorStandPDB.has(main.getArmorStandManager().getAttachedQuestsShowingKey(), PersistentDataType.STRING)){
                    hasShowingQuestsPDBKey = true;
                }
                if(armorStandPDB.has(main.getArmorStandManager().getAttachedQuestsNonShowingKey(), PersistentDataType.STRING)) {
                    hasNonShowingQuestsPDBKey = true;
                }

                if(hasShowingQuestsPDBKey || hasNonShowingQuestsPDBKey){
                    main.getArmorStandManager().addArmorStandWithQuestsAttachedToThem(armorStand);
                }
            }
        }

    }
    @EventHandler
    private void onArmorStandUnload(EntitiesUnloadEvent event) {
        if (!main.getDataManager().getConfiguration().isArmorStandQuestGiverIndicatorParticleEnabled()) {
            return;
        }
        for (final Entity entity : event.getEntities()) {
            if (entity instanceof ArmorStand armorStand) {
                final PersistentDataContainer armorStandPDB = armorStand.getPersistentDataContainer();


                boolean hasShowingQuestsPDBKey = false;
                boolean hasNonShowingQuestsPDBKey = false;


                if (armorStandPDB.has(main.getArmorStandManager().getAttachedQuestsShowingKey(), PersistentDataType.STRING)) {
                    hasShowingQuestsPDBKey = true;
                }
                if (armorStandPDB.has(main.getArmorStandManager().getAttachedQuestsNonShowingKey(), PersistentDataType.STRING)) {
                    hasNonShowingQuestsPDBKey = true;
                }

                if (hasShowingQuestsPDBKey || hasNonShowingQuestsPDBKey) {
                    main.getArmorStandManager().removeArmorStandWithQuestsAttachedToThem(armorStand);

                }
            }
        }
    }


    //This probably will never happen and is not really needed, because armor stands are not spawned immediately with the PDB - you have to assign it to them first
    @EventHandler
    public void onArmorStandSpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof ArmorStand armorStand) {
            if (!main.getDataManager().getConfiguration().isArmorStandQuestGiverIndicatorParticleEnabled()) {
                return;
            }
            final PersistentDataContainer armorStandPDB = armorStand.getPersistentDataContainer();

            boolean hasShowingQuestsPDBKey = false;
            boolean hasNonShowingQuestsPDBKey = false;


            if (armorStandPDB.has(main.getArmorStandManager().getAttachedQuestsShowingKey(), PersistentDataType.STRING)) {
                hasShowingQuestsPDBKey = true;
            }
            if (armorStandPDB.has(main.getArmorStandManager().getAttachedQuestsNonShowingKey(), PersistentDataType.STRING)) {
                hasNonShowingQuestsPDBKey = true;
            }

            if (hasShowingQuestsPDBKey || hasNonShowingQuestsPDBKey) {
                main.getArmorStandManager().addArmorStandWithQuestsAttachedToThem(armorStand);
            }
        }

    }


    @EventHandler
    public void onArmorStandSpawn(EntityDeathEvent event) {
        if (event.getEntity() instanceof ArmorStand armorStand) {
            if (!main.getDataManager().getConfiguration().isArmorStandQuestGiverIndicatorParticleEnabled()) {
                return;
            }
            final PersistentDataContainer armorStandPDB = armorStand.getPersistentDataContainer();

            boolean hasShowingQuestsPDBKey = false;
            boolean hasNonShowingQuestsPDBKey = false;


            if (armorStandPDB.has(main.getArmorStandManager().getAttachedQuestsShowingKey(), PersistentDataType.STRING)) {
                hasShowingQuestsPDBKey = true;
            }
            if (armorStandPDB.has(main.getArmorStandManager().getAttachedQuestsNonShowingKey(), PersistentDataType.STRING)) {
                hasNonShowingQuestsPDBKey = true;
            }

            if (hasShowingQuestsPDBKey || hasNonShowingQuestsPDBKey) {
                main.getArmorStandManager().removeArmorStandWithQuestsAttachedToThem(armorStand);
            }
        }

    }


}
