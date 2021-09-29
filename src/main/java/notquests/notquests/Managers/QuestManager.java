package notquests.notquests.Managers;

import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitInfo;
import net.md_5.bungee.api.chat.*;
import notquests.notquests.NotQuests;
import notquests.notquests.QuestGiverNPCTrait;
import notquests.notquests.Structs.ActiveObjective;
import notquests.notquests.Structs.ActiveQuest;
import notquests.notquests.Structs.Objectives.*;
import notquests.notquests.Structs.Quest;
import notquests.notquests.Structs.QuestPlayer;
import notquests.notquests.Structs.Requirements.*;
import notquests.notquests.Structs.Rewards.*;
import notquests.notquests.Structs.Triggers.Action;
import notquests.notquests.Structs.Triggers.Trigger;
import notquests.notquests.Structs.Triggers.TriggerTypes.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;


public class QuestManager {

    private final NotQuests main;

    private final ArrayList<Quest> quests;
    private final ArrayList<Action> actions;

    private boolean questDataLoaded = false;

    private final String rewardTypesList, requirementsTypesList;
    private String objectiveTypesList;


    public QuestManager(NotQuests main) {
        this.main = main;
        quests = new ArrayList<>();
        actions = new ArrayList<>();

        objectiveTypesList = """
                §eObjective Types:
                §bBreakBlocks
                §bCollectItems
                §bCraftItems
                §bKillMobs
                §bTriggerCommand
                §bOtherQuest
                §bConsumeItems
                §bDeliverItems
                §bTalkToNPC
                §bEscortNPC
                """;

        if (main.isEliteMobsEnabled()) {
            objectiveTypesList += "\n§9KillEliteMobs §7[Special Integration]";
        }

        rewardTypesList = """
                §eReward Types:
                §bConsoleCommand
                §bQuestPoints
                §bItem
                §bMoney
                """;

        requirementsTypesList = """
                §eRequirement Types:
                §bOtherQuest
                §bQuestPoints
                §bPermission
                §bMoney
                §bPlaceholder (WIP)
                """;
    }


