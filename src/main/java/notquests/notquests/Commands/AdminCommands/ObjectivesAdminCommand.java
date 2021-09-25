package notquests.notquests.Commands.AdminCommands;


import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Objectives.*;
import notquests.notquests.Structs.Quest;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

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
                    if (objective instanceof BreakBlocksObjective breakBlocksObjective) {
                        sender.sendMessage("    §7Block to break: §f" + breakBlocksObjective.getBlockToBreak().toString() + " §7x " + objective.getProgressNeeded());
                    } else if (objective instanceof CollectItemsObjective collectItemsObjective) {
                        sender.sendMessage("    §7Items to collect: §f" + collectItemsObjective.getItemToCollect().getType() + " (" + collectItemsObjective.getItemToCollect().getItemMeta().getDisplayName() + ")" + " §7x " + objective.getProgressNeeded());
                    }else if (objective instanceof CraftItemsObjective craftItemsObjective) {
                        sender.sendMessage("    §7Items to craft: §f" + craftItemsObjective.getItemToCraft().getType() + " (" + craftItemsObjective.getItemToCraft().getItemMeta().getDisplayName() + ")" + " §7x " + objective.getProgressNeeded());
                    } else if (objective instanceof TriggerCommandObjective triggerCommandObjective) {
                        sender.sendMessage("    §7Goal: §f" + triggerCommandObjective.getTriggerName() + " §7x " + objective.getProgressNeeded());
                    } else if (objective instanceof OtherQuestObjective otherQuestObjective) {
                        sender.sendMessage("    §7Quest completion: §f" + otherQuestObjective.getOtherQuest().getQuestName() + " §7x " + objective.getProgressNeeded());
                    } else if (objective instanceof KillMobsObjective killMobsObjective) {
                        sender.sendMessage("    §7Mob to kill: §f" + killMobsObjective.getMobToKill().toString() + " §7x " + objective.getProgressNeeded());
                    } else if (objective instanceof ConsumeItemsObjective consumeItemsObjective) {
                        sender.sendMessage("    §7Items to consume: §f" + consumeItemsObjective.getItemToConsume().getType() + " (" + consumeItemsObjective.getItemToConsume().getItemMeta().getDisplayName() + ")" + " §7x " + objective.getProgressNeeded());
                    } else if (objective instanceof DeliverItemsObjective deliverItemsObjective) {
                        sender.sendMessage("    §7Items to deliver: §f" + deliverItemsObjective.getItemToDeliver().getType() + " (" + deliverItemsObjective.getItemToDeliver().getItemMeta().getDisplayName() + ")" + " §7x " + objective.getProgressNeeded());
                        if(main.isCitizensEnabled()){
                            final NPC npc = CitizensAPI.getNPCRegistry().getById(deliverItemsObjective.getRecipientNPCID());
                            if (npc != null) {
                                sender.sendMessage("    §7Deliver it to §f" + npc.getName());
                            } else {
                                sender.sendMessage("    §7The delivery NPC is currently not available!");
                            }
                        }else{
                            sender.sendMessage("    §cError: Citizens plugin not installed. Contact an admin.");
                        }

                    } else if (objective instanceof TalkToNPCObjective talkToNPCObjective) {
                        if(main.isCitizensEnabled()){
                            final NPC npc = CitizensAPI.getNPCRegistry().getById(talkToNPCObjective.getNPCtoTalkID());
                            if (npc != null) {
                                sender.sendMessage("    §7Talk to §f" + npc.getName());
                            } else {
                                sender.sendMessage("    §7The target NPC is currently not available!");
                            }
                        }else{
                            sender.sendMessage("    §cError: Citizens plugin not installed. Contact an admin.");
                        }

                    } else if (objective instanceof EscortNPCObjective escortNPCObjective) {
                        if(main.isCitizensEnabled()){
                            final NPC npc = CitizensAPI.getNPCRegistry().getById(escortNPCObjective.getNpcToEscortID());
                            final NPC npcDestination = CitizensAPI.getNPCRegistry().getById(escortNPCObjective.getNpcToEscortToID());

                            if (npc != null && npcDestination != null) {
                                sender.sendMessage("    §7Escort §f" + npc.getName() + " §7to §f" + npcDestination.getName());
                            } else {
                                sender.sendMessage("    §7The target or destination NPC is currently not available!");
                            }
                        }else{
                            sender.sendMessage("    §cError: Citizens plugin not installed. Contact an admin.");
                        }

                    }
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
                    if(!main.isCitizensEnabled()){
                        sender.sendMessage("§cError: Any kind of NPC stuff has been disabled, because you don't have the Citizens plugin installed on your server. You need to install the Citizens plugin in order for NPC stuff to work.");
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
                    EntityType entityType = EntityType.valueOf(args[5]);
                    int amountToKill = Integer.parseInt(args[6]);


                    KillMobsObjective killMobsObjective = new KillMobsObjective(main, quest, quest.getObjectives().size() + 1, entityType, amountToKill);
                    quest.addObjective(killMobsObjective, true);
                    sender.sendMessage("§aObjective successfully added to quest §b" + quest.getQuestName() + "§a!");


                } else if (args[4].equalsIgnoreCase("ConsumeItems")) {
                    if (args[5].equalsIgnoreCase("hand")) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
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
                            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §2OtherQuest §3[Other Quest Name] §3[amount of completions needed] [countPreviouslyCompletedQuests?: yes/no]");
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
                    if(!main.isCitizensEnabled()){
                        sender.sendMessage("§cError: Any kind of NPC stuff has been disabled, because you don't have the Citizens plugin installed on your server. You need to install the Citizens plugin in order for NPC stuff to work.");
                        return;
                    }
                    final int NPCID = Integer.parseInt(args[7]);
                    final NPC npc = CitizensAPI.getNPCRegistry().getById(NPCID);
                    if (npc != null) {

                        if (args[5].equalsIgnoreCase("hand")) {
                            if (sender instanceof Player) {
                                Player player = (Player) sender;
                                ItemStack holdingItem = player.getInventory().getItemInMainHand();
                                int amountToCollect = Integer.parseInt(args[6]);

                                DeliverItemsObjective deliverItemsObjective = new DeliverItemsObjective(main, quest, quest.getObjectives().size() + 1, holdingItem, amountToCollect, NPCID);
                                quest.addObjective(deliverItemsObjective, true);
                                sender.sendMessage("§aObjective successfully added to quest §b" + quest.getQuestName() + "§a!");

                            } else {
                                sender.sendMessage("§cThis command can only be run as a player.");
                            }
                        } else {
                            Material itemMaterial = Material.getMaterial(args[5]);
                            if (itemMaterial != null) {
                                ItemStack itemStack = new ItemStack(itemMaterial, 1);
                                int amountToCollect = Integer.parseInt(args[6]);


                                DeliverItemsObjective deliverItemsObjective = new DeliverItemsObjective(main, quest, quest.getObjectives().size() + 1, itemStack, amountToCollect, NPCID);
                                quest.addObjective(deliverItemsObjective, true);
                                sender.sendMessage("§aObjective successfully added to quest §b" + quest.getQuestName() + "§a!");


                            } else {
                                sender.sendMessage("§cItem §b" + args[5] + " §cnot found!");
                            }
                        }


                    } else {
                        sender.sendMessage("§cError: NPC with the ID §b" + NPCID + " §cwas not found!");
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

                        final int completionNPCID = Integer.parseInt(args[7]);

                        objective.setCompletionNPCID(completionNPCID, true);
                        sender.sendMessage("§aThe completionNPCID of the objective with the ID §b" + objectiveID + " §ahas been set to the NPC with the ID §b" + completionNPCID + "§a!");


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

    public List<String> handleCompletions(final CommandSender sender, final String[] args) {
        main.getDataManager().completions.clear();

        final Quest quest = main.getQuestManager().getQuest(args[1]);
        if (quest != null) {
            if (args.length == 4) {
                main.getDataManager().completions.add("add");
                main.getDataManager().completions.add("edit");
                main.getDataManager().completions.add("list");
                main.getDataManager().completions.add("clear");
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
                    return main.getDataManager().completions;
                } else if (args[3].equalsIgnoreCase("edit")) {
                    for (final Objective objective : quest.getObjectives()) {
                        main.getDataManager().completions.add("" + objective.getObjectiveID());
                    }
                    return main.getDataManager().completions;
                }

            } else if (args.length == 6) {
                if (args[3].equalsIgnoreCase("add")) {
                    if (args[4].equalsIgnoreCase("BreakBlocks")) {
                        for (Material material : Material.values()) {
                            main.getDataManager().completions.add(material.toString());
                        }
                        return main.getDataManager().completions;
                    } else if (args[4].equalsIgnoreCase("CollectItems")) {
                        for (Material material : Material.values()) {
                            main.getDataManager().completions.add(material.toString());
                        }
                        main.getDataManager().completions.add("hand");
                        return main.getDataManager().completions;
                    }else if (args[4].equalsIgnoreCase("CraftItems")) {
                        for (Material material : Material.values()) {
                            main.getDataManager().completions.add(material.toString());
                        }
                        main.getDataManager().completions.add("hand");
                        return main.getDataManager().completions;
                    } else if (args[4].equalsIgnoreCase("TriggerCommand")) {
                        main.getDataManager().completions.add("<Enter new TriggerCommand name>");
                        return main.getDataManager().completions;
                    } else if (args[4].equalsIgnoreCase("OtherQuest")) {
                        for (Quest oneOfAllQuests : main.getQuestManager().getAllQuests()) {
                            main.getDataManager().completions.add(oneOfAllQuests.getQuestName());
                        }
                        return main.getDataManager().completions;
                    } else if (args[4].equalsIgnoreCase("KillMobs")) {
                        return main.getDataManager().standardEntityTypeCompletions;
                    } else if (args[4].equalsIgnoreCase("ConsumeItems")) {
                        for (Material material : Material.values()) {
                            if (material.isEdible()) {
                                main.getDataManager().completions.add(material.toString());
                            }
                        }
                        main.getDataManager().completions.add("hand");
                        return main.getDataManager().completions;
                    } else if (args[4].equalsIgnoreCase("DeliverItems")) {
                        for (Material material : Material.values()) {
                            main.getDataManager().completions.add(material.toString());
                        }
                        main.getDataManager().completions.add("hand");
                        return main.getDataManager().completions;
                    } else if (args[4].equalsIgnoreCase("TalkToNPC")) {
                        if(main.isCitizensEnabled()){
                            for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
                                main.getDataManager().completions.add("" + npc.getId());
                            }
                        }

                        return main.getDataManager().completions;
                    } else if (args[4].equalsIgnoreCase("EscortNPC")) {
                        if(main.isCitizensEnabled()){
                            for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
                                main.getDataManager().completions.add("" + npc.getId());
                            }
                        }

                        return main.getDataManager().completions;
                    }
                } else if (args[3].equalsIgnoreCase("edit")) {
                    main.getDataManager().completions.add("info");
                    main.getDataManager().completions.add("description");
                    main.getDataManager().completions.add("displayName");
                    main.getDataManager().completions.add("dependencies");
                    main.getDataManager().completions.add("completionNPC");
                    main.getDataManager().completions.add("remove");
                    return main.getDataManager().completions;
                }

            } else if (args.length == 7) {
                if (args[3].equalsIgnoreCase("add")) {
                    if (args[4].equalsIgnoreCase("BreakBlocks")) {
                        return main.getDataManager().numberPositiveCompletions;
                    } else if (args[4].equalsIgnoreCase("CollectItems")) {
                        return main.getDataManager().numberPositiveCompletions;
                    }else if (args[4].equalsIgnoreCase("CraftItems")) {
                        return main.getDataManager().numberPositiveCompletions;
                    } else if (args[4].equalsIgnoreCase("TriggerCommand")) {
                        return main.getDataManager().numberPositiveCompletions;
                    } else if (args[4].equalsIgnoreCase("OtherQuest")) {
                        return main.getDataManager().numberPositiveCompletions;
                    } else if (args[4].equalsIgnoreCase("KillMobs")) {
                        return main.getDataManager().numberPositiveCompletions;
                    } else if (args[4].equalsIgnoreCase("ConsumeItems")) {
                        return main.getDataManager().numberPositiveCompletions;
                    } else if (args[4].equalsIgnoreCase("DeliverItems")) {
                        return main.getDataManager().numberPositiveCompletions;
                    } else if (args[4].equalsIgnoreCase("EscortNPC")) {
                        return main.getDataManager().numberPositiveCompletions;
                    }
                } else if (args[3].equalsIgnoreCase("edit")) {
                    if (args[5].equalsIgnoreCase("displayName")) {
                        main.getDataManager().completions.add("set");
                        main.getDataManager().completions.add("remove");
                        main.getDataManager().completions.add("show");
                        return main.getDataManager().completions;
                    } else if (args[5].equalsIgnoreCase("description")) {
                        main.getDataManager().completions.add("set");
                        main.getDataManager().completions.add("remove");
                        main.getDataManager().completions.add("show");
                        return main.getDataManager().completions;
                    } else if (args[5].equalsIgnoreCase("dependencies")) {
                        main.getDataManager().completions.add("add");
                        main.getDataManager().completions.add("remove");
                        main.getDataManager().completions.add("list");
                        main.getDataManager().completions.add("clear");
                        return main.getDataManager().completions;
                    } else if (args[5].equalsIgnoreCase("completionNPC")) {
                        main.getDataManager().completions.add("set");
                        main.getDataManager().completions.add("show");
                        return main.getDataManager().completions;
                    }

                }
            } else if (args.length >= 8 && args[3].equalsIgnoreCase("edit") && (args[5].equalsIgnoreCase("displayName") || args[5].equalsIgnoreCase("description"))) {
                if (args[5].equalsIgnoreCase("displayName")) {
                    if (args[6].equalsIgnoreCase("set")) {
                        main.getDataManager().completions.add("<Enter new Display Name for this objective>");
                        return main.getDataManager().completions;
                    }

                } else if (args[5].equalsIgnoreCase("description")) {
                    if (args[6].equalsIgnoreCase("set")) {
                        main.getDataManager().completions.add("<Enter new description for this objective>");
                        return main.getDataManager().completions;
                    }
                }

            } else if (args.length == 8) {
                if (args[3].equalsIgnoreCase("add")) {
                    if (args[4].equalsIgnoreCase("BreakBlocks")) {
                        main.getDataManager().completions.add("Yes");
                        main.getDataManager().completions.add("No");
                        return main.getDataManager().completions;
                    } else if (args[4].equalsIgnoreCase("OtherQuest")) {
                        main.getDataManager().completions.add("Yes");
                        main.getDataManager().completions.add("No");
                        return main.getDataManager().completions;
                    } else if (args[4].equalsIgnoreCase("DeliverItems")) {

                        if(main.isCitizensEnabled()){
                            for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
                                main.getDataManager().completions.add("" + npc.getId());
                            }
                        }

                        return main.getDataManager().completions;
                    }
                } else if (args[3].equalsIgnoreCase("edit")) {
                    if (args[5].equalsIgnoreCase("dependencies")) {
                        final Objective objective = quest.getObjectiveFromID(Integer.parseInt(args[4]));
                        if (objective != null) {
                            if (args[6].equalsIgnoreCase("add")) {
                                for (final Objective questObjective : quest.getObjectives()) {
                                    if (questObjective.getObjectiveID() != objective.getObjectiveID()) {
                                        main.getDataManager().completions.add(questObjective.getObjectiveID() + "");
                                    }
                                }
                                return main.getDataManager().completions;
                            } else if (args[6].equalsIgnoreCase("remove") || args[6].equalsIgnoreCase("delete")) {
                                for (final Objective dependingObjective : objective.getDependantObjectives()) {
                                    main.getDataManager().completions.add(dependingObjective.getObjectiveID() + "");

                                }
                                return main.getDataManager().completions;
                            }
                        }


                    } else if (args[5].equalsIgnoreCase("completionNPC")) {

                        if (args[6].equalsIgnoreCase("set")) {
                            final Objective objective = quest.getObjectiveFromID(Integer.parseInt(args[4]));
                            if (objective != null) {

                                if(main.isCitizensEnabled()){
                                    for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
                                        main.getDataManager().completions.add("" + npc.getId());
                                    }
                                }
                                return main.getDataManager().completions;
                            }


                        }
                    }
                }
            }
        }


        return main.getDataManager().completions;
    }


    private void showUsage(final Quest quest, final CommandSender sender, final String[] args) {
        if (args.length == 3) {
            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §3[Objective Type] ...");
            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §3[Objective ID] ...");
            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives list");
            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives clear");
        } else if (args.length == 4) {
            if (args[2].equalsIgnoreCase("add")) {
                sender.sendMessage("§cPlease specify an objective type!");
                sender.sendMessage("§eObjective Types:");
                sender.sendMessage("§bBreakBlocks");
                sender.sendMessage("§bCollectItems");
                sender.sendMessage("§bCraftItems");
                sender.sendMessage("§bKillMobs");
                sender.sendMessage("§bTriggerCommand");
                sender.sendMessage("§bOtherQuest");
                sender.sendMessage("§bConsumeItems");
                sender.sendMessage("§bDeliverItems");
                sender.sendMessage("§bTalkToNPC");
                sender.sendMessage("§bEscortNPC");
                sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §3[Objective Type] ...");
            } else if (args[2].equalsIgnoreCase("edit")) {
                sender.sendMessage("§cPlease specify the Objective ID.");
                sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §3[Objective ID] ...");
            } else {
                sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage"));
                sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §3[Objective Type] ...");
                sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §3[Objective ID] ...");
                sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives list");
                sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives clear");
            }


        } else if (args.length == 5) {
            if (args[3].equalsIgnoreCase("add")) {
                if (args[4].equalsIgnoreCase("BreakBlocks")) {
                    sender.sendMessage("§cMissing 6. argument §3[Block Name]§c. Specify the §bblock§c the player has to break.");
                    sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §2BreakBlocks §3[Block Name] [AmountToBreak] [deductIfBlockIsPlaced?: yes/no]");
                } else if (args[4].equalsIgnoreCase("CollectItems")) {
                    sender.sendMessage("§cMissing 6. argument §3[Item Name]§c. Specify the §bitem§c the player has to collect.");
                    sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §2CollectItems §3[Item Name/hand] [Amount To Collect]");
                }else if (args[4].equalsIgnoreCase("CraftItems")) {
                    sender.sendMessage("§cMissing 6. argument §3[Item Name]§c. Specify the §bitem§c the player has to craft.");
                    sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §2CraftItems §3[Item Name/hand] [Amount To Craft]");
                } else if (args[4].equalsIgnoreCase("TriggerCommand")) {
                    sender.sendMessage("§cMissing 6. argument §3[Trigger Name]§c. Specify the §bname§c of the trigger to complete the objective.");
                    sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §2TriggerCommand §3[Trigger Name] [Amount To Trigger]");
                } else if (args[4].equalsIgnoreCase("OtherQuest")) {
                    sender.sendMessage("§cMissing 6. argument §3[Other Quest Name]§c. Specify the §bname§c of the other quests which needs to be completed to complete the objective.");
                    sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §2OtherQuest §3[Other Quest Name] §3[amount of completions needed] [countPreviouslyCompletedQuests?: yes/no]");
                } else if (args[4].equalsIgnoreCase("KillMobs")) {
                    sender.sendMessage("§cMissing 6. argument §3[Mob Name]§c. Specify the §bname§c of the mob the player needs to kill to complete the objective.");
                    sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §2KillMobs §3[Mob Name] §3[amount of kills needed]");
                } else if (args[4].equalsIgnoreCase("ConsumeItems")) {
                    sender.sendMessage("§cMissing 6. argument §3[Item Name/hand]§c. Specify the §bitem§c the player has to consume.");
                    sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §2ConsumeItems §3[Item Name/hand] [Amount To Consume]");
                } else if (args[4].equalsIgnoreCase("DeliverItems")) {
                    sender.sendMessage("§cMissing 6. argument §3[Item Name]§c. Specify the §bitem§c the player has to deliver.");
                    sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §2DeliverItems §3[Item Name/hand] [Amount To Collect] [Recipient NPC ID]");
                } else if (args[4].equalsIgnoreCase("TalkToNPC")) {
                    sender.sendMessage("§cMissing 6. argument §3[Item Name]§c. Specify the §bID of the NPC§c who the player has to talk to.");
                    sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §2TalkToNPC §3[Target NPC ID]");
                } else if (args[4].equalsIgnoreCase("EscortNPC")) {
                    sender.sendMessage("§cMissing 6. argument §3[NPC to escort ID]§c. Specify the §bID of the NPC§c the player has to escort.");
                    sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §2EscortNPC §3[NPC to escort ID] [Destination NPC ID]");
                } else {
                    sender.sendMessage("§cInvalid ObjectiveType.");
                    sender.sendMessage("§eObjective Types:");
                    sender.sendMessage("§bBreakBlocks");
                    sender.sendMessage("§bCollectItems");
                    sender.sendMessage("§bCraftItems");
                    sender.sendMessage("§bKillMobs");
                    sender.sendMessage("§bTriggerCommand");
                    sender.sendMessage("§bOtherQuest");
                    sender.sendMessage("§bConsumeItems");
                    sender.sendMessage("§bDeliverItems");
                    sender.sendMessage("§bTalkToNPC");
                    sender.sendMessage("§bEscortNPC");
                }
            } else if (args[3].equalsIgnoreCase("edit")) {
                try {
                    final int objectiveID = Integer.parseInt(args[4]);
                    if (quest.getObjectives().size() >= objectiveID && objectiveID > 0) {
                        final Objective objective = quest.getObjectiveFromID(objectiveID);
                        if (objective != null) {
                            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3info §7 | Shows everything there is to know about this objective");
                            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3description ... §7 | Manages the description of the objective");
                            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3displayName ... §7 | Manages the display name of the objective");
                            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3dependencies ... §7 | Manage objective dependencies (objectives which need to be completed before this objective)");
                            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3completionNPC ... §7 | Manage completion NPC ID (-1 = default = complete automatically)");
                            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3remove §7 | Remove the objective from the quest");

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
                    sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §2OtherQuest §3[Other Quest Name] §3[amount of completions needed] [countPreviouslyCompletedQuests?: yes/no]");
                } else if (args[4].equalsIgnoreCase("TriggerCommand")) {

                    sender.sendMessage("§cMissing 7. argument §3[AmountToTrigger]§c. Specify the §bamount of times§c the trigger needs to be triggered to complete the objective.");
                    sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §2TriggerCommand §3[Trigger Name] [AmountToTrigger]");

                } else if (args[4].equalsIgnoreCase("BreakBlocks")) {
                    sender.sendMessage("§cMissing 7. argument §3[AmountToBreak]§c. Specify the §bamount of blocks§c the player has to break.");
                    sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §2BreakBlocks §3[Block Name] [AmountToBreak] [deductIfBlockIsPlaced?: yes/no]");

                } else if (args[4].equalsIgnoreCase("CollectItems")) {
                    sender.sendMessage("§cMissing 7. argument §3[Amount To Collect]§c. Specify the §bamount of items§c the player has to collect.");
                    sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §2CollectItems §3[Item Name/hand] [Amount To Collect]");
                } else if (args[4].equalsIgnoreCase("CraftItems")) {
                    sender.sendMessage("§cMissing 7. argument §3[Amount To Craft]§c. Specify the §bamount of items§c the player has to craft.");
                    sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §2CraftItems §3[Item Name/hand] [Amount To Craft]");
                } else if (args[4].equalsIgnoreCase("KillMobs")) {
                    sender.sendMessage("§cMissing 7. argument §3[amount of kills needed]§c. Specify the §bamount of times§c the player has to kill the mob.");
                    sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §2KillMobs §3[Mob Name] §3[amount of kills needed]");
                } else if (args[4].equalsIgnoreCase("ConsumeItems")) {
                    sender.sendMessage("§cMissing 7. argument §3[Amount To Consume]§c. Specify the bamount of items§c the player has to consume.");
                    sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §2ConsumeItems §3[Item Name/hand] [Amount To Consume]");
                } else if (args[4].equalsIgnoreCase("DeliverItems")) {
                    sender.sendMessage("§cMissing 7. argument §3[Amount To Deliver]§c. Specify the §bamount of items§c the player has to deliver.");
                    sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §2DeliverItems §3[Item Name/hand] [Amount To Deliver] [Recipient NPC ID]");

                } else if (args[4].equalsIgnoreCase("EscortNPC")) {
                    sender.sendMessage("§cMissing 7. argument §3[Destination NPC ID]§c. Specify the §bID of the NPC§c where the player has to escort the escort NPC to.");
                    sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §2EscortNPC " + args[5] + " §3[Destination NPC ID]");
                } else {
                    sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage"));
                }
            } else if (args[3].equalsIgnoreCase("edit")) {
                final int objectiveID = Integer.parseInt(args[4]);
                final Objective objective = quest.getObjectiveFromID(objectiveID);
                if (objective != null) {
                    if (args[5].equalsIgnoreCase("description")) {
                        sender.sendMessage("§cMissing 7. argument!");
                        sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3description set <New Description> §7 | Sets new objective description");
                        sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3description remove §7 | Removes current objective description");
                        sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3description show §7 | Show current objective description");

                    } else if (args[5].equalsIgnoreCase("displayName")) {
                        sender.sendMessage("§cMissing 7. argument!");
                        sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3displayName set <New DisplayName> §7 | Sets new objective Display Name");
                        sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3displayName remove §7 | Removes current objective Display Name");
                        sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3displayName show §7 | Shows current objective Display Name");

                    } else if (args[5].equalsIgnoreCase("dependencies")) {
                        sender.sendMessage("§cMissing 7. argument!");
                        sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3dependencies add <Objective ID> §7 | Adds an objective as a dependency (needs to be completed before this one)");
                        sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3dependencies remove <Objective ID> §7 | Removes an objective from a dependency (needs to be completed before this one)");
                        sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3dependencies list  §7 | Lists all objective dependencies of this objective");
                        sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3dependencies clear  §7 | Removes all objective dependencies from this objective");
                    } else if (args[5].equalsIgnoreCase("completionNPC")) {
                        sender.sendMessage("§cMissing 7. argument!");
                        sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3completionNPC set <CompletionNPC ID> §7 | Sets the completion NPC ID (-1 = default = complete automatically)");
                        sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3completionNPC show §7 | Shows the current completion NPC ID (-1 = default = complete automatically)");

                    } else {
                        sender.sendMessage("§cMissing 7. argument!");
                        sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3info §7 | Shows everything there is to know about this objective");
                        sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3description ... §7 | Manages the description of the objective");
                        sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3displayName ... §7 | Manages the display name of the objective");
                        sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3dependencies ... §7 | Manage objective dependencies (objectives which need to be completed before this objective)");
                        sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3completionNPC ... §7 | Manage completion NPC ID (-1 = default = complete automatically)");
                        sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3remove §7 | Remove the objective from the quest");
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
                    sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §2OtherQuest §3[Other Quest Name] §3[amount of completions needed] [countPreviouslyCompletedQuests?: yes/no]");

                } else if (args[4].equalsIgnoreCase("BreakBlocks")) {
                    sender.sendMessage("§cMissing last argument §3[deductIfBlockIsPlaced?: yes/no]§c. Specify §bYes §cor §b No");
                    sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §2BreakBlocks §3[Block Name] [AmountToBreak] [deductIfBlockIsPlaced?: yes/no]");
                } else if (args[4].equalsIgnoreCase("DeliverItems")) {
                    sender.sendMessage("§cMissing last argument §3[Recipient NPC ID]§c. Enter the §bID of the NPC§c to whom the player has to deliver the items to.");
                    sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives add §2DeliverItems §3[Item Name/hand] [Amount To Deliver] [Recipient NPC ID]");

                }
            } else if (args[3].equalsIgnoreCase("edit")) {

                final int objectiveID = Integer.parseInt(args[4]);
                final Objective objective = quest.getObjectiveFromID(objectiveID);
                if (objective != null) {
                    if (args[5].equalsIgnoreCase("description")) {
                        if (args[6].equalsIgnoreCase("set")) {
                            sender.sendMessage("§cMissing argument <New Description>");
                            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3description set <New Description> §7 | Sets new objective description");

                        } else if (!args[6].equalsIgnoreCase("show") && !args[6].equalsIgnoreCase("remove")) {
                            sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage"));
                        }

                    } else if (args[5].equalsIgnoreCase("displayName")) {
                        if (args[6].equalsIgnoreCase("set")) {
                            sender.sendMessage("§cMissing argument <New DisplayName>");
                            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3displayName set <New DisplayName> §7 | Sets new objective Display Name");

                        } else if (!args[6].equalsIgnoreCase("show") && !args[6].equalsIgnoreCase("remove")) {
                            sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage"));
                        }
                    } else if (args[5].equalsIgnoreCase("dependencies")) {
                        if (!args[6].equalsIgnoreCase("list") && !args[6].equalsIgnoreCase("clear")) {
                            if (args[6].equalsIgnoreCase("add")) {
                                sender.sendMessage("§cMissing 8. argument <Objective ID>");
                                sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3dependencies add <Objective ID> §7 | Adds an objective as a dependency (needs to be completed before this one)");

                            } else if (args[6].equalsIgnoreCase("remove") || args[6].equalsIgnoreCase("delete")) {
                                sender.sendMessage("§cMissing 8. argument <Objective ID>");
                                sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3dependencies remove <Objective ID> §7 | Removes an objective from a dependency (needs to be completed before this one)");

                            } else {
                                sender.sendMessage(main.getLanguageManager().getString("chat.wrong-command-usage"));
                            }

                        }
                    } else if (args[5].equalsIgnoreCase("completionNPC")) {
                        if (args[6].equalsIgnoreCase("set")) {
                            sender.sendMessage("§cMissing 8. argument <CompletionNPC ID> ");
                            sender.sendMessage("§e/nquestsadmin §6edit §2" + args[1] + " §6objectives edit §2" + objectiveID + " §3completionNPC set <CompletionNPC ID> §7 | Sets the completion NPC ID (-1 = default = complete automatically)");

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
            sender.sendMessage("§e/nquestsadmin §6edit §3 [Quest Name] §6objectives add §3[Objective Type] ...");
            sender.sendMessage("§e/nquestsadmin §6edit §3 [Quest Name] §6objectives edit §3[Objective ID] ...");
            sender.sendMessage("§e/nquestsadmin §6edit §3 [Quest Name] §6objectives list");
            sender.sendMessage("§e/nquestsadmin §6edit §3 [Quest Name] §6objectives clear");
        }

    }


}
