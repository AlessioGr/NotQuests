package notquests.notquests.Events;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.CitizensEnableEvent;
import net.citizensnpcs.api.event.CitizensReloadEvent;
import net.citizensnpcs.api.event.NPCDeathEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.trait.FollowTrait;
import notquests.notquests.NotQuests;
import notquests.notquests.QuestGiverNPCTrait;
import notquests.notquests.Structs.ActiveObjective;
import notquests.notquests.Structs.ActiveQuest;
import notquests.notquests.Structs.Objectives.DeliverItemsObjective;
import notquests.notquests.Structs.Objectives.EscortNPCObjective;
import notquests.notquests.Structs.Objectives.TalkToNPCObjective;
import notquests.notquests.Structs.QuestPlayer;
import notquests.notquests.Structs.Triggers.ActiveTrigger;
import notquests.notquests.Structs.Triggers.TriggerTypes.NPCDeathTrigger;
import notquests.notquests.Structs.Triggers.TriggerTypes.TriggerType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.logging.Level;

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
                        if (activeTrigger.getTrigger().getTriggerType() == TriggerType.NPCDEATH) {
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
            if (questPlayer.getActiveQuests().size() > 0) {
                for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                    for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                        if (activeObjective.isUnlocked()) {
                            if (activeObjective.getObjective() instanceof final DeliverItemsObjective objective) {
                                if (objective.getRecipientNPCID() == npc.getId()) {
                                    for (final ItemStack itemStack : player.getInventory().getContents()) {
                                        if (itemStack != null) {
                                            if (objective.getItemToDeliver().getType().equals(itemStack.getType()) && objective.getItemToDeliver().getItemMeta().equals(itemStack.getItemMeta())) {

                                                final long progressLeft = activeObjective.getProgressNeeded() - activeObjective.getCurrentProgress();
                                                //player.sendMessage("§6Progress Needed: §b" + activeObjective.getProgressNeeded());
                                                //player.sendMessage("§6Progress left: §b" + progressLeft);
                                                //player.sendMessage("§6ItemStack amount: §b" + itemStack.getAmount());
                                                if (progressLeft < itemStack.getAmount()) {
                                                    //player.sendMessage("§6new amount: " + (itemStack.getAmount() - (int)progressLeft));
                                                    itemStack.setAmount((itemStack.getAmount() - (int) progressLeft));
                                                    activeObjective.addProgress(progressLeft, npc.getId());
                                                    player.sendMessage("§aYou have delivered §b" + progressLeft + " §aitems to §b" + npc.getName());
                                                    break;
                                                } else {
                                                    player.getInventory().removeItem(itemStack);
                                                    activeObjective.addProgress(itemStack.getAmount(), npc.getId());
                                                    player.sendMessage("§aYou have delivered §b" + itemStack.getAmount() + " §aitems to §b" + npc.getName());
                                                }
                                            }
                                        }

                                    }

                                }
                            } else if (activeObjective.getObjective() instanceof final TalkToNPCObjective objective) {
                                if (objective.getNPCtoTalkID() == npc.getId()) {
                                    activeObjective.addProgress(1, npc.getId());
                                    player.sendMessage("§aYou talked to §b" + npc.getName());

                                }
                            } else if (activeObjective.getObjective() instanceof final EscortNPCObjective objective) {
                                if (objective.getNpcToEscortToID() == npc.getId()) {
                                    final NPC npcToEscort = CitizensAPI.getNPCRegistry().getById(objective.getNpcToEscortID());
                                    if (npcToEscort != null) {
                                        if (npcToEscort.isSpawned() && (npcToEscort.getEntity().getLocation().distance(player.getLocation()) < 6)) {
                                            activeObjective.addProgress(1, npc.getId());
                                            player.sendMessage("§aYou have successfully delivered the NPC §b" + npcToEscort.getName());

                                            FollowTrait followerTrait = null;
                                            for (final Trait trait : npcToEscort.getTraits()) {
                                                if (trait.getName().toLowerCase().contains("follow")) {
                                                    followerTrait = (FollowTrait) trait;
                                                }
                                            }
                                            if (followerTrait != null) {
                                                npc.removeTrait(followerTrait.getClass());
                                            }

                                            npcToEscort.despawn();
                                        } else {
                                            player.sendMessage("§cThe NPC you have to escort is not close enough to you!");
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
        main.getLogger().log(Level.INFO, "§aNotQuests > Processing Citizens Enable Event...");
        main.getLogger().log(Level.INFO, "§aNotQuests > Registering Citizens nquestgiver trait...");

        final ArrayList<TraitInfo> toDeregister = new ArrayList<>();
        for (final TraitInfo traitInfo : net.citizensnpcs.api.CitizensAPI.getTraitFactory().getRegisteredTraits()) {
            if (traitInfo.getTraitName().equals("nquestgiver")) {
                toDeregister.add(traitInfo);

            }
        }
        for (final TraitInfo traitInfo : toDeregister) {
            net.citizensnpcs.api.CitizensAPI.getTraitFactory().deregisterTrait(traitInfo);
        }
        net.citizensnpcs.api.CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(QuestGiverNPCTrait.class).withName("nquestgiver"));

        main.getLogger().log(Level.INFO, "§aNotQuests > Citizens nquestgiver trait has been registered!");
        if (!main.getDataManager().isAlreadyLoadedNPCs()) {
            main.getDataManager().loadNPCData();
        }


    }

    @EventHandler
    private void onCitizensReload(CitizensReloadEvent e) {
        main.getLogger().log(Level.INFO, "§aNotQuests > Processing Citizens Reload Event...");
        main.getLogger().log(Level.INFO, "§aNotQuests > Registering Citizens nquestgiver trait...");

        final ArrayList<TraitInfo> toDeregister = new ArrayList<>();
        for (final TraitInfo traitInfo : net.citizensnpcs.api.CitizensAPI.getTraitFactory().getRegisteredTraits()) {
            if (traitInfo.getTraitName().equals("nquestgiver")) {
                toDeregister.add(traitInfo);

            }
        }
        for (final TraitInfo traitInfo : toDeregister) {
            net.citizensnpcs.api.CitizensAPI.getTraitFactory().deregisterTrait(traitInfo);
        }
        net.citizensnpcs.api.CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(QuestGiverNPCTrait.class).withName("nquestgiver"));
        main.getLogger().log(Level.INFO, "§aNotQuests > Citizens nquestgiver trait has been registered!");
        if (!main.getDataManager().isAlreadyLoadedNPCs()) {
            main.getDataManager().loadNPCData();
        }

    }
}