    public final String createQuest(String questName) {
        if (getQuest(questName) == null) {
            if(questName.contains("°")){
                return "The symbol ° cannot be used, because it's used for some important, plugin-internal stuff.";
            }
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

        if(main.isCitizensEnabled()){
            main.getLogManager().log(Level.INFO, "Registering Citizens nquestgiver trait...");

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
            main.getLogManager().log(Level.INFO, "Citizens nquestgiver trait has been registered!");
        }


        try {
            main.getLogManager().log(Level.INFO, "Loading Quests data...");

            quests.clear();

            //Actions
            final ConfigurationSection actionsConfigurationSection = main.getDataManager().getQuestsData().getConfigurationSection("actions");
            if (actionsConfigurationSection != null) {
                for (final String actionName : actionsConfigurationSection.getKeys(false)) {
                    final String consoleCommand = main.getDataManager().getQuestsData().getString("actions." + actionName + ".consoleCommand", "");
                    if (consoleCommand.equalsIgnoreCase("")) {
                        main.getLogManager().log(Level.WARNING, "Action has an empty console command. This should NOT be possible! Creating an action with an empty console command... Action name: §b" + actionName);

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
                        main.getLogManager().log(Level.WARNING, "§eNotQuests > Action already exists. This should NOT be possible! Skipping action creation... Action name: §b" + actionName);

                        main.getLogManager().log(Level.SEVERE, "Plugin disabled, because there was an error while loading quests action data.");
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
                                if (triggerType == TriggerType.COMPLETE) {
                                    trigger = new CompleteTrigger(main, foundAction, applyOn, worldName);
                                } else if (triggerType == TriggerType.BEGIN) {
                                    trigger = new BeginTrigger(main, foundAction, applyOn, worldName);
                                } else if (triggerType == TriggerType.DEATH) {

                                    trigger = new DeathTrigger(main, foundAction, applyOn, worldName, amountNeeded);
                                } else if (triggerType == TriggerType.FAIL) {
                                    trigger = new FailTrigger(main, foundAction, applyOn, worldName);
                                } else if (triggerType == TriggerType.DISCONNECT) {
                                    trigger = new DisconnectTrigger(main, foundAction, applyOn, worldName);
                                } else if (triggerType == TriggerType.NPCDEATH) {
                                    final int npcToDie = main.getDataManager().getQuestsData().getInt("quests." + questName + ".triggers." + triggerNumber + ".specifics.npcToDie");
                                    trigger = new NPCDeathTrigger(main, foundAction, applyOn, worldName, amountNeeded, npcToDie);
                                } else if (triggerType == TriggerType.WORLDENTER) {
                                    final String worldToEnter = main.getDataManager().getQuestsData().getString("quests." + questName + ".triggers." + triggerNumber + ".specifics.worldToEnter", "ALL");
                                    trigger = new WorldEnterTrigger(main, foundAction, applyOn, worldName, amountNeeded, worldToEnter);
                                } else if (triggerType == TriggerType.WORLDLEAVE) {
                                    final String worldToLeave = main.getDataManager().getQuestsData().getString("quests." + questName + ".triggers." + triggerNumber + ".specifics.worldToLeave", "ALL");
                                    trigger = new WorldLeaveTrigger(main, foundAction, applyOn, worldName, amountNeeded, worldToLeave);
                                } else {
                                    main.getLogManager().log(Level.SEVERE, "ERROR when loading trigger with the triggerNumber §b" + triggerNumber + " §c: TriggerType is unknown. Trigger creation SKIPPED!");

                                    main.getLogManager().log(Level.SEVERE, "Plugin disabled, because there was an error while loading quests trigger data.");
                                    main.getDataManager().setSavingEnabled(false);
                                    main.getServer().getPluginManager().disablePlugin(main);
                                }
                                if (trigger != null) {
                                    quest.addTrigger(trigger);
                                }

                            } else {
                                main.getLogManager().log(Level.SEVERE, "ERROR when loading trigger with the triggerNumber §b" + triggerNumber + " §c: Action could not be loaded. Trigger creation SKIPPED!");

                                main.getLogManager().log(Level.SEVERE, "Plugin disabled, because there was an error while loading quests trigger data.");
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
                                main.getLogManager().log(Level.SEVERE, "Error parsing objective Type of objective with ID §b" + objectiveNumber + "§c and Quest §b" + quest.getQuestName() + "§c. Objective creation skipped...");

                                ex.printStackTrace();
                                main.getLogManager().log(Level.SEVERE, "Plugin disabled, because there was an error while loading quests objective Type data.");
                                main.getDataManager().setSavingEnabled(false);
                                main.getServer().getPluginManager().disablePlugin(main);
                            }
                            final int progressNeeded = main.getDataManager().getQuestsData().getInt("quests." + questName + ".objectives." + objectiveNumber + ".progressNeeded");

                            int objectiveID = -1;
                            boolean validObjectiveID = true;
                            try {
                                objectiveID = Integer.parseInt(objectiveNumber);
                            } catch (java.lang.NumberFormatException ex) {
                                main.getLogManager().log(Level.SEVERE, "Error parsing loaded objective ID §b" + objectiveNumber + "§c. Objective creation skipped...");

                                validObjectiveID = false;
                                main.getLogManager().log(Level.SEVERE, "Plugin disabled, because there was an error while loading quests objective ID data.");
                                main.getDataManager().setSavingEnabled(false);
                                main.getServer().getPluginManager().disablePlugin(main);
                            }
                            if (validObjectiveID && objectiveID > 0 && objectiveType != null) {


                                Objective objective = null;

                                try {
                                    if (objectiveType == ObjectiveType.BreakBlocks) {
                                        final Material blockToBreak = Material.valueOf(main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.blockToBreak.material"));
                                        final boolean deductIfBlockPlaced = main.getDataManager().getQuestsData().getBoolean("quests." + questName + ".objectives." + objectiveNumber + ".specifics.deductIfBlockPlaced");
                                        objective = new BreakBlocksObjective(main, quest, objectiveID, blockToBreak, progressNeeded, deductIfBlockPlaced);
                                    } else if (objectiveType == ObjectiveType.CollectItems) {
                                        final ItemStack itemToCollect = main.getDataManager().getQuestsData().getItemStack("quests." + questName + ".objectives." + objectiveNumber + ".specifics.itemToCollect.itemstack");
                                        objective = new CollectItemsObjective(main, quest, objectiveID, itemToCollect, progressNeeded);
                                    } else if (objectiveType == ObjectiveType.CraftItems) {
                                        final ItemStack itemToCraft = main.getDataManager().getQuestsData().getItemStack("quests." + questName + ".objectives." + objectiveNumber + ".specifics.itemToCraft.itemstack");
                                        objective = new CraftItemsObjective(main, quest, objectiveID, itemToCraft, progressNeeded);
                                    } else if (objectiveType == ObjectiveType.TriggerCommand) {
                                        final String triggerName = main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.triggerName");
                                        objective = new TriggerCommandObjective(main, quest, objectiveID, triggerName, progressNeeded);
                                    } else if (objectiveType == ObjectiveType.OtherQuest) {
                                        final String otherQuestName = main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.otherQuestName");
                                        final boolean countPreviousCompletions = main.getDataManager().getQuestsData().getBoolean("quests." + questName + ".objectives." + objectiveNumber + ".specifics.countPreviousCompletions");
                                        objective = new OtherQuestObjective(main, quest, objectiveID, otherQuestName, progressNeeded, countPreviousCompletions);
                                    } else if (objectiveType == ObjectiveType.KillMobs) {
                                        final String mobToKill = main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.mobToKill");
                                        final int amountToKill = main.getDataManager().getQuestsData().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.amountToKill");
                                        objective = new KillMobsObjective(main, quest, objectiveID, mobToKill, amountToKill);
                                    } else if (objectiveType == ObjectiveType.ConsumeItems) {
                                        final ItemStack itemToConsume = main.getDataManager().getQuestsData().getItemStack("quests." + questName + ".objectives." + objectiveNumber + ".specifics.itemToConsume.itemstack");
                                        objective = new ConsumeItemsObjective(main, quest, objectiveID, itemToConsume, progressNeeded);
                                    } else if (objectiveType == ObjectiveType.DeliverItems) {
                                        final ItemStack itemToCollect = main.getDataManager().getQuestsData().getItemStack("quests." + questName + ".objectives." + objectiveNumber + ".specifics.itemToCollect.itemstack");
                                        final int recipientNPCID = main.getDataManager().getQuestsData().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.recipientNPCID");

                                        if (recipientNPCID != -1) {
                                            objective = new DeliverItemsObjective(main, quest, objectiveID, itemToCollect, progressNeeded, recipientNPCID);
                                        } else {
                                            final String armorStandUUIDString = main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.recipientArmorStandID");
                                            if (armorStandUUIDString != null) {
                                                final UUID armorStandUUID = UUID.fromString(armorStandUUIDString);
                                                objective = new DeliverItemsObjective(main, quest, objectiveID, itemToCollect, progressNeeded, armorStandUUID);
                                            } else {
                                                objective = new DeliverItemsObjective(main, quest, objectiveID, itemToCollect, progressNeeded, recipientNPCID);
                                            }
                                        }
                                    } else if (objectiveType == ObjectiveType.TalkToNPC) {
                                        final int NPCtoTalkID = main.getDataManager().getQuestsData().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.NPCtoTalkID", -1);
                                        if (NPCtoTalkID != -1) {
                                            objective = new TalkToNPCObjective(main, quest, objectiveID, NPCtoTalkID);
                                        } else {
                                            final String armorStandUUIDString = main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".specifics.ArmorStandToTalkUUID");
                                            if (armorStandUUIDString != null) {
                                                final UUID armorStandUUID = UUID.fromString(armorStandUUIDString);
                                                objective = new TalkToNPCObjective(main, quest, objectiveID, armorStandUUID);
                                            } else {
                                                objective = new TalkToNPCObjective(main, quest, objectiveID, NPCtoTalkID);
                                            }

                                        }
                                    } else if (objectiveType == ObjectiveType.EscortNPC) {
                                        final int NPCtoEscortID = main.getDataManager().getQuestsData().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.NPCToEscortID");
                                        final int destinationNPCID = main.getDataManager().getQuestsData().getInt("quests." + questName + ".objectives." + objectiveNumber + ".specifics.destinationNPCID");
                                        objective = new EscortNPCObjective(main, quest, objectiveID, NPCtoEscortID, destinationNPCID);
                                    }                                } catch (java.lang.NullPointerException ex) {
                                    main.getLogManager().log(Level.SEVERE, "Error parsing objective Type of objective with ID §b" + objectiveNumber + "§c and Quest §b" + quest.getQuestName() + "§c. Objective creation skipped...");

                                    ex.printStackTrace();
                                    main.getLogManager().log(Level.SEVERE, "Plugin disabled, because there was an error while loading quests objective Type data.");
                                    main.getDataManager().setSavingEnabled(false);
                                    main.getServer().getPluginManager().disablePlugin(main);
                                }




                                if (objective != null) {

                                    final String objectiveDisplayName = main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".displayName", "");
                                    final String objectiveDescription = main.getDataManager().getQuestsData().getString("quests." + questName + ".objectives." + objectiveNumber + ".description", "");
                                    final int completionNPCID = main.getDataManager().getQuestsData().getInt("quests." + quest.getQuestName() + ".objectives." + objectiveNumber + ".completionNPCID", -1);
                                    final String completionArmorStandUUIDString = main.getDataManager().getQuestsData().getString("quests." + quest.getQuestName() + ".objectives." + objectiveNumber + ".completionArmorStandUUID", null);
                                    if (completionArmorStandUUIDString != null) {
                                        final UUID completionArmorStandUUID = UUID.fromString(completionArmorStandUUIDString);
                                        objective.setCompletionArmorStandUUID(completionArmorStandUUID, false);
                                    }

                                    objective.setObjectiveDisplayName(objectiveDisplayName, false);
                                    objective.setObjectiveDescription(objectiveDescription, false);
                                    objective.setCompletionNPCID(completionNPCID, false);
                                    quest.addObjective(objective, false);
                                } else {
                                    main.getLogManager().log(Level.SEVERE, "Error loading objective");

                                    main.getLogManager().log(Level.SEVERE, "Plugin disabled, because there was an error while loading quests objective data.");
                                    main.getDataManager().setSavingEnabled(false);
                                    main.getServer().getPluginManager().disablePlugin(main);
                                }

                            } else {
                                main.getLogManager().log(Level.SEVERE, "Plugin disabled, because there was an error while loading quests objective data (2).");
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
                            } else if (rewardType == RewardType.Item) {
                                ItemStack rewardItem = main.getDataManager().getQuestsData().getItemStack("quests." + questName + ".rewards." + rewardNumber + ".specifics.rewardItem");
                                reward = new ItemReward(main, rewardItem);
                            } else if (rewardType == RewardType.Money) {
                                long rewardedMoneyAmount = main.getDataManager().getQuestsData().getLong("quests." + questName + ".rewards." + rewardNumber + ".specifics.rewardedMoneyAmount");
                                reward = new MoneyReward(main, rewardedMoneyAmount);
                            }

                            if (reward != null) {
                                quest.addReward(reward);
                            } else {
                                main.getLogManager().log(Level.SEVERE, "Error loading reward");

                                main.getLogManager().log(Level.SEVERE, "Plugin disabled, because there was an error while loading quests reward data.");
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
                                main.getLogManager().log(Level.SEVERE, "Error loading requirement");

                                main.getLogManager().log(Level.SEVERE, "Plugin disabled, because there was an error while loading quests requirement data.");
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
            main.getLogManager().log(Level.SEVERE, "Plugin disabled, because there was an exception while loading quests data.");
            main.getDataManager().setSavingEnabled(false);
            main.getServer().getPluginManager().disablePlugin(main);
            //return;
        }


    }


    public final ArrayList<Quest> getAllQuestsAttachedToArmorstand(final ArmorStand armorstand) {
        return new ArrayList<>() {{
            PersistentDataContainer armorstandPDB = armorstand.getPersistentDataContainer();
            NamespacedKey attachedShowingQuestsKey = new NamespacedKey(main, "notquests-attachedQuests-showing");
            NamespacedKey attachedNonShowingQuestsKey = new NamespacedKey(main, "notquests-attachedQuests-nonshowing");

            //Showing
            if (armorstandPDB.has(attachedShowingQuestsKey, PersistentDataType.STRING)) {
                String existingAttachedQuests = armorstandPDB.get(attachedShowingQuestsKey, PersistentDataType.STRING);
                if (existingAttachedQuests != null && existingAttachedQuests.length() >= 1) {
                    for (String split : existingAttachedQuests.split("°")) {
                        final Quest foundQuest = getQuest(split);
                        if (foundQuest != null) {
                            add(foundQuest);
                        }
                    }
                }
            }

            //Nonshowing
            if (armorstandPDB.has(attachedNonShowingQuestsKey, PersistentDataType.STRING)) {
                String existingAttachedQuests = armorstandPDB.get(attachedNonShowingQuestsKey, PersistentDataType.STRING);
                if (existingAttachedQuests != null && existingAttachedQuests.length() >= 1) {
                    for (String split : existingAttachedQuests.split("°")) {
                        final Quest foundQuest = getQuest(split);
                        if (foundQuest != null) {
                            add(foundQuest);
                        }
                    }
                }
            }

        }};

    }
    public final ArrayList<Quest> getQuestsAttachedToArmorstandWithShowing(final ArmorStand armorstand) {
        return new ArrayList<>() {{
            PersistentDataContainer armorstandPDB = armorstand.getPersistentDataContainer();
            NamespacedKey attachedQuestsKey = new NamespacedKey(main, "notquests-attachedQuests-showing");

            if (armorstandPDB.has(attachedQuestsKey, PersistentDataType.STRING)) {
                String existingAttachedQuests = armorstandPDB.get(attachedQuestsKey, PersistentDataType.STRING);
                if (existingAttachedQuests != null && existingAttachedQuests.length() >= 1) {
                    for (String split : existingAttachedQuests.split("°")) {
                        final Quest foundQuest = getQuest(split);
                        if (foundQuest != null) {
                            add(foundQuest);
                        }
                    }
                }
            }
        }};

    }
    public final ArrayList<Quest> getQuestsAttachedToArmorstandWithoutShowing(final ArmorStand armorstand) {
        return new ArrayList<>() {{
            PersistentDataContainer armorstandPDB = armorstand.getPersistentDataContainer();
            NamespacedKey attachedQuestsKey = new NamespacedKey(main, "notquests-attachedQuests-nonshowing");

            if (armorstandPDB.has(attachedQuestsKey, PersistentDataType.STRING)) {
                String existingAttachedQuests = armorstandPDB.get(attachedQuestsKey, PersistentDataType.STRING);
                if (existingAttachedQuests != null && existingAttachedQuests.length() >= 1) {
                    for (String split : existingAttachedQuests.split("°")) {
                        final Quest foundQuest = getQuest(split);
                        if (foundQuest != null) {
                            add(foundQuest);
                        }
                    }
                }
            }
        }};
    }

    public final ArrayList<Quest> getAllQuestsAttachedToNPC(final NPC npc) {
        final ArrayList<Quest> questsAttached = new ArrayList<>();
        for (Quest quest : quests) {
            if (quest.getAttachedNPCsWithQuestShowing().contains(npc) || quest.getAttachedNPCsWithoutQuestShowing().contains(npc)) {
                questsAttached.add(quest);
            }
        }
        // System.out.println("§esize: " + questsattached.size());
        return questsAttached;
    }

    public final ArrayList<Quest> getQuestsAttachedToNPCWithShowing(final NPC npc) {
        final ArrayList<Quest> questsAttached = new ArrayList<>();
        for (Quest quest : quests) {
            if (quest.getAttachedNPCsWithQuestShowing().contains(npc)) {
                questsAttached.add(quest);
            }
        }
        // System.out.println("§esize: " + questsattached.size());
        return questsAttached;
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

    public final ArrayList<NPC> getAllNPCsAttachedToQuest(final Quest quest) {
        final ArrayList<NPC> npcsAttached = new ArrayList<>();
        npcsAttached.addAll(quest.getAttachedNPCsWithQuestShowing());
        npcsAttached.addAll(quest.getAttachedNPCsWithoutQuestShowing());
        return npcsAttached;
    }

    public final ArrayList<NPC> getNPCsAttachedToQuestWithShowing(final Quest quest) {
        return quest.getAttachedNPCsWithQuestShowing();
    }

    public final ArrayList<NPC> getNPCsAttachedToQuestWithoutShowing(final Quest quest) {
        return quest.getAttachedNPCsWithoutQuestShowing();
    }


    public boolean sendQuestsPreviewOfQuestShownArmorstands(ArmorStand armorStand, Player player) {
        final ArrayList<Quest> questsAttachedToNPC = getQuestsAttachedToArmorstandWithShowing(armorStand);

        //No quests attached or all quests are set to not showing (more likely). THen nothing should show. That should make it work with Interactions plugin and takeEnabled = false.
        if (questsAttachedToNPC.size() == 0) {
            return false;
        }

        if (main.getDataManager().getConfiguration().isQuestPreviewUseGUI()) {
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
            main.getLogManager().log(Level.INFO, "§7NotQuests > All quest count: " + quests.size());

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

                player.spigot().sendMessage(component);


                counter++;
            }
            //getQuestsAttachedToNPC(npc);
        }

        return true;

    }


    public void sendQuestsPreviewOfQuestShownNPCs(NPC npc, Player player) {
        final ArrayList<Quest> questsAttachedToNPC = getQuestsAttachedToNPCWithShowing(npc);

        //No quests attached or all quests are set to not showing (more likely). THen nothing should show. That should make it work with Interactions plugin and takeEnabled = false.
        if(questsAttachedToNPC.size() == 0){
            return;
        }

        if (main.getDataManager().getConfiguration().isQuestPreviewUseGUI()) {
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
            main.getLogManager().log(Level.INFO, "§7NotQuests > All quest count: " + quests.size());

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

                player.spigot().sendMessage(component);


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
            player.sendMessage(main.getLanguageManager().getString("chat.missing-quest-description"));
        }

        player.sendMessage("§9Quest Requirements:");

        player.sendMessage(getQuestRequirements(quest));


        BaseComponent acceptComponent = new TextComponent("§a§l[ACCEPT THIS QUEST]");
        acceptComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nquests take " + quest.getQuestName()));
        acceptComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§aClick to accept this quest").create()));



       /*Paper only Component acceptQuestComponent = Component.text("[ACCEPT THIS QUEST]", NamedTextColor.GREEN, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/nquests take " + quest.getQuestName()))
                .hoverEvent(HoverEvent.showText(Component.text("Click to accept this quest", NamedTextColor.GREEN)));*/


        player.sendMessage("");
        //only paper player.sendMessage(acceptQuestComponent);
        player.spigot().sendMessage(acceptComponent);
        player.sendMessage("§7-----------------------------------");


    }

    public void loadNPCData() {
        main.getLogManager().log(Level.INFO, "Loading NPC data...");

        if(!main.isCitizensEnabled()){
            main.getLogManager().log(Level.WARNING, "§eNPC data loading has been cancelled, because Citizens is not installed. Install the Citizens plugin if you want NPC stuff to work.");
            return;
        }

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
                                                    main.getLogManager().log(Level.INFO, "attaching quest with the name §b" + quest.getQuestName() + " §ato NPC with the ID §b" + npc.getId() + " §aand name §b" + npc.getName());

                                                    quest.removeNPC(npc);
                                                    quest.bindToNPC(npc, questShowing);


                                                } else {
                                                    main.getLogManager().log(Level.WARNING, "Error attaching npc with ID §b" + main.getDataManager().getQuestsData().getInt("quests." + questName + ".npcs." + npcNumber + ".npcID") + " to quest - NPC not found.");

                                                }
                                            } else {
                                                main.getLogManager().log(Level.WARNING, "Error: quests data is null");


                                            }


                                        }


                                    }
                                } else {
                                    main.getLogManager().log(Level.WARNING, "Error: Quest not found while trying to load NPC");

                                }
                            }
                            main.getLogManager().log(Level.INFO, "Requesting cleaning of bugged NPCs in loadNPCData()...");

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
                                            main.getLogManager().log(Level.INFO, "attaching quest with the name §b" + quest.getQuestName() + " §ato NPC with the ID §b" + npc.getId() + " §aand name §b" + npc.getName());

                                            quest.removeNPC(npc);
                                            quest.bindToNPC(npc, questShowing);


                                        } else {
                                            main.getLogManager().log(Level.WARNING, "Error attaching npc with ID §b" + main.getDataManager().getQuestsData().getInt("quests." + questName + ".npcs." + npcNumber + ".npcID") + " to quest - NPC not found.");

                                        }

                                    }
                                    main.getLogManager().log(Level.INFO, "Requesting cleaning of bugged NPCs in loadNPCData()...");

                                    cleanupBuggedNPCs();


                                }
                            } else {
                                main.getLogManager().log(Level.WARNING, "Error: Quest not found while trying to load NPC");

                            }


                        }
                    }


                } else {
                    main.getLogManager().log(Level.INFO, "Skipped loading NPC data because questsConfigurationSetting was null.");

                }
                main.getLogManager().log(Level.INFO, "Npc data loaded!");


                main.getDataManager().setAlreadyLoadedNPCs(true);


            } catch (Exception e) {
                e.printStackTrace();

                main.getLogManager().log(Level.SEVERE, "Plugin disabled, because there was an exception while loading quests NPC data.");
                main.getDataManager().setSavingEnabled(false);
                main.getServer().getPluginManager().disablePlugin(main);
                //return;
            }

        } else {
            main.getLogManager().log(Level.INFO, "§eNotQuests > Tried to load NPC data before quest data was loaded. skipping scheduling another load...");

            Bukkit.getScheduler().runTaskLaterAsynchronously(main, () -> {
                main.getLogManager().log(Level.INFO, "§eNotQuests > Trying to load NPC quest data again...");

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
            main.getLogManager().log(Level.INFO, "Quests data loaded!");

        }
    }


    public void cleanupBuggedNPCs() {
        if(!main.isCitizensEnabled()){
            main.getLogManager().log(Level.WARNING, "§eChecking for bugged NPCs has been cancelled, because Citizens is not installed on your server. The Citizens plugin is needed for NPC stuff to work.");

            return;
        }
        main.getLogManager().log(Level.INFO, "Checking for bugged NPCs...");

        int buggedNPCsFound = 0;
        int allNPCsFound = 0;
        //Clean up bugged NPCs with quests attached wrongly
        final ArrayList<Trait> traitsToRemove = new ArrayList<>();
        for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
            allNPCsFound += 1;
            if (getAllQuestsAttachedToNPC(npc).size() == 0) {
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
                    main.getLogManager().log(Level.INFO, "§eNotQuests > Bugged trait removed from npc with ID §b" + npc.getId() + " §eand name §b" + npc.getName() + " §e!");

                }


            } else {
                //TODO: Remove debug shit or improve performance
                final ArrayList<String> attachedQuestNames = new ArrayList<>();
                for (final Quest attachedQuest : getAllQuestsAttachedToNPC(npc)) {
                    attachedQuestNames.add(attachedQuest.getQuestName());
                }
                main.getLogManager().log(Level.INFO, "NPC with the ID: §b" + npc.getId() + " §ais not bugged, because it has the following quests attached: §b" + attachedQuestNames);

            }
            traitsToRemove.clear();

        }
        if (buggedNPCsFound == 0) {
            main.getLogManager().log(Level.INFO, "No bugged NPCs found! Amount of checked NPCs: §b" + allNPCsFound);

        } else {
            main.getLogManager().log(Level.INFO, "§eNotQuests > §b" + buggedNPCsFound + " §ebugged NPCs have  been found and removed! Amount of checked NPCs: §b" + allNPCsFound);

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

            sender.sendMessage(getObjectiveTaskDescription(activeObjective.getObjective(), true));
            sender.sendMessage("   §7§mProgress: §f§m" + activeObjective.getCurrentProgress() + " / " + activeObjective.getProgressNeeded());
        }
    }


    public final String getObjectiveTaskDescription(final Objective objective, boolean completed) {
        String toReturn = "";
        String eventualColor = "";
        if (completed) {
            eventualColor = "§m";
        }

        if (objective instanceof BreakBlocksObjective breakBlocksObjective) {
            toReturn = "    §7" + eventualColor + "Block to break: §f" + eventualColor + breakBlocksObjective.getBlockToBreak().toString();
        } else if (objective instanceof CollectItemsObjective collectItemsObjective) {
            toReturn = "    §7" + eventualColor + "Items to collect: §f" + eventualColor + collectItemsObjective.getItemToCollect().getType() + " (" + collectItemsObjective.getItemToCollect().getItemMeta().getDisplayName() + ")";
        } else if (objective instanceof CraftItemsObjective craftItemsObjective) {
            toReturn = "    §7" + eventualColor + "Items to craft: §f" + eventualColor + craftItemsObjective.getItemToCraft().getType() + " (" + craftItemsObjective.getItemToCraft().getItemMeta().getDisplayName() + ")";
        } else if (objective instanceof TriggerCommandObjective triggerCommandObjective) {
            toReturn = "    §7" + eventualColor + "Goal: §f" + eventualColor + triggerCommandObjective.getTriggerName();
        } else if (objective instanceof OtherQuestObjective otherQuestObjective) {
            toReturn = "    §7" + eventualColor + "Quest completion: §f" + eventualColor + otherQuestObjective.getOtherQuest().getQuestName();
        } else if (objective instanceof KillMobsObjective killMobsObjective) {
            toReturn = "    §7" + eventualColor + "Mob to kill: §f" + eventualColor + killMobsObjective.getMobToKill();
        } else if (objective instanceof ConsumeItemsObjective consumeItemsObjective) {
            toReturn = "    §7" + eventualColor + "Items to consume: §f" + eventualColor + consumeItemsObjective.getItemToConsume().getType() + " (" + consumeItemsObjective.getItemToConsume().getItemMeta().getDisplayName() + ")";
        } else if (objective instanceof DeliverItemsObjective deliverItemsObjective) {
            toReturn = "    §7" + eventualColor + "Items to deliver: §f" + eventualColor + deliverItemsObjective.getItemToDeliver().getType() + " (" + deliverItemsObjective.getItemToDeliver().getItemMeta().getDisplayName() + ")\n";
            if (main.isCitizensEnabled() && deliverItemsObjective.getRecipientNPCID() != -1) {
                final NPC npc = CitizensAPI.getNPCRegistry().getById(deliverItemsObjective.getRecipientNPCID());
                if (npc != null) {
                    toReturn += "    §7" + eventualColor + "Deliver it to §f" + eventualColor + npc.getName();
                } else {
                    toReturn += "    §7" + eventualColor + "The delivery NPC is currently not available!";
                }
            } else {

                if (deliverItemsObjective.getRecipientNPCID() != -1) {
                    toReturn += "    §cError: Citizens plugin not installed. Contact an admin.";
                } else { //Armor Stands
                    final UUID armorStandUUID = deliverItemsObjective.getRecipientArmorStandUUID();
                    if (armorStandUUID != null) {
                        toReturn += "    §7" + eventualColor + "Deliver it to §f" + eventualColor + main.getArmorStandManager().getArmorStandName(armorStandUUID);
                    } else {
                        toReturn += "    §7" + eventualColor + "The target Armor Stand is currently not available!";
                    }
                }

            }

        } else if (objective instanceof TalkToNPCObjective talkToNPCObjective) {
            if (main.isCitizensEnabled() && talkToNPCObjective.getNPCtoTalkID() != -1) {
                final NPC npc = CitizensAPI.getNPCRegistry().getById(talkToNPCObjective.getNPCtoTalkID());
                if (npc != null) {
                    toReturn = "    §7" + eventualColor + "Talk to §f" + eventualColor + npc.getName();
                } else {
                    toReturn = "    §7" + eventualColor + "The target NPC is currently not available!";
                }
            } else {
                if (talkToNPCObjective.getNPCtoTalkID() != -1) {
                    toReturn += "    §cError: Citizens plugin not installed. Contact an admin.";
                } else { //Armor Stands
                    final UUID armorStandUUID = talkToNPCObjective.getArmorStandUUID();
                    if (armorStandUUID != null) {
                        toReturn = "    §7" + eventualColor + "Talk to §f" + eventualColor + main.getArmorStandManager().getArmorStandName(armorStandUUID);
                    } else {
                        toReturn += "    §7" + eventualColor + "The target Armor Stand is currently not available!";
                    }
                }
            }

        } else if (objective instanceof EscortNPCObjective escortNPCObjective) {
            if (main.isCitizensEnabled()) {
                final NPC npc = CitizensAPI.getNPCRegistry().getById(escortNPCObjective.getNpcToEscortID());
                final NPC npcDestination = CitizensAPI.getNPCRegistry().getById(escortNPCObjective.getNpcToEscortToID());

                if (npc != null && npcDestination != null) {
                    toReturn = "    §7" + eventualColor + "Escort §f" + eventualColor + npc.getName() + " §7" + eventualColor + "to §f" + eventualColor + npcDestination.getName();
                } else {
                    toReturn = "    §7" + eventualColor + "The target or destination NPC is currently not available!";
                }
            } else {
                toReturn += "    §cError: Citizens plugin not installed. Contact an admin.";
            }

        }
        if (objective.getCompletionNPCID() != -1) {
            if (main.isCitizensEnabled()) {
                final NPC npc = CitizensAPI.getNPCRegistry().getById(objective.getCompletionNPCID());
                if (npc != null) {
                    toReturn += "\n    §7" + eventualColor + "To complete: Talk to §b" + eventualColor + npc.getName();
                } else {
                    toReturn += "\n    §7" + eventualColor + "To complete: Talk to NPC with ID §b" + eventualColor + objective.getCompletionNPCID() + " §c" + eventualColor + "[Currently not available]";
                }
            } else {
                toReturn += "    §cError: Citizens plugin not installed. Contact an admin.";
            }
        }
        if (objective.getCompletionArmorStandUUID() != null) {
            toReturn += "\n    §7" + eventualColor + "To complete: Talk to §b" + eventualColor + "" + main.getArmorStandManager().getArmorStandName(objective.getCompletionArmorStandUUID());
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

                sender.sendMessage(getObjectiveTaskDescription(activeObjective.getObjective(), false));

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

            sender.sendMessage(getObjectiveTaskDescription(objective, false));


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

            sender.sendMessage(getObjectiveTaskDescription(objective, false));

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

            sender.sendMessage(getObjectiveTaskDescription(activeObjective.getObjective(), false));



            sender.sendMessage("   §7Progress: §f" + activeObjective.getCurrentProgress() + " / " + activeObjective.getProgressNeeded());
        } else {
            sender.sendMessage("§e" + activeObjective.getObjective().getObjectiveID() + ". §7§l[HIDDEN]");

        }


    }


    /**
     * Checks if the player is close to a Citizens NPC or Armor Stand which has the specified Quest attached to it
     *
     * @param player the player who should be close to the Citizens NPC or Armor Stand
     * @param quest the Quest which needs to be attached to the Citizens NPC or Armor Stand
     * @return if the player is close to a Citizens NPC or Armor Stand which has the specified Quest attached to it
     */
    public final boolean isPlayerCloseToCitizenOrArmorstandWithQuest(final Player player, final Quest quest){
        final int closenessCheckDistance = 6;

        //First check Armor stands since I think that check is probably faster - especially if the user has a lot of Citizen NPCs
        for(final Entity entity : player.getNearbyEntities(closenessCheckDistance, closenessCheckDistance, closenessCheckDistance)){
            if(entity instanceof ArmorStand armorStand){
                if(getAllQuestsAttachedToArmorstand(armorStand).contains(quest)){
                    return true;
                }
            }
        }


        //Then Citizens
        if(main.isCitizensEnabled()){
            for (NPC npc : getAllNPCsAttachedToQuest(quest)) {
                if (npc == null || npc.getEntity() == null) {
                    main.getLogManager().warn("§cA quest has an invalid npc attached to it, which should be removed. Report it to an admin. Quest name: §b" + quest.getQuestName());
                    continue;
                }
                final Location npcLocation = npc.getEntity().getLocation();
                if (npcLocation.getWorld().equals(player.getWorld())) {
                    if (npcLocation.distance(player.getLocation()) < closenessCheckDistance) {
                        return true;
                    }
                }
            }
        }

        return false;


    }

    public final String getObjectiveTypesList() {
        return objectiveTypesList;
    }

    public final String getRewardTypesList() {
        return rewardTypesList;
    }

    public final String getRequirementsTypesList() {
        return requirementsTypesList;
    }

}
