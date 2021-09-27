package notquests.notquests.Events;

import notquests.notquests.NotQuests;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
            ArmorStand armorstand = (ArmorStand) event.getRightClicked();

            final ItemStack heldItem = event.getPlayer().getInventory().getItemInMainHand();

            if (heldItem.getType() != Material.AIR && heldItem.getItemMeta() != null) {
                final PersistentDataContainer container = heldItem.getItemMeta().getPersistentDataContainer();

                final NamespacedKey specialItemKey = new NamespacedKey(main, "notquests-item");


                if (container.has(specialItemKey, PersistentDataType.INTEGER)) {

                    int id = container.get(specialItemKey, PersistentDataType.INTEGER); //Not null, because we check for it in container.has()

                    final NamespacedKey questsKey = new NamespacedKey(main, "notquests-questname");

                    final String questName = container.get(questsKey, PersistentDataType.STRING);

                    if (questName == null && id >= 0 && id <= 3) {
                        player.sendMessage("§cError: Your item has no valid quest attached to it.");
                        return;
                    }

                    if(id == 0 || id == 1){ //Add
                        PersistentDataContainer armorstandPDB = armorstand.getPersistentDataContainer();
                        final NamespacedKey attachedQuestsKey;
                        if(id == 0){ //showing
                            attachedQuestsKey  = main.getArmorStandManager().getAttachedQuestsShowingKey();
                        }else{
                            attachedQuestsKey  = main.getArmorStandManager().getAttachedQuestsNonShowingKey();
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

                        }else{
                            armorstandPDB.set(attachedQuestsKey, PersistentDataType.STRING, "°"+questName+"°");
                            player.sendMessage("§aQuest with the name §b" + questName + " §awas added to this poor little armorstand!");
                            player.sendMessage("§2Attached Quests: §b" + "°"+questName+"°");
                        }


                    }else if(id == 2 || id == 3){ //Remove
                        PersistentDataContainer armorstandPDB = armorstand.getPersistentDataContainer();

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
                                }else{
                                    armorstandPDB.set(attachedQuestsKey, PersistentDataType.STRING, existingAttachedQuests);
                                }


                                player.sendMessage("§2Quest with the name §b" + questName + " §2was removed from this armor stand!");
                                player.sendMessage("§2Attached Quests: §b" + existingAttachedQuests);

                            }else{
                                player.sendMessage("§cError: That armor stand does not have the Quest §b" + questName + " §cattached to it!");
                                player.sendMessage("§2Attached Quests: §b" + existingAttachedQuests);
                            }
                        }else{
                            player.sendMessage("§cThis armor stand has no quests attached to it!");
                        }


                    }else if(id == 4){ //Check

                        player.sendMessage("§7Armor Stand Entity ID: §f" + armorstand.getUniqueId().toString());


                        //Get all Quests attached to this armor stand:
                        PersistentDataContainer armorstandPDB = armorstand.getPersistentDataContainer();

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
                            int counter=0;
                            for(final String questNameInList : nonShowingQuests){
                                if(!questNameInList.isBlank()){ //empty or null or only whitespaces
                                    counter++;
                                    player.sendMessage("§7" + counter + ". §e" + questNameInList);
                                }

                            }
                        }

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





    @EventHandler
    private void onArmorStandLoad(EntitiesLoadEvent event){
        if (!main.getDataManager().getConfiguration().isQuestGiverIndicatorParticleEnabled()){
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
                    Bukkit.getPlayer("NoeX").sendMessage("Armor Stand added to list");
                }
            }
        }

    }
    @EventHandler
    private void onArmorStandUnload(EntitiesUnloadEvent event) {
        if (!main.getDataManager().getConfiguration().isQuestGiverIndicatorParticleEnabled()) {
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







}
