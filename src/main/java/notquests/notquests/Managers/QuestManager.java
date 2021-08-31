package notquests.notquests.Managers;

import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import notquests.notquests.NotQuests;
import notquests.notquests.QuestGiverNPCTrait;
import notquests.notquests.Structs.ActiveObjective;
import notquests.notquests.Structs.ActiveQuest;
import notquests.notquests.Structs.Objectives.*;
import notquests.notquests.Structs.Quest;
import notquests.notquests.Structs.QuestPlayer;
import notquests.notquests.Structs.Requirements.*;
import notquests.notquests.Structs.Rewards.CommandReward;
import notquests.notquests.Structs.Rewards.QuestPointsReward;
import notquests.notquests.Structs.Rewards.Reward;
import notquests.notquests.Structs.Rewards.RewardType;
import notquests.notquests.Structs.Triggers.Action;
import notquests.notquests.Structs.Triggers.Trigger;
import notquests.notquests.Structs.Triggers.TriggerTypes.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.logging.Level;


public class QuestManager {

    private final NotQuests main;

    private final ArrayList<Quest> quests;
    private final ArrayList<Action> actions;

    private boolean questDataLoaded = false;


    public QuestManager(NotQuests main) {
        this.main = main;
        quests = new ArrayList<>();
        actions = new ArrayList<>();
    }


    public final String createQuest(String questName) {
        if (getQuest(questName) == null) {
            Quest newQuest = new Quest(main, questName);
            quests.add(newQuest);
            main.getDataManager().getQuestsData().set("quests." + questName, "");
            return "§aQuest successfully created!";
        } else {
            return "§cQuest already exists!";
        }
    }

    public final String deleteQuest(String questName) {
        if (getQuest(questName) != null) {
            Quest questToDelete = getQuest(questName);
            quests.remove(questToDelete);
            main.getDataManager().getQuestsData().set("quests." + questName, null);
            return "§aQuest successfully deleted!";
        } else {
            return "§cQuest doesn't exist!";
        }
    }

    public final Quest getQuest(String questName) {
        for (Quest quest : quests) {
            if (quest.getQuestName().equalsIgnoreCase(questName)) {
                return quest;
            }
        }
        return null;
    }

    public final ArrayList<Quest> getAllQuests() {
        return quests;
    }


