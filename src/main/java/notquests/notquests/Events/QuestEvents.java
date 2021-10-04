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
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;


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
                                    if (killMobsObjective.getMobToKill().equalsIgnoreCase("any") || killMobsObjective.getMobToKill().equalsIgnoreCase(killedMob.toString())) {
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

    //For ReachLocation
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!main.getDataManager().getConfiguration().isMoveEventEnabled()) {
            return;
        }


        if (e.getTo() == null) return;

        if (e.getFrom().getBlockX() != e.getTo().getBlockX() || e.getFrom().getBlockY() != e.getTo().getBlockY() || e.getFrom().getBlockZ() != e.getTo().getBlockZ()) {
            checkIfInReachLocation(e);
        }

    }

    public void checkIfInReachLocation(final PlayerMoveEvent e) {
        if (!e.isCancelled()) {
            final Player player = e.getPlayer();
            final QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId());
            if (questPlayer != null) {
                if (questPlayer.getActiveQuests().size() > 0) {
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                            if (activeObjective.isUnlocked()) {
                                if (activeObjective.getObjective() instanceof ReachLocationObjective reachLocationObjective) {
                                    final Location currentLocation = e.getTo();
                                    if (currentLocation == null) {
                                        return;
                                    }
                                    final Location minLocation = reachLocationObjective.getMinLocation();
                                    if (minLocation.getWorld() != null && currentLocation.getWorld() != null && !currentLocation.getWorld().equals(minLocation.getWorld())) {
                                        return;
                                    }
                                    final Location maxLocation = reachLocationObjective.getMaxLocation();
                                    if (currentLocation.getX() >= minLocation.getX() && currentLocation.getX() <= maxLocation.getX()) {
                                        if (currentLocation.getZ() >= minLocation.getZ() && currentLocation.getZ() <= maxLocation.getZ()) {
                                            if (currentLocation.getY() >= minLocation.getY() && currentLocation.getY() <= maxLocation.getY()) {
                                                activeObjective.addProgress(1, -1);
                                            }
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


    @EventHandler(priority = EventPriority.MONITOR)
    protected void onPluginEnable(final PluginEnableEvent event) {
        if (event.getPlugin().getName().equals("MythicMobs") && !main.isMythicMobsEnabled()) {
            // Turn on support for the plugin
            main.enableMythicMobs();
        }

    }


}
