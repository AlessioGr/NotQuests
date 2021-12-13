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

package rocks.gravili.notquests.Managers;

import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.ActiveObjective;
import rocks.gravili.notquests.Structs.ActiveQuest;
import rocks.gravili.notquests.Structs.Conditions.Condition;
import rocks.gravili.notquests.Structs.Objectives.Objective;
import rocks.gravili.notquests.Structs.Quest;
import rocks.gravili.notquests.Structs.QuestPlayer;
import rocks.gravili.notquests.Structs.Rewards.Reward;
import rocks.gravili.notquests.Structs.Triggers.Action;
import rocks.gravili.notquests.Structs.Triggers.Trigger;

import java.util.ArrayList;
import java.util.UUID;

import static rocks.gravili.notquests.Commands.NotQuestColors.*;


public class QuestManager {

    private final NotQuests main;

    private final ArrayList<Quest> quests;



    private final ArrayList<String> rewardTypesList, requirementsTypesList;

    private final ArrayList<Player> debugEnabledPlayers;
    protected final MiniMessage miniMessage = MiniMessage.miniMessage();


    public QuestManager(NotQuests main) {
        this.main = main;
        quests = new ArrayList<>();

        debugEnabledPlayers = new ArrayList<>();

        rewardTypesList = new ArrayList<>();
        requirementsTypesList = new ArrayList<>();

        rewardTypesList.add("ConsoleCommand");
        rewardTypesList.add("QuestPoints");
        rewardTypesList.add("Item");
        rewardTypesList.add("Money");

        requirementsTypesList.add("OtherQuest");
        requirementsTypesList.add("QuestPoints");
        requirementsTypesList.add("Permission");
        requirementsTypesList.add("Money");
        requirementsTypesList.add("Placeholder (WIP)");

    }


    public final String createQuest(String questName) {
        if (getQuest(questName) == null) {
            if(questName.contains("°")){
                return (NotQuestColors.errorGradient + "The symbol ° cannot be used, because it's used for some important, plugin-internal stuff.");
            }
            Quest newQuest = new Quest(main, questName);
            quests.add(newQuest);
            main.getDataManager().getQuestsConfig().set("quests." + questName, "");
            return (NotQuestColors.successGradient + "Quest successfully created!");
        } else {
            return (NotQuestColors.errorGradient + "Quest already exists!");
        }
    }

