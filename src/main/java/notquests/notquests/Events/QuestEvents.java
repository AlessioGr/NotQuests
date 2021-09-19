package notquests.notquests.Events;


import notquests.notquests.NotQuests;
import notquests.notquests.Structs.ActiveObjective;
import notquests.notquests.Structs.ActiveQuest;
import notquests.notquests.Structs.Objectives.*;
import notquests.notquests.Structs.QuestPlayer;
import notquests.notquests.Structs.Triggers.ActiveTrigger;
import notquests.notquests.Structs.Triggers.TriggerTypes.TriggerType;
import notquests.notquests.Structs.Triggers.TriggerTypes.WorldEnterTrigger;
import org.bukkit.Bukkit;
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
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

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


}