    public void loadData() {
        System.out.println("§aNotQuests > Registering Citizens nquestgiver trait...");
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
        System.out.println("§aNotQuests > Citizens nquestgiver trait has been registered!");
        try {
            System.out.println("§aLoading Quests data...");
            quests.clear();

            //Actions
            final ConfigurationSection actionsConfigurationSection = main.getDataManager().getQuestsData().getConfigurationSection("actions");
            if (actionsConfigurationSection != null) {
                for (final String actionName : actionsConfigurationSection.getKeys(false)) {
                    final String consoleCommand = main.getDataManager().getQuestsData().getString("actions." + actionName + ".consoleCommand", "");
                    if (consoleCommand.equalsIgnoreCase("")) {
                        System.out.println("§cNotQuests > Action has an empty console command. This should NOT be possible! Creating an action with an empty console command... Action name: §b" + actionName);
                    }
                    boolean nameAlreadyExists = false;
                    for (final Action action : actions) {
                        if (action.getActionName().equalsIgnoreCase(actionName)) {
                            nameAlreadyExists = true;
                            break;
                        }
                    }

                    if (!nameAlreadyExists) {
                        final Action newAction = new Action(main, actionName, consoleCommand);
                        actions.add(newAction);
                        main.getDataManager().getQuestsData().set("actions." + actionName + ".consoleCommand", consoleCommand);
                    } else {
                        System.out.println("§eNotQuests > Action already exists. This should NOT be possible! Skipping action creation... Action name: §b" + actionName);
                        main.getLogger().log(Level.SEVERE, "§cNotQuests > Plugin disabled, because there was an error while loading quests action data.");
                        main.getDataManager().setSavingEnabled(false);
                        main.getServer().getPluginManager().disablePlugin(main);
                    }

                }
            }

            //Quests
            final ConfigurationSection questsConfigurationSection = main.getDataManager().getQuestsData().getConfigurationSection("quests");
            if (questsConfigurationSection != null) {
                for (String questName : questsConfigurationSection.getKeys(false)) {
                    Quest quest = new Quest(main, questName);
                    quest.setMaxAccepts(main.getDataManager().getQuestsData().getInt("quests." + questName + ".maxAccepts", -1));
                    quest.setTakeEnabled(main.getDataManager().getQuestsData().getBoolean("quests." + questName + ".takeEnabled", true));
                    quest.setAcceptCooldown(main.getDataManager().getQuestsData().getLong("quests." + questName + ".acceptCooldown", -1));
                    quest.setQuestDescription(main.getDataManager().getQuestsData().getString("quests." + questName + ".description", ""));
                    quest.setQuestDisplayName(main.getDataManager().getQuestsData().getString("quests." + questName + ".displayName", ""));


                    final ConfigurationSection triggersConfigurationSection = main.getDataManager().getQuestsData().getConfigurationSection("quests." + questName + ".triggers");
                    if (triggersConfigurationSection != null) {
                        for (final String triggerNumber : triggersConfigurationSection.getKeys(false)) {


                            //Triggers:
                            final String triggerTypeString = main.getDataManager().getQuestsData().getString("quests." + questName + ".triggers." + triggerNumber + ".triggerType");
                            final String triggerActionName = main.getDataManager().getQuestsData().getString("quests." + questName + ".triggers." + triggerNumber + ".triggerActionName");
                            final long amountNeeded = main.getDataManager().getQuestsData().getLong("quests." + questName + ".triggers." + triggerNumber + ".amountNeeded", 1);

                            final int applyOn = main.getDataManager().getQuestsData().getInt("quests." + questName + ".triggers." + triggerNumber + ".applyOn");
                            final String worldName = main.getDataManager().getQuestsData().getString("quests." + questName + ".triggers." + triggerNumber + ".worldName", "ALL");


                            final TriggerType triggerType = TriggerType.valueOf(triggerTypeString);

                            Action foundAction = null;
                            for (final Action action : actions) {
                                if (action.getActionName().equalsIgnoreCase(triggerActionName)) {
                                    foundAction = action;
                                    break;
                                }
                            }
                            if (foundAction != null) {
                                Trigger trigger = null;
                                if (triggerType.equals(TriggerType.COMPLETE)) {
                                    trigger = new CompleteTrigger(main, foundAction, applyOn, worldName);
                                } else if (triggerType.equals(TriggerType.BEGIN)) {
                                    trigger = new BeginTrigger(main, foundAction, applyOn, worldName);
                                } else if (triggerType.equals(TriggerType.DEATH)) {

                                    trigger = new DeathTrigger(main, foundAction, applyOn, worldName, amountNeeded);
                                } else if (triggerType.equals(TriggerType.FAIL)) {
                                    trigger = new FailTrigger(main, foundAction, applyOn, worldName);
                                } else if (triggerType.equals(TriggerType.DISCONNECT)) {
                                    trigger = new DisconnectTrigger(main, foundAction, applyOn, worldName);
                                } else if (triggerType.equals(TriggerType.NPCDEATH)) {
                                    final int npcToDie = main.getDataManager().getQuestsData().getInt("quests." + questName + ".triggers." + triggerNumber + ".specifics.npcToDie");
                                    trigger = new NPCDeathTrigger(main, foundAction, applyOn, worldName, amountNeeded, npcToDie);
                                } else if (triggerType.equals(TriggerType.WORLDENTER)) {
                                    final String worldToEnter = main.getDataManager().getQuestsData().getString("quests." + questName + ".triggers." + triggerNumber + ".specifics.worldToEnter", "ALL");
                                    trigger = new WorldEnterTrigger(main, foundAction, applyOn, worldName, amountNeeded, worldToEnter);
                                } else if (triggerType.equals(TriggerType.WORLDLEAVE)) {
                                    final String worldToLeave = main.getDataManager().getQuestsData().getString("quests." + questName + ".triggers." + triggerNumber + ".specifics.worldToLeave", "ALL");
                                    trigger = new WorldLeaveTrigger(main, foundAction, applyOn, worldName, amountNeeded, worldToLeave);
                                } else {
                                    System.out.println("§cNotQuests > ERROR when loading trigger with the triggerNumber §b" + triggerNumber + " §c: TriggerType is unknown. Trigger creation SKIPPED!");
                                    main.getLogger().log(Level.SEVERE, "§cNotQuests > Plugin disabled, because there was an error while loading quests trigger data.");
                                    main.getDataManager().setSavingEnabled(false);
                                    main.getServer().getPluginManager().disablePlugin(main);
                                }
                                if (trigger != null) {
                                    quest.addTrigger(trigger);
                                }

                            } else {
                                System.out.println("§cNotQuests > ERROR when loading trigger with the triggerNumber §b" + triggerNumber + " §c: Action could not be loaded. Trigger creation SKIPPED!");
                                main.getLogger().log(Level.SEVERE, "§cNotQuests > Plugin disabled, because there was an error while loading quests trigger data.");
                                main.getDataManager().setSavingEnabled(false);
                                main.getServer().getPluginManager().disablePlugin(main);
                            }


                        }
                    }


                    //Objectives:
                    final ConfigurationSection objectivesConfigurationSection = main.getDataManager().getQuestsData().getConfigurationSection("quests." + questName + ".objectives");
                    if (objectivesConfigurationSection != null) {
                        for (final String objectiveNumber : objectivesConfigurationSection.getKeys(false)) {
                            ObjectiveType objectiveType = null;
                            try {
                                objectiveType = ObjectiveType.valueOf(main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".objectiveType"));
                            } catch (java.lang.NullPointerException ex) {
                                System.out.println("§cNotQuests > Error parsing objective Type of objective with ID §b" + objectiveNumber + "§c and Quest §b" + quest.getQuestName() + "§c. Objective creation skipped...");
                                ex.printStackTrace();
                                main.getLogger().log(Level.SEVERE, "§cNotQuests > Plugin disabled, because there was an error while loading quests objective Type data.");
                                main.getDataManager().setSavingEnabled(false);
                                main.getServer().getPluginManager().disablePlugin(main);
                            }
                            final int progressNeeded = main.getDataManager().getQuestsData().getInt("quests." + questName + ".objectives." + objectiveNumber + ".progressNeeded");

                            int objectiveID = -1;
                            boolean validObjectiveID = true;
                            try {
                                objectiveID = Integer.parseInt(objectiveNumber);
                            } catch (java.lang.NumberFormatException ex) {
                                System.out.println("§cNotQuests > Error parsing loaded objective ID §b" + objectiveNumber + "§c. Objective creation skipped...");
                                validObjectiveID = false;
                                main.getLogger().log(Level.SEVERE, "§cNotQuests > Plugin disabled, because there was an error while loading quests objective ID data.");
                                main.getDataManager().setSavingEnabled(false);
                                main.getServer().getPluginManager().disablePlugin(main);
                            }
                            if (validObjectiveID && objectiveID > 0 && objectiveType != null) {


                                Objective objective = null;

                                if (objectiveType == ObjectiveType.BreakBlocks) {
                                    final Material blockToBreak = Material.valueOf(main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.blockToBreak.material"));
                                    final boolean deductIfBlockPlaced = main.getDataManager().getQuestsData().getBoolean("quests." + questName + ".objectives." + objectiveNumber + ".specifics.deductIfBlockPlaced");
                                    objective = new BreakBlocksObjective(main, quest, objectiveID, blockToBreak, progressNeeded, deductIfBlockPlaced);
                                } else if (objectiveType == ObjectiveType.CollectItems) {
                                    final ItemStack itemToCollect = main.getDataManager().getQuestsData().getItemStack("quests." + questName + ".objectives." + objectiveNumber + ".specifics.itemToCollect.itemstack");
                                    objective = new CollectItemsObjective(main, quest, objectiveID, itemToCollect, progressNeeded);
                                } else if (objectiveType == ObjectiveType.TriggerCommand) {
                                    final String triggerName = main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.triggerName");
                                    objective = new TriggerCommandObjective(main, quest, objectiveID, triggerName, progressNeeded);
                                } else if (objectiveType == ObjectiveType.OtherQuest) {
                                    final String otherQuestName = main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.otherQuestName");
                                    final boolean countPreviousCompletions = main.getDataManager().getQuestsData().getBoolean("quests." + questName + ".objectives." + objectiveNumber + ".specifics.countPreviousCompletions");
                                    objective = new OtherQuestObjective(main, quest, objectiveID, otherQuestName, progressNeeded, countPreviousCompletions);
                                } else if (objectiveType == ObjectiveType.KillMobs) {
                                    final EntityType mobToKill = EntityType.valueOf(main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.mobToKill"));
                                    final int amountToKill = main.getDataManager().getQuestsData().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.amountToKill");
                                    objective = new KillMobsObjective(main, quest, objectiveID, mobToKill, amountToKill);
                                } else if (objectiveType == ObjectiveType.ConsumeItems) {
                                    final ItemStack itemToConsume = main.getDataManager().getQuestsData().getItemStack("quests." + questName + ".objectives." + objectiveNumber + ".specifics.itemToConsume.itemstack");
                                    objective = new ConsumeItemsObjective(main, quest, objectiveID, itemToConsume, progressNeeded);
                                } else if (objectiveType == ObjectiveType.DeliverItems) {
                                    final ItemStack itemToCollect = main.getDataManager().getQuestsData().getItemStack("quests." + questName + ".objectives." + objectiveNumber + ".specifics.itemToCollect.itemstack");
                                    final int recipientNPCID = main.getDataManager().getQuestsData().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.recipientNPCID");
                                    objective = new DeliverItemsObjective(main, quest, objectiveID, itemToCollect, progressNeeded, recipientNPCID);
                                } else if (objectiveType == ObjectiveType.TalkToNPC) {
                                    final int NPCtoTalkID = main.getDataManager().getQuestsData().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.NPCtoTalkID");
                                    objective = new TalkToNPCObjective(main, quest, objectiveID, NPCtoTalkID);
                                } else if (objectiveType == ObjectiveType.EscortNPC) {
                                    final int NPCtoEscortID = main.getDataManager().getQuestsData().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.NPCToEscortID");
                                    final int destinationNPCID = main.getDataManager().getQuestsData().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.destinationNPCID");
                                    objective = new EscortNPCObjective(main, quest, objectiveID, NPCtoEscortID, destinationNPCID);
                                }


                                if (objective != null) {

                                    final String objectiveDisplayName = main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".displayName", "");
                                    final String objectiveDescription = main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".description", "");
                                    final int completionNPCID = main.getDataManager().getQuestsData().getInt("quests." + quest.getQuestName() + ".objectives." + objectiveNumber + ".completionNPCID", -1);

                                    objective.setObjectiveDisplayName(objectiveDisplayName, false);
                                    objective.setObjectiveDescription(objectiveDescription, false);
                                    objective.setCompletionNPCID(completionNPCID, false);
                                    quest.addObjective(objective, false);
                                } else {
                                    System.out.println("§cNotQuests > Error loading objective");
                                    main.getLogger().log(Level.SEVERE, "§cNotQuests > Plugin disabled, because there was an error while loading quests objective data.");
                                    main.getDataManager().setSavingEnabled(false);
                                    main.getServer().getPluginManager().disablePlugin(main);
                                }

                            } else {
                                main.getLogger().log(Level.SEVERE, "§cNotQuests > Plugin disabled, because there was an error while loading quests objective data (2).");
                                main.getDataManager().setSavingEnabled(false);
                                main.getServer().getPluginManager().disablePlugin(main);
                            }
                        }
                    }
                    //Objective Dependencies
                    for (final Objective objective : quest.getObjectives()) {
                        final ConfigurationSection objectiveDependenciesConfigurationSection = main.getDataManager().getQuestsData().getConfigurationSection("quests." + quest.getQuestName() + ".objectives." + objective.getObjectiveID() + ".dependantObjectives.");
                        if (objectiveDependenciesConfigurationSection != null) {
                            for (String objectiveDependencyNumber : objectiveDependenciesConfigurationSection.getKeys(false)) {
                                int dependantObjectiveID = main.getDataManager().getQuestsData().getInt("quests." + quest.getQuestName() + ".objectives." + (objective.getObjectiveID()) + ".dependantObjectives." + objectiveDependencyNumber + ".objectiveID", objective.getObjectiveID());
                                final Objective dependantObjective = quest.getObjectiveFromID(dependantObjectiveID);
                                objective.addDependantObjective(dependantObjective, false);
                            }
                        }
                    }


                    //Rewards:
                    final ConfigurationSection rewardsConfigurationSection = main.getDataManager().getQuestsData().getConfigurationSection("quests." + questName + ".rewards");
                    if (rewardsConfigurationSection != null) {
                        for (String rewardNumber : rewardsConfigurationSection.getKeys(false)) {

                            RewardType rewardType = RewardType.valueOf(main.getDataManager().getQuestsData().getString("quests." + questName + ".rewards." + rewardNumber + ".rewardType"));

                            Reward reward = null;

                            if (rewardType == RewardType.ConsoleCommand) {
                                String consoleCommand = main.getDataManager().getQuestsData().getString("quests." + questName + ".rewards." + rewardNumber + ".specifics.consoleCommand");
                                reward = new CommandReward(main, consoleCommand);
                            } else if (rewardType == RewardType.QuestPoints) {
                                long rewardedQuestPoints = main.getDataManager().getQuestsData().getLong("quests." + questName + ".rewards." + rewardNumber + ".specifics.rewardedQuestPoints");
                                reward = new QuestPointsReward(main, rewardedQuestPoints);
                            }

                            if (reward != null) {
                                quest.addReward(reward);
                            } else {
                                System.out.println("§cNotQuests > Error loading reward");
                                main.getLogger().log(Level.SEVERE, "§cNotQuests > Plugin disabled, because there was an error while loading quests reward data.");
                                main.getDataManager().setSavingEnabled(false);
                                main.getServer().getPluginManager().disablePlugin(main);
                            }

                        }
                    }


                    //Requirements:
                    final ConfigurationSection requirementsConfigurationSection = main.getDataManager().getQuestsData().getConfigurationSection("quests." + questName + ".requirements");
                    if (requirementsConfigurationSection != null) {
                        for (String requirementsNumber : requirementsConfigurationSection.getKeys(false)) {

                            RequirementType requirementType = RequirementType.valueOf(main.getDataManager().getQuestsData().getString("quests." + questName + ".requirements." + requirementsNumber + ".requirementType"));
                            int progressNeeded = main.getDataManager().getQuestsData().getInt("quests." + questName + ".requirements." + requirementsNumber + ".progressNeeded");

                            Requirement requirement = null;

                            if (requirementType == RequirementType.OtherQuest) {
                                final String otherQuestName = main.getDataManager().getQuestsData().getString("quests." + questName + ".requirements." + requirementsNumber + ".specifics.otherQuestRequirememt");
                                requirement = new OtherQuestRequirement(main, otherQuestName, progressNeeded);
                            } else if (requirementType == RequirementType.QuestPoints) {
                                final long questPointRequirement = main.getDataManager().getQuestsData().getLong("quests." + questName + ".requirements." + requirementsNumber + ".specifics.questPointRequirement");
                                final boolean deductQuestPoints = main.getDataManager().getQuestsData().getBoolean("quests." + questName + ".requirements." + requirementsNumber + ".specifics.deductQuestPoints");
                                requirement = new QuestPointsRequirement(main, questPointRequirement, deductQuestPoints);
                            } else if (requirementType == RequirementType.Money) {
                                final long moneyRequirement = main.getDataManager().getQuestsData().getLong("quests." + questName + ".requirements." + requirementsNumber + ".specifics.moneyRequirement");
                                final boolean deductMoney = main.getDataManager().getQuestsData().getBoolean("quests." + questName + ".requirements." + requirementsNumber + ".specifics.deductMoney");
                                requirement = new MoneyRequirement(main, moneyRequirement, deductMoney);
                            } else if (requirementType == RequirementType.Permission) {
                                final String requiredPermission = main.getDataManager().getQuestsData().getString("quests." + questName + ".requirements." + requirementsNumber + ".specifics.requiredPermission");
                                requirement = new PermissionRequirement(main, requiredPermission);
                            }

                            if (requirement != null) {
                                quest.addRequirement(requirement);
                            } else {
                                System.out.println("§cNotQuests > Error loading requirement");
                                main.getLogger().log(Level.SEVERE, "§cNotQuests > Plugin disabled, because there was an error while loading quests requirement data.");
                                main.getDataManager().setSavingEnabled(false);
                                main.getServer().getPluginManager().disablePlugin(main);
                            }

                        }
                    }


                    quests.add(quest);
                }
            }


            setQuestDataLoaded(true);
        } catch (Exception e) {
            e.printStackTrace();
            main.getLogger().log(Level.SEVERE, "§cNotQuests > Plugin disabled, because there was an exception while loading quests data.");
            main.getDataManager().setSavingEnabled(false);
            main.getServer().getPluginManager().disablePlugin(main);
            //return;
        }


    }


    public final ArrayList<Quest> getQuestsAttachedToNPC(final NPC npc) {
        final ArrayList<Quest> questsattached = new ArrayList<>();
        for (Quest quest : quests) {
            if (quest.getAttachedNPCsWithQuestShowing().contains(npc) || quest.getAttachedNPCsWithoutQuestShowing().contains(npc)) {
                questsattached.add(quest);
            }
        }
        // System.out.println("§esize: " + questsattached.size());
        return questsattached;
    }

    public final ArrayList<Quest> getQuestsAttachedToNPCWithShowing(final NPC npc) {
        final ArrayList<Quest> questsattached = new ArrayList<>();
        for (Quest quest : quests) {
            if (quest.getAttachedNPCsWithQuestShowing().contains(npc)) {
                questsattached.add(quest);
            }
        }
        // System.out.println("§esize: " + questsattached.size());
        return questsattached;
    }

    public final ArrayList<Quest> getQuestsAttachedToNPCWithoutShowing(final NPC npc) {
        final ArrayList<Quest> questsattached = new ArrayList<>();
        for (Quest quest : quests) {
            if (quest.getAttachedNPCsWithoutQuestShowing().contains(npc)) {
                questsattached.add(quest);
            }
        }
        // System.out.println("§esize: " + questsattached.size());
        return questsattached;
    }

    public final ArrayList<NPC> getAllNPCsAttackedToQuest(final Quest quest) {
        final ArrayList<NPC> npcsAttached = new ArrayList<>();
        npcsAttached.addAll(quest.getAttachedNPCsWithQuestShowing());
        npcsAttached.addAll(quest.getAttachedNPCsWithoutQuestShowing());
        return npcsAttached;
    }

    public final ArrayList<NPC> getNPCsAttackedToQuestWithShowing(final Quest quest) {
        return quest.getAttachedNPCsWithQuestShowing();
    }

    public final ArrayList<NPC> getNPCsAttackedToQuestWithoutShowing(final Quest quest) {
        return quest.getAttachedNPCsWithoutQuestShowing();
    }

    public void sendQuestsPreviewOfQuestShownNPCs(NPC npc, Player player) {
        final boolean guiEnabled = true;
        final ArrayList<Quest> questsAttachedToNPC = getQuestsAttachedToNPCWithShowing(npc);

        if (guiEnabled) {
            String[] guiSetup = {
                    "xxxxxxxxx",
                    "xgggggggx",
                    "xgggggggx",
                    "xgggggggx",
                    "xgggggggx",
                    "pxxxxxxxn"
            };
            InventoryGui gui = new InventoryGui(main, player, "          §9Available Quests", guiSetup);
            gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

            int count = 0;
            GuiElementGroup group = new GuiElementGroup('g');

            for (final Quest quest : questsAttachedToNPC) {
                final Material materialToUse = Material.BOOK;

                String displayName = quest.getQuestName();
                if (!quest.getQuestDisplayName().equals("")) {
                    displayName = quest.getQuestDisplayName();
                }
                displayName = "§b" + displayName;
                QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer((player.getUniqueId()));

                if (questPlayer != null && questPlayer.hasAcceptedQuest(quest)) {
                    displayName += " §a[ACCEPTED]";
                }
                String description = "";
                if (!quest.getQuestDescription().equals("")) {
                    description = "§8" + quest.getQuestDescription();
                }
                count++;


                group.addElement(new StaticGuiElement('e',
                        new ItemStack(materialToUse),
                        count, // Display a number as the item count
                        click -> {
                            player.chat("/q preview " + quest.getQuestName());
                            //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                            return true; // returning true will cancel the click event and stop taking the item

                        },
                        displayName,
                        description,
                        "§aClick to open Quest"
                ));

            }


            gui.addElement(group);

            // Previous page
            gui.addElement(new GuiPageElement('p', new ItemStack(Material.SPECTRAL_ARROW), GuiPageElement.PageAction.PREVIOUS, "Go to previous page (%prevpage%)"));

            // Next page
            gui.addElement(new GuiPageElement('n', new ItemStack(Material.ARROW), GuiPageElement.PageAction.NEXT, "Go to next page (%nextpage%)"));

            gui.addElement(new StaticGuiElement('x',
                    new ItemStack(Material.BLACK_STAINED_GLASS_PANE),
                    0, // Display a number as the item count
                    click -> {

                        return true; // returning true will cancel the click event and stop taking the item

                    },
                    " "
            ));


            gui.show(player);
        } else {
            System.out.println("§eAll quest count: " + quests.size());

            player.sendMessage("");
            player.sendMessage("§9" + questsAttachedToNPC.size() + " Available Quests:");
            int counter = 1;

            for (Quest quest : questsAttachedToNPC) {


                BaseComponent component;


                BaseComponent acceptComponent = new TextComponent("§a§l[CHOOSE]");
                acceptComponent.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/nquests preview " + quest.getQuestName()));
                if (quest.getQuestDisplayName().length() >= 1) {
                    acceptComponent.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aClick to preview/choose the quest §b" + quest.getQuestDisplayName()).create()));
                    component = new TextComponent("§e" + counter + ". §b" + quest.getQuestDisplayName() + " ");
                } else {
                    acceptComponent.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aClick to preview/choose the quest §b" + quest.getQuestName()).create()));
                    component = new TextComponent("§e" + counter + ". §b" + quest.getQuestName() + " ");
                }

                component.addExtra(acceptComponent);

                player.sendMessage(component);


                counter++;
            }
            //getQuestsAttachedToNPC(npc);
        }

    }

    public final String getQuestRequirements(final Quest quest) {
        String requirements = "";
        int counter = 1;
        for (Requirement requirement : quest.getRequirements()) {
            requirements = "§a" + counter + ". §e" + requirement.getRequirementType().toString() + "\n";
            if (requirement instanceof OtherQuestRequirement) {
                requirements += "§7-- Finish Quest first: " + ((OtherQuestRequirement) requirement).getOtherQuestName();
            } else if (requirement instanceof QuestPointsRequirement) {
                requirements += "§7-- Quest points needed: " + ((QuestPointsRequirement) requirement).getQuestPointRequirement() + "\n";
                if (((QuestPointsRequirement) requirement).isDeductQuestPoints()) {
                    requirements += "§7--- §cQuest points WILL BE DEDUCTED!";
                } else {
                    requirements += "§7--- Will quest points be deducted?: No";
                }

            } else if (requirement instanceof MoneyRequirement) {
                requirements += "§7-- Money needed: " + ((MoneyRequirement) requirement).getMoneyRequirement() + "\n";
                if (((MoneyRequirement) requirement).isDeductMoney()) {
                    requirements += "§7--- §cMoney WILL BE DEDUCTED!";
                } else {
                    requirements += "§7--- Will money be deducted?: No";
                }

            } else if (requirement instanceof PermissionRequirement) {
                requirements += "§7-- Permission needed: " + ((PermissionRequirement) requirement).getRequiredPermission();
            }

            counter += 1;
        }
        return requirements;
    }

    public void sendSingleQuestPreview(Player player, Quest quest) {
        player.sendMessage("");
        player.sendMessage("§7-----------------------------------");
        if (!quest.getQuestDisplayName().equals("")) {
            player.sendMessage("§9Quest Preview for Quest §b" + quest.getQuestDisplayName() + "§9:");
        } else {
            player.sendMessage("§9Quest Preview for Quest §b" + quest.getQuestName() + "§9:");
        }


        if (quest.getQuestDescription().length() >= 1) {
            player.sendMessage("§eQuest description: §7" + quest.getQuestDescription());
        } else {
            player.sendMessage("§eThis quest has no quest description.");
        }

        player.sendMessage("§9Quest Requirements:");

        player.sendMessage(getQuestRequirements(quest));

        Component acceptQuestComponent = Component.text("[ACCEPT THIS QUEST]", NamedTextColor.GREEN, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/nquests take " + quest.getQuestName()))
                .hoverEvent(HoverEvent.showText(Component.text("Click to accept this quest", NamedTextColor.GREEN)));


        player.sendMessage("");
        player.sendMessage(acceptQuestComponent);
        player.sendMessage("§7-----------------------------------");


    }

    public void loadNPCData() {
        System.out.println("§aLoading NPC data...");
        if (isQuestDataLoaded()) {
            try {

                final ConfigurationSection questsConfigurationSetting = main.getDataManager().getQuestsData().getConfigurationSection("quests");
                if (questsConfigurationSetting != null) {
                    if (!Bukkit.isPrimaryThread()) {
                        Bukkit.getScheduler().runTask(main, () -> {
                            for (String questName : questsConfigurationSetting.getKeys(false)) {
                                Quest quest = getQuest(questName);

                                if (quest != null) {
                                    //NPC
                                    final ConfigurationSection npcsConfigurationSection = main.getDataManager().getQuestsData().getConfigurationSection("quests." + questName + ".npcs");
                                    if (npcsConfigurationSection != null) {


                                        for (String npcNumber : npcsConfigurationSection.getKeys(false)) {
                                            if (main.getDataManager().getQuestsData() != null) {


                                                final NPC npc = CitizensAPI.getNPCRegistry().getById(main.getDataManager().getQuestsData().getInt("quests." + questName + ".npcs." + npcNumber + ".npcID"));


                                                if (npc != null) {
                                                    final boolean questShowing = main.getDataManager().getQuestsData().getBoolean("quests." + questName + ".npcs." + npc.getId() + ".questShowing", true);


                                                    // call the callback with the result
                                                    System.out.println("§aNotQuests > attaching quest with the name §b" + quest.getQuestName() + " §ato NPC with the ID §b" + npc.getId() + " §aand name §b" + npc.getName());
                                                    quest.removeNPC(npc);
                                                    quest.bindToNPC(npc, questShowing);


                                                } else {
                                                    System.out.println("§cNotQuests > Error attaching npc with ID §b" + main.getDataManager().getQuestsData().getInt("quests." + questName + ".npcs." + npcNumber + ".npcID") + " to quest - NPC not found.");
                                                }
                                            } else {
                                                System.out.println("§cNotQuests > Error: quests data is null");

                                            }


                                        }


                                    }
                                } else {
                                    System.out.println("§cNotQuests > Error: Quest not found while trying to load NPC");
                                }
                            }
                            System.out.println("§aNotQuests > Requesting cleaning of bugged NPCs in loadNPCData()...");
                            cleanupBuggedNPCs();
                        });

                    } else {
                        for (String questName : questsConfigurationSetting.getKeys(false)) {
                            Quest quest = getQuest(questName);

                            if (quest != null) {
                                //NPC
                                final ConfigurationSection npcsConfigurationSection = main.getDataManager().getQuestsData().getConfigurationSection("quests." + questName + ".npcs");
                                if (npcsConfigurationSection != null) {
                                    for (String npcNumber : npcsConfigurationSection.getKeys(false)) {
                                        final NPC npc = CitizensAPI.getNPCRegistry().getById(main.getDataManager().getQuestsData().getInt("quests." + questName + ".npcs." + npcNumber + ".npcID"));
                                        final boolean questShowing = main.getDataManager().getQuestsData().getBoolean("quests." + questName + ".npcs." + npc.getId() + ".questShowing", true);

                                        if (npc != null) {

                                            // call the callback with the result
                                            System.out.println("§aNotQuests > attaching quest with the name §b" + quest.getQuestName() + " §ato NPC with the ID §b" + npc.getId() + " §aand name §b" + npc.getName());
                                            quest.removeNPC(npc);
                                            quest.bindToNPC(npc, questShowing);


                                        } else {
                                            System.out.println("§cNotQuests > Error attaching npc with ID §b" + main.getDataManager().getQuestsData().getInt("quests." + questName + ".npcs." + npcNumber + ".npcID") + " to quest - NPC not found.");
                                        }

                                    }
                                    System.out.println("§aNotQuests > Requesting cleaning of bugged NPCs in loadNPCData()...");
                                    cleanupBuggedNPCs();


                                }
                            } else {
                                System.out.println("§cNotQuests > Error: Quest not found while trying to load NPC");
                            }


                        }
                    }


                } else {
                    System.out.println("§cNotQuests > Skipped loading NPC data because questsConfigurationSetting was null.");
                }
                System.out.println("§aNpc data loaded!");

                main.getDataManager().setAlreadyLoadedNPCs(true);


            } catch (Exception e) {
                e.printStackTrace();
                main.getLogger().log(Level.SEVERE, "§cNotQuests > Plugin disabled, because there was an exception while loading quests NPC data.");
                main.getDataManager().setSavingEnabled(false);
                main.getServer().getPluginManager().disablePlugin(main);
                //return;
            }

        } else {
            System.out.println("§eNotQuests > Tried to load NPC data before quest data was loaded. skipping scheduling another load...");
            Bukkit.getScheduler().runTaskLaterAsynchronously(main, () -> {
                System.out.println("§eNotQuests > Trying to load NPC quest data again...");
                main.getDataManager().loadNPCData();
            }, 40);
        }


    }

    public final boolean isQuestDataLoaded() {
        return questDataLoaded;
    }

    public void setQuestDataLoaded(boolean questDataLoaded) {
        this.questDataLoaded = questDataLoaded;
        if (questDataLoaded) {
            System.out.println("§aQuests data loaded!");
        }
    }


    public void cleanupBuggedNPCs() {
        System.out.println("§aNotQuests > Checking for bugged NPCs...");
        int buggedNPCsFound = 0;
        int allNPCsFound = 0;
        //Clean up bugged NPCs with quests attached wrongly
        final ArrayList<Trait> traitsToRemove = new ArrayList<>();
        for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
            allNPCsFound += 1;
            if (getQuestsAttachedToNPC(npc).size() == 0) {
                for (final Trait trait : npc.getTraits()) {
                    if (trait.getName().contains("questgiver")) {
                        traitsToRemove.add(trait);
                    }
                }


                if (!Bukkit.isPrimaryThread()) {
                    Bukkit.getScheduler().runTask(main, () -> {
                        for (Trait trait : traitsToRemove) {
                            npc.removeTrait(trait.getClass());
                        }
                    });
                } else {
                    for (Trait trait : traitsToRemove) {
                        npc.removeTrait(trait.getClass());
                    }
                }

                if (!traitsToRemove.isEmpty()) {
                    buggedNPCsFound += 1;
                    System.out.println("§eNotQuests > Bugged trait removed from npc with ID §b" + npc.getId() + " §eand name §b" + npc.getName() + " §e!");
                }


            } else {
                //TODO: Remove debug shit or improve performance
                final ArrayList<String> attachedQuestNames = new ArrayList<>();
                for (final Quest attachedQuest : getQuestsAttachedToNPC(npc)) {
                    attachedQuestNames.add(attachedQuest.getQuestName());
                }
                System.out.println("§aNotQuests > NPC with the ID: §b" + npc.getId() + " §ais not bugged, because it has the following quests attached: §b" + attachedQuestNames);
            }
            traitsToRemove.clear();

        }
        if (buggedNPCsFound == 0) {
            System.out.println("§aNotQuests > No bugged NPCs found! Amount of checked NPCs: §b" + allNPCsFound);
        } else {
            System.out.println("§eNotQuests > §b" + buggedNPCsFound + " §ebugged NPCs have  been found and removed! Amount of checked NPCs: §b" + allNPCsFound);
        }
    }

    public final String createAction(String actionName, String consoleCommand) {
        boolean nameAlreadyExists = false;
        for (Action action : actions) {
            if (action.getActionName().equalsIgnoreCase(actionName)) {
                nameAlreadyExists = true;
                break;
            }
        }

        if (!nameAlreadyExists) {
            final Action newAction = new Action(main, actionName, consoleCommand);
            actions.add(newAction);
            main.getDataManager().getQuestsData().set("actions." + actionName + ".consoleCommand", consoleCommand);
            return "§aAction successfully created!";
        } else {
            return "§cAction already exists!";
        }
    }

    public final ArrayList<Action> getAllActions() {
        return actions;
    }

    public final Action getAction(String actionName) {
        for (Action action : actions) {
            if (action.getActionName().equalsIgnoreCase(actionName)) {
                return action;
            }
        }
        return null;
    }

    public String removeAction(Action actionToDelete) {
        actions.remove(actionToDelete);
        main.getDataManager().getQuestsData().set("actions." + actionToDelete.getActionName(), null);
        return "§aAction successfully deleted!";

    }


    public void sendCompletedObjectivesAndProgress(final CommandSender sender, final ActiveQuest activeQuest) {

        for (ActiveObjective activeObjective : activeQuest.getCompletedObjectives()) {

            final String objectiveDisplayName = activeObjective.getObjective().getObjectiveDisplayName();
            final String objectiveDescription = activeObjective.getObjective().getObjectiveDescription();
            if (!objectiveDisplayName.equals("")) {
                sender.sendMessage("§7§m" + activeObjective.getObjective().getObjectiveID() + ". " + activeObjective.getObjective().getObjectiveDisplayName() + ":");
            } else {
                sender.sendMessage("§7§m" + activeObjective.getObjective().getObjectiveID() + ". " + activeObjective.getObjective().getObjectiveType().toString() + ":");
            }

            if (!objectiveDescription.equals("")) {
                sender.sendMessage("   §7§mDescription: §f§m" + objectiveDescription);
            }

            sender.sendMessage(getCompletedObjectiveDescription(activeObjective));
            sender.sendMessage("   §7§mProgress: §f§m" + activeObjective.getCurrentProgress() + " / " + activeObjective.getProgressNeeded());
        }
    }

    public final String getCompletedObjectiveDescription(final ActiveObjective activeObjective) {
        String toReturn = "";
        if (activeObjective.getObjective() instanceof BreakBlocksObjective) {
            toReturn = "    §7§mBlock to break: §f§m" + ((BreakBlocksObjective) activeObjective.getObjective()).getBlockToBreak().toString();
        } else if (activeObjective.getObjective() instanceof CollectItemsObjective) {
            toReturn = "    §7§mItems to collect: §f§m" + ((CollectItemsObjective) activeObjective.getObjective()).getItemToCollect().getType() + " (" + ((CollectItemsObjective) activeObjective.getObjective()).getItemToCollect().getItemMeta().getDisplayName() + ")";
        } else if (activeObjective.getObjective() instanceof TriggerCommandObjective) {
            toReturn = "    §7§mGoal: §f§m" + ((TriggerCommandObjective) activeObjective.getObjective()).getTriggerName();
        } else if (activeObjective.getObjective() instanceof OtherQuestObjective) {
            toReturn = "    §7§mQuest completion: §f§m" + ((OtherQuestObjective) activeObjective.getObjective()).getOtherQuest().getQuestName();
        } else if (activeObjective.getObjective() instanceof KillMobsObjective) {
            toReturn = "    §7§mMob to kill: §f§m" + ((KillMobsObjective) activeObjective.getObjective()).getMobToKill().toString();
        } else if (activeObjective.getObjective() instanceof ConsumeItemsObjective) {
            toReturn = "    §7§mItems to consume: §f§m" + ((ConsumeItemsObjective) activeObjective.getObjective()).getItemToConsume().getType() + " (" + ((ConsumeItemsObjective) activeObjective.getObjective()).getItemToConsume().getItemMeta().getDisplayName() + ")";
        } else if (activeObjective.getObjective() instanceof DeliverItemsObjective) {
            toReturn = "    §7§mItems to deliver: §f§m" + ((DeliverItemsObjective) activeObjective.getObjective()).getItemToCollect().getType() + " (" + ((DeliverItemsObjective) activeObjective.getObjective()).getItemToCollect().getItemMeta().getDisplayName() + ")\n";
            final NPC npc = CitizensAPI.getNPCRegistry().getById(((DeliverItemsObjective) activeObjective.getObjective()).getRecipientNPCID());
            if (npc != null) {
                toReturn += "    §7§mDeliver it to §f§m" + npc.getName();
            } else {
                toReturn += "    §7§mThe delivery NPC is currently not available!";
            }
        } else if (activeObjective.getObjective() instanceof TalkToNPCObjective) {
            final NPC npc = CitizensAPI.getNPCRegistry().getById(((TalkToNPCObjective) activeObjective.getObjective()).getNPCtoTalkID());
            if (npc != null) {
                toReturn = "    §7§mTalk to §f§m" + npc.getName();
            } else {
                toReturn = "    §7§mThe target NPC is currently not available!";
            }
        } else if (activeObjective.getObjective() instanceof EscortNPCObjective) {
            final NPC npc = CitizensAPI.getNPCRegistry().getById(((EscortNPCObjective) activeObjective.getObjective()).getNpcToEscortID());
            final NPC npcDestination = CitizensAPI.getNPCRegistry().getById(((EscortNPCObjective) activeObjective.getObjective()).getNpcToEscortToID());

            if (npc != null && npcDestination != null) {
                toReturn = "    §7§mEscort §f§m" + npc.getName() + " §7§mto §f§m" + npcDestination.getName();
            } else {
                toReturn = "    §7§mThe target or destination NPC is currently not available!";
            }
        }
        if (activeObjective.getObjective().getCompletionNPCID() != -1) {
            final NPC npc = CitizensAPI.getNPCRegistry().getById((activeObjective.getObjective()).getCompletionNPCID());
            if (npc != null) {
                toReturn += "\n    §7§mTo complete: Talk to §b§m" + npc.getName();
            } else {
                toReturn += "\n    §7§mTo complete: Talk to NPC with ID §b§m" + activeObjective.getObjective().getCompletionNPCID() + " §c§m[Currently not available]";
            }

        }
        return toReturn;
    }

    public final String getActiveObjectiveDescription(final ActiveObjective activeObjective) {
        String toReturn = "";
        if (activeObjective.getObjective() instanceof BreakBlocksObjective) {
            toReturn = "    §7Block to break: §f" + ((BreakBlocksObjective) activeObjective.getObjective()).getBlockToBreak().toString();
        } else if (activeObjective.getObjective() instanceof CollectItemsObjective) {
            toReturn = "    §7Items to collect: §f" + ((CollectItemsObjective) activeObjective.getObjective()).getItemToCollect().getType() + " (" + ((CollectItemsObjective) activeObjective.getObjective()).getItemToCollect().getItemMeta().getDisplayName() + ")";
        } else if (activeObjective.getObjective() instanceof TriggerCommandObjective) {
            toReturn = "    §7Goal: §f" + ((TriggerCommandObjective) activeObjective.getObjective()).getTriggerName();
        } else if (activeObjective.getObjective() instanceof OtherQuestObjective) {
            toReturn = "    §7Quest completion: §f" + ((OtherQuestObjective) activeObjective.getObjective()).getOtherQuest().getQuestName();
        } else if (activeObjective.getObjective() instanceof KillMobsObjective) {
            toReturn = "    §7Mob to kill: §f" + ((KillMobsObjective) activeObjective.getObjective()).getMobToKill().toString();
        } else if (activeObjective.getObjective() instanceof ConsumeItemsObjective) {
            toReturn = "    §7Items to consume: §f" + ((ConsumeItemsObjective) activeObjective.getObjective()).getItemToConsume().getType() + " (" + ((ConsumeItemsObjective) activeObjective.getObjective()).getItemToConsume().getItemMeta().getDisplayName() + ")";
        } else if (activeObjective.getObjective() instanceof DeliverItemsObjective) {
            toReturn = "    §7Items to deliver: §f" + ((DeliverItemsObjective) activeObjective.getObjective()).getItemToCollect().getType() + " (" + ((DeliverItemsObjective) activeObjective.getObjective()).getItemToCollect().getItemMeta().getDisplayName() + ")\n";
            final NPC npc = CitizensAPI.getNPCRegistry().getById(((DeliverItemsObjective) activeObjective.getObjective()).getRecipientNPCID());
            if (npc != null) {
                toReturn += "    §7Deliver it to §f" + npc.getName();
            } else {
                toReturn += "    §7The delivery NPC is currently not available!";
            }
        } else if (activeObjective.getObjective() instanceof TalkToNPCObjective) {
            final NPC npc = CitizensAPI.getNPCRegistry().getById(((TalkToNPCObjective) activeObjective.getObjective()).getNPCtoTalkID());
            if (npc != null) {
                toReturn = "    §7Talk to §f" + npc.getName();
            } else {
                toReturn = "    §7The target NPC is currently not available!";
            }
        } else if (activeObjective.getObjective() instanceof EscortNPCObjective) {
            final NPC npc = CitizensAPI.getNPCRegistry().getById(((EscortNPCObjective) activeObjective.getObjective()).getNpcToEscortID());
            final NPC npcDestination = CitizensAPI.getNPCRegistry().getById(((EscortNPCObjective) activeObjective.getObjective()).getNpcToEscortToID());

            if (npc != null && npcDestination != null) {
                toReturn = "    §7Escort §f" + npc.getName() + " §7to §f" + npcDestination.getName();
            } else {
                toReturn = "    §7The target or destination NPC is currently not available!";
            }
        }
        if (activeObjective.getObjective().getCompletionNPCID() != -1) {
            final NPC npc = CitizensAPI.getNPCRegistry().getById((activeObjective.getObjective()).getCompletionNPCID());
            if (npc != null) {
                toReturn += "\n    §7To complete: Talk to §b" + npc.getName();
            } else {
                toReturn += "\n    §7To complete: Talk to NPC with ID §b" + activeObjective.getObjective().getCompletionNPCID() + " §c[Currently not available]";
            }

        }
        return toReturn;
    }

    public void sendActiveObjectivesAndProgress(final CommandSender sender, final ActiveQuest activeQuest) {

        for (ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {

            if (activeObjective.isUnlocked()) {
                final String objectiveDisplayName = activeObjective.getObjective().getObjectiveDisplayName();
                final String objectiveDescription = activeObjective.getObjective().getObjectiveDescription();
                if (!objectiveDisplayName.equals("")) {
                    sender.sendMessage("§e" + activeObjective.getObjective().getObjectiveID() + ". " + activeObjective.getObjective().getObjectiveDisplayName() + ":");
                } else {
                    sender.sendMessage("§e" + activeObjective.getObjective().getObjectiveID() + ". " + activeObjective.getObjective().getObjectiveType().toString() + ":");
                }

                if (!objectiveDescription.equals("")) {
                    sender.sendMessage("   §9Description: §6" + objectiveDescription);
                }

                sender.sendMessage(getActiveObjectiveDescription(activeObjective));

                sender.sendMessage("   §7Progress: §f" + activeObjective.getCurrentProgress() + " / " + activeObjective.getProgressNeeded());
            } else {
                sender.sendMessage("§e" + activeObjective.getObjective().getObjectiveID() + ". §7§l[HIDDEN]");

            }

        }
    }

    public void sendObjectives(final CommandSender sender, final Quest quest) {
        for (final Objective objective : quest.getObjectives()) {
            final String objectiveDisplayName = objective.getObjectiveDisplayName();
            final String objectiveDescription = objective.getObjectiveDescription();
            if (!objectiveDisplayName.equals("")) {
                sender.sendMessage("§a" + objective.getObjectiveID() + ". §e" + objectiveDisplayName);
            } else {
                sender.sendMessage("§a" + objective.getObjectiveID() + ". §e" + objective.getObjectiveType().toString());
            }

            if (!objectiveDisplayName.equals("")) {
                sender.sendMessage("   §9Description: §6" + objectiveDescription);
            }

            if (objective instanceof BreakBlocksObjective) {
                sender.sendMessage("    §7Break Blocks: §f" + ((BreakBlocksObjective) objective).getBlockToBreak().toString() + " §7x " + objective.getProgressNeeded());
            } else if (objective instanceof CollectItemsObjective) {
                sender.sendMessage("    §7Collect Items: §f" + ((CollectItemsObjective) objective).getItemToCollect().getType() + " (" + ((CollectItemsObjective) objective).getItemToCollect().getItemMeta().getDisplayName() + ")" + " §7x " + objective.getProgressNeeded());
            } else if (objective instanceof TriggerCommandObjective) {
                sender.sendMessage("    §7Reach Goal: §f" + ((TriggerCommandObjective) objective).getTriggerName() + " §7x " + objective.getProgressNeeded());
            } else if (objective instanceof OtherQuestObjective) {
                sender.sendMessage("    §7Complete Quest: §f" + ((OtherQuestObjective) objective).getOtherQuest().getQuestName() + " §7x " + objective.getProgressNeeded());
            } else if (objective instanceof KillMobsObjective) {
                sender.sendMessage("    §7Kill Mob: §f" + ((KillMobsObjective) objective).getMobToKill().toString() + " §7x " + objective.getProgressNeeded());
            } else if (objective instanceof ConsumeItemsObjective) {
                sender.sendMessage("    §7Consume Item: §f" + ((ConsumeItemsObjective) objective).getItemToConsume().getType() + " (" + ((ConsumeItemsObjective) objective).getItemToConsume().getItemMeta().getDisplayName() + ")" + " §7x " + objective.getProgressNeeded());
            } else if (objective instanceof DeliverItemsObjective) {
                sender.sendMessage("    §7Items to deliver: §f" + ((DeliverItemsObjective) objective).getItemToCollect().getType() + " (" + ((DeliverItemsObjective) objective).getItemToCollect().getItemMeta().getDisplayName() + ")");
                final NPC npc = CitizensAPI.getNPCRegistry().getById(((DeliverItemsObjective) objective).getRecipientNPCID());
                if (npc != null) {
                    sender.sendMessage("    §7Deliver it to §f" + npc.getName());
                } else {
                    sender.sendMessage("    §7The delivery NPC is currently not available!");
                }
            } else if (objective instanceof TalkToNPCObjective) {
                final NPC npc = CitizensAPI.getNPCRegistry().getById(((TalkToNPCObjective) objective).getNPCtoTalkID());
                if (npc != null) {
                    sender.sendMessage("    §7Talk to §f" + npc.getName());
                } else {
                    sender.sendMessage("    §7The target NPC is currently not available!");
                }
            } else if (objective instanceof EscortNPCObjective) {
                final NPC npc = CitizensAPI.getNPCRegistry().getById(((EscortNPCObjective) objective).getNpcToEscortID());
                final NPC npcDestination = CitizensAPI.getNPCRegistry().getById(((EscortNPCObjective) objective).getNpcToEscortToID());

                if (npc != null && npcDestination != null) {
                    sender.sendMessage("    §7Escort §f" + npc.getName() + " §7to §f" + npcDestination.getName());
                } else {
                    sender.sendMessage("    §7The target or destination NPC is currently not available!");
                }
            }

            if (objective.getCompletionNPCID() != -1) {
                final NPC npc = CitizensAPI.getNPCRegistry().getById((objective).getCompletionNPCID());
                if (npc != null) {
                    sender.sendMessage("    §7To complete: Talk to §b" + npc.getName());
                } else {
                    sender.sendMessage("    §7To complete: Talk to NPC with ID §b" + objective.getCompletionNPCID() + " §c[Currently not available]");
                }

            }

        }
    }

    public void sendObjectivesAdmin(final CommandSender sender, final Quest quest) {

        for (final Objective objective : quest.getObjectives()) {
            final String objectiveDisplayName = objective.getObjectiveDisplayName();
            final String objectiveDescription = objective.getObjectiveDescription();
            if (!objectiveDisplayName.equals("")) {
                sender.sendMessage("§a" + objective.getObjectiveID() + ". §e" + objectiveDisplayName);
            } else {
                sender.sendMessage("§a" + objective.getObjectiveID() + ". §e" + objective.getObjectiveType().toString());
            }

            if (!objectiveDisplayName.equals("")) {
                sender.sendMessage("   §9Description: §6" + objectiveDescription);
            }


            sender.sendMessage("   §9Depending objectives:");
            int counter2 = 1;
            for (final Objective dependantObjective : objective.getDependantObjectives()) {
                sender.sendMessage("         §e" + counter2 + ". Objective ID: §b" + dependantObjective.getObjectiveID());
                counter2++;
            }
            if (counter2 == 1) {
                sender.sendMessage("      §8No depending objectives found!");
            }


            if (objective instanceof BreakBlocksObjective) {
                sender.sendMessage("    §7Break Blocks: §f" + ((BreakBlocksObjective) objective).getBlockToBreak().toString() + " §7x " + objective.getProgressNeeded());
            } else if (objective instanceof CollectItemsObjective) {
                sender.sendMessage("    §7Collect Items: §f" + ((CollectItemsObjective) objective).getItemToCollect().getType() + " (" + ((CollectItemsObjective) objective).getItemToCollect().getItemMeta().getDisplayName() + ")" + " §7x " + objective.getProgressNeeded());
            } else if (objective instanceof TriggerCommandObjective) {
                sender.sendMessage("    §7Reach Goal: §f" + ((TriggerCommandObjective) objective).getTriggerName() + " §7x " + objective.getProgressNeeded());
            } else if (objective instanceof OtherQuestObjective) {
                sender.sendMessage("    §7Complete Quest: §f" + ((OtherQuestObjective) objective).getOtherQuest().getQuestName() + " §7x " + objective.getProgressNeeded());
            } else if (objective instanceof KillMobsObjective) {
                sender.sendMessage("    §7Kill Mob: §f" + ((KillMobsObjective) objective).getMobToKill().toString() + " §7x " + objective.getProgressNeeded());
            } else if (objective instanceof ConsumeItemsObjective) {
                sender.sendMessage("    §7Consume Item: §f" + ((ConsumeItemsObjective) objective).getItemToConsume().getType() + " (" + ((ConsumeItemsObjective) objective).getItemToConsume().getItemMeta().getDisplayName() + ")" + " §7x " + objective.getProgressNeeded());
            } else if (objective instanceof DeliverItemsObjective) {
                sender.sendMessage("    §7Items to deliver: §f" + ((DeliverItemsObjective) objective).getItemToCollect().getType() + " (" + ((DeliverItemsObjective) objective).getItemToCollect().getItemMeta().getDisplayName() + ")");
                final NPC npc = CitizensAPI.getNPCRegistry().getById(((DeliverItemsObjective) objective).getRecipientNPCID());
                if (npc != null) {
                    sender.sendMessage("    §7Deliver it to §f" + npc.getName());
                } else {
                    sender.sendMessage("    §7The delivery NPC is currently not available!");
                }
            } else if (objective instanceof TalkToNPCObjective) {
                final NPC npc = CitizensAPI.getNPCRegistry().getById(((TalkToNPCObjective) objective).getNPCtoTalkID());
                if (npc != null) {
                    sender.sendMessage("    §7Talk to §f" + npc.getName());
                } else {
                    sender.sendMessage("    §7The target NPC is currently not available!");
                }
            } else if (objective instanceof EscortNPCObjective) {
                final NPC npc = CitizensAPI.getNPCRegistry().getById(((EscortNPCObjective) objective).getNpcToEscortID());
                final NPC npcDestination = CitizensAPI.getNPCRegistry().getById(((EscortNPCObjective) objective).getNpcToEscortToID());

                if (npc != null && npcDestination != null) {
                    sender.sendMessage("    §7Escort §f" + npc.getName() + " §7to §f" + npcDestination.getName());
                } else {
                    sender.sendMessage("    §7The target or destination NPC is currently not available!");
                }
            }

            if (objective.getCompletionNPCID() != -1) {
                final NPC npc = CitizensAPI.getNPCRegistry().getById((objective).getCompletionNPCID());
                if (npc != null) {
                    sender.sendMessage("    §7To complete: Talk to §b" + npc.getName());
                } else {
                    sender.sendMessage("    §7To complete: Talk to NPC with ID §b" + objective.getCompletionNPCID() + " §c[Currently not available]");
                }

            }
        }
    }


    public void sendActiveObjective(final CommandSender sender, ActiveObjective activeObjective) {

        if (activeObjective.isUnlocked()) {
            final String objectiveDisplayName = activeObjective.getObjective().getObjectiveDisplayName();
            final String objectiveDescription = activeObjective.getObjective().getObjectiveDescription();
            if (!objectiveDisplayName.equals("")) {
                sender.sendMessage("§e" + activeObjective.getObjective().getObjectiveID() + ". " + activeObjective.getObjective().getObjectiveDisplayName() + ":");
            } else {
                sender.sendMessage("§e" + activeObjective.getObjective().getObjectiveID() + ". " + activeObjective.getObjective().getObjectiveType().toString() + ":");
            }

            if (!objectiveDisplayName.equals("")) {
                sender.sendMessage("   §9Description: §6" + objectiveDescription);
            }

            if (activeObjective.getObjective() instanceof BreakBlocksObjective) {
                sender.sendMessage("    §7Block to break: §f" + ((BreakBlocksObjective) activeObjective.getObjective()).getBlockToBreak().toString());
            } else if (activeObjective.getObjective() instanceof CollectItemsObjective) {
                sender.sendMessage("    §7Items to collect: §f" + ((CollectItemsObjective) activeObjective.getObjective()).getItemToCollect().getType() + " (" + ((CollectItemsObjective) activeObjective.getObjective()).getItemToCollect().getItemMeta().getDisplayName() + ")");
            } else if (activeObjective.getObjective() instanceof TriggerCommandObjective) {
                sender.sendMessage("    §7Goal: §f" + ((TriggerCommandObjective) activeObjective.getObjective()).getTriggerName());
            } else if (activeObjective.getObjective() instanceof OtherQuestObjective) {
                sender.sendMessage("    §7Quest completion: §f" + ((OtherQuestObjective) activeObjective.getObjective()).getOtherQuest().getQuestName());
            } else if (activeObjective.getObjective() instanceof KillMobsObjective) {
                sender.sendMessage("    §7Mob to kill: §f" + ((KillMobsObjective) activeObjective.getObjective()).getMobToKill().toString());
            } else if (activeObjective.getObjective() instanceof ConsumeItemsObjective) {
                sender.sendMessage("    §7Items to consume: §f" + ((ConsumeItemsObjective) activeObjective.getObjective()).getItemToConsume().getType() + " (" + ((ConsumeItemsObjective) activeObjective.getObjective()).getItemToConsume().getItemMeta().getDisplayName() + ")");
            } else if (activeObjective.getObjective() instanceof DeliverItemsObjective) {
                sender.sendMessage("    §7Items to deliver: §f" + ((DeliverItemsObjective) activeObjective.getObjective()).getItemToCollect().getType() + " (" + ((DeliverItemsObjective) activeObjective.getObjective()).getItemToCollect().getItemMeta().getDisplayName() + ")");
                final NPC npc = CitizensAPI.getNPCRegistry().getById(((DeliverItemsObjective) activeObjective.getObjective()).getRecipientNPCID());
                if (npc != null) {
                    sender.sendMessage("    §7Deliver it to §f" + npc.getName());
                } else {
                    sender.sendMessage("    §7The delivery NPC is currently not available!");
                }
            } else if (activeObjective.getObjective() instanceof TalkToNPCObjective) {
                final NPC npc = CitizensAPI.getNPCRegistry().getById(((TalkToNPCObjective) activeObjective.getObjective()).getNPCtoTalkID());
                if (npc != null) {
                    sender.sendMessage("    §7Talk to §f" + npc.getName());
                } else {
                    sender.sendMessage("    §7The target NPC is currently not available!");
                }
            } else if (activeObjective.getObjective() instanceof EscortNPCObjective) {
                final NPC npc = CitizensAPI.getNPCRegistry().getById(((EscortNPCObjective) activeObjective.getObjective()).getNpcToEscortID());
                final NPC npcDestination = CitizensAPI.getNPCRegistry().getById(((EscortNPCObjective) activeObjective.getObjective()).getNpcToEscortToID());

                if (npc != null && npcDestination != null) {
                    sender.sendMessage("    §7Escort §f" + npc.getName() + " §7to §f" + npcDestination.getName());
                } else {
                    sender.sendMessage("    §7The target or destination NPC is currently not available!");
                }
            }
            if (activeObjective.getObjective().getCompletionNPCID() != -1) {
                final NPC npc = CitizensAPI.getNPCRegistry().getById((activeObjective.getObjective()).getCompletionNPCID());
                if (npc != null) {
                    sender.sendMessage("    §7To complete: Talk to §b" + npc.getName());
                } else {
                    sender.sendMessage("    §7To complete: Talk to NPC with ID §b" + activeObjective.getObjective().getCompletionNPCID() + " §c[Currently not available]");
                }

            }


            sender.sendMessage("   §7Progress: §f" + activeObjective.getCurrentProgress() + " / " + activeObjective.getProgressNeeded());
        } else {
            sender.sendMessage("§e" + activeObjective.getObjective().getObjectiveID() + ". §7§l[HIDDEN]");

        }


    }
}