    public final String deleteQuest(String questName) {
        if (getQuest(questName) != null) {
            Quest questToDelete = getQuest(questName);
            quests.remove(questToDelete);
            main.getDataManager().getQuestsConfig().set("quests." + questName, null);
            return (NotQuestColors.successGradient + "Quest successfully deleted!");
        } else {
            return (NotQuestColors.errorGradient + "Quest doesn't exists!");
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


    public void loadQuestsFromConfig() {

        if(main.isCitizensEnabled()){
            main.getCitizensManager().registerQuestGiverTrait();
        }


        try {
            main.getLogManager().info("Loading Quests data...");

            quests.clear();


            //Actions load from quests.yml, so we can migrate them to actions.yml
            final ConfigurationSection oldActionsConfigurationSection = main.getDataManager().getQuestsConfig().getConfigurationSection("actions");
            if (oldActionsConfigurationSection != null) {
                for (final String actionName : oldActionsConfigurationSection.getKeys(false)) {
                    final String consoleCommand = main.getDataManager().getQuestsConfig().getString("actions." + actionName + ".consoleCommand", "");
                    if (consoleCommand.equalsIgnoreCase("")) {
                        main.getLogManager().warn("Action has an empty console command. This should NOT be possible! Creating an action with an empty console command... Action name: <AQUA>" + actionName + "</AQUA>");
                    }

                    main.getActionsManager().createAction(actionName, consoleCommand);
                    main.getLogManager().info("Migrated the following action from quests.yml to actions.yml: <AQUA>" + actionName + "</AQUA>");


                }
            }

            //Now that they are loaded, let's delete them from the quests.yml and save the actions.yml
            main.getDataManager().getQuestsConfig().set("actions", null);
            main.getDataManager().saveQuestsConfig();


            //save them to write them to the actions.yml (in case of migration)
            main.getActionsManager().saveActions();

            //Quests
            final ConfigurationSection questsConfigurationSection = main.getDataManager().getQuestsConfig().getConfigurationSection("quests");
            if (questsConfigurationSection != null) {
                for (String questName : questsConfigurationSection.getKeys(false)) {
                    Quest quest = new Quest(main, questName);
                    quest.setMaxAccepts(main.getDataManager().getQuestsConfig().getInt("quests." + questName + ".maxAccepts", -1));
                    quest.setTakeEnabled(main.getDataManager().getQuestsConfig().getBoolean("quests." + questName + ".takeEnabled", true));
                    quest.setAcceptCooldown(main.getDataManager().getQuestsConfig().getLong("quests." + questName + ".acceptCooldown", -1));
                    quest.setQuestDescription(main.getDataManager().getQuestsConfig().getString("quests." + questName + ".description", ""));
                    quest.setQuestDisplayName(main.getDataManager().getQuestsConfig().getString("quests." + questName + ".displayName", ""));


                    //Objectives:
                    final ConfigurationSection objectivesConfigurationSection = main.getDataManager().getQuestsConfig().getConfigurationSection("quests." + questName + ".objectives");
                    if (objectivesConfigurationSection != null) {
                        for (final String objectiveNumber : objectivesConfigurationSection.getKeys(false)) {
                            Class<? extends Objective> objectiveType = null;

                            try {
                                objectiveType = main.getObjectiveManager().getObjectiveClass(main.getDataManager().getQuestsConfig().getString("quests." + questName + ".objectives." + objectiveNumber + ".objectiveType"));
                            } catch (java.lang.NullPointerException ex) {
                                main.getDataManager().disablePluginAndSaving("Error parsing objective Type of objective with ID <AQUA>" + objectiveNumber + "</AQUA> and Quest <AQUA>" + quest.getQuestName() + "</AQUA>.", ex);
                            }
                            final int progressNeeded = main.getDataManager().getQuestsConfig().getInt("quests." + questName + ".objectives." + objectiveNumber + ".progressNeeded", 1);

                            int objectiveID = -1;
                            boolean validObjectiveID = true;
                            try {
                                objectiveID = Integer.parseInt(objectiveNumber);
                            } catch (java.lang.NumberFormatException ex) {
                                validObjectiveID = false;
                                main.getDataManager().disablePluginAndSaving("Error parsing loaded objective ID <AQUA>" + objectiveNumber + "</AQUA>.", ex);

                            }
                            if (validObjectiveID && objectiveID > 0 && objectiveType != null) {


                                Objective objective = null;

                                try {
                                    objective = objectiveType.getDeclaredConstructor(NotQuests.class, Quest.class, int.class, int.class).newInstance(main, quest, objectiveID, progressNeeded);

                                } catch (Exception ex) {
                                    main.getDataManager().disablePluginAndSaving("Error parsing objective Type of objective with ID <AQUA>" + objectiveNumber + "</AQUA> and Quest <AQUA>" + quest.getQuestName() + "</AQUA>.", ex);
                                }


                                if (objective != null) {

                                    final String objectiveDisplayName = main.getDataManager().getQuestsConfig().getString("quests." + questName + ".objectives." + objectiveNumber + ".displayName", "");
                                    final String objectiveDescription = main.getDataManager().getQuestsConfig().getString("quests." + questName + ".objectives." + objectiveNumber + ".description", "");
                                    final int completionNPCID = main.getDataManager().getQuestsConfig().getInt("quests." + quest.getQuestName() + ".objectives." + objectiveNumber + ".completionNPCID", -1);
                                    final String completionArmorStandUUIDString = main.getDataManager().getQuestsConfig().getString("quests." + quest.getQuestName() + ".objectives." + objectiveNumber + ".completionArmorStandUUID", null);
                                    if (completionArmorStandUUIDString != null) {
                                        final UUID completionArmorStandUUID = UUID.fromString(completionArmorStandUUIDString);
                                        objective.setCompletionArmorStandUUID(completionArmorStandUUID, false);
                                    }

                                    objective.setObjectiveDisplayName(objectiveDisplayName, false);
                                    objective.setObjectiveDescription(objectiveDescription, false);
                                    objective.setCompletionNPCID(completionNPCID, false);
                                    quest.addObjective(objective, false);
                                } else {
                                    main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an error while loading quests objective data.");
                                }

                            } else {
                                main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an error while loading quests objective data (2).");
                            }
                        }
                    }

                    //Requirements:
                    final ConfigurationSection requirementsConfigurationSection = main.getDataManager().getQuestsConfig().getConfigurationSection("quests." + questName + ".requirements");
                    if (requirementsConfigurationSection != null) {
                        for (String requirementNumber : requirementsConfigurationSection.getKeys(false)) {

                            int requirementID = -1;
                            boolean validRequirementID = true;
                            try {
                                requirementID = Integer.parseInt(requirementNumber);
                            } catch (java.lang.NumberFormatException ex) {
                                validRequirementID = false;
                                main.getDataManager().disablePluginAndSaving("Error parsing loaded requirement ID <AQUA>" + requirementNumber + "</AQUA>.", ex);
                            }

                            Class<? extends Condition> conditionType = null;

                            try {
                                String conditionTypeString = main.getDataManager().getQuestsConfig().getString("quests." + questName + ".requirements." + requirementNumber + ".conditionType", "");

                                if(conditionTypeString.isBlank()){//User might be using old system with requirementType instead of conditionType. Let's convert it!
                                    conditionTypeString = main.getUpdateManager().convertQuestRequirementTypeToConditionType(questName, requirementNumber, conditionTypeString);
                                }

                                conditionType = main.getConditionsManager().getConditionClass(conditionTypeString);
                            } catch (java.lang.NullPointerException ex) {
                                main.getDataManager().disablePluginAndSaving("Error parsing requirement Type of requirement with ID <AQUA>" + requirementNumber + "</AQUA> and Quest <AQUA>" + quest.getQuestName() + "<AQUA>.", ex);
                            }

                            //RequirementType requirementType = RequirementType.valueOf(main.getDataManager().getQuestsData().getString("quests." + questName + ".requirements." + requirementNumber + ".requirementType"));
                            int progressNeeded = main.getDataManager().getQuestsConfig().getInt("quests." + questName + ".requirements." + requirementNumber + ".progressNeeded");

                            if (validRequirementID && requirementID > 0 && conditionType != null) {
                                Condition condition = null;

                                try {
                                    condition = conditionType.getDeclaredConstructor(NotQuests.class, Object[].class).newInstance(main, new Object[]{progressNeeded, quest});
                                    condition.load("quests." + questName + ".requirements." + requirementID);
                                } catch (Exception ex) {
                                    main.getDataManager().disablePluginAndSaving("Error parsing requirement Type of requirement with ID <AQUA>" + requirementNumber + "</AQUA> and Quest <AQUA>" + quest.getQuestName() + "</AQUA>.");
                                }
                                if (condition != null) {
                                    quest.addRequirement(condition);
                                }

                            } else {
                                main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an error while loading quests requirement data.");
                            }

                        }
                    }


                    //Rewards:
                    final ConfigurationSection rewardsConfigurationSection = main.getDataManager().getQuestsConfig().getConfigurationSection("quests." + questName + ".rewards");
                    if (rewardsConfigurationSection != null) {
                        for (String rewardNumber : rewardsConfigurationSection.getKeys(false)) {

                            int rewardID = -1;
                            boolean validRewardID = true;
                            try {
                                rewardID = Integer.parseInt(rewardNumber);
                            } catch (java.lang.NumberFormatException ex) {
                                validRewardID = false;
                                main.getDataManager().disablePluginAndSaving("Error parsing loaded reward ID <AQUA>" + rewardNumber + "</AQUA>.", ex);
                            }

                            Class<? extends Reward> rewardType = null;

                            try {
                                rewardType = main.getRewardManager().getRewardClass(main.getDataManager().getQuestsConfig().getString("quests." + questName + ".rewards." + rewardNumber + ".rewardType"));
                            } catch (java.lang.NullPointerException ex) {
                                main.getDataManager().disablePluginAndSaving("Error parsing reward Type of reward with ID <AQUA>" + rewardNumber + "</AQUA> and Quest <AQUA>" + quest.getQuestName() + "<AQUA>.", ex);
                            }

                            if (validRewardID && rewardID > 0 && rewardType != null) {
                                Reward reward = null;

                                try {
                                    reward = rewardType.getDeclaredConstructor(NotQuests.class, Quest.class, int.class).newInstance(main, quest, rewardID);

                                } catch (Exception ex) {
                                    main.getDataManager().disablePluginAndSaving("Error parsing reward Type of reward with ID <AQUA>" + rewardNumber + "</AQUA> and Quest <AQUA>" + quest.getQuestName() + "</AQUA>.", ex);
                                }

                                if (reward != null) {
                                    final String rewardDisplayName = main.getDataManager().getQuestsConfig().getString("quests." + questName + ".rewards." + rewardNumber + ".displayName", "");
                                    reward.setRewardDisplayName(rewardDisplayName);
                                    quest.addReward(reward);
                                } else {
                                    main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an error while loading quests reward data.");
                                }
                            }


                        }
                    }


                    //Triggers:
                    final ConfigurationSection triggersConfigurationSection = main.getDataManager().getQuestsConfig().getConfigurationSection("quests." + questName + ".triggers");
                    if (triggersConfigurationSection != null) {
                        for (final String triggerNumber : triggersConfigurationSection.getKeys(false)) {


                            int triggerID = -1;
                            boolean validTriggerID = true;
                            try {
                                triggerID = Integer.parseInt(triggerNumber);
                            } catch (java.lang.NumberFormatException ex) {
                                validTriggerID = false;
                                main.getDataManager().disablePluginAndSaving("Error parsing loaded trigger ID ID <AQUA>" + triggerNumber + "</AQUA>.", ex);
                            }

                            Class<? extends Trigger> triggerType = null;
                            final String triggerTypeString = main.getDataManager().getQuestsConfig().getString("quests." + questName + ".triggers." + triggerNumber + ".triggerType");

                            try {
                                triggerType = main.getTriggerManager().getTriggerClass(triggerTypeString);
                            } catch (java.lang.NullPointerException ex) {
                                main.getDataManager().disablePluginAndSaving("Error parsing trigger Type of trigger with ID <AQUA>" + triggerNumber + "</AQUA> and Quest <AQUA>" + quest.getQuestName() + "<AQUA>.");
                            }


                            final String triggerActionName = main.getDataManager().getQuestsConfig().getString("quests." + questName + ".triggers." + triggerNumber + ".triggerActionName");
                            final long amountNeeded = main.getDataManager().getQuestsConfig().getLong("quests." + questName + ".triggers." + triggerNumber + ".amountNeeded", 1);

                            final int applyOn = main.getDataManager().getQuestsConfig().getInt("quests." + questName + ".triggers." + triggerNumber + ".applyOn");
                            final String worldName = main.getDataManager().getQuestsConfig().getString("quests." + questName + ".triggers." + triggerNumber + ".worldName", "ALL");


                            Action foundAction = null;
                            for (final Action action : main.getActionsManager().getActions()) {
                                if (action.getActionName().equalsIgnoreCase(triggerActionName)) {
                                    foundAction = action;
                                    break;
                                }
                            }
                            if (validTriggerID && triggerID > 0 && triggerType != null && foundAction != null) {
                                Trigger trigger = null;

                                try {
                                    trigger = triggerType.getDeclaredConstructor(NotQuests.class, Quest.class, int.class, Action.class, int.class, String.class, long.class).newInstance(main, quest, triggerID, foundAction, applyOn, worldName, amountNeeded);

                                } catch (Exception ex) {
                                    main.getDataManager().disablePluginAndSaving("Error parsing requirement Type of trigger with ID <AQUA>" + triggerNumber + "</AQUA> and Quest <AQUA>" + quest.getQuestName() + "</AQUA>.", ex);
                                }
                                if (trigger != null) {
                                    quest.addTrigger(trigger);
                                }

                            } else {
                                main.getDataManager().disablePluginAndSaving("ERROR when loading trigger with the triggerNumber <AQUA>" + triggerNumber + " </AQUA>: Action could not be loaded.");
                            }


                        }
                    }


                    //Convert old dependencies
                    main.getUpdateManager().convertObjectiveDependenciesToNewObjectiveConditions(quest);


                    //Objective Conditions
                    main.getLogManager().info("Loading objective conditions...");
                    for (final Objective objective : quest.getObjectives()) { //TODO: Add objective name to error or debug messages to discern from normal requirement loading
                        final ConfigurationSection objectiveConditionsConfigurationSection = main.getDataManager().getQuestsConfig().getConfigurationSection("quests." + quest.getQuestName() + ".objectives." + objective.getObjectiveID() + ".conditions.");
                        if (objectiveConditionsConfigurationSection != null) {
                            for (String objectiveConditionNumber : objectiveConditionsConfigurationSection.getKeys(false)) {
                                int conditionID = -1;
                                boolean validConditionID = true;
                                try {
                                    conditionID = Integer.parseInt(objectiveConditionNumber);
                                } catch (java.lang.NumberFormatException ex) {
                                    validConditionID = false;
                                    main.getDataManager().disablePluginAndSaving("Error parsing loaded condition ID <AQUA>" + objectiveConditionNumber + "</AQUA>.", ex);
                                }

                                Class<? extends Condition> conditionType = null;

                                try {

                                    conditionType = main.getConditionsManager().getConditionClass(main.getDataManager().getQuestsConfig().getString("quests." + questName + ".objectives." + (objective.getObjectiveID())  + ".conditions."  + objectiveConditionNumber + ".conditionType"));
                                } catch (java.lang.NullPointerException ex) {
                                    main.getDataManager().disablePluginAndSaving("Error parsing condition Type of requirement with ID <AQUA>" + objectiveConditionNumber + "</AQUA> and Quest <AQUA>" + quest.getQuestName() + "<AQUA>.", ex);
                                }

                                //RequirementType requirementType = RequirementType.valueOf(main.getDataManager().getQuestsData().getString("quests." + questName + ".requirements." + requirementNumber + ".requirementType"));
                                int progressNeeded = main.getDataManager().getQuestsConfig().getInt("quests." + questName + ".objectives." + (objective.getObjectiveID())  + ".conditions."  + objectiveConditionNumber + ".progressNeeded");

                                if (validConditionID && conditionID > 0 && conditionType != null) {
                                    Condition condition = null;

                                    try {
                                        condition = conditionType.getDeclaredConstructor(NotQuests.class, Object[].class).newInstance(main, new Object[]{progressNeeded, quest});
                                        condition.load("quests." + questName + ".objectives." + (objective.getObjectiveID())  + ".conditions."  + objectiveConditionNumber);
                                    } catch (Exception ex) {
                                        main.getDataManager().disablePluginAndSaving("Error parsing condition Type of requirement with ID <AQUA>" + objectiveConditionNumber + "</AQUA> and Quest <AQUA>" + quest.getQuestName() + "</AQUA>.", ex);
                                    }
                                    if (condition != null) {
                                        objective.addCondition(condition, false);
                                    }

                                } else {
                                    main.getDataManager().disablePluginAndSaving("Error loading condition. ValidRequirementID: " + validConditionID + " conditionID: " + conditionID + " ConditionTypeNull?" + (conditionType == null) + " ConditionType: " + (conditionType != null ? conditionType.toString() : "null"));
                                }
                            }
                        }
                    }


                    //TakeItem:
                    quest.setTakeItem(Material.valueOf(main.getDataManager().getQuestsConfig().getString("quests." + questName + ".takeItem", "BOOK")));

                    quests.add(quest);
                }
            }

            main.getDataManager().setAlreadyLoadedQuests(true);
        } catch (Exception ex) {
            main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an exception while loading quests data.", ex);
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
            InventoryGui gui = new InventoryGui(main, player, main.getLanguageManager().getString("gui.availableQuests.title", player), guiSetup);
            gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

            int count = 0;
            GuiElementGroup group = new GuiElementGroup('g');

            for (final Quest quest : questsAttachedToNPC) {
                final Material materialToUse = quest.getTakeItem();

                String displayName = quest.getQuestFinalName();

                displayName = main.getLanguageManager().getString("gui.availableQuests.button.questPreview.questNamePrefix", player, quest) + displayName;
                QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer((player.getUniqueId()));

                if (questPlayer != null && questPlayer.hasAcceptedQuest(quest)) {
                    displayName += main.getLanguageManager().getString("gui.availableQuests.button.questPreview.acceptedSuffix", player, quest);
                }
                String description = "";
                if (!quest.getQuestDescription().isBlank()) {
                    description = main.getLanguageManager().getString("gui.availableQuests.button.questPreview.questDescriptionPrefix", player, quest) + quest.getQuestDescription(main.getDataManager().getConfiguration().guiQuestDescriptionMaxLineLength);
                }
                count++;


                group.addElement(new StaticGuiElement('e',
                        new ItemStack(materialToUse),
                        count, // Display a number as the item count
                        click -> {
                            player.chat("/notquests preview " + quest.getQuestName());
                            //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                            return true; // returning true will cancel the click event and stop taking the item

                        },
                        displayName,
                        description,
                        main.getLanguageManager().getString("gui.availableQuests.button.questPreview.bottomText", player, questPlayer, quest)
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
            main.getLogManager().info("NotQuests > All quest count: <AQUA>" + quests.size() + "</AQUA>");

            player.sendMessage("");
            player.sendMessage("§9" + questsAttachedToNPC.size() + " Available Quests:");
            int counter = 1;

            for (Quest quest : questsAttachedToNPC) {


                net.md_5.bungee.api.chat.BaseComponent component;


                net.md_5.bungee.api.chat.BaseComponent acceptComponent = new net.md_5.bungee.api.chat.TextComponent("§a§l[CHOOSE]");
                acceptComponent.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/nquests preview " + quest.getQuestName()));
                acceptComponent.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.ComponentBuilder("§aClick to preview/choose the quest §b" + quest.getQuestFinalName()).create()));
                component = new net.md_5.bungee.api.chat.TextComponent("§e" + counter + ". §b" + quest.getQuestFinalName() + " ");

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
            InventoryGui gui = new InventoryGui(main, player, main.getLanguageManager().getString("gui.availableQuests.title", player), guiSetup);
            gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

            int count = 0;
            GuiElementGroup group = new GuiElementGroup('g');

            for (final Quest quest : questsAttachedToNPC) {
                final Material materialToUse = quest.getTakeItem();


                String displayName = quest.getQuestFinalName();

                displayName = main.getLanguageManager().getString("gui.availableQuests.button.questPreview.questNamePrefix", player, quest) + displayName;
                QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer((player.getUniqueId()));

                if (questPlayer != null && questPlayer.hasAcceptedQuest(quest)) {
                    displayName += main.getLanguageManager().getString("gui.availableQuests.button.questPreview.acceptedSuffix", player, quest);
                }
                String description = "";
                if (!quest.getQuestDescription().isBlank()) {
                    description = main.getLanguageManager().getString("gui.availableQuests.button.questPreview.questDescriptionPrefix", player, quest) + quest.getQuestDescription(main.getDataManager().getConfiguration().guiQuestDescriptionMaxLineLength);
                }
                count++;


                group.addElement(new StaticGuiElement('e',
                        new ItemStack(materialToUse),
                        count, // Display a number as the item count
                        click -> {
                            player.chat("/notquests preview " + quest.getQuestName());
                            //click.getEvent().getWhoClicked().sendMessage(ChatColor.RED + "I am Redstone!");
                            return true; // returning true will cancel the click event and stop taking the item

                        },
                        displayName,
                        description,
                        main.getLanguageManager().getString("gui.availableQuests.button.questPreview.bottomText", player, questPlayer, quest)
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
            main.getLogManager().info("NotQuests > All quest count: <AQUA>" + quests.size() + "</AQUA>");

            player.sendMessage("");
            player.sendMessage("§9" + questsAttachedToNPC.size() + " Available Quests:");
            int counter = 1;

            for (Quest quest : questsAttachedToNPC) {


                net.md_5.bungee.api.chat.BaseComponent component;


                net.md_5.bungee.api.chat.BaseComponent acceptComponent = new net.md_5.bungee.api.chat.TextComponent("§a§l[CHOOSE]");
                acceptComponent.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/nquests preview " + quest.getQuestName()));
                acceptComponent.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.ComponentBuilder("§aClick to preview/choose the quest §b" + quest.getQuestFinalName()).create()));
                component = new net.md_5.bungee.api.chat.TextComponent("§e" + counter + ". §b" + quest.getQuestFinalName() + " ");

                component.addExtra(acceptComponent);

                player.spigot().sendMessage(component);


                counter++;
            }
            //getQuestsAttachedToNPC(npc);
        }

    }

    public final String getQuestRequirements(final Quest quest) {
        StringBuilder requirements = new StringBuilder();
        int counter = 1;
        for (final Condition condition : quest.getRequirements()) {
            if(counter != 1){
                requirements.append("\n");
            }
            requirements.append("§a").append(counter).append(". §e").append(condition.getConditionType()).append("\n");

            requirements.append(condition.getConditionDescription());


            counter += 1;
        }
        return requirements.toString();
    }

    public final String getQuestRewards(final Quest quest) {
        StringBuilder rewards = new StringBuilder();
        int counter = 1;
        for (final Reward reward : quest.getRewards()) {
            if(counter != 1){
                rewards.append("\n");
            }
            if(!reward.getRewardDisplayName().isBlank()){
                rewards.append("§a").append(counter).append(". §9").append(reward.getRewardDisplayName());
            }else{
                rewards.append("§a").append(counter).append(main.getLanguageManager().getString("gui.reward-hidden-text", null, quest, reward));

            }
            counter += 1;

        }
        return rewards.toString();
    }

    public void sendSingleQuestPreview(Player player, Quest quest) {
        player.sendMessage("");
        player.sendMessage("§7-----------------------------------");
        player.sendMessage("§9Quest Preview for Quest §b" + quest.getQuestFinalName() + "§9:");


        if (quest.getQuestDescription().length() >= 1) {
            player.sendMessage("§eQuest description: §7" + quest.getQuestDescription());
        } else {
            player.sendMessage(main.getLanguageManager().getString("chat.missing-quest-description", player));
        }

        player.sendMessage("§9Quest Requirements:");

        player.sendMessage(getQuestRequirements(quest));

        player.sendMessage("§9Quest Rewards:");

        player.sendMessage(getQuestRewards(quest));


        net.md_5.bungee.api.chat.BaseComponent acceptComponent = new net.md_5.bungee.api.chat.TextComponent("§a§l[ACCEPT THIS QUEST]");
        acceptComponent.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/nquests take " + quest.getQuestName()));
        acceptComponent.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.ComponentBuilder("§aClick to accept this quest").create()));



       /*Paper only Component acceptQuestComponent = Component.text("[ACCEPT THIS QUEST]", NamedTextColor.GREEN, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/nquests take " + quest.getQuestName()))
                .hoverEvent(HoverEvent.showText(Component.text("Click to accept this quest", NamedTextColor.GREEN)));*/


        player.sendMessage("");
        //only paper player.sendMessage(acceptQuestComponent);
        player.spigot().sendMessage(acceptComponent);
        player.sendMessage("§7-----------------------------------");


    }

    public void loadNPCData() {
        main.getLogManager().info("Loading NPC data...");

        if(!main.isCitizensEnabled()){
            main.getLogManager().warn("NPC data loading has been cancelled, because Citizens is not installed. Install the Citizens plugin if you want NPC stuff to work.");
            return;
        }

        if (main.getDataManager().isAlreadyLoadedQuests()) {
            try {

                final ConfigurationSection questsConfigurationSetting = main.getDataManager().getQuestsConfig().getConfigurationSection("quests");
                if (questsConfigurationSetting != null) {
                    if (!Bukkit.isPrimaryThread()) {
                        Bukkit.getScheduler().runTask(main, () -> {
                            for (String questName : questsConfigurationSetting.getKeys(false)) {
                                Quest quest = getQuest(questName);

                                if (quest != null) {
                                    //NPC
                                    final ConfigurationSection npcsConfigurationSection = main.getDataManager().getQuestsConfig().getConfigurationSection("quests." + questName + ".npcs");
                                    if (npcsConfigurationSection != null) {


                                        for (String npcNumber : npcsConfigurationSection.getKeys(false)) {
                                            if (main.getDataManager().getQuestsConfig() != null) {


                                                final NPC npc = CitizensAPI.getNPCRegistry().getById(main.getDataManager().getQuestsConfig().getInt("quests." + questName + ".npcs." + npcNumber + ".npcID"));


                                                if (npc != null) {
                                                    final boolean questShowing = main.getDataManager().getQuestsConfig().getBoolean("quests." + questName + ".npcs." + npc.getId() + ".questShowing", true);


                                                    // call the callback with the result
                                                    main.getLogManager().info("Attaching Quest with the name <AQUA>" + quest.getQuestName() + "</AQUA> to NPC with the ID <AQUA>" + npc.getId() + " </AQUA>and name <AQUA>" + npc.getName());

                                                    quest.removeNPC(npc);
                                                    quest.bindToNPC(npc, questShowing);


                                                } else {
                                                    main.getLogManager().warn("Error attaching npc with ID <AQUA>" + main.getDataManager().getQuestsConfig().getInt("quests." + questName + ".npcs." + npcNumber + ".npcID")
                                                            + "</AQUA> to quest <AQUA>" + quest.getQuestName() + "</AQUA> - NPC not found.");

                                                }
                                            } else {
                                                main.getLogManager().warn("Error: quests data is null");


                                            }


                                        }


                                    }
                                } else {
                                    main.getLogManager().warn("Error: Quest not found while trying to load NPC");

                                }
                            }
                            main.getLogManager().info("Requesting cleaning of bugged NPCs in loadNPCData()...");

                            cleanupBuggedNPCs();
                        });

                    } else {
                        for (String questName : questsConfigurationSetting.getKeys(false)) {
                            Quest quest = getQuest(questName);

                            if (quest != null) {
                                //NPC
                                final ConfigurationSection npcsConfigurationSection = main.getDataManager().getQuestsConfig().getConfigurationSection("quests." + questName + ".npcs");
                                if (npcsConfigurationSection != null) {
                                    for (String npcNumber : npcsConfigurationSection.getKeys(false)) {
                                        final NPC npc = CitizensAPI.getNPCRegistry().getById(main.getDataManager().getQuestsConfig().getInt("quests." + questName + ".npcs." + npcNumber + ".npcID"));
                                        final boolean questShowing = main.getDataManager().getQuestsConfig().getBoolean("quests." + questName + ".npcs." + npc.getId() + ".questShowing", true);

                                        if (npc != null) {

                                            // call the callback with the result
                                            main.getLogManager().info("Attaching Quest with the name <AQUA>" + quest.getQuestName() + " </AQUA>to NPC with the ID <AQUA>" + npc.getId() + " </AQUA>and name <AQUA>" + npc.getName());

                                            quest.removeNPC(npc);
                                            quest.bindToNPC(npc, questShowing);


                                        } else {
                                            main.getLogManager().warn("Error attaching npc with ID <AQUA>" + main.getDataManager().getQuestsConfig().getInt("quests." + questName + ".npcs." + npcNumber + ".npcID")
                                                    + "</AQUA> to quest <AQUA>" + quest.getQuestName() + "</AQUA> - NPC not found.");
                                        }

                                    }
                                    main.getLogManager().info("Requesting cleaning of bugged NPCs in loadNPCData()...");

                                    cleanupBuggedNPCs();


                                }
                            } else {
                                main.getLogManager().warn("Error: Quest not found while trying to load NPC");

                            }


                        }
                    }


                } else {
                    main.getLogManager().info("Skipped loading NPC data because questsConfigurationSetting was null.");
                }
                main.getLogManager().info("NPC data loaded!");


                main.getDataManager().setAlreadyLoadedNPCs(true);


            } catch (Exception ex) {
                main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an exception while loading quests NPC data.", ex);
            }

        } else {
            main.getLogManager().warn("NotQuests > Tried to load NPC data before quest data was loaded. skipping scheduling another load...");

            Bukkit.getScheduler().runTaskLaterAsynchronously(main, () -> {
                if(!main.getDataManager().isAlreadyLoadedNPCs()){
                    main.getLogManager().warn("NotQuests > Trying to load NPC quest data again...");
                    main.getDataManager().loadNPCData();
                }
            }, 40);
        }


    }

    public void cleanupBuggedNPCs() {
        if(!main.isCitizensEnabled()){
            main.getLogManager().warn("Checking for bugged NPCs has been cancelled, because Citizens is not installed on your server. The Citizens plugin is needed for NPC stuff to work.");

            return;
        }
        main.getLogManager().info("Checking for bugged NPCs...");

        int buggedNPCsFound = 0;
        int allNPCsFound = 0;
        //Clean up bugged NPCs with quests attached wrongly
        final ArrayList<Trait> traitsToRemove = new ArrayList<>();
        for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
            allNPCsFound += 1;

            //No quests attached to NPC => check if it has the trait
            if (getAllQuestsAttachedToNPC(npc).size() == 0 && (main.getConversationManager().getConversationForNPCID(npc.getId()) == null)) {
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
                    main.getLogManager().info("NotQuests > Bugged trait removed from npc with ID <AQUA>" + npc.getId() + "</AQUA> and name <AQUA>" + npc.getName() + "</AQUA>!");

                }


            } else {
                //TODO: Remove debug shit or improve performance
                final ArrayList<String> attachedQuestNames = new ArrayList<>();
                for (final Quest attachedQuest : getAllQuestsAttachedToNPC(npc)) {
                    attachedQuestNames.add(attachedQuest.getQuestName());
                }
                main.getLogManager().info("NPC with the ID: <AQUA>" + npc.getId() + "</AQUA> is not bugged, because it has the following quests attached: <AQUA>" + attachedQuestNames + "</AQUA>");

            }
            traitsToRemove.clear();

        }
        if (buggedNPCsFound == 0) {
            main.getLogManager().info("No bugged NPCs found! Amount of checked NPCs: <AQUA>" + allNPCsFound + "</AQUA>");

        } else {
            main.getLogManager().info("<YELLOW><AQUA>" + buggedNPCsFound + "</AQUA> bugged NPCs have been found and removed! Amount of checked NPCs: <AQUA>" + allNPCsFound + "</AQUA>");

        }
    }




    public void sendCompletedObjectivesAndProgress(final Player player, final ActiveQuest activeQuest) {

        for (ActiveObjective activeObjective : activeQuest.getCompletedObjectives()) {

            final String objectiveDescription = activeObjective.getObjective().getObjectiveDescription();

            player.sendMessage("§7§m" + activeObjective.getObjective().getObjectiveID() + ". " + activeObjective.getObjective().getObjectiveFinalName() + ":");


            if (!objectiveDescription.isBlank()) {
                player.sendMessage("   §7§mDescription: §f§m" + objectiveDescription);
            }

            player.sendMessage(getObjectiveTaskDescription(activeObjective.getObjective(), true, player));
            player.sendMessage("   §7§mProgress: §f§m" + activeObjective.getCurrentProgress() + " / " + activeObjective.getProgressNeeded());
        }
    }


    public final String getObjectiveTaskDescription(final Objective objective, boolean completed, final Player player) {
        String toReturn = "";
        String eventualColor = "";
        if (completed) {
            eventualColor = "§m";
        }
        toReturn += objective.getObjectiveTaskDescription(eventualColor, player);

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

    public void sendActiveObjectivesAndProgress(final Player player, final ActiveQuest activeQuest) {

        for (ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {

            if (activeObjective.isUnlocked()) {
                final String objectiveDescription = activeObjective.getObjective().getObjectiveDescription();
                player.sendMessage(main.getLanguageManager().getString("chat.objectives.counter", player, activeQuest, activeObjective));

                if (!objectiveDescription.isBlank()) {
                    player.sendMessage(main.getLanguageManager().getString("chat.objectives.description", player, activeQuest, activeObjective)
                            .replace("%OBJECTIVEDESCRIPTION%", activeObjective.getObjective().getObjectiveDescription()));
                }

                player.sendMessage(getObjectiveTaskDescription(activeObjective.getObjective(), false, player));

                player.sendMessage(main.getLanguageManager().getString("chat.objectives.progress", player, activeQuest, activeObjective));
            } else {
                player.sendMessage(main.getLanguageManager().getString("chat.objectives.hidden", player, activeObjective, activeObjective));

            }

        }
    }

    public void sendObjectives(final Player player, final Quest quest) {
        for (final Objective objective : quest.getObjectives()) {
            final String objectiveDescription = objective.getObjectiveDescription();
            player.sendMessage("§a" + objective.getObjectiveID() + ". §e" + objective.getObjectiveFinalName());


            if (!objectiveDescription.isBlank()) {
                player.sendMessage("   §9Description: §6" + objectiveDescription);
            }

            player.sendMessage(getObjectiveTaskDescription(objective, false, player));


        }
    }


    public void sendObjectivesAdmin(final Audience audience, final Quest quest) {

        for (final Objective objective : quest.getObjectives()) {

            final String objectiveDescription = objective.getObjectiveDescription();
            audience.sendMessage(miniMessage.parse(
                    highlightGradient + objective.getObjectiveID() + ".</gradient> " + mainGradient + objective.getObjectiveFinalName()
            ));


            if (!objectiveDescription.isBlank()) {
                audience.sendMessage(miniMessage.parse(
                        highlightGradient + "   Description:</gradient> " + mainGradient + objectiveDescription
                ));
            }


            audience.sendMessage(miniMessage.parse(
                    highlightGradient + "   Conditions:</gradient>"
            ));
            int counter2 = 1;
            for (final Condition condition : objective.getConditions()) {
                audience.sendMessage(miniMessage.parse(
                        highlightGradient + "         " + counter2 + ".</gradient>" + mainGradient + " Condition: </gradient>" + highlight2Gradient + condition.getConditionDescription()
                ));
                counter2++;
            }
            if (counter2 == 1) {
                audience.sendMessage(miniMessage.parse(
                        unimportant + "      No conditions found!"
                ));
            }

            audience.sendMessage(miniMessage.parse(getObjectiveTaskDescription(objective, false, null)));

        }
    }


    public void sendActiveObjective(final Player player, ActiveObjective activeObjective) {

        if (activeObjective.isUnlocked()) {
            final String objectiveDescription = activeObjective.getObjective().getObjectiveDescription();

            player.sendMessage("§e" + activeObjective.getObjective().getObjectiveID() + ". " + activeObjective.getObjective().getObjectiveFinalName() + ":");


            if (!objectiveDescription.isBlank()) {
                player.sendMessage("   §9Description: §6" + objectiveDescription);
            }

            player.sendMessage(getObjectiveTaskDescription(activeObjective.getObjective(), false, player));


            player.sendMessage("   §7Progress: §f" + activeObjective.getCurrentProgress() + " / " + activeObjective.getProgressNeeded());
        } else {
            player.sendMessage("§e" + activeObjective.getObjective().getObjectiveID() + ". §7§l[HIDDEN]");

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
                    main.getLogManager().warn("A quest has an invalid npc attached to it, which should be removed. Report it to an admin. Quest name: <AQUA>" + quest.getQuestName() + "</AQUA>");
                    continue;
                }
                final Location npcLocation = npc.getEntity().getLocation();
                if (npcLocation.getWorld() != null && npcLocation.getWorld().equals(player.getWorld())) {
                    if (npcLocation.distance(player.getLocation()) < closenessCheckDistance) {
                        return true;
                    }
                }
            }
        }

        return false;


    }


    public final ArrayList<String> getRewardTypesList() {
        return rewardTypesList;
    }

    public final ArrayList<String> getRequirementsTypesList() {
        return requirementsTypesList;
    }

    public final ArrayList<Player> getDebugEnabledPlayers() {
        return debugEnabledPlayers;
    }

    public void addDebugEnabledPlayer(final Player player) {
        this.debugEnabledPlayers.add(player);
    }

    public void removeDebugEnabledPlayer(final Player player) {
        this.debugEnabledPlayers.remove(player);
    }

    public final boolean isDebugEnabledPlayer(final Player player) {
        return this.debugEnabledPlayers.contains(player);
    }

}
