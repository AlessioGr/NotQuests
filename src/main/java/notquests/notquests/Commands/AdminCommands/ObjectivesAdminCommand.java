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

package notquests.notquests.Commands.AdminCommands;


import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.audience.Audience;
import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Objectives.*;
import notquests.notquests.Structs.Objectives.hooks.KillEliteMobsObjective;
import notquests.notquests.Structs.Quest;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ObjectivesAdminCommand {

    private final NotQuests main;


    public ObjectivesAdminCommand(final NotQuests main) {
        this.main = main;
    }


    public void handleObjectivesAdminCommand(final CommandSender sender, final String[] args, final Quest quest) {
        if (args.length == 3) {
            showUsage(quest, sender, args);
        } else if (args.length == 4) {
            if (args[3].equalsIgnoreCase("add")) {
                showUsage(quest, sender, args);
            } else if (args[3].equalsIgnoreCase("edit")) {
                showUsage(quest, sender, args);
            } else if (args[3].equalsIgnoreCase("list")) {
                sender.sendMessage("§9Objectives for quest §b" + quest.getQuestName() + "§9:");

                main.getQuestManager().sendObjectivesAdmin(sender, quest);

            } else if (args[3].equalsIgnoreCase("clear")) {
                quest.removeAllObjectives();
                sender.sendMessage("§aAll objectives of quest §b" + quest.getQuestName() + " §ahave been removed!");
            } else {
                showUsage(quest, sender, args);
            }
        } else if (args.length >= 5 && args[3].equalsIgnoreCase("add") && args[4].equalsIgnoreCase("KillEliteMobs")) {
            handleCommandsKillEliteMobsObjective(sender, args, quest);
        } else if (args.length == 5) {
            showUsage(quest, sender, args);

        } else if (args.length == 6) {
            if (args[3].equalsIgnoreCase("edit") && args[5].equalsIgnoreCase("info")) {
                final int objectiveID = Integer.parseInt(args[4]);
                final Objective objective = quest.getObjectiveFromID(objectiveID);
                if (objective != null) {
                    sender.sendMessage("§eInformation of objective with the ID §b" + objectiveID + " §efrom quest §b" + quest.getQuestName() + "§e:");
                    sender.sendMessage("§aObjective Type: §b" + objective.getObjectiveType().toString());
                    sender.sendMessage("§aObjective Content: ");

                    sender.sendMessage(main.getQuestManager().getObjectiveTaskDescription(objective, false));


                    sender.sendMessage("§aObjective DisplayName: §b" + objective.getObjectiveDisplayName());
                    sender.sendMessage("§aObjective Description: §b" + objective.getObjectiveDescription());
                    sender.sendMessage("§aObjective Dependencies:");
                    int counter = 1;
                    for (final Objective dependantObjective : objective.getDependantObjectives()) {
                        sender.sendMessage("    §e" + counter + ". Type: §f" + dependantObjective.getObjectiveType() + " §eQuest Name: §f" + quest.getQuestName() + " §eID: §f" + dependantObjective.getObjectiveID());
                        counter++;
                    }

                } else {
                    sender.sendMessage("§cError: Objective with the ID §b" + objectiveID + " §cwas not found for quest §b" + quest.getQuestName() + "§c!");
                }
            } else if (args[3].equalsIgnoreCase("edit") && (args[5].equalsIgnoreCase("remove") || args[5].equalsIgnoreCase("delete"))) {
                final int objectiveID = Integer.parseInt(args[4]);
                final Objective objective = quest.getObjectiveFromID(objectiveID);
                if (objective != null) {
                    quest.removeObjective(objectiveID - 1);
                    sender.sendMessage("§aObjective with the ID §b" + objectiveID + " §ahas successfully been removed from Quest §b" + quest.getQuestName());
                } else {
                    sender.sendMessage("§cError: Objective with the ID §b" + objectiveID + " §cwas not found for quest §b" + quest.getQuestName() + "§c!");
                }
            } else if (args[3].equalsIgnoreCase("add")) {
                if (args[4].equalsIgnoreCase("TalkToNPC")) {
                    if (!args[5].equalsIgnoreCase("armorstand")) {
                        if (!main.isCitizensEnabled()) {
                            sender.sendMessage("§cError: Any kind of NPC stuff has been disabled, because you don't have the Citizens plugin installed on your server. You need to install the Citizens plugin in order to use Citizen NPCs. You can, however, use armor stands as an alternative. To do that, just enter 'armorstand' instead of the NPC ID.");
                            return;
                        }
                        final int NPCID = Integer.parseInt(args[5]);
                        final NPC npc = CitizensAPI.getNPCRegistry().getById(NPCID);
                        if (npc != null) {


                            TalkToNPCObjective talkToNPCObjective = new TalkToNPCObjective(main, quest, quest.getObjectives().size() + 1, NPCID);
                            quest.addObjective(talkToNPCObjective, true);
                            sender.sendMessage("§aObjective successfully added to quest §b" + quest.getQuestName() + "§a!");


                        } else {
                            sender.sendMessage("§cError: NPC with the ID §b" + NPCID + " §cwas not found!");
                        }
                    } else { //Armor Stands
                        if (sender instanceof Player player) {
                            ItemStack itemStack = new ItemStack(Material.PAPER, 1);
                            //give a specialitem. clicking an armorstand with that special item will remove the pdb.

                            NamespacedKey key = new NamespacedKey(main, "notquests-item");
                            NamespacedKey QuestNameKey = new NamespacedKey(main, "notquests-questname");

                            ItemMeta itemMeta = itemStack.getItemMeta();
                            //Only paper List<Component> lore = new ArrayList<>();
                            List<String> lore = new ArrayList<>();

                            assert itemMeta != null;

                            itemMeta.getPersistentDataContainer().set(QuestNameKey, PersistentDataType.STRING, quest.getQuestName());
                            itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 5);

                            //Only paper itemMeta.displayName(Component.text("§dCheck Armor Stand", NamedTextColor.LIGHT_PURPLE));
                            itemMeta.setDisplayName("§dAdd TalkToNPC Objective to Armor Stand");
                            //Only paper lore.add(Component.text("§fRight-click an Armor Stand to see which Quests are attached to it."));
                            lore.add("§fRight-click an Armor Stand to add the following objective to it:");
                            lore.add("§eTalkToNPC §fObjective of Quest §b" + quest.getQuestName() + "§f.");

                            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                            //Only paper itemMeta.lore(lore);

                            itemMeta.setLore(lore);
                            itemStack.setItemMeta(itemMeta);

                            player.getInventory().addItem(itemStack);

                            player.sendMessage("§aYou have been given an item with which you can add the TalkToNPC Objective to an armor stand. Check your inventory!");


                        } else {
                            sender.sendMessage("§cMust be a player!");
                            showUsage(quest, sender, args);
                        }
                    }

                } else {
                    showUsage(quest, sender, args);
                }
            } else {
                showUsage(quest, sender, args);
            }

        } else if (args.length == 7) {
            showUsage(quest, sender, args);
            if (args[3].equalsIgnoreCase("add")) {
                if (args[4].equalsIgnoreCase("EscortNPC")) {
                    if(!main.isCitizensEnabled()){
                        sender.sendMessage("§cError: Any kind of NPC stuff has been disabled, because you don't have the Citizens plugin installed on your server. You need to install the Citizens plugin in order for NPC stuff to work.");
                        return;
                    }
                    final int NPCID = Integer.parseInt(args[5]);
                    final int NPCDestinationID = Integer.parseInt(args[6]);
                    final NPC npc = CitizensAPI.getNPCRegistry().getById(NPCID);
                    final NPC destinationNPC = CitizensAPI.getNPCRegistry().getById(NPCDestinationID);
                    if (npc != null) {
                        if (destinationNPC != null) {
                            EscortNPCObjective escortNPCObjective = new EscortNPCObjective(main, quest, quest.getObjectives().size() + 1, NPCID, NPCDestinationID);
                            quest.addObjective(escortNPCObjective, true);
                            sender.sendMessage("§aObjective successfully added to quest §b" + quest.getQuestName() + "§a!");
                        } else {
                            sender.sendMessage("§cError: Destination NPC with the ID §b" + NPCDestinationID + " §cwas not found!");
                        }

                    } else {
                        sender.sendMessage("§cError: NPC to escort with the ID §b" + NPCID + " §cwas not found!");
                    }
                } else if (args[4].equalsIgnoreCase("TriggerCommand")) {

                    String triggerName = args[5];
                    int amountToTrigger = Integer.parseInt(args[6]);
                    TriggerCommandObjective triggerCommandObjective = new TriggerCommandObjective(main, quest, quest.getObjectives().size() + 1, triggerName, amountToTrigger);
                    quest.addObjective(triggerCommandObjective, true);
                    sender.sendMessage("§aObjective successfully added to quest §b" + quest.getQuestName() + "§a!");

                } else if (args[4].equalsIgnoreCase("CollectItems")) {
                    if (args[5].equalsIgnoreCase("hand")) {
                        if (sender instanceof Player player) {
                            ItemStack holdingItem = player.getInventory().getItemInMainHand();
                            int amountToCollect = Integer.parseInt(args[6]);

                            CollectItemsObjective collectItemsObjective = new CollectItemsObjective(main, quest, quest.getObjectives().size() + 1, holdingItem, amountToCollect);
                            quest.addObjective(collectItemsObjective, true);
                            sender.sendMessage("§aObjective successfully added to quest §b" + quest.getQuestName() + "§a!");

                        } else {
                            sender.sendMessage("§cThis command can only be run as a player.");
                        }
                    } else {
                        Material itemMaterial = Material.getMaterial(args[5]);
                        if (itemMaterial != null) {
                            ItemStack itemStack = new ItemStack(itemMaterial, 1);
                            int amountToCollect = Integer.parseInt(args[6]);


                            CollectItemsObjective collectItemsObjective = new CollectItemsObjective(main, quest, quest.getObjectives().size() + 1, itemStack, amountToCollect);
                            quest.addObjective(collectItemsObjective, true);
                            sender.sendMessage("§aObjective successfully added to quest §b" + quest.getQuestName() + "§a!");


                        } else {
                            sender.sendMessage("§cItem §b" + args[5] + " §cnot found!");
                        }
                    }


                }else if (args[4].equalsIgnoreCase("CraftItems")) {
                    if (args[5].equalsIgnoreCase("hand")) {
                        if (sender instanceof Player player) {
                            ItemStack holdingItem = player.getInventory().getItemInMainHand();
                            int amountToCollect = Integer.parseInt(args[6]);

                            CraftItemsObjective craftItemsObjective = new CraftItemsObjective(main, quest, quest.getObjectives().size() + 1, holdingItem, amountToCollect);
                            quest.addObjective(craftItemsObjective, true);
                            sender.sendMessage("§aObjective successfully added to quest §b" + quest.getQuestName() + "§a!");

                        } else {
                            sender.sendMessage("§cThis command can only be run as a player.");
                        }
                    } else {
                        Material itemMaterial = Material.getMaterial(args[5]);
                        if (itemMaterial != null) {
                            ItemStack itemStack = new ItemStack(itemMaterial, 1);
                            int amountToCollect = Integer.parseInt(args[6]);


                            CraftItemsObjective craftItemsObjective = new CraftItemsObjective(main, quest, quest.getObjectives().size() + 1, itemStack, amountToCollect);
                            quest.addObjective(craftItemsObjective, true);
                            sender.sendMessage("§aObjective successfully added to quest §b" + quest.getQuestName() + "§a!");


                        } else {
                            sender.sendMessage("§cItem §b" + args[5] + " §cnot found!");
                        }
                    }


                } else if (args[4].equalsIgnoreCase("KillMobs")) {
                    final String mobEntityType = args[5];

                    boolean foundValidMob = false;
                    for (final String validMob : main.getDataManager().standardEntityTypeCompletions) {
                        if (validMob.toLowerCase(Locale.ROOT).equalsIgnoreCase(mobEntityType.toLowerCase(Locale.ROOT))) {
                            foundValidMob = true;
                        }
                    }
                    if (!foundValidMob) {
                        sender.sendMessage("§cError: the mob type §b" + mobEntityType + " §cwas not found!");
                        return;
                    }

                    int amountToKill = Integer.parseInt(args[6]);


                    KillMobsObjective killMobsObjective = new KillMobsObjective(main, quest, quest.getObjectives().size() + 1, mobEntityType, amountToKill);
                    quest.addObjective(killMobsObjective, true);
                    sender.sendMessage("§aObjective successfully added to quest §b" + quest.getQuestName() + "§a!");


                } else if (args[4].equalsIgnoreCase("ConsumeItems")) {
                    if (args[5].equalsIgnoreCase("hand")) {
                        if (sender instanceof Player player) {
                            ItemStack holdingItem = player.getInventory().getItemInMainHand();
                            int amountToConsume = Integer.parseInt(args[6]);

                            ConsumeItemsObjective ConsumeItemsObjective = new ConsumeItemsObjective(main, quest, quest.getObjectives().size() + 1, holdingItem, amountToConsume);
                            quest.addObjective(ConsumeItemsObjective, true);
                            sender.sendMessage("§aObjective successfully added to quest §b" + quest.getQuestName() + "§a!");

                        } else {
                            sender.sendMessage("§cThis command can only be run as a player.");
                        }
                    } else {
                        Material itemMaterial = Material.getMaterial(args[5]);
                        if (itemMaterial != null) {
                            ItemStack itemStack = new ItemStack(itemMaterial, 1);
                            int amountToConsume = Integer.parseInt(args[6]);


                            ConsumeItemsObjective ConsumeItemsObjective = new ConsumeItemsObjective(main, quest, quest.getObjectives().size() + 1, itemStack, amountToConsume);
                            quest.addObjective(ConsumeItemsObjective, true);
                            sender.sendMessage("§aObjective successfully added to quest §b" + quest.getQuestName() + "§a!");


                        } else {
                            sender.sendMessage("§cItem §b" + args[5] + " §cnot found!");
                        }
                    }
                } else {
                    sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage"));
                }
            } else if (args[3].equalsIgnoreCase("edit") && args[5].equalsIgnoreCase("description")) {
                if (args[6].equalsIgnoreCase("show")) {
                    final int objectiveID = Integer.parseInt(args[4]);
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    if (objective != null) {
                        sender.sendMessage("§aCurrent description of objective with ID §b" + objectiveID + "§a: §e" + objective.getObjectiveDescription());
                    } else {
                        sender.sendMessage("§cError: Objective with the ID §b" + objectiveID + " §c was not found for quest §b" + quest.getQuestName() + "§c!");
                    }
                } else if (args[6].equalsIgnoreCase("remove")) {
                    final int objectiveID = Integer.parseInt(args[4]);
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    if (objective != null) {
                        objective.removeObjectiveDescription(true);
                        sender.sendMessage("§aDescription successfully removed from objective with ID §b" + objectiveID + "§a! New description: §e" + objective.getObjectiveDescription());
                    } else {
                        sender.sendMessage("§cError: Objective with the ID §b" + objectiveID + " §c was not found for quest §b" + quest.getQuestName() + "§c!");
                    }
                }

            } else if (args[3].equalsIgnoreCase("edit") && args[5].equalsIgnoreCase("displayName")) {
                if (args[6].equalsIgnoreCase("show")) {
                    final int objectiveID = Integer.parseInt(args[4]);
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    if (objective != null) {
                        sender.sendMessage("§aCurrent display name of objective with ID §b" + objectiveID + "§a: §e" + objective.getObjectiveDisplayName());
                    } else {
                        sender.sendMessage("§cError: Objective with the ID §b" + objectiveID + " §c was not found for quest §b" + quest.getQuestName() + "§c!");
                    }
                } else if (args[6].equalsIgnoreCase("remove")) {
                    final int objectiveID = Integer.parseInt(args[4]);
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    if (objective != null) {
                        objective.removeObjectiveDisplayName(true);
                        sender.sendMessage("§aDisplay Name successfully removed from objective with ID §b" + objectiveID + "§a! New description: §e" + objective.getObjectiveDisplayName());
                    } else {
                        sender.sendMessage("§cError: Objective with the ID §b" + objectiveID + " §c was not found for quest §b" + quest.getQuestName() + "§c!");
                    }
                }
            } else if (args[3].equalsIgnoreCase("edit") && args[5].equalsIgnoreCase("dependencies")) {
                if (args[6].equalsIgnoreCase("list")) {
                    final int objectiveID = Integer.parseInt(args[4]);
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    if (objective != null) {
                        sender.sendMessage("§aDepending objectives of objective with ID §b" + objectiveID + "§a §f(What needs to be completed BEFORE this objective can be started):");
                        int counter = 1;
                        for (final Objective dependantObjective : objective.getDependantObjectives()) {
                            sender.sendMessage("§e" + counter + ". Objective ID: §b" + dependantObjective.getObjectiveID());
                            counter++;
                        }
                        if (counter == 1) {
                            sender.sendMessage("§eNo depending objectives found!");
                        }
                        sender.sendMessage("§7------");


                        sender.sendMessage("§aObjectives where this objective with ID §b" + objectiveID + "§a is a dependant on §f(What can only be started AFTER this objective is completed):");
                        int counter2 = 1;
                        for (final Objective otherObjective : quest.getObjectives()) {
                            if (otherObjective.getDependantObjectives().contains(objective)) {
                                sender.sendMessage("§e" + counter2 + ". Objective ID: §b" + otherObjective.getObjectiveID());
                                counter2++;
                            }
                        }
                        if (counter2 == 1) {
                            sender.sendMessage("§eNo objectives where this objective is a dependant of found!");
                        }
                    } else {
                        sender.sendMessage("§cError: Objective with the ID §b" + objectiveID + " §cwas not found for quest §b" + quest.getQuestName() + "§c!");
                    }
                } else if (args[6].equalsIgnoreCase("clear")) {
                    final int objectiveID = Integer.parseInt(args[4]);
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    if (objective != null) {
                        objective.clearDependantObjectives();
                        sender.sendMessage("§aAll depending objectives of objective with ID §b" + objectiveID + " §ahave been removed!");

                    } else {
                        sender.sendMessage("§cError: Objective with the ID §b" + objectiveID + " §cwas not found for quest §b" + quest.getQuestName() + "§c!");
                    }
                }
            } else if (args[3].equalsIgnoreCase("edit") && args[5].equalsIgnoreCase("completionNPC")) {
                if (args[6].equalsIgnoreCase("show") || args[6].equalsIgnoreCase("view")) {

                    final int objectiveID = Integer.parseInt(args[4]);
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    if (objective != null) {

                        sender.sendMessage("§aThe completionNPCID of the objective with the ID §b" + objectiveID + " §ais §b" + objective.getCompletionNPCID() + "§a!");
                        if (objective.getCompletionArmorStandUUID() != null) {
                            sender.sendMessage("§aThe completionNPCUUID (for armor stands) of the objective with the ID §b" + objectiveID + " §ais §b" + objective.getCompletionArmorStandUUID() + "§a!");
                        } else {
                            sender.sendMessage("§aThe completionNPCUUID (for armor stands) of the objective with the ID §b" + objectiveID + " §ais §bnull§a!");
                        }


                    } else {
                        sender.sendMessage("§cError: Objective with the ID §b" + objectiveID + " §cwas not found for quest §b" + quest.getQuestName() + "§c!");
                    }
                }

            } else {
                sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage"));
            }
        } else if (args.length >= 8 && args[3].equalsIgnoreCase("edit") && (args[5].equalsIgnoreCase("displayName") || args[5].equalsIgnoreCase("description")) && args[6].equalsIgnoreCase("set")) {
            final int objectiveID = Integer.parseInt(args[4]);
            final Objective objective = quest.getObjectiveFromID(objectiveID);
            if (objective != null) {

                StringBuilder descriptionOrDisplayName = new StringBuilder();
                for (int start = 7; start < args.length; start++) {
                    descriptionOrDisplayName.append(args[start]);
                    if (start < args.length - 1) {
                        descriptionOrDisplayName.append(" ");
                    }
                }

                if (args[5].equalsIgnoreCase("displayName")) {
                    objective.setObjectiveDisplayName(descriptionOrDisplayName.toString(), true);
                    sender.sendMessage("§aDisplay Name successfully added to objective with ID §b" + objectiveID + "§a! New description: §e" + objective.getObjectiveDisplayName());
                } else if (args[5].equalsIgnoreCase("description")) {
                    objective.setObjectiveDescription(descriptionOrDisplayName.toString(), true);
                    sender.sendMessage("§aDescription successfully added to objective with ID §b" + objectiveID + "§a! New description: §e" + objective.getObjectiveDescription());
                } else {
                    sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage"));
                }


            } else {
                sender.sendMessage("§cError: Objective with the ID §b" + objectiveID + " §c was not found for quest §b" + quest.getQuestName() + "§c!");
            }
        } else if (args.length == 8) {
            if (args[3].equalsIgnoreCase("add")) {
                if (args[4].equalsIgnoreCase("OtherQuest")) {


                    String otherQuestName = args[5];

                    Quest otherQuest = main.getQuestManager().getQuest(otherQuestName);

                    if (otherQuest != null) {
                        int amountOfCompletionsNeeded = Integer.parseInt(args[6]);

                        boolean countPreviouslyCompletedQuests = true;
                        boolean didntSpecify = true;
                        if (args[7].equalsIgnoreCase("yes") || args[7].equalsIgnoreCase("true")) {
                            didntSpecify = false;
                        } else if (args[7].equalsIgnoreCase("no") || args[7].equalsIgnoreCase("false")) {
                            countPreviouslyCompletedQuests = false;
                            didntSpecify = false;
                        }

                        if (amountOfCompletionsNeeded <= 0) {
                            amountOfCompletionsNeeded = 1;
                        }

                        if (!didntSpecify) {
                            OtherQuestObjective otherQuestObjective = new OtherQuestObjective(main, quest, quest.getObjectives().size() + 1, otherQuest.getQuestName(), amountOfCompletionsNeeded, countPreviouslyCompletedQuests);
                            quest.addObjective(otherQuestObjective, true);
                            sender.sendMessage("§aObjective successfully added to quest §b" + quest.getQuestName() + "§a!");
                        } else {
                            sender.sendMessage("§cWrong last argument. Specify §bYes §cor §b No");
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2OtherQuest §3[Other Quest Name] §3[amount of completions needed] [countPreviouslyCompletedQuests?: yes/no]");
                        }


                    } else {
                        sender.sendMessage("§cQuest §b" + otherQuestName + " §cdoes not exist");
                    }

                } else if (args[4].equalsIgnoreCase("BreakBlocks")) {
                    Material block = Material.getMaterial(args[5]);
                    if (block != null) {
                        int amountToBreak = Integer.parseInt(args[6]);

                        boolean deductifblockisplaced = true;
                        boolean didntspecify = true;
                        if (args[7].equalsIgnoreCase("yes") || args[7].equalsIgnoreCase("true")) {
                            didntspecify = false;
                        } else if (args[7].equalsIgnoreCase("no") || args[7].equalsIgnoreCase("false")) {
                            deductifblockisplaced = false;
                            didntspecify = false;
                        }
                        if (!didntspecify) {
                            BreakBlocksObjective breakBlocksObjective = new BreakBlocksObjective(main, quest, quest.getObjectives().size() + 1, block, amountToBreak, deductifblockisplaced);
                            quest.addObjective(breakBlocksObjective, true);
                            sender.sendMessage("§aObjective successfully added to quest §b" + quest.getQuestName() + "§a!");
                        } else {
                            sender.sendMessage("§cWrong last argument. Specify §bYes §cor §b No");
                        }

                    } else {
                        sender.sendMessage("§cBlock §b" + args[5] + " §cnot found!");
                    }


                } else if (args[4].equalsIgnoreCase("DeliverItems")) {

                    if (!args[7].equalsIgnoreCase("armorstand")) {
                        if (!main.isCitizensEnabled()) {
                            sender.sendMessage("§cError: Any kind of NPC stuff has been disabled, because you don't have the Citizens plugin installed on your server. You need to install the Citizens plugin in order for NPC stuff to work.");
                            return;
                        }
                        final int NPCID = Integer.parseInt(args[7]);
                        final NPC npc = CitizensAPI.getNPCRegistry().getById(NPCID);
                        if (npc != null) {
                            if (args[5].equalsIgnoreCase("hand")) {
                                if (sender instanceof Player player) {
                                    ItemStack holdingItem = player.getInventory().getItemInMainHand();
                                    int amountToDeliver = Integer.parseInt(args[6]);

                                    DeliverItemsObjective deliverItemsObjective = new DeliverItemsObjective(main, quest, quest.getObjectives().size() + 1, holdingItem, amountToDeliver, NPCID);
                                    quest.addObjective(deliverItemsObjective, true);
                                    sender.sendMessage("§aObjective successfully added to quest §b" + quest.getQuestName() + "§a!");

                                } else {
                                    sender.sendMessage("§cThis command can only be run as a player.");
                                }
                            } else {
                                Material itemMaterial = Material.getMaterial(args[5]);
                                if (itemMaterial != null) {
                                    ItemStack itemStack = new ItemStack(itemMaterial, 1);
                                    int amountToDeliver = Integer.parseInt(args[6]);


                                    DeliverItemsObjective deliverItemsObjective = new DeliverItemsObjective(main, quest, quest.getObjectives().size() + 1, itemStack, amountToDeliver, NPCID);
                                    quest.addObjective(deliverItemsObjective, true);
                                    sender.sendMessage("§aObjective successfully added to quest §b" + quest.getQuestName() + "§a!");


                                } else {
                                    sender.sendMessage("§cItem §b" + args[5] + " §cnot found!");
                                }
                            }


                        } else {
                            sender.sendMessage("§cError: NPC with the ID §b" + NPCID + " §cwas not found!");
                        }
                    } else { //Armor Stands
                        if (sender instanceof Player player) {

                            final ItemStack itemToDeliver;
                            int amountToDeliver = Integer.parseInt(args[6]);
                            if (args[5].equalsIgnoreCase("hand")) {
                                itemToDeliver = player.getInventory().getItemInMainHand();
                            } else {
                                final Material itemMaterial = Material.getMaterial(args[5]);
                                if (itemMaterial != null) {
                                    itemToDeliver = new ItemStack(itemMaterial, 1);
                                } else {
                                    sender.sendMessage("§cItem §b" + args[5] + " §cnot found!");
                                    return;
                                }
                            }

                            Random rand = new Random();
                            int randomNum = rand.nextInt((1000000 - 1) + 1) + 1;

                            main.getDataManager().getItemStackCache().put(randomNum, itemToDeliver);


                            ItemStack itemStack = new ItemStack(Material.PAPER, 1);
                            //give a specialitem. clicking an armorstand with that special item will remove the pdb.

                            NamespacedKey key = new NamespacedKey(main, "notquests-item");
                            NamespacedKey QuestNameKey = new NamespacedKey(main, "notquests-questname");

                            NamespacedKey ItemStackKey = new NamespacedKey(main, "notquests-itemstackcache");
                            NamespacedKey ItemStackAmountKey = new NamespacedKey(main, "notquests-itemstackamount");

                            ItemMeta itemMeta = itemStack.getItemMeta();
                            //Only paper List<Component> lore = new ArrayList<>();
                            List<String> lore = new ArrayList<>();

                            assert itemMeta != null;

                            itemMeta.getPersistentDataContainer().set(QuestNameKey, PersistentDataType.STRING, quest.getQuestName());
                            itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 7);


                            itemMeta.getPersistentDataContainer().set(ItemStackKey, PersistentDataType.INTEGER, randomNum);
                            itemMeta.getPersistentDataContainer().set(ItemStackAmountKey, PersistentDataType.INTEGER, amountToDeliver);


                            //Only paper itemMeta.displayName(Component.text("§dCheck Armor Stand", NamedTextColor.LIGHT_PURPLE));
                            itemMeta.setDisplayName("§dAdd DeliverItems Objective to Armor Stand");
                            //Only paper lore.add(Component.text("§fRight-click an Armor Stand to see which Quests are attached to it."));
                            lore.add("§fRight-click an Armor Stand to add the following objective to it:");
                            lore.add("§eDeliverItems §fObjective of Quest §b" + quest.getQuestName() + "§f.");

                            itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                            //Only paper itemMeta.lore(lore);

                            itemMeta.setLore(lore);
                            itemStack.setItemMeta(itemMeta);

                            player.getInventory().addItem(itemStack);

                            player.sendMessage("§aYou have been given an item with which you can add the DeliverItems Objective to an armor stand. Check your inventory!");


                        } else {
                            sender.sendMessage("§cMust be a player!");
                            showUsage(quest, sender, args);
                        }
                    }


                } else {
                    sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage"));
                }


            } else if (args[3].equalsIgnoreCase("edit") && args[5].equalsIgnoreCase("dependencies")) {
                if (args[6].equalsIgnoreCase("add")) {
                    final int objectiveID = Integer.parseInt(args[4]);
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    if (objective != null) {

                        final int dependingObjectiveID = Integer.parseInt(args[7]);
                        final Objective dependingObjective = quest.getObjectiveFromID(dependingObjectiveID);
                        if (dependingObjective != null) {
                            if (dependingObjective != objective) {
                                objective.addDependantObjective(dependingObjective, true);
                                sender.sendMessage("§aThe objective with the ID §b" + dependingObjectiveID + " §ahas been added as a dependency to the objective with the ID §b" + objectiveID + "§a!");
                            } else {
                                sender.sendMessage("§cError: You cannot set an objective to depend on itself!");
                            }

                        } else {
                            sender.sendMessage("§cError: Objective with the ID §b" + dependingObjectiveID + " §c(which should be added as a dependency) was not found in quest §b" + quest.getQuestName() + "§c!");

                        }

                    } else {
                        sender.sendMessage("§cError: Objective with the ID §b" + objectiveID + " §cwas not found for quest §b" + quest.getQuestName() + "§c!");
                    }
                } else if (args[6].equalsIgnoreCase("remove") || args[6].equalsIgnoreCase("delete")) {
                    final int objectiveID = Integer.parseInt(args[4]);
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    if (objective != null) {

                        final int dependingObjectiveID = Integer.parseInt(args[7]);
                        final Objective dependingObjective = quest.getObjectiveFromID(dependingObjectiveID);
                        if (dependingObjective != null) {
                            objective.removeDependantObjective(dependingObjective, true);
                            sender.sendMessage("§aThe objective with the ID §b" + dependingObjectiveID + " §ahas been removed as a dependency from the objective with the ID §b" + objectiveID + "§a!");

                        } else {
                            sender.sendMessage("§cError: Objective with the ID §b" + dependingObjectiveID + " §c(which should be added as a dependency) was not found in quest §b" + quest.getQuestName() + "§c!");

                        }

                    } else {
                        sender.sendMessage("§cError: Objective with the ID §b" + objectiveID + " §cwas not found for quest §b" + quest.getQuestName() + "§c!");
                    }
                }
            } else if (args[3].equalsIgnoreCase("edit") && args[5].equalsIgnoreCase("completionNPC")) {
                if (args[6].equalsIgnoreCase("set")) {

                    final int objectiveID = Integer.parseInt(args[4]);
                    final Objective objective = quest.getObjectiveFromID(objectiveID);
                    if (objective != null) {

                        if (args[7].equalsIgnoreCase("-1")) {

                            objective.setCompletionNPCID(-1, true);
                            objective.setCompletionArmorStandUUID(null, true);
                            sender.sendMessage("§aThe completionNPC of the objective with the ID §b" + objectiveID + " §ahas been removed!");

                        } else if (!args[7].equalsIgnoreCase("armorstand")) {
                            final int completionNPCID = Integer.parseInt(args[7]);

                            objective.setCompletionNPCID(completionNPCID, true);
                            sender.sendMessage("§aThe completionNPCID of the objective with the ID §b" + objectiveID + " §ahas been set to the NPC with the ID §b" + completionNPCID + "§a!");

                        } else { //Armor Stands

                            if (sender instanceof Player player) {
                                ItemStack itemStack = new ItemStack(Material.PAPER, 1);
                                //give a specialitem. clicking an armorstand with that special item will remove the pdb.

                                NamespacedKey key = new NamespacedKey(main, "notquests-item");
                                NamespacedKey QuestNameKey = new NamespacedKey(main, "notquests-questname");
                                NamespacedKey ObjectiveIDKey = new NamespacedKey(main, "notquests-objectiveid");

                                ItemMeta itemMeta = itemStack.getItemMeta();
                                //Only paper List<Component> lore = new ArrayList<>();
                                List<String> lore = new ArrayList<>();

                                assert itemMeta != null;

                                itemMeta.getPersistentDataContainer().set(QuestNameKey, PersistentDataType.STRING, quest.getQuestName());
                                itemMeta.getPersistentDataContainer().set(ObjectiveIDKey, PersistentDataType.INTEGER, objectiveID);
                                itemMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 6);


                                //Only paper itemMeta.displayName(Component.text("§dCheck Armor Stand", NamedTextColor.LIGHT_PURPLE));
                                itemMeta.setDisplayName("§dSet completionNPC of Quest §b" + quest.getQuestName() + " §dto this Armor Stand");
                                //Only paper lore.add(Component.text("§fRight-click an Armor Stand to see which Quests are attached to it."));
                                lore.add("§fRight-click an Armor Stand to set it as the completionNPC of Quest §b" + quest.getQuestName() + " §fand ObjectiveID §b" + objectiveID + "§f.");

                                itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                                //Only paper itemMeta.lore(lore);

                                itemMeta.setLore(lore);
                                itemStack.setItemMeta(itemMeta);

                                player.getInventory().addItem(itemStack);

                                player.sendMessage("§aYou have been given an item with which you can add the completionNPC of this Objective to an armor stand. Check your inventory!");


                            } else {
                                sender.sendMessage("§cMust be a player!");
                                showUsage(quest, sender, args);
                            }


                        }


                    } else {
                        sender.sendMessage("§cError: Objective with the ID §b" + objectiveID + " §cwas not found for quest §b" + quest.getQuestName() + "§c!");
                    }
                } else {
                    showUsage(quest, sender, args);
                }

            } else {
                sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage"));
            }
        } else {

            sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage"));
            showUsage(quest, sender, args);
        }
    }




    private void showUsage(final Quest quest, final CommandSender sender, final String[] args) {
        if (args.length == 3) {
            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §3[Objective Type] ...");
            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §3[Objective ID] ...");
            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives list");
            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives clear");
        } else if (args.length == 4) {
            if (args[2].equalsIgnoreCase("add")) {
                sender.sendMessage("§cPlease specify an objective type!");
                sender.sendMessage(main.getQuestManager().getObjectiveTypesList());

                sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §3[Objective Type] ...");
            } else if (args[2].equalsIgnoreCase("edit")) {
                sender.sendMessage("§cPlease specify the Objective ID.");
                sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §3[Objective ID] ...");
            } else {
                sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage"));
                sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §3[Objective Type] ...");
                sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §3[Objective ID] ...");
                sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives list");
                sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives clear");
            }


        } else if (args.length == 5) {
            if (args[3].equalsIgnoreCase("add")) {
                if (args[4].equalsIgnoreCase("BreakBlocks")) {
                    sender.sendMessage("§cMissing 6. argument §3[Block Name]§c. Specify the §bblock§c the player has to break.");
                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2BreakBlocks §3[Block Name] [AmountToBreak] [deductIfBlockIsPlaced?: yes/no]");
                } else if (args[4].equalsIgnoreCase("CollectItems")) {
                    sender.sendMessage("§cMissing 6. argument §3[Item Name]§c. Specify the §bitem§c the player has to collect.");
                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2CollectItems §3[Item Name/hand] [Amount To Collect]");
                }else if (args[4].equalsIgnoreCase("CraftItems")) {
                    sender.sendMessage("§cMissing 6. argument §3[Item Name]§c. Specify the §bitem§c the player has to craft.");
                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2CraftItems §3[Item Name/hand] [Amount To Craft]");
                } else if (args[4].equalsIgnoreCase("TriggerCommand")) {
                    sender.sendMessage("§cMissing 6. argument §3[Trigger Name]§c. Specify the §bname§c of the trigger to complete the objective.");
                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2TriggerCommand §3[Trigger Name] [Amount To Trigger]");
                } else if (args[4].equalsIgnoreCase("OtherQuest")) {
                    sender.sendMessage("§cMissing 6. argument §3[Other Quest Name]§c. Specify the §bname§c of the other quests which needs to be completed to complete the objective.");
                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2OtherQuest §3[Other Quest Name] §3[amount of completions needed] [countPreviouslyCompletedQuests?: yes/no]");
                } else if (args[4].equalsIgnoreCase("KillMobs")) {
                    sender.sendMessage("§cMissing 6. argument §3[Mob Name]§c. Specify the §bname§c of the mob the player needs to kill to complete the objective.");
                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2KillMobs §3[Mob Name] §3[amount of kills needed]");
                } else if (args[4].equalsIgnoreCase("ConsumeItems")) {
                    sender.sendMessage("§cMissing 6. argument §3[Item Name/hand]§c. Specify the §bitem§c the player has to consume.");
                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2ConsumeItems §3[Item Name/hand] [Amount To Consume]");
                } else if (args[4].equalsIgnoreCase("DeliverItems")) {
                    sender.sendMessage("§cMissing 6. argument §3[Item Name]§c. Specify the §bitem§c the player has to deliver.");
                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2DeliverItems §3[Item Name/hand] [Amount To deliver] [Recipient NPC ID / 'armorstand']");
                } else if (args[4].equalsIgnoreCase("TalkToNPC")) {
                    sender.sendMessage("§cMissing 6. argument §3[Item Name]§c. Specify the §bID of the NPC§c who the player has to talk to.");
                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2TalkToNPC §3[Target NPC ID / armorstand]");
                } else if (args[4].equalsIgnoreCase("EscortNPC")) {
                    sender.sendMessage("§cMissing 6. argument §3[NPC to escort ID]§c. Specify the §bID of the NPC§c the player has to escort.");
                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2EscortNPC §3[NPC to escort ID] [Destination NPC ID]");
                } else {
                    sender.sendMessage("§cInvalid ObjectiveType.");
                    sender.sendMessage(main.getQuestManager().getObjectiveTypesList());
                }
            } else if (args[3].equalsIgnoreCase("edit")) {
                try {
                    final int objectiveID = Integer.parseInt(args[4]);
                    if (quest.getObjectives().size() >= objectiveID && objectiveID > 0) {
                        final Objective objective = quest.getObjectiveFromID(objectiveID);
                        if (objective != null) {
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3info §7 | Shows everything there is to know about this objective");
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3description ... §7 | Manages the description of the objective");
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3displayName ... §7 | Manages the display name of the objective");
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3dependencies ... §7 | Manage objective dependencies (objectives which need to be completed before this objective)");
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3completionNPC ... §7 | Manage completion NPC ID (-1 = default = complete automatically)");
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3remove §7 | Remove the objective from the quest");

                        } else {
                            sender.sendMessage("§cError: Objective with the ID §b" + objectiveID + " §cwas not found for quest §b" + quest.getQuestName() + "§c!");
                        }
                    } else {
                        sender.sendMessage("§cError: Objective with the ID §b" + objectiveID + " §cwas not found for quest §b" + quest.getQuestName() + "§c!");
                    }
                } catch (java.lang.NumberFormatException ex) {
                    sender.sendMessage("§cInvalid objective ID §b'" + args[4] + "'§c.");
                }


            } else {
                sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage"));
            }
        } else if (args.length == 6) {
            if (args[3].equalsIgnoreCase("add")) {
                if (args[4].equalsIgnoreCase("OtherQuest")) {
                    sender.sendMessage("§cMissing 7. argument §3[amount of completions needed]§c. Specify the §bamount of completionens§c needed for the other quests which needs to be completed to complete the objective.");
                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2OtherQuest §3[Other Quest Name] §3[amount of completions needed] [countPreviouslyCompletedQuests?: yes/no]");
                } else if (args[4].equalsIgnoreCase("TriggerCommand")) {

                    sender.sendMessage("§cMissing 7. argument §3[AmountToTrigger]§c. Specify the §bamount of times§c the trigger needs to be triggered to complete the objective.");
                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2TriggerCommand §3[Trigger Name] [AmountToTrigger]");

                } else if (args[4].equalsIgnoreCase("BreakBlocks")) {
                    sender.sendMessage("§cMissing 7. argument §3[AmountToBreak]§c. Specify the §bamount of blocks§c the player has to break.");
                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2BreakBlocks §3[Block Name] [AmountToBreak] [deductIfBlockIsPlaced?: yes/no]");

                } else if (args[4].equalsIgnoreCase("CollectItems")) {
                    sender.sendMessage("§cMissing 7. argument §3[Amount To Collect]§c. Specify the §bamount of items§c the player has to collect.");
                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2CollectItems §3[Item Name/hand] [Amount To Collect]");
                } else if (args[4].equalsIgnoreCase("CraftItems")) {
                    sender.sendMessage("§cMissing 7. argument §3[Amount To Craft]§c. Specify the §bamount of items§c the player has to craft.");
                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2CraftItems §3[Item Name/hand] [Amount To Craft]");
                } else if (args[4].equalsIgnoreCase("KillMobs")) {
                    sender.sendMessage("§cMissing 7. argument §3[amount of kills needed]§c. Specify the §bamount of times§c the player has to kill the mob.");
                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2KillMobs §3[Mob Name] §3[amount of kills needed]");
                } else if (args[4].equalsIgnoreCase("ConsumeItems")) {
                    sender.sendMessage("§cMissing 7. argument §3[Amount To Consume]§c. Specify the bamount of items§c the player has to consume.");
                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2ConsumeItems §3[Item Name/hand] [Amount To Consume]");
                } else if (args[4].equalsIgnoreCase("DeliverItems")) {
                    sender.sendMessage("§cMissing 7. argument §3[Amount To Deliver]§c. Specify the §bamount of items§c the player has to deliver.");
                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2DeliverItems §3[Item Name/hand] [Amount To Deliver] [Recipient NPC ID / 'armorstand']");

                } else if (args[4].equalsIgnoreCase("EscortNPC")) {
                    sender.sendMessage("§cMissing 7. argument §3[Destination NPC ID]§c. Specify the §bID of the NPC§c where the player has to escort the escort NPC to.");
                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2EscortNPC " + args[5] + " §3[Destination NPC ID]");
                } else {
                    sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage"));
                }
            } else if (args[3].equalsIgnoreCase("edit")) {
                final int objectiveID = Integer.parseInt(args[4]);
                final Objective objective = quest.getObjectiveFromID(objectiveID);
                if (objective != null) {
                    if (args[5].equalsIgnoreCase("description")) {
                        sender.sendMessage("§cMissing 7. argument!");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3description set <New Description> §7 | Sets new objective description");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3description remove §7 | Removes current objective description");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3description show §7 | Show current objective description");

                    } else if (args[5].equalsIgnoreCase("displayName")) {
                        sender.sendMessage("§cMissing 7. argument!");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3displayName set <New DisplayName> §7 | Sets new objective Display Name");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3displayName remove §7 | Removes current objective Display Name");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3displayName show §7 | Shows current objective Display Name");

                    } else if (args[5].equalsIgnoreCase("dependencies")) {
                        sender.sendMessage("§cMissing 7. argument!");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3dependencies add <Objective ID> §7 | Adds an objective as a dependency (needs to be completed before this one)");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3dependencies remove <Objective ID> §7 | Removes an objective from a dependency (needs to be completed before this one)");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3dependencies list  §7 | Lists all objective dependencies of this objective");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3dependencies clear  §7 | Removes all objective dependencies from this objective");
                    } else if (args[5].equalsIgnoreCase("completionNPC")) {
                        sender.sendMessage("§cMissing 7. argument!");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3completionNPC set <CompletionNPC ID / armorstand> §7 | Sets the completion NPC ID (-1 = default = complete automatically)");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3completionNPC show §7 | Shows the current completion NPC ID (-1 = default = complete automatically)");

                    } else {
                        sender.sendMessage("§cMissing 7. argument!");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3info §7 | Shows everything there is to know about this objective");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3description ... §7 | Manages the description of the objective");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3displayName ... §7 | Manages the display name of the objective");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3dependencies ... §7 | Manage objective dependencies (objectives which need to be completed before this objective)");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3completionNPC ... §7 | Manage completion NPC ID (-1 = default = complete automatically)");
                        sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3remove §7 | Remove the objective from the quest");
                    }
                } else {
                    sender.sendMessage("§cError: Objective with the ID §b" + objectiveID + " §c was not found for quest §b" + quest.getQuestName() + "§c!");
                }


            } else {
                sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage"));
            }
        } else if (args.length == 7) {
            if (args[3].equalsIgnoreCase("add")) {
                if (args[4].equalsIgnoreCase("OtherQuest")) {

                    sender.sendMessage("§cMissing 9. argument §3[countPreviouslyCompletedQuests?: yes/no]§c. Specify the §byes§c or §bno§c depending on if you want previously completed quests to count.");
                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2OtherQuest §3[Other Quest Name] §3[amount of completions needed] [countPreviouslyCompletedQuests?: yes/no]");

                } else if (args[4].equalsIgnoreCase("BreakBlocks")) {
                    sender.sendMessage("§cMissing last argument §3[deductIfBlockIsPlaced?: yes/no]§c. Specify §bYes §cor §b No");
                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2BreakBlocks §3[Block Name] [AmountToBreak] [deductIfBlockIsPlaced?: yes/no]");
                } else if (args[4].equalsIgnoreCase("DeliverItems")) {
                    sender.sendMessage("§cMissing last argument §3[Recipient NPC ID]§c. Enter the §bID of the NPC§c to whom the player has to deliver the items to. Alternatively, you can enter 'armorstand' to use armor stands instead of Citizens NPCs.");
                    sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2DeliverItems §3[Item Name/hand] [Amount To Deliver] [Recipient NPC ID / 'armorstand']");

                }
            } else if (args[3].equalsIgnoreCase("edit")) {

                final int objectiveID = Integer.parseInt(args[4]);
                final Objective objective = quest.getObjectiveFromID(objectiveID);
                if (objective != null) {
                    if (args[5].equalsIgnoreCase("description")) {
                        if (args[6].equalsIgnoreCase("set")) {
                            sender.sendMessage("§cMissing argument <New Description>");
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3description set <New Description> §7 | Sets new objective description");

                        } else if (!args[6].equalsIgnoreCase("show") && !args[6].equalsIgnoreCase("remove")) {
                            sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage"));
                        }

                    } else if (args[5].equalsIgnoreCase("displayName")) {
                        if (args[6].equalsIgnoreCase("set")) {
                            sender.sendMessage("§cMissing argument <New DisplayName>");
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3displayName set <New DisplayName> §7 | Sets new objective Display Name");

                        } else if (!args[6].equalsIgnoreCase("show") && !args[6].equalsIgnoreCase("remove")) {
                            sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage"));
                        }
                    } else if (args[5].equalsIgnoreCase("dependencies")) {
                        if (!args[6].equalsIgnoreCase("list") && !args[6].equalsIgnoreCase("clear")) {
                            if (args[6].equalsIgnoreCase("add")) {
                                sender.sendMessage("§cMissing 8. argument <Objective ID>");
                                sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3dependencies add <Objective ID> §7 | Adds an objective as a dependency (needs to be completed before this one)");

                            } else if (args[6].equalsIgnoreCase("remove") || args[6].equalsIgnoreCase("delete")) {
                                sender.sendMessage("§cMissing 8. argument <Objective ID>");
                                sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3dependencies remove <Objective ID> §7 | Removes an objective from a dependency (needs to be completed before this one)");

                            } else {
                                sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage"));
                            }

                        }
                    } else if (args[5].equalsIgnoreCase("completionNPC")) {
                        if (args[6].equalsIgnoreCase("set")) {
                            sender.sendMessage("§cMissing 8. argument <CompletionNPC ID / armorstand> ");
                            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3completionNPC set <CompletionNPC ID / armorstand> §7 | Sets the completion NPC ID (-1 = default = complete automatically)");

                        } else if (!args[6].equalsIgnoreCase("show") && !args[6].equalsIgnoreCase("view")) {
                            sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage"));

                        }
                    } else {
                        sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage"));
                    }
                } else {
                    sender.sendMessage("§cError: Objective with the ID §b" + objectiveID + " §c was not found for quest §b" + quest.getQuestName() + "§c!");
                }

            } else {
                sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage"));
            }
        } else {
            sender.sendMessage("§e/qadmin §6edit §3 [Quest Name] §6objectives add §3[Objective Type] ...");
            sender.sendMessage("§e/qadmin §6edit §3 [Quest Name] §6objectives edit §3[Objective ID] ...");
            sender.sendMessage("§e/qadmin §6edit §3 [Quest Name] §6objectives list");
            sender.sendMessage("§e/qadmin §6edit §3 [Quest Name] §6objectives clear");
        }

    }


    public void handleCommandsKillEliteMobsObjective(final CommandSender sender, final String[] args, final Quest quest) { //qa edit xxx objectives add KillMobsObjective
        if (args.length == 5) {
            sender.sendMessage("§cMissing 6. argument §3[Mob Name contains / any]§c. Specify the §bname§c of the elite mob the player needs to kill to complete the objective. This can be just part of its name, like 'Elite Zombie'. Use 'any' if the kind of mob doesn't matter.");
            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2KillEliteMobs §3[Mob Name contains / any] [Minimum Level / any] [Maximum Level / any] [Spawn Reason / any] [Minimum Damage Percentage / any] [Amount to kill]");
        } else if (args.length == 6) {
            sender.sendMessage("§cMissing 7. argument §3[Minimum Level / any]§c. Specify the §bminimum level§c the elite mob which the player needs to kill should have. Use 'any' if the minimum level doesn't matter.");
            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2KillEliteMobs §2" + args[5] + " §3[Minimum Level / any] [Maximum Level / any] [Spawn Reason / any] [Minimum Damage Percentage / any] [Amount to kill]");
        } else if (args.length == 7) {
            sender.sendMessage("§cMissing 8. argument §3[Maximum Level / any]§c. Specify the §bmaximum level§c the elite mob which the player needs to kill can have. Use 'any' if the maximum level doesn't matter.");
            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2KillEliteMobs §2" + args[5] + " " + args[6] + " §3[Maximum Level / any] [Spawn Reason / any] [Minimum Damage Percentage / any] [Amount to kill]");
        } else if (args.length == 8) {
            sender.sendMessage("§cMissing 9. argument §3[Spawn Reason / any]§c. Specify the §bspawn reason§c which spawned the elite mob. Use 'any' if the spawn reason doesn't matter.");
            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2KillEliteMobs §2" + args[5] + " " + args[6] + " " + args[7] + " §3[Spawn Reason / any] [Minimum Damage Percentage / any] [Amount to kill]");
        } else if (args.length == 9) {
            sender.sendMessage("§cMissing 10. argument §3[Minimum Damage Percentage / any]§c. Specify the §bminimum damage in percent§c which the player needs to have inflicted on the elite mob in order for it to count as a kill. Use 'any' if it doesn't matter - no matter how much damage the player does to the elite mob, as long as the player just damages it once.");
            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2KillEliteMobs §2" + args[5] + " " + args[6] + " " + args[7] + " " + args[8] + " §3[Minimum Damage Percentage / any] [Amount to kill]");
        } else if (args.length == 10) {
            sender.sendMessage("§cMissing 11. argument §3[Amount to kill]§c. Specify the §bamount of times§c the player needs to kill the elite mob in order to complete this objective.");
            sender.sendMessage("§e/qadmin §6edit §2" + args[1] + " §6objectives add §2KillEliteMobs §2" + args[5] + " " + args[6] + " " + args[7] + " " + args[8] + " " + args[9] + " §3[Amount to kill]");
        } else if (args.length == 11) {
            //Input data
            final String eliteMobName;
            if (args[5].equalsIgnoreCase("any")) {
                eliteMobName = "";
            } else {
                eliteMobName = args[5].replaceAll("_", " ");
            }
            final int minimumLevel;
            if (args[6].equalsIgnoreCase("any")) {
                minimumLevel = -1;
            } else {
                minimumLevel = Integer.parseInt(args[6]);
            }

            final int maximumLevel;
            if (args[7].equalsIgnoreCase("any")) {
                maximumLevel = -1;
            } else {
                maximumLevel = Integer.parseInt(args[7]);
            }
            final String spawnReason;
            if (args[8].equalsIgnoreCase("any")) {
                spawnReason = "";
            } else {
                spawnReason = args[8];
            }
            final int minimumDamagePercentage;
            if (args[9].equalsIgnoreCase("any")) {
                minimumDamagePercentage = -1;
            } else {
                minimumDamagePercentage = Integer.parseInt(args[9].replaceAll("%", ""));
            }

            final int amountToKill = Integer.parseInt(args[10]);

            //Input data validation
            if (!main.isEliteMobsEnabled()) {
                sender.sendMessage("§cError: The Elite Mobs integration is not enabled. Thus, you cannot create an EliteMobs Objective.");
                return;
            }

            if (!spawnReason.isBlank()) { //'any' is being set to ""
                try {
                    CreatureSpawnEvent.SpawnReason.valueOf(spawnReason);
                } catch (IllegalArgumentException exception) {
                    sender.sendMessage("§cError: Invalid Spawn Reason.");
                    return;
                }
            }


            if (maximumLevel > 0 && minimumLevel > maximumLevel) {
                sender.sendMessage("§cError: [Minimum Level / any] needs to be smaller than [Maximum Level / any]");
            }

            if (amountToKill < 0) {
                sender.sendMessage("§cError: [Amount to kill] needs to be at least 1.");
                return;
            }


            //Create Objective
            KillEliteMobsObjective killEliteMobsObjective = new KillEliteMobsObjective(main, quest, quest.getObjectives().size() + 1, eliteMobName, minimumLevel, maximumLevel, spawnReason, minimumDamagePercentage, amountToKill);
            quest.addObjective(killEliteMobsObjective, true);
            sender.sendMessage("§aKillEliteMobs Objective successfully added to quest §b" + quest.getQuestName() + "§a!");

        } else {
            sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage"));
        }
    }


    //Completions
    public List<String> handleCompletions(final String[] args, final CommandSender sender) {
        final Audience audience = main.adventure().sender(sender);
        main.getDataManager().completions.clear();

        final Quest quest = main.getQuestManager().getQuest(args[1]);
        if (quest != null) {
            if (args.length == 4) {
                main.getDataManager().completions.add("add");
                main.getDataManager().completions.add("edit");
                main.getDataManager().completions.add("list");
                main.getDataManager().completions.add("clear");

                //For fancy action bar only
                final String currentArg = args[args.length - 1];
                if (currentArg.equalsIgnoreCase("add")) {
                    main.getUtilManager().sendFancyActionBar(audience, args, "[add / edit / list / clear]", "[Objective Type]");
                } else if (currentArg.equalsIgnoreCase("edit")) {
                    main.getUtilManager().sendFancyActionBar(audience, args, "[add / edit / list / clear]", "[Objective ID]");
                } else if (currentArg.equalsIgnoreCase("list")) {
                    main.getUtilManager().sendFancyActionBar(audience, args, "[add / edit / list / clear]", "");
                } else if (currentArg.equalsIgnoreCase("clear")) {
                    main.getUtilManager().sendFancyActionBar(audience, args, "[add / edit / list / clear]", "");
                } else {
                    main.getUtilManager().sendFancyActionBar(audience, args, "[add / edit / list / clear]", "...");
                }
                return main.getDataManager().completions;
            } else if (args.length == 5) {
                if (args[3].equalsIgnoreCase("add")) {
                    main.getDataManager().completions.add("BreakBlocks");
                    main.getDataManager().completions.add("CollectItems");
                    main.getDataManager().completions.add("CraftItems");
                    main.getDataManager().completions.add("KillMobs");
                    main.getDataManager().completions.add("TriggerCommand");
                    main.getDataManager().completions.add("OtherQuest");
                    main.getDataManager().completions.add("ConsumeItems");
                    main.getDataManager().completions.add("DeliverItems");
                    main.getDataManager().completions.add("TalkToNPC");
                    main.getDataManager().completions.add("EscortNPC");
                    if (main.isEliteMobsEnabled()) {
                        main.getDataManager().completions.add("KillEliteMobs");
                    }

                    //For fancy action bar only
                    final String currentArg = args[args.length - 1];
                    if (currentArg.equalsIgnoreCase("BreakBlocks")) {
                        main.getUtilManager().sendFancyActionBar(audience, args, "[Objective Type]", "[Block Name]");
                    } else if (currentArg.equalsIgnoreCase("CollectItems")) {
                        main.getUtilManager().sendFancyActionBar(audience, args, "[Objective Type]", "[Item Name / 'hand']");
                    } else if (currentArg.equalsIgnoreCase("CraftItems")) {
                        main.getUtilManager().sendFancyActionBar(audience, args, "[Objective Type]", "[Item Name / 'hand']");
                    } else if (currentArg.equalsIgnoreCase("KillMobs")) {
                        main.getUtilManager().sendFancyActionBar(audience, args, "[Objective Type]", "[Mob Name]");
                    } else if (currentArg.equalsIgnoreCase("TriggerCommand")) {
                        main.getUtilManager().sendFancyActionBar(audience, args, "[Objective Type]", "[Trigger Name]");
                    } else if (currentArg.equalsIgnoreCase("OtherQuest")) {
                        main.getUtilManager().sendFancyActionBar(audience, args, "[Objective Type]", "[Other Quest Name]");
                    } else if (currentArg.equalsIgnoreCase("ConsumeItems")) {
                        main.getUtilManager().sendFancyActionBar(audience, args, "[Objective Type]", "[Item Name / 'hand']");
                    } else if (currentArg.equalsIgnoreCase("DeliverItems")) {
                        main.getUtilManager().sendFancyActionBar(audience, args, "[Objective Type]", "[Item Name / 'hand']");
                    } else if (currentArg.equalsIgnoreCase("TalkToNPC")) {
                        main.getUtilManager().sendFancyActionBar(audience, args, "[Objective Type]", "[Target NPC ID / 'armorstand'");
                    } else if (currentArg.equalsIgnoreCase("EscortNPC")) {
                        main.getUtilManager().sendFancyActionBar(audience, args, "[Objective Type]", "[NPC to escort ID]");
                    } else if (main.isEliteMobsEnabled() && currentArg.equalsIgnoreCase("KillEliteMobs")) {
                        main.getUtilManager().sendFancyActionBar(audience, args, "[Objective Type]", "[Part of elite mob name / 'any']");
                    } else {
                        main.getUtilManager().sendFancyActionBar(audience, args, "[Objective Type]", "...");
                    }

                    return main.getDataManager().completions;
                } else if (args[3].equalsIgnoreCase("edit")) {
                    for (final Objective objective : quest.getObjectives()) {
                        main.getDataManager().completions.add("" + objective.getObjectiveID());
                    }
                    main.getUtilManager().sendFancyActionBar(audience, args, "[Objective ID]", "[info / description / displayName / dependencies / completionNPC / remove]");
                    return main.getDataManager().completions;
                }

            } else if (args.length >= 6 && args[3].equalsIgnoreCase("add") && args[4].equalsIgnoreCase("BreakBlocks")) {
                return handleCompletionsBreakBlocksObjective(args, sender);
            } else if (args.length >= 6 && args[3].equalsIgnoreCase("add") && args[4].equalsIgnoreCase("CollectItems")) {
                return handleCompletionsCollectItemsObjective(args, sender);
            } else if (args.length >= 6 && args[3].equalsIgnoreCase("add") && args[4].equalsIgnoreCase("CraftItems")) {
                return handleCompletionsCraftItemsObjective(args, sender);
            } else if (args.length >= 6 && args[3].equalsIgnoreCase("add") && args[4].equalsIgnoreCase("KillMobs")) {
                return handleCompletionsKillMobsObjective(args, sender);
            } else if (args.length >= 6 && args[3].equalsIgnoreCase("add") && args[4].equalsIgnoreCase("TriggerCommand")) {
                return handleCompletionsTriggerCommandObjective(args, sender);
            } else if (args.length >= 6 && args[3].equalsIgnoreCase("add") && args[4].equalsIgnoreCase("OtherQuest")) {
                return handleCompletionsOtherQuestObjective(args, sender);
            } else if (args.length >= 6 && args[3].equalsIgnoreCase("add") && args[4].equalsIgnoreCase("ConsumeItems")) {
                return handleCompletionsConsumeItemsObjective(args, sender);
            } else if (args.length >= 6 && args[3].equalsIgnoreCase("add") && args[4].equalsIgnoreCase("DeliverItems")) {
                return handleCompletionsDeliverItemsObjective(args, sender);
            } else if (args.length >= 6 && args[3].equalsIgnoreCase("add") && args[4].equalsIgnoreCase("TalkToNPC")) {
                return handleCompletionsTalkToNPCObjective(args, sender);
            } else if (args.length >= 6 && args[3].equalsIgnoreCase("add") && args[4].equalsIgnoreCase("EscortNPC")) {
                return handleCompletionsEscortNPCObjective(args, sender);
            } else if (args.length >= 6 && args[3].equalsIgnoreCase("add") && args[4].equalsIgnoreCase("KillEliteMobs")) {
                return handleCompletionsKillEliteMobsObjective(args, sender);
            } else if (args.length >= 6 && args[3].equalsIgnoreCase("edit")) {
                return handleCompletionsEdit(args, sender, quest);
            }
        }


        return main.getDataManager().completions;
    }


    public final List<String> handleCompletionsEdit(final String[] args, final CommandSender sender, final Quest quest) {
        final Audience audience = main.adventure().sender(sender);
        if (args.length == 6) {
            main.getDataManager().completions.add("info");
            main.getDataManager().completions.add("description");
            main.getDataManager().completions.add("displayName");
            main.getDataManager().completions.add("dependencies");
            main.getDataManager().completions.add("completionNPC");
            main.getDataManager().completions.add("remove");

            //For fancy action bar only
            final String currentArg = args[args.length - 1];
            if (currentArg.equalsIgnoreCase("info")) {
                main.getUtilManager().sendFancyActionBar(audience, args, "[info / description / displayName / dependencies / completionNPC / remove]", "");
            } else if (currentArg.equalsIgnoreCase("description")) {
                main.getUtilManager().sendFancyActionBar(audience, args, "[info / description / displayName / dependencies / completionNPC / remove]", "[set / remove / show]");
            } else if (currentArg.equalsIgnoreCase("displayName")) {
                main.getUtilManager().sendFancyActionBar(audience, args, "[info / description / displayName / dependencies / completionNPC / remove]", "[set / remove / show]");
            } else if (currentArg.equalsIgnoreCase("dependencies")) {
                main.getUtilManager().sendFancyActionBar(audience, args, "[info / description / displayName / dependencies / completionNPC / remove]", "[add / remove / list / clear]");
            } else if (currentArg.equalsIgnoreCase("completionNPC")) {
                main.getUtilManager().sendFancyActionBar(audience, args, "[info / description / displayName / dependencies / completionNPC / remove]", "[set / show]");
            } else if (currentArg.equalsIgnoreCase("remove")) {
                main.getUtilManager().sendFancyActionBar(audience, args, "[info / description / displayName / dependencies / completionNPC / remove]", "");
            } else {
                main.getUtilManager().sendFancyActionBar(audience, args, "[info / description / displayName / dependencies / completionNPC / remove]", "...");
            }

        } else if (args.length == 7) {
            if (args[5].equalsIgnoreCase("description")) {
                main.getDataManager().completions.add("set");
                main.getDataManager().completions.add("remove");
                main.getDataManager().completions.add("show");

                //For fancy action bar only
                final String currentArg = args[args.length - 1];
                if (currentArg.equalsIgnoreCase("set")) {
                    main.getUtilManager().sendFancyActionBar(audience, args, "[set / remove / show]", "<Enter new description for this objective>");
                } else if (currentArg.equalsIgnoreCase("remove")) {
                    main.getUtilManager().sendFancyActionBar(audience, args, "[set / remove / show]", "");
                } else if (currentArg.equalsIgnoreCase("show")) {
                    main.getUtilManager().sendFancyActionBar(audience, args, "[set / remove / show]", "");
                } else {
                    main.getUtilManager().sendFancyActionBar(audience, args, "[set / remove / show]", "...");
                }

            } else if (args[5].equalsIgnoreCase("displayName")) {
                main.getDataManager().completions.add("set");
                main.getDataManager().completions.add("remove");
                main.getDataManager().completions.add("show");

                //For fancy action bar only
                final String currentArg = args[args.length - 1];
                if (currentArg.equalsIgnoreCase("set")) {
                    main.getUtilManager().sendFancyActionBar(audience, args, "[set / remove / show]", "<Enter new Display Name for this objective>");
                } else if (currentArg.equalsIgnoreCase("remove")) {
                    main.getUtilManager().sendFancyActionBar(audience, args, "[set / remove / show]", "");
                } else if (currentArg.equalsIgnoreCase("show")) {
                    main.getUtilManager().sendFancyActionBar(audience, args, "[set / remove / show]", "");
                } else {
                    main.getUtilManager().sendFancyActionBar(audience, args, "[set / remove / show]", "...");
                }

            } else if (args[5].equalsIgnoreCase("dependencies")) {
                main.getDataManager().completions.add("add");
                main.getDataManager().completions.add("remove");
                main.getDataManager().completions.add("list");
                main.getDataManager().completions.add("clear");

                //For fancy action bar only
                final String currentArg = args[args.length - 1];
                if (currentArg.equalsIgnoreCase("add")) {
                    main.getUtilManager().sendFancyActionBar(audience, args, "[add / remove / list / clear]", "[Objective ID]");
                } else if (currentArg.equalsIgnoreCase("remove")) {
                    main.getUtilManager().sendFancyActionBar(audience, args, "[add / remove / list / clear]", "[Objective ID]");
                } else if (currentArg.equalsIgnoreCase("list")) {
                    main.getUtilManager().sendFancyActionBar(audience, args, "[add / remove / list / clear]", "");
                } else if (currentArg.equalsIgnoreCase("clear")) {
                    main.getUtilManager().sendFancyActionBar(audience, args, "[add / remove / list / clear]", "");
                } else {
                    main.getUtilManager().sendFancyActionBar(audience, args, "[add / remove / list / clear]", "...");
                }

            } else if (args[5].equalsIgnoreCase("completionNPC")) {
                main.getDataManager().completions.add("set");
                main.getDataManager().completions.add("show");

                //For fancy action bar only
                final String currentArg = args[args.length - 1];
                if (currentArg.equalsIgnoreCase("set")) {
                    main.getUtilManager().sendFancyActionBar(audience, args, "[set / show]", "[CompletionNPC ID / 'armorstand']");
                } else if (currentArg.equalsIgnoreCase("show")) {
                    main.getUtilManager().sendFancyActionBar(audience, args, "[set / show]", "");
                } else {
                    main.getUtilManager().sendFancyActionBar(audience, args, "[set / show]", "...");
                }
            }
        } else if (args.length >= 8 && (args[5].equalsIgnoreCase("displayName") || args[5].equalsIgnoreCase("description"))) {
            if (args[5].equalsIgnoreCase("displayName")) {
                if (args[6].equalsIgnoreCase("set")) {
                    main.getDataManager().completions.add("<Enter new Display Name for this objective>");

                    main.getUtilManager().sendFancyActionBar(audience, args, "<Enter new Display Name for this objective>", "");
                }

            } else if (args[5].equalsIgnoreCase("description")) {
                if (args[6].equalsIgnoreCase("set")) {
                    main.getDataManager().completions.add("<Enter new description for this objective>");

                    main.getUtilManager().sendFancyActionBar(audience, args, "<Enter new description for this objective>", "");

                }
            }

        } else if (args.length == 8) {
            if (args[5].equalsIgnoreCase("dependencies")) {
                final Objective objective = quest.getObjectiveFromID(Integer.parseInt(args[4]));
                if (objective != null) {
                    if (args[6].equalsIgnoreCase("add")) {
                        for (final Objective questObjective : quest.getObjectives()) {
                            if (questObjective.getObjectiveID() != objective.getObjectiveID()) {
                                main.getDataManager().completions.add(questObjective.getObjectiveID() + "");
                            }
                        }
                        main.getUtilManager().sendFancyActionBar(audience, args, "[Objective ID]", "");

                    } else if (args[6].equalsIgnoreCase("remove") || args[6].equalsIgnoreCase("delete")) {
                        for (final Objective dependingObjective : objective.getDependantObjectives()) {
                            main.getDataManager().completions.add(dependingObjective.getObjectiveID() + "");
                        }
                        main.getUtilManager().sendFancyActionBar(audience, args, "[Objective ID]", "");
                    }
                }


            } else if (args[5].equalsIgnoreCase("completionNPC")) {

                if (args[6].equalsIgnoreCase("set")) {
                    final Objective objective = quest.getObjectiveFromID(Integer.parseInt(args[4]));
                    if (objective != null) {

                        if (main.isCitizensEnabled()) {
                            for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
                                main.getDataManager().completions.add("" + npc.getId());
                            }
                        }
                        main.getDataManager().completions.add("-1");
                        main.getDataManager().completions.add("armorstand");
                    }

                    main.getUtilManager().sendFancyActionBar(audience, args, "[CompletionNPC ID / 'armorstand'", "");
                }
            }

        }
        return main.getDataManager().completions;
    }


    public final List<String> handleCompletionsBreakBlocksObjective(final String[] args, final CommandSender sender) {
        final Audience audience = main.adventure().sender(sender);
        if (args.length == 6) {
            for (Material material : Material.values()) {
                main.getDataManager().completions.add(material.toString());
            }
            main.getUtilManager().sendFancyActionBar(audience, args, "[Block Name]", "[Amount to break]");

        } else if (args.length == 7) {
            main.getUtilManager().sendFancyActionBar(audience, args, "[Amount to break]", "[Yes / No (Remove progress if block is placed?)]");
            return main.getDataManager().numberPositiveCompletions;
        } else if (args.length == 8) {
            main.getDataManager().completions.add("Yes");
            main.getDataManager().completions.add("No");
            main.getUtilManager().sendFancyActionBar(audience, args, "[Yes / No (Remove progress if block is placed?)]", "");
        }
        return main.getDataManager().completions;
    }

    public final List<String> handleCompletionsCollectItemsObjective(final String[] args, final CommandSender sender) {
        final Audience audience = main.adventure().sender(sender);
        if (args.length == 6) {
            for (Material material : Material.values()) {
                main.getDataManager().completions.add(material.toString());
            }
            main.getDataManager().completions.add("hand");
            main.getUtilManager().sendFancyActionBar(audience, args, "[Item Name / 'hand']", "[Amount to collect]");
        } else if (args.length == 7) {
            main.getUtilManager().sendFancyActionBar(audience, args, "[Amount to collect]", "");
            return main.getDataManager().numberPositiveCompletions;
        }
        return main.getDataManager().completions;
    }

    public final List<String> handleCompletionsCraftItemsObjective(final String[] args, final CommandSender sender) {
        final Audience audience = main.adventure().sender(sender);
        if (args.length == 6) {
            for (Material material : Material.values()) {
                main.getDataManager().completions.add(material.toString());
            }
            main.getDataManager().completions.add("hand");
            main.getUtilManager().sendFancyActionBar(audience, args, "[Item Name / 'hand']", "[Amount to craft]");
        } else if (args.length == 7) {
            main.getUtilManager().sendFancyActionBar(audience, args, "[Amount to craft]", "");
            return main.getDataManager().numberPositiveCompletions;
        }
        return main.getDataManager().completions;
    }

    public final List<String> handleCompletionsKillMobsObjective(final String[] args, final CommandSender sender) {
        final Audience audience = main.adventure().sender(sender);
        if (args.length == 6) {
            main.getUtilManager().sendFancyActionBar(audience, args, "[Mob Name]", "[Amount of kills needed]");
            return main.getDataManager().standardEntityTypeCompletions;
        } else if (args.length == 7) {
            main.getUtilManager().sendFancyActionBar(audience, args, "[Amount of kills needed]", "");
            return main.getDataManager().numberPositiveCompletions;
        }
        return main.getDataManager().completions;
    }

    public final List<String> handleCompletionsTriggerCommandObjective(final String[] args, final CommandSender sender) {
        final Audience audience = main.adventure().sender(sender);
        if (args.length == 6) {
            main.getDataManager().completions.add("<Enter new TriggerCommand name>");
            main.getUtilManager().sendFancyActionBar(audience, args, "[New Trigger Name]", "[Amount of triggers needed]");
        } else if (args.length == 7) {
            main.getUtilManager().sendFancyActionBar(audience, args, "[Amount of triggers needed]", "");
            return main.getDataManager().numberPositiveCompletions;
        }
        return main.getDataManager().completions;
    }

    public final List<String> handleCompletionsOtherQuestObjective(final String[] args, final CommandSender sender) {
        final Audience audience = main.adventure().sender(sender);
        if (args.length == 6) {
            for (final Quest oneOfAllQuests : main.getQuestManager().getAllQuests()) {
                main.getDataManager().completions.add(oneOfAllQuests.getQuestName());
            }
            main.getUtilManager().sendFancyActionBar(audience, args, "[Other Quest Name]", "[Amount of completions needed]");
        } else if (args.length == 7) {
            main.getUtilManager().sendFancyActionBar(audience, args, "[Amount of completions needed]", "[Yes / No (Count previously-completed Quests?)]");
            return main.getDataManager().numberPositiveCompletions;
        } else if (args.length == 8) {
            main.getDataManager().completions.add("Yes");
            main.getDataManager().completions.add("No");
            main.getUtilManager().sendFancyActionBar(audience, args, "[Yes / No (Count previously-completed Quests?)]", "");
        }
        return main.getDataManager().completions;
    }

    public final List<String> handleCompletionsConsumeItemsObjective(final String[] args, final CommandSender sender) {
        final Audience audience = main.adventure().sender(sender);

        if (args.length == 6) {
            for (Material material : Material.values()) {
                if (material.isEdible()) {
                    main.getDataManager().completions.add(material.toString());
                }
            }
            main.getDataManager().completions.add("hand");
            main.getUtilManager().sendFancyActionBar(audience, args, "[Item Name / 'hand']", "[Amount to consume]");
        } else if (args.length == 7) {
            main.getUtilManager().sendFancyActionBar(audience, args, "[Amount to consume]", "");
            return main.getDataManager().numberPositiveCompletions;
        }

        return main.getDataManager().completions;
    }

    public final List<String> handleCompletionsDeliverItemsObjective(final String[] args, final CommandSender sender) {
        final Audience audience = main.adventure().sender(sender);

        if (args.length == 6) {
            for (Material material : Material.values()) {
                main.getDataManager().completions.add(material.toString());
            }
            main.getDataManager().completions.add("hand");

            main.getUtilManager().sendFancyActionBar(audience, args, "[Item Name / 'hand']", "[Amount to deliver]");
        } else if (args.length == 7) {
            main.getUtilManager().sendFancyActionBar(audience, args, "[Amount to deliver]", "[Recipient NPC ID / 'armorstand]");
            return main.getDataManager().numberPositiveCompletions;
        } else if (args.length == 8) {
            if (main.isCitizensEnabled()) {
                for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
                    main.getDataManager().completions.add("" + npc.getId());
                }
            }
            main.getDataManager().completions.add("armorstand");

            main.getUtilManager().sendFancyActionBar(audience, args, "[Recipient NPC ID / 'armorstand]", "");

        }

        return main.getDataManager().completions;
    }

    public final List<String> handleCompletionsTalkToNPCObjective(final String[] args, final CommandSender sender) {
        final Audience audience = main.adventure().sender(sender);

        if (args.length == 6) {
            if (main.isCitizensEnabled()) {
                for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
                    main.getDataManager().completions.add("" + npc.getId());
                }
            }
            main.getDataManager().completions.add("armorstand");

            main.getUtilManager().sendFancyActionBar(audience, args, "[NPC ID / 'armorstand']", "");
        }

        return main.getDataManager().completions;
    }

    public final List<String> handleCompletionsEscortNPCObjective(final String[] args, final CommandSender sender) {
        final Audience audience = main.adventure().sender(sender);
        if (args.length == 6) {
            if (main.isCitizensEnabled()) {
                for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
                    main.getDataManager().completions.add("" + npc.getId());
                }
            }

            main.getUtilManager().sendFancyActionBar(audience, args, "[NPC to escort ID]", "[Destination NPC ID]");
        } else if (args.length == 7) {
            main.getUtilManager().sendFancyActionBar(audience, args, "[Destination NPC ID]", "");
            return main.getDataManager().numberPositiveCompletions;
        }
        return main.getDataManager().completions;
    }

    public final List<String> handleCompletionsKillEliteMobsObjective(final String[] args, final CommandSender sender) {
        final Audience audience = main.adventure().sender(sender);
        if (args.length == 6) { //[Mob Name contains / any]
            main.getDataManager().completions.add("any");
            if (main.isEliteMobsEnabled()) {
                main.getDataManager().completions.addAll(main.getDataManager().standardEliteMobNamesCompletions);
            }

            main.getUtilManager().sendFancyActionBar(audience, args, "[Part of Elite Mob Name / any]", "[Minimum Level / any]");

        } else if (args.length == 7) { //[Minimum Level / any]
            main.getDataManager().completions.add("any");
            main.getDataManager().completions.addAll(main.getDataManager().numberPositiveCompletions);
            main.getUtilManager().sendFancyActionBar(audience, args, "[Minimum Level / any]", "[Maximum Level / any]");

        } else if (args.length == 8) { //[Maximum Level / any]
            main.getDataManager().completions.add("any");
            main.getDataManager().completions.addAll(main.getDataManager().numberPositiveCompletions);

            main.getUtilManager().sendFancyActionBar(audience, args, "[Maximum Level / any]", "[Spawn Reason / any]");

        } else if (args.length == 9) { //[Spawn Reason / any]
            main.getDataManager().completions.add("any");
            for (final CreatureSpawnEvent.SpawnReason spawnReason : CreatureSpawnEvent.SpawnReason.values()) {
                main.getDataManager().completions.add(spawnReason.toString());
            }

            main.getUtilManager().sendFancyActionBar(audience, args, "[Spawn Reason / any]", "[Minimum Damage Percentage / any]");

        } else if (args.length == 10) { //[Minimum Damage Percentage / any]
            main.getDataManager().completions.add("any");
            for (int i = 50; i <= 100; i++) {
                main.getDataManager().completions.add("" + i);
            }

            main.getUtilManager().sendFancyActionBar(audience, args, "[Minimum Damage Percentage / any]", "[Amount to kill]");

        } else if (args.length == 11) { //[Amount to kill]

            main.getUtilManager().sendFancyActionBar(audience, args, "[Amount to kill]", "");

            return main.getDataManager().numberPositiveCompletions;
        }

        return main.getDataManager().completions;
    }


}
