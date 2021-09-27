package notquests.notquests.Events;


import notquests.notquests.NotQuests;
import notquests.notquests.Structs.ActiveObjective;
import notquests.notquests.Structs.ActiveQuest;
import notquests.notquests.Structs.Objectives.*;
import notquests.notquests.Structs.QuestPlayer;
import notquests.notquests.Structs.Triggers.ActiveTrigger;
import notquests.notquests.Structs.Triggers.TriggerTypes.TriggerType;
import notquests.notquests.Structs.Triggers.TriggerTypes.WorldEnterTrigger;
import notquests.notquests.Structs.Triggers.TriggerTypes.WorldLeaveTrigger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;

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
                                if (activeObjective.getObjective() instanceof BreakBlocksObjective breakBlocksObjective) {
                                    if (breakBlocksObjective.getBlockToBreak().equals(e.getBlock().getType())) {
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
                            //This is for the BreakBlocksObjective. It should deduct the progress if the player placed the same block again (if willDeductIfBlockPlaced() is set to true)
                            if (activeObjective.getObjective() instanceof BreakBlocksObjective breakBlocksObjective) {
                                if (breakBlocksObjective.getBlockToBreak().equals(e.getBlock().getType())) {
                                    if (breakBlocksObjective.willDeductIfBlockPlaced()) {
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
                                if (activeObjective.getObjective() instanceof final CollectItemsObjective collectItemsObjective) {


                                    //Check if the Material of the collected item is equal to the Material needed in the CollectItemsObjective
                                    if (!collectItemsObjective.getItemToCollect().getType().equals(e.getItem().getItemStack().getType())) {
                                        return;
                                    }

                                    //If the objective-item which needs to be collected has an ItemMeta...
                                    if (collectItemsObjective.getItemToCollect().getItemMeta() != null) {
                                        //then check if the ItemMeta of the collected item is equal to the ItemMeta needed in the CollectItemsObjective
                                        if (!collectItemsObjective.getItemToCollect().getItemMeta().equals(e.getItem().getItemStack().getItemMeta())) {
                                            return;
                                        }
                                    }

                                    activeObjective.addProgress(e.getItem().getItemStack().getAmount(), -1);

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
                                if (activeObjective.getObjective() instanceof final CollectItemsObjective collectItemsObjective) {

                                    //Check if the Material of the collected item is equal to the Material needed in the CollectItemsObjective
                                    if (!collectItemsObjective.getItemToCollect().getType().equals(e.getItemDrop().getItemStack().getType())) {
                                        return;
                                    }

                                    //If the objective-item which needs to be collected has an ItemMeta...
                                    if (collectItemsObjective.getItemToCollect().getItemMeta() != null) {
                                        //then check if the ItemMeta of the collected item is equal to the ItemMeta needed in the CollectItemsObjective
                                        if (!collectItemsObjective.getItemToCollect().getItemMeta().equals(e.getItemDrop().getItemStack().getItemMeta())) {
                                            return;
                                        }
                                    }

                                    activeObjective.removeProgress(e.getItemDrop().getItemStack().getAmount(), false);

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

                    //Iterator<ActiveQuest> iter = questPlayer.getActiveQuests().iterator(); //Why was that needed?

                    for (int i = 0; i < questPlayer.getActiveQuests().size(); i++) {
                        final ActiveQuest activeQuest = questPlayer.getActiveQuests().get(i);
                        for (final ActiveTrigger activeTrigger : activeQuest.getActiveTriggers()) {
                            if (activeTrigger.getTrigger().getTriggerType() == TriggerType.DEATH) {
                                handleGeneralTrigger(questPlayer, activeTrigger);

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
                            if (activeObjective.getObjective() instanceof KillMobsObjective killMobsObjective) {
                                if (activeObjective.isUnlocked()) {
                                    final EntityType killedMob = e.getEntity().getType();
                                    if (killMobsObjective.getMobToKill().equals(killedMob)) {
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
                        if (activeObjective.getObjective() instanceof ConsumeItemsObjective consumeItemsObjective) {
                            if (activeObjective.isUnlocked()) {

                                //Check if the Material of the consumed item is equal to the Material needed in the ConsumeItemsObjective
                                if (!consumeItemsObjective.getItemToConsume().getType().equals(e.getItem().getType())) {
                                    return;
                                }

                                //If the objectiv-item which needs to be crafted has an ItemMeta...
                                if (consumeItemsObjective.getItemToConsume().getItemMeta() != null) {
                                    //then check if the ItemMeta of the consumed item is equal to the ItemMeta needed in the ConsumeItemsObjective
                                    if (!consumeItemsObjective.getItemToConsume().getItemMeta().equals(e.getItem().getItemMeta())) {
                                        return;
                                    }
                                }

                                activeObjective.addProgress(1, -1);

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
                            handleGeneralTrigger(questPlayer, activeTrigger);
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
                        if (activeTrigger.getTrigger() instanceof WorldEnterTrigger worldEnterTrigger) {
                            if (e.getPlayer().getWorld().getName().equals(worldEnterTrigger.getWorldToEnterName())) {
                                handleGeneralTrigger(questPlayer, activeTrigger);

                            }

                        } else if (activeTrigger.getTrigger() instanceof WorldLeaveTrigger worldLeaveTrigger) {
                            if (e.getFrom().getName().equals(worldLeaveTrigger.getWorldToLeaveName())) {
                                handleGeneralTrigger(questPlayer, activeTrigger);
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

                        }else{
                            armorstandPDB.set(attachedQuestsKey, PersistentDataType.STRING, "°"+questName+"°");
                            player.sendMessage("§aQuest with the name §b" + questName + " §awas added to this poor little armorstand!");
                            player.sendMessage("§2Attached Quests: §b" + "°"+questName+"°");
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
                                    if (c != '°') {
                                        foundNonSeparator = true;
                                        break;
                                    }
                                }
                                if(!foundNonSeparator){
                                    existingAttachedQuests = "";
                                }

                                armorstandPDB.set(attachedQuestsKey, PersistentDataType.STRING, existingAttachedQuests);
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
                        NamespacedKey attachedQuestsShowingKey = new NamespacedKey(main, "notquests-attachedQuests-showing");
                        NamespacedKey attachedQuestsNonShowingKey = new NamespacedKey(main, "notquests-attachedQuests-nonshowing");

                        final ArrayList<String> showingQuests = new ArrayList<>();
                        final ArrayList<String> nonShowingQuests = new ArrayList<>();

                        boolean hasShowingQuestsPDBKey = false;
                        boolean hasNonShowingQuestsPDBKey = false;


                        if(armorstandPDB.has(attachedQuestsShowingKey, PersistentDataType.STRING)){
                            String existingAttachedQuests = armorstandPDB.get(attachedQuestsShowingKey, PersistentDataType.STRING);
                            hasShowingQuestsPDBKey = true;
                            if(existingAttachedQuests != null && existingAttachedQuests.length() >= 1){
                                showingQuests.addAll(Arrays.asList(existingAttachedQuests.split("°")));
                            }
                        }
                        if(armorstandPDB.has(attachedQuestsNonShowingKey, PersistentDataType.STRING)){
                            String existingAttachedQuests = armorstandPDB.get(attachedQuestsNonShowingKey, PersistentDataType.STRING);
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
    private void onCraftItemEvent(CraftItemEvent e) {
        final Entity entity = e.getWhoClicked();
        if (entity instanceof final Player player && e.getInventory().getResult() != null) {
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                if (questPlayer.getActiveQuests().size() > 0) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                            if (activeObjective.isUnlocked()) {
                                if (activeObjective.getObjective() instanceof final CraftItemsObjective craftItemsObjective) {

                                    //Check if the Material of the crafted item is equal to the Material needed in the CraftItemsObjective
                                    if (!craftItemsObjective.getItemToCraft().getType().equals(e.getInventory().getResult().getType())) {
                                        return;
                                    }

                                    //If the objectiv-item which needs to be crafted has an ItemMeta...
                                    if (craftItemsObjective.getItemToCraft().getItemMeta() != null) {
                                        //then check if the ItemMeta of the crafted item is equal to the ItemMeta needed in the CraftItemsObjective
                                        if (!craftItemsObjective.getItemToCraft().getItemMeta().equals(e.getInventory().getResult().getItemMeta())) {
                                            return;
                                        }
                                    }

                                    //Now we gotta figure out the real amount of items which have been crafted, which is trickier than expected:
                                    ClickType click = e.getClick();

                                    int recipeAmount = e.getRecipe().getResult().getAmount();

                                    switch (click) {
                                        case NUMBER_KEY:
                                            //If the hotbar is full, the item will not be crafted but it will still trigger this event for some reason. That's
                                            //why we manually have to set the amount to 0 here
                                            if (e.getWhoClicked().getInventory().getItem(e.getHotbarButton()) != null) {
                                                recipeAmount = 0;
                                            }
                                            break;

                                        case DROP:
                                        case CONTROL_DROP:
                                            // If we are holding items, craft-via-drop fails (vanilla behavior)
                                            ItemStack cursor = e.getCursor();
                                            // Cursor is either null or AIR
                                            if (!(cursor == null || cursor.getType() == Material.AIR)) {
                                                recipeAmount = 0;
                                            }

                                            break;

                                        case SHIFT_RIGHT:
                                        case SHIFT_LEFT:
                                            if (recipeAmount == 0)
                                                break;

                                            int maxCraftable = getMaxCraftAmount(e.getInventory());
                                            int capacity = fits(e.getRecipe().getResult(), e.getView().getBottomInventory());

                                            // If we can't fit everything, increase "space" to include the items dropped by
                                            // crafting
                                            // (Think: Uncrafting 8 iron blocks into 1 slot)
                                            if (capacity < maxCraftable)
                                                maxCraftable = ((capacity + recipeAmount - 1) / recipeAmount) * recipeAmount;

                                            recipeAmount = maxCraftable;
                                            break;
                                        default:
                                    }

                                    // No use continuing if we haven't actually crafted a thing
                                    if (recipeAmount == 0)
                                        return;

                                    activeObjective.addProgress(recipeAmount, -1);


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


    private int getMaxCraftAmount(CraftingInventory inv) {
        if (inv.getResult() == null)
            return 0;

        int resultCount = inv.getResult().getAmount();
        int materialCount = Integer.MAX_VALUE;

        for (ItemStack is : inv.getMatrix())
            if (is != null && is.getAmount() < materialCount)
                materialCount = is.getAmount();

        return resultCount * materialCount;
    }

    private int fits(ItemStack stack, Inventory inv) {
        ItemStack[] contents = inv.getContents();
        int result = 0;

        for (ItemStack is : contents)
            if (is == null)
                result += stack.getMaxStackSize();
            else if (is.isSimilar(stack))
                result += Math.max(stack.getMaxStackSize() - is.getAmount(), 0);

        return result;
    }


    /**
     * This method handles the most commonly used type of trigger, which should simply add to the progress.
     * Apart from adding the progress, this method checks for the triggers applyOn and the triggers worldName
     *
     * @param questPlayer   is the QuestPlayer object, used to check the world of the player
     * @param activeTrigger is the trigger which we need in order to add progress to it
     */
    private void handleGeneralTrigger(final QuestPlayer questPlayer, final ActiveTrigger activeTrigger) {

        //Handle Trigger applyOn
        if (activeTrigger.getTrigger().getApplyOn() >= 1) { //Trigger applies to a specific objective of the Quest and not the Quest itself
            //Get the active Objective for which the trigger applies to
            final ActiveObjective activeObjective = activeTrigger.getActiveQuest().getActiveObjectiveFromID(activeTrigger.getTrigger().getApplyOn());
            //Return, if the active objective which the trigger needs doesn't exist or is not yet unlocked (so hidden)
            if (activeObjective == null || !activeObjective.isUnlocked()) {
                return;
            }
        }

        //Handle Trigger World Name
        if (!activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
            final Player qPlayer = Bukkit.getPlayer(questPlayer.getUUID());
            //If the player is not in the world which the Trigger needs, cancel.
            if (qPlayer == null || !qPlayer.getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
                return;
            }
        }

        //Finally, we can add to the trigger and check if it can trigger now if the progress is full
        activeTrigger.addAndCheckTrigger(activeTrigger.getActiveQuest());


    }


}
