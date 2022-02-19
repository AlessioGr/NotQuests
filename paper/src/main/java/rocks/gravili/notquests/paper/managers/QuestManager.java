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

package rocks.gravili.notquests.paper.managers;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
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
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.structs.*;
import rocks.gravili.notquests.paper.structs.actions.*;
import rocks.gravili.notquests.paper.structs.conditions.*;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.triggers.Trigger;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


public class QuestManager {

    private final NotQuests main;

    private final ArrayList<Quest> quests;

    private final ArrayList<Player> debugEnabledPlayers;


    public QuestManager(NotQuests main) {
        this.main = main;
        quests = new ArrayList<>();

        debugEnabledPlayers = new ArrayList<>();
    }

    public final String createQuest(String questName, final Category category) {
        if (getQuest(questName) == null) {
            if(questName.contains("°")){
                return ("<error>The symbol <highlight>°</highlight> cannot be used, because it's used for some important, plugin-internal stuff.");
            }
            Quest newQuest = new Quest(main, questName, category);
            quests.add(newQuest);
            category.getQuestsConfig().set("quests." + questName, "");
            category.saveQuestsConfig();
            return ("<success>Quest <highlight>" + questName + "</highlight> successfully created!");
        } else {
            return ("<error>Quest <highlight>" + questName + "</highlight> already exists!");
        }
    }

    public final String createQuest(String questName) {
        if (getQuest(questName) == null) {
            if(questName.contains("°")){
                return ("<error>The symbol <highlight>°</highlight> cannot be used, because it's used for some important, plugin-internal stuff.");
            }
            Quest newQuest = new Quest(main, questName);
            quests.add(newQuest);
            newQuest.getCategory().getQuestsConfig().set("quests." + questName, "");
            newQuest.getCategory().saveQuestsConfig();
            return ("<success>Quest <highlight>" + questName + "</highlight> successfully created!");
        } else {
            return ("<error>Quest <highlight>" + questName + "</highlight> already exists!");
        }
    }

    public final String deleteQuest(String questName) {
        Quest questToDelete = getQuest(questName);

        if (questToDelete != null) {
            quests.remove(questToDelete);
            questToDelete.getCategory().getQuestsConfig().set("quests." + questName, null);
            questToDelete.getCategory().saveQuestsConfig();
            return ("<success>Quest <highlight>" + questName + "</highlight> successfully deleted!");
        } else {
            return ("<error>Quest <highlight>" + questName + "</highlight> doesn't exists!");
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
        try{
            if (main.getIntegrationsManager().isCitizensEnabled()) {
                main.getIntegrationsManager().getCitizensManager().registerQuestGiverTrait();
            }
        }catch (Exception e){
            if(main.getConfiguration().isDebug()){
                e.printStackTrace();
            }else{
                main.getLogManager().warn("Citizens threw a random error - I don't know why. You can probably safely ignore this.");
            }
        }


        ArrayList<String> categoriesStringList = new ArrayList<>();
        for (final Category category : main.getDataManager().getCategories()) {
            categoriesStringList.add(category.getCategoryFullName());
        }
        main.getLogManager().info("Scheduled Quest Data load for following categories: <highlight>" + categoriesStringList.toString() );

        quests.clear();
        for (final Category category : main.getDataManager().getCategories()) {
            loadQuestsFromConfig(category);
        }

    }

    public void loadQuestsFromConfig(final Category category) {
        try {
            main.getLogManager().info("Loading Quests data from <highlight>" + category.getCategoryName() + "</highlight> category...");
            //main.getUpdateManager().convertQuestsYMLActions();
            //main.getUpdateManager().convertActionsYMLBeforeVersion3();

            if(category.getQuestsConfig() == null){
                main.getLogManager().severe("Error: Cannot load quests of category <highlight>" + category.getCategoryFullName() + "</highlight>, because it doesn't have a quests config. This category has been skipped.");
                return;
            }

            //Quests
            final ConfigurationSection questsConfigurationSection = category.getQuestsConfig().getConfigurationSection("quests");


            if (questsConfigurationSection != null) {
                for (String questName : questsConfigurationSection.getKeys(false)) {
                    main.getLogManager().info("   Loading Quest <highlight>" + questName + "</highlight>...");

                    Quest quest = new Quest(main, questName, category);
                    quest.setMaxAccepts(category.getQuestsConfig().getInt("quests." + questName + ".maxAccepts", -1));
                    quest.setTakeEnabled(category.getQuestsConfig().getBoolean("quests." + questName + ".takeEnabled", true));
                    quest.setAcceptCooldown(category.getQuestsConfig().getLong("quests." + questName + ".acceptCooldown", -1));

                    quest.setQuestDescription(
                            category.getQuestsConfig().getString("quests." + questName + ".description", "")
                                    .replace("\\n", "\n"), false
                    );
                    quest.setQuestDisplayName(
                            category.getQuestsConfig().getString("quests." + questName + ".displayName", "")
                                    .replace("\\n", "\n"), false
                    );

                    //Objectives:
                    final ConfigurationSection objectivesConfigurationSection = category.getQuestsConfig().getConfigurationSection("quests." + questName + ".objectives");
                    if (objectivesConfigurationSection != null) {
                        for (final String objectiveNumber : objectivesConfigurationSection.getKeys(false)) {
                            Class<? extends Objective> objectiveType = null;

                            try {
                                objectiveType = main.getObjectiveManager().getObjectiveClass(category.getQuestsConfig().getString("quests." + questName + ".objectives." + objectiveNumber + ".objectiveType"));
                            } catch (java.lang.NullPointerException ex) {
                                main.getDataManager().disablePluginAndSaving("Error parsing objective Type of objective with ID <highlight>" + objectiveNumber + "</highlight>.", quest, ex);
                                return;
                            }
                            final int progressNeeded = category.getQuestsConfig().getInt("quests." + questName + ".objectives." + objectiveNumber + ".progressNeeded", 1);

                            final Location location = category.getQuestsConfig().getLocation("quests." + questName + ".objectives." + objectiveNumber + ".location", null);
                            final boolean showLocation = category.getQuestsConfig().getBoolean("quests." + questName + ".objectives." + objectiveNumber + ".showLocation", false);


                            int objectiveID = -1;
                            boolean validObjectiveID = true;
                            try {
                                objectiveID = Integer.parseInt(objectiveNumber);
                            } catch (java.lang.NumberFormatException ex) {
                                validObjectiveID = false;
                                main.getDataManager().disablePluginAndSaving("Error parsing loaded objective ID <highlight>" + objectiveNumber + "</highlight>.", quest, ex);
                                return;
                            }

                            if (validObjectiveID && objectiveID > 0 && objectiveType != null) {


                                Objective objective = null;

                                try {
                                    objective = objectiveType.getDeclaredConstructor(NotQuests.class).newInstance(main);
                                    objective.setQuest(quest);
                                    objective.setObjectiveID(objectiveID);
                                    objective.setProgressNeeded(progressNeeded);
                                    objective.setLocation(location, false);
                                    objective.setShowLocation(showLocation, false);

                                    objective.load(category.getQuestsConfig(), "quests." + questName + ".objectives." + objectiveNumber);

                                } catch (Exception ex) {
                                    main.getDataManager().disablePluginAndSaving("Error parsing objective Type of objective with ID <highlight>" + objectiveNumber + "</highlight>.", quest, ex);
                                    return;
                                }


                                if (objective != null) {

                                    final String objectiveDisplayName = category.getQuestsConfig().getString("quests." + questName + ".objectives." + objectiveNumber + ".displayName", "");
                                    String objectiveDescription = category.getQuestsConfig().getString("quests." + questName + ".objectives." + objectiveNumber + ".description", "");
                                    final int completionNPCID = category.getQuestsConfig().getInt("quests." + quest.getQuestName() + ".objectives." + objectiveNumber + ".completionNPCID", -1);
                                    final String completionArmorStandUUIDString = category.getQuestsConfig().getString("quests." + quest.getQuestName() + ".objectives." + objectiveNumber + ".completionArmorStandUUID", null);
                                    if (completionArmorStandUUIDString != null) {
                                        final UUID completionArmorStandUUID = UUID.fromString(completionArmorStandUUIDString);
                                        objective.setCompletionArmorStandUUID(completionArmorStandUUID, false);
                                    }

                                    objective.setObjectiveDescription(objectiveDescription.replace("\\n", "\n"), false);

                                    objective.setObjectiveDisplayName(objectiveDisplayName.replace("\\n", "\n"), false);


                                    objective.setCompletionNPCID(completionNPCID, false);
                                    quest.addObjective(objective, false);
                                } else {
                                    main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an error while loading quests objective data. Objective ID: <highlight>" + objectiveNumber, quest);
                                    return;
                                }

                            } else {
                                main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an error while loading quests objective data (2). Objective ID: <highlight>" + objectiveNumber, quest);
                                return;
                            }
                        }
                    }

                    //Requirements:
                    final ConfigurationSection requirementsConfigurationSection = category.getQuestsConfig().getConfigurationSection("quests." + questName + ".requirements");
                    if (requirementsConfigurationSection != null) {
                        for (String requirementNumber : requirementsConfigurationSection.getKeys(false)) {

                            int requirementID = -1;
                            boolean validRequirementID = true;
                            try {
                                requirementID = Integer.parseInt(requirementNumber);
                            } catch (java.lang.NumberFormatException ex) {
                                validRequirementID = false;
                                main.getDataManager().disablePluginAndSaving("Error parsing loaded requirement ID <highlight>" + requirementNumber + "</highlight>.", quest, ex);
                                return;
                            }

                            Class<? extends Condition> conditionType = null;
                            String conditionTypeString = category.getQuestsConfig().getString("quests." + questName + ".requirements." + requirementNumber + ".conditionType", "");


                            try {

                                /*if(conditionTypeString.isBlank()){//User might be using old system with requirementType instead of conditionType. Let's convert it!
                                    conditionTypeString = main.getUpdateManager().convertQuestRequirementTypeToConditionType(questName, requirementNumber);
                                }
                                conditionTypeString = main.getUpdateManager().convertOldConditionTypesToNewConditionTypes(conditionTypeString);*/

                                conditionType = main.getConditionsManager().getConditionClass(conditionTypeString);
                            } catch (java.lang.NullPointerException ex) {
                                main.getDataManager().disablePluginAndSaving("Error parsing requirement Type of requirement with ID <highlight>" + requirementNumber + "</highlight>.", quest, ex);
                                return;
                            }

                            //RequirementType requirementType = RequirementType.valueOf(main.getDataManager().getQuestsData().getString("quests." + questName + ".requirements." + requirementNumber + ".requirementType"));
                            int progressNeeded = category.getQuestsConfig().getInt("quests." + questName + ".requirements." + requirementNumber + ".progressNeeded");
                            boolean negated = category.getQuestsConfig().getBoolean("quests." + questName + ".requirements." + requirementNumber + ".negated", false);
                            String description = category.getQuestsConfig().getString("quests." + questName + ".requirements." + requirementNumber + ".description", "");

                            if (validRequirementID && requirementID > 0 && conditionType != null) {
                                Condition condition = null;

                                try {
                                    condition = conditionType.getDeclaredConstructor(NotQuests.class).newInstance(main);
                                    condition.setProgressNeeded(progressNeeded);
                                    condition.setQuest(quest);
                                    condition.setNegated(negated);
                                    condition.setDescription(description);
                                    condition.setCategory(category);
                                    condition.setConditionID(requirementID);
                                    condition.load(category.getQuestsConfig(), "quests." + questName + ".requirements." + requirementID);
                                } catch (Exception ex) {
                                    main.getDataManager().disablePluginAndSaving("Error parsing requirement Type of requirement with ID <highlight>" + requirementNumber + "</highlight>.", quest, ex);
                                    return;
                                }
                                if (condition != null) {
                                    quest.addRequirement(condition, false);
                                }

                            } else {
                                main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an error while loading quests requirement data. Valid Requirement ID: " + validRequirementID + ". requirementID > 0: " + (requirementID > 0) + ". conditionType != null: " + (conditionType != null) + " ConditionTypeString: " + conditionTypeString + " Quest Name: " + questName + " Requirement ID: " + requirementNumber + " ConditionType String: " + conditionTypeString, quest);
                                return;
                            }

                        }
                    }


                    //Rewards for Quests:
                    final ConfigurationSection rewardsConfigurationSection = category.getQuestsConfig().getConfigurationSection("quests." + questName + ".rewards");
                    if (rewardsConfigurationSection != null) {
                        for (String rewardNumber : rewardsConfigurationSection.getKeys(false)) {

                            int rewardID = -1;
                            boolean validRewardID = true;
                            try {
                                rewardID = Integer.parseInt(rewardNumber);
                            } catch (java.lang.NumberFormatException ex) {
                                validRewardID = false;
                                main.getDataManager().disablePluginAndSaving("Error parsing loaded reward ID <highlight>" + rewardNumber + "</highlight>.", quest, ex);
                                return;
                            }

                            Class<? extends Action> actionType = null;
                            String actionTypeString = category.getQuestsConfig().getString("quests." + questName + ".rewards." + rewardNumber + ".actionType", "");
                            /*if (actionTypeString.isBlank()) {
                                actionTypeString = main.getUpdateManager().convertQuestRewardTypeToActionType(questName, rewardNumber);
                            }*/

                            try {
                                actionType = main.getActionManager().getActionClass(actionTypeString);
                            } catch (java.lang.NullPointerException ex) {
                                main.getDataManager().disablePluginAndSaving("Error parsing reward Type of reward with ID <highlight>" + rewardNumber + "</highlight>.", quest, ex);
                                return;
                            }

                            if (validRewardID && rewardID > 0 && actionType != null) {
                                Action reward = null;

                                try {
                                    reward = actionType.getDeclaredConstructor(NotQuests.class).newInstance(main);
                                    reward.setQuest(quest);
                                    reward.setCategory(category);
                                    reward.setActionID(rewardID);
                                    reward.load(category.getQuestsConfig(), "quests." + questName + ".rewards." + rewardID);

                                } catch (Exception ex) {
                                    main.getDataManager().disablePluginAndSaving("Error parsing reward Type of reward with ID <highlight>" + rewardNumber + "</highlight>.", quest, ex);
                                    return;
                                }

                                if (reward != null) {
                                    final String rewardDisplayName = category.getQuestsConfig().getString("quests." + questName + ".rewards." + rewardNumber + ".displayName", "");
                                    reward.setActionName(rewardDisplayName);
                                    quest.addReward(reward, false);
                                } else {
                                    main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an error while loading quests reward data for Reward with the ID <highlight>" + rewardNumber + "</highlight>.", quest);
                                    return;
                                }
                            } else {
                                main.getDataManager().disablePluginAndSaving("Error loading Quest reward <highlight>" + rewardNumber + "</highlight>.", quest);
                                return;
                            }


                        }
                    }


                    //Triggers:
                    final ConfigurationSection triggersConfigurationSection = category.getQuestsConfig().getConfigurationSection("quests." + questName + ".triggers");
                    if (triggersConfigurationSection != null) {
                        for (final String triggerNumber : triggersConfigurationSection.getKeys(false)) {


                            int triggerID = -1;
                            boolean validTriggerID = true;
                            try {
                                triggerID = Integer.parseInt(triggerNumber);
                            } catch (java.lang.NumberFormatException ex) {
                                validTriggerID = false;
                                main.getDataManager().disablePluginAndSaving("Error parsing loaded trigger ID ID <highlight>" + triggerNumber + "</highlight>.", quest, ex);
                                return;
                            }

                            Class<? extends Trigger> triggerType = null;
                            final String triggerTypeString = category.getQuestsConfig().getString("quests." + questName + ".triggers." + triggerNumber + ".triggerType");

                            try {
                                triggerType = main.getTriggerManager().getTriggerClass(triggerTypeString);
                            } catch (java.lang.NullPointerException ex) {
                                main.getDataManager().disablePluginAndSaving("Error parsing trigger Type of trigger with ID <highlight>" + triggerNumber + "</highlight>.", quest);
                                return;
                            }


                            final String triggerActionName = category.getQuestsConfig().getString("quests." + questName + ".triggers." + triggerNumber + ".triggerActionName");
                            final long amountNeeded = category.getQuestsConfig().getLong("quests." + questName + ".triggers." + triggerNumber + ".amountNeeded", 1);

                            final int applyOn = category.getQuestsConfig().getInt("quests." + questName + ".triggers." + triggerNumber + ".applyOn");
                            final String worldName = category.getQuestsConfig().getString("quests." + questName + ".triggers." + triggerNumber + ".worldName", "ALL");

                            if (Bukkit.getWorld(worldName) == null) {
                                main.getLogManager().warn("The world of the trigger <highlight>" + triggerNumber + "</highlight> of Quest <highlight>" + questName + "</highlight> was not found.");
                            }


                            Action foundAction = main.getActionsYMLManager().getAction(triggerActionName);

                            if (validTriggerID && triggerID > 0 && triggerType != null && foundAction != null) {
                                Trigger trigger = null;

                                try {
                                    trigger = triggerType.getDeclaredConstructor(NotQuests.class).newInstance(main);
                                    trigger.setQuest(quest);
                                    trigger.setTriggerID(triggerID);
                                    trigger.setAction(foundAction);
                                    trigger.setApplyOn(applyOn);
                                    trigger.setWorldName(worldName);
                                    trigger.setAmountNeeded(amountNeeded);
                                    trigger.setCategory(category);

                                    trigger.load(category.getQuestsConfig(), "quests." + questName + ".triggers." + triggerNumber);
                                } catch (Exception ex) {
                                    main.getDataManager().disablePluginAndSaving("Error parsing requirement Type of trigger with ID <highlight>" + triggerNumber + "</highlight>.", quest, ex);
                                    return;
                                }
                                if (trigger != null) {
                                    quest.addTrigger(trigger, false);
                                }

                            } else {
                                main.getDataManager().disablePluginAndSaving("ERROR when loading trigger with the triggerNumber <highlight>" + triggerNumber + " </highlight>: Action could not be loaded.", quest);
                                return;
                            }


                        }
                    }


                    //Convert old dependencies
                    //main.getUpdateManager().convertObjectiveDependenciesToNewObjectiveConditions(quest);


                    //Objective Conditions and Rewards
                    main.getLogManager().debug("Loading objective conditions and rewards...");
                    for (final Objective objective : quest.getObjectives()) { //TODO: Add objective name to error or debug messages to discern from normal requirement loading
                        final ConfigurationSection objectiveConditionsConfigurationSection = category.getQuestsConfig().getConfigurationSection("quests." + quest.getQuestName() + ".objectives." + objective.getObjectiveID() + ".conditions.");
                        if (objectiveConditionsConfigurationSection != null) {
                            for (String objectiveConditionNumber : objectiveConditionsConfigurationSection.getKeys(false)) {
                                int conditionID = -1;
                                boolean validConditionID = true;
                                try {
                                    conditionID = Integer.parseInt(objectiveConditionNumber);
                                } catch (java.lang.NumberFormatException ex) {
                                    validConditionID = false;
                                    main.getDataManager().disablePluginAndSaving("Error parsing loaded condition ID <highlight>" + objectiveConditionNumber + "</highlight>.", quest, objective, ex);
                                    return;
                                }

                                Class<? extends Condition> conditionType = null;
                                String conditionTypeString = category.getQuestsConfig().getString("quests." + questName + ".objectives." + (objective.getObjectiveID()) + ".conditions." + objectiveConditionNumber + ".conditionType", "");
                                //conditionTypeString = main.getUpdateManager().convertOldConditionTypesToNewConditionTypes(conditionTypeString);

                                try {
                                    conditionType = main.getConditionsManager().getConditionClass(conditionTypeString);
                                } catch (java.lang.NullPointerException ex) {
                                    main.getDataManager().disablePluginAndSaving("Error parsing condition Type of requirement with ID <highlight>" + objectiveConditionNumber + "</highlight>.", quest, objective, ex);
                                    return;
                                }

                                int progressNeeded = category.getQuestsConfig().getInt("quests." + questName + ".objectives." + (objective.getObjectiveID()) + ".conditions." + objectiveConditionNumber + ".progressNeeded");
                                boolean negated = category.getQuestsConfig().getBoolean("quests." + questName + ".objectives." + (objective.getObjectiveID()) + ".conditions." + objectiveConditionNumber + ".negated", false);
                                String description = category.getQuestsConfig().getString("quests." + questName + ".objectives." + (objective.getObjectiveID()) + ".conditions." + objectiveConditionNumber + ".description", "");

                                if (validConditionID && conditionID > 0 && conditionType != null) {
                                    Condition condition = null;

                                    try {
                                        condition = conditionType.getDeclaredConstructor(NotQuests.class).newInstance(main);
                                        condition.setProgressNeeded(progressNeeded);
                                        condition.setQuest(quest);
                                        condition.setObjective(objective);
                                        condition.setNegated(negated);
                                        condition.setDescription(description);
                                        condition.setCategory(category);
                                        condition.setConditionID(conditionID);

                                        condition.load(category.getQuestsConfig(), "quests." + questName + ".objectives." + (objective.getObjectiveID()) + ".conditions." + objectiveConditionNumber);
                                    } catch (Exception ex) {
                                        main.getDataManager().disablePluginAndSaving("Error parsing condition Type of requirement with ID <highlight>" + objectiveConditionNumber + "</highlight>.", quest, objective, ex);
                                        return;
                                    }
                                    if (condition != null) {
                                        objective.addCondition(condition, false);
                                    }

                                } else {
                                    main.getDataManager().disablePluginAndSaving("Error loading condition. ValidRequirementID: " + validConditionID + " conditionID: " + conditionID + " ConditionTypeNull?" + (conditionType == null) + " ConditionType: " + (conditionType != null ? conditionType.toString() : "null") + " conditionTypeString: " + conditionTypeString, quest, objective);
                                    return;
                                }
                            }
                        }


                        final String initialObjectiveRewardsPath = "quests." + quest.getQuestName() + ".objectives." + objective.getObjectiveID() + ".rewards.";
                        final ConfigurationSection objectiveRewardsConfigurationSection = category.getQuestsConfig().getConfigurationSection(initialObjectiveRewardsPath);
                        if (objectiveRewardsConfigurationSection != null) {
                            for (String objectiveRewardNumber : objectiveRewardsConfigurationSection.getKeys(false)) {
                                int rewardID = -1;
                                boolean validRewardID = true;
                                try {
                                    rewardID = Integer.parseInt(objectiveRewardNumber);
                                } catch (java.lang.NumberFormatException ex) {
                                    validRewardID = false;
                                    main.getDataManager().disablePluginAndSaving("Error parsing loaded objective reward ID <highlight>" + objectiveRewardNumber + "</highlight>.", quest, objective, ex);
                                    return;
                                }

                                Class<? extends Action> actionType = null;
                                final String actionTypeString = category.getQuestsConfig().getString(initialObjectiveRewardsPath + objectiveRewardNumber + ".actionType", "");

                                try {
                                    actionType = main.getActionManager().getActionClass(actionTypeString);
                                } catch (java.lang.NullPointerException ex) {
                                    main.getDataManager().disablePluginAndSaving("Error parsing action Type of objective reward with ID <highlight>" + objectiveRewardNumber + "</highlight>.", quest, objective, ex);
                                    return;
                                }

                                if (validRewardID && rewardID > 0 && actionType != null) {
                                    Action reward = null;

                                    try {
                                        reward = actionType.getDeclaredConstructor(NotQuests.class).newInstance(main);
                                        reward.setQuest(quest);
                                        reward.load(category.getQuestsConfig(), initialObjectiveRewardsPath + rewardID);
                                        reward.setCategory(category);
                                        reward.setActionID(rewardID);

                                    } catch (Exception ex) {
                                        main.getDataManager().disablePluginAndSaving("Error parsing reward Type of reward with ID <highlight>" + objectiveRewardNumber + "</highlight>.", quest, objective, ex);
                                        return;
                                    }

                                    if (reward != null) {
                                        final String rewardDisplayName = category.getQuestsConfig().getString(initialObjectiveRewardsPath + rewardID + ".displayName", "");
                                        reward.setActionName(rewardDisplayName);
                                        objective.addReward(reward, false);
                                    } else {
                                        main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an error while loading objective reward data.");
                                        return;
                                    }
                                } else {
                                    main.getDataManager().disablePluginAndSaving("Error loading Objective reward <highlight>" + objectiveRewardNumber + "</highlight>.", quest, objective);
                                    return;
                                }

                            }
                        }
                    }


                    //TakeItem:
                    quest.setTakeItem(category.getQuestsConfig().getItemStack("quests." + questName + ".takeItem"));

                    quests.add(quest);
                }
            }
            main.getDataManager().setAlreadyLoadedQuests(true);
        } catch (Exception ex) {
            main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an exception while loading quests data.", ex);
            return;
        }


    }


    public final ArrayList<Quest> getAllQuestsAttachedToArmorstand(final ArmorStand armorstand) {
        return new ArrayList<>() {{
            PersistentDataContainer armorstandPDB = armorstand.getPersistentDataContainer();
            NamespacedKey attachedShowingQuestsKey = new NamespacedKey(main.getMain(), "notquests-attachedQuests-showing");
            NamespacedKey attachedNonShowingQuestsKey = new NamespacedKey(main.getMain(), "notquests-attachedQuests-nonshowing");

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
            NamespacedKey attachedQuestsKey = new NamespacedKey(main.getMain(), "notquests-attachedQuests-showing");

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
            NamespacedKey attachedQuestsKey = new NamespacedKey(main.getMain(), "notquests-attachedQuests-nonshowing");

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

        if (main.getConfiguration().isQuestPreviewUseGUI()) {

            main.getGuiManager().showTakeQuestsGUI(main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId()), player, questsAttachedToNPC);
            /*String[] guiSetup = {
                    "xxxxxxxxx",
                    "xgggggggx",
                    "xgggggggx",
                    "xgggggggx",
                    "xgggggggx",
                    "pxxxxxxxn"
            };
            InventoryGui gui = new InventoryGui(main.getMain(), player, main.getUtilManager().miniMessageToLegacyWithSpigotRGB(main.getLanguageManager().getString("gui.availableQuests.title", player)), guiSetup);
            gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

            int count = 0;
            GuiElementGroup group = new GuiElementGroup('g');

            for (final Quest quest : questsAttachedToNPC) {
                final ItemStack materialToUse = quest.getTakeItem();

                String displayName = quest.getQuestFinalName();

                displayName = main.getLanguageManager().getString("gui.availableQuests.button.questPreview.questNamePrefix", player, quest) + displayName;
                QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer((player.getUniqueId()));

                if (questPlayer != null && questPlayer.hasAcceptedQuest(quest)) {
                    displayName += main.getLanguageManager().getString("gui.availableQuests.button.questPreview.acceptedSuffix", player, quest);
                }
                String description = "";
                if (!quest.getQuestDescription().isBlank()) {
                    description = main.getLanguageManager().getString("gui.availableQuests.button.questPreview.questDescriptionPrefix", player, quest) + quest.getQuestDescription(main.getConfiguration().guiQuestDescriptionMaxLineLength);
                }
                count++;


                group.addElement(new StaticGuiElement('e',
                        materialToUse,
                        count, // Display a number as the item count
                        click -> {
                            player.chat("/notquests preview " + quest.getQuestName());
                            return true; // returning true will cancel the click event and stop taking the item

                        },
                        main.getUtilManager().miniMessageToLegacyWithSpigotRGB(displayName),
                        main.getUtilManager().miniMessageToLegacyWithSpigotRGB(description),
                        main.getUtilManager().miniMessageToLegacyWithSpigotRGB(main.getLanguageManager().getString("gui.availableQuests.button.questPreview.bottomText", player, questPlayer, quest))
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


            gui.show(player);*/
        } else {
            main.getLogManager().info("NotQuests > All quest count: <highlight>" + quests.size() + "</highlight>");

            player.sendMessage(Component.empty());
            player.sendMessage(main.parse(
                    "<BLUE>" + questsAttachedToNPC.size() + " Available Quests:"
            ));
            int counter = 1;

            for (Quest quest : questsAttachedToNPC) {

                Component acceptComponent = main.parse("<GREEN>**[CHOOSE]")
                        .clickEvent(ClickEvent.runCommand("/nquests preview " + quest.getQuestName()))
                        .hoverEvent(HoverEvent.showText(main.parse("<GREEN>Click to preview/choose the quest <highlight>" + quest.getQuestFinalName())));

                Component component = main.parse("<YELLOW>" + counter + ". <highlight>" + quest.getQuestFinalName() + " ")
                        .append(acceptComponent);

                player.sendMessage(component);


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
        if (main.getConfiguration().isQuestPreviewUseGUI()) {
            main.getGuiManager().showTakeQuestsGUI(main.getQuestPlayerManager().getQuestPlayer(player.getUniqueId()), player, questsAttachedToNPC );
            /*String[] guiSetup = {
                    "xxxxxxxxx",
                    "xgggggggx",
                    "xgggggggx",
                    "xgggggggx",
                    "xgggggggx",
                    "pxxxxxxxn"
            };
            InventoryGui gui = new InventoryGui(main.getMain(), player, main.getUtilManager().miniMessageToLegacyWithSpigotRGB(main.getLanguageManager().getString("gui.availableQuests.title", player)), guiSetup);
            gui.setFiller(new ItemStack(Material.AIR, 1)); // fill the empty slots with this

            int count = 0;
            GuiElementGroup group = new GuiElementGroup('g');

            for (final Quest quest : questsAttachedToNPC) {
                final ItemStack materialToUse = quest.getTakeItem();


                String displayName = quest.getQuestFinalName();

                displayName = main.getLanguageManager().getString("gui.availableQuests.button.questPreview.questNamePrefix", player, quest) + displayName;
                QuestPlayer questPlayer = main.getQuestPlayerManager().getQuestPlayer((player.getUniqueId()));

                if (questPlayer != null && questPlayer.hasAcceptedQuest(quest)) {
                    displayName += main.getLanguageManager().getString("gui.availableQuests.button.questPreview.acceptedSuffix", player, quest);
                }
                String description = "";
                if (!quest.getQuestDescription().isBlank()) {
                    description = main.getLanguageManager().getString("gui.availableQuests.button.questPreview.questDescriptionPrefix", player, quest) + quest.getQuestDescription(main.getConfiguration().guiQuestDescriptionMaxLineLength);
                }
                count++;


                group.addElement(new StaticGuiElement('e',
                        materialToUse,
                        count, // Display a number as the item count
                        click -> {
                            player.chat("/notquests preview " + quest.getQuestName());
                            return true; // returning true will cancel the click event and stop taking the item

                        },
                        main.getUtilManager().miniMessageToLegacyWithSpigotRGB(displayName),
                        main.getUtilManager().miniMessageToLegacyWithSpigotRGB(description),
                        main.getUtilManager().miniMessageToLegacyWithSpigotRGB(main.getLanguageManager().getString("gui.availableQuests.button.questPreview.bottomText", player, questPlayer, quest))
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


            gui.show(player);*/
        } else {
            main.getLogManager().info("NotQuests > All quest count: <highlight>" + quests.size() + "</highlight>");

            player.sendMessage(Component.empty());
            player.sendMessage(main.parse(
                    "<BLUE>" + questsAttachedToNPC.size() + " Availahle Quests:"
            ));
            int counter = 1;

            for (Quest quest : questsAttachedToNPC) {

                Component acceptComponent = main.parse("<GREEN>**[CHOOSE]")
                        .clickEvent(ClickEvent.runCommand("/nquests preview " + quest.getQuestName()))
                        .hoverEvent(HoverEvent.showText(main.parse("<GREEN>Click to preview/choose the quest <highlight>" + quest.getQuestFinalName())));

                Component component = main.parse("<YELLOW>" + counter + ". <highlight>" + quest.getQuestFinalName() + " ")
                        .append(acceptComponent);

                player.sendMessage(component);

                counter++;
            }
            //getQuestsAttachedToNPC(npc);
        }

    }

    public final String getQuestRequirements(final Quest quest, final Player player) {
        StringBuilder requirements = new StringBuilder();
        int counter = 1;
        for (final Condition condition : quest.getRequirements()) {
            if(counter != 1){
                requirements.append("\n");
            }
            requirements.append("<GREEN>").append(counter).append(". <YELLOW>").append(condition.getConditionType()).append("\n");

            requirements.append(condition.getConditionDescription(player)).append("\n");


            counter += 1;
        }
        return requirements.toString();
    }

    public final String getDisplayActionType(final Action action){
        String actionType = action.getActionType();

        if(action instanceof NumberAction numberAction){
            actionType = numberAction.getVariableName();
        }else if(action instanceof StringAction stringAction){
            actionType = stringAction.getVariableName();
        }else if(action instanceof BooleanAction booleanAction){
            actionType = booleanAction.getVariableName();
        }else if(action instanceof ListAction listAction){
            actionType = listAction.getVariableName();
        }else if(action instanceof ItemStackListAction itemStackListAction){
            actionType = itemStackListAction.getVariableName();
        }

        return actionType;
    }

    public final String getDisplayConditionType(final Condition condition){
        String conditionType = condition.getConditionType();


        if(condition instanceof NumberCondition numberCondition){
            conditionType = numberCondition.getVariableName();
        }else if(condition instanceof StringCondition stringCondition){
            conditionType = stringCondition.getVariableName();
        }else if(condition instanceof BooleanCondition booleanCondition){
            conditionType = booleanCondition.getVariableName();
        }else if(condition instanceof ListCondition listCondition){
            conditionType = listCondition.getVariableName();
        }else if(condition instanceof ItemStackListCondition itemStackListCondition){
            conditionType = itemStackListCondition.getVariableName();
        }
        return conditionType;
    }

    public final ArrayList<String> getQuestRequirementsList(final Quest quest, final Player player) {
        final ArrayList<String> requirements = new ArrayList<>();
        int counter = 1;
        for (final Condition condition : quest.getRequirements()) {
            requirements.add("<GREEN>" + counter + ". <YELLOW>" + getDisplayConditionType(condition));
            requirements.add(condition.getConditionDescription(player, quest));

            counter += 1;
        }
        return requirements;
    }

    public final String getQuestRewards(final Quest quest, final Player player) {
        StringBuilder rewards = new StringBuilder();
        int counter = 1;
        for (final Action reward : quest.getRewards()) {
            if (counter != 1) {
                rewards.append("\n");
            }
            if (!reward.getActionName().isBlank()) {
                rewards.append("<GREEN>").append(counter).append(". <BLUE>").append(reward.getActionName()).append("</GREEN>");
            } else {
                if (main.getConfiguration().hideRewardsWithoutName) {
                    rewards.append("<GREEN>").append(counter).append(main.getLanguageManager().getString("gui.reward-hidden-text", player, quest, reward)).append("</GREEN>");
                } else {
                    rewards.append("<GREEN>").append(counter).append(". <BLUE>").append(reward.getActionDescription(player)).append("</GREEN>");
                }

            }
            counter += 1;

        }
        return rewards.toString();
    }
    public final ArrayList<String> getQuestRewardsList(final Quest quest, final Player player) {
        ArrayList<String> rewards = new ArrayList<>();
        int counter = 1;
        for (final Action reward : quest.getRewards()) {
            if (!reward.getActionName().isBlank()) {
                rewards.add("<GREEN>" + counter + ". <BLUE>" + reward.getActionName() + "</GREEN>");
            } else {
                if (main.getConfiguration().hideRewardsWithoutName) {
                    rewards.add("<GREEN>" + counter + main.getLanguageManager().getString("gui.reward-hidden-text", player, quest, reward) + "</GREEN>");
                } else {
                    rewards.add("<GREEN>" + counter + ". <BLUE>" + reward.getActionDescription(player) + "</GREEN>");
                }
            }
            counter += 1;

        }
        return rewards;
    }

    public void sendSingleQuestPreview(Player player, Quest quest) {
        player.sendMessage(Component.empty());
        player.sendMessage(main.parse("<GRAY>-----------------------------------"));
        player.sendMessage(main.parse(
                "<BLUE>Quest Preview for Quest <highlight>" + quest.getQuestFinalName() + "</highlight>:"
        ));


        if (quest.getQuestDescription().length() >= 1) {
            player.sendMessage(main.parse(
                    "<YELLOW>Quest description: <GRAY>" + quest.getQuestDescription()
            ));
        } else {
            player.sendMessage(main.parse(
                    main.getLanguageManager().getString("chat.missing-quest-description", player)
            ));
        }

        player.sendMessage(main.parse(
                "<BLUE>Quest Requirements:"
        ));

        player.sendMessage(main.parse(
                getQuestRequirements(quest, player)
        ));

        player.sendMessage(main.parse(
                "<BLUE>Quest Rewards:"
        ));
        player.sendMessage(main.parse(
                getQuestRewards(quest, player)
        ));

        Component acceptComponent = main.parse("<GREEN>**[ACCEPT THIS QUEST]")
                .clickEvent(ClickEvent.runCommand("/nquests take " + quest.getQuestName()))
                .hoverEvent(HoverEvent.showText(main.parse("<GREEN>Click to accept the Quest <highlight>" + quest.getQuestFinalName())));

        player.sendMessage(Component.empty());
        player.sendMessage(acceptComponent);
        player.sendMessage(main.parse(
                "<GRAY>-----------------------------------"
        ));


    }

    public void loadNPCData() {
        if (main.getDataManager().isAlreadyLoadedQuests()) {
            for (final Category category : main.getDataManager().getCategories()) {
                loadNPCData(category);
            }
        } else {
            main.getLogManager().info("Tried to load NPC data before quest data was loaded. NotQuests is scheduling another load...");

            Bukkit.getScheduler().runTaskLaterAsynchronously(main.getMain(), () -> {
                if (!main.getDataManager().isAlreadyLoadedNPCs()) {
                    main.getLogManager().info("Trying to load NPC quest data again...");
                    main.getDataManager().loadNPCData();
                }
            }, 60);
        }

    }

    public void loadNPCData(final Category category) {
        main.getLogManager().info("Loading NPC data...");

        if (!main.getIntegrationsManager().isCitizensEnabled()) {
            main.getLogManager().warn("NPC data loading has been cancelled, because Citizens is not installed. Install the Citizens plugin if you want NPC stuff to work.");
            return;
        }




        try {
            final ConfigurationSection questsConfigurationSetting = category.getQuestsConfig().getConfigurationSection("quests");
            if (questsConfigurationSetting != null) {
                if (!Bukkit.isPrimaryThread()) {
                    Bukkit.getScheduler().runTask(main.getMain(), () -> {
                        for (String questName : questsConfigurationSetting.getKeys(false)) {
                            Quest quest = getQuest(questName);
                            if (quest != null) {
                                //NPC
                                final ConfigurationSection npcsConfigurationSection = category.getQuestsConfig().getConfigurationSection("quests." + questName + ".npcs");
                                if (npcsConfigurationSection != null) {
                                    for (String npcNumber : npcsConfigurationSection.getKeys(false)) {

                                        if (category.getQuestsConfig() != null) {
                                            final NPC npc = CitizensAPI.getNPCRegistry().getById(category.getQuestsConfig().getInt("quests." + questName + ".npcs." + npcNumber + ".npcID"));
                                            if (npc != null) {
                                                final boolean questShowing = category.getQuestsConfig().getBoolean("quests." + questName + ".npcs." + npc.getId() + ".questShowing", true);
                                                // call the callback with the result
                                                main.getLogManager().info("Attaching Quest with the name <highlight>" + quest.getQuestName() + "</highlight> to NPC with the ID <highlight>" + npc.getId() + " </highlight>and name <highlight>" + npc.getName());
                                                quest.removeNPC(npc);
                                                quest.bindToNPC(npc, questShowing);
                                            } else {
                                                main.getLogManager().warn("Error attaching npc with ID <highlight>" + category.getQuestsConfig().getInt("quests." + questName + ".npcs." + npcNumber + ".npcID")
                                                        + "</highlight> to quest <highlight>" + quest.getQuestName() + "</highlight> - NPC not found.");
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
                            final ConfigurationSection npcsConfigurationSection = category.getQuestsConfig().getConfigurationSection("quests." + questName + ".npcs");
                            if (npcsConfigurationSection != null) {
                                for (String npcNumber : npcsConfigurationSection.getKeys(false)) {

                                    final NPC npc = CitizensAPI.getNPCRegistry().getById(category.getQuestsConfig().getInt("quests." + questName + ".npcs." + npcNumber + ".npcID"));
                                    final boolean questShowing = category.getQuestsConfig().getBoolean("quests." + questName + ".npcs." + npc.getId() + ".questShowing", true);
                                    if (npc != null) {
                                        // call the callback with the result
                                        main.getLogManager().info("Attaching Quest with the name <highlight>" + quest.getQuestName() + " </highlight>to NPC with the ID <highlight>" + npc.getId() + " </highlight>and name <highlight>" + npc.getName());
                                        quest.removeNPC(npc);
                                        quest.bindToNPC(npc, questShowing);
                                    } else {
                                        main.getLogManager().warn("Error attaching npc with ID <highlight>" + category.getQuestsConfig().getInt("quests." + questName + ".npcs." + npcNumber + ".npcID")
                                                + "</highlight> to quest <highlight>" + quest.getQuestName() + "</highlight> - NPC not found.");
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
            return;
        }


    }

    public void cleanupBuggedNPCs() {
        if (!main.getIntegrationsManager().isCitizensEnabled()) {
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
                    Bukkit.getScheduler().runTask(main.getMain(), () -> {
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
                    main.getLogManager().info("NotQuests > Bugged trait removed from npc with ID <highlight>" + npc.getId() + "</highlight> and name <highlight>" + npc.getName() + "</highlight>!");

                }


            } else {
                //TODO: Remove debug shit or improve performance
                final ArrayList<String> attachedQuestNames = new ArrayList<>();
                for (final Quest attachedQuest : getAllQuestsAttachedToNPC(npc)) {
                    attachedQuestNames.add(attachedQuest.getQuestName());
                }
                main.getLogManager().info("NPC with the ID: <highlight>" + npc.getId() + "</highlight> is not bugged, because it has the following quests attached: <highlight>" + attachedQuestNames + "</highlight>");

            }
            traitsToRemove.clear();

        }
        if (buggedNPCsFound == 0) {
            main.getLogManager().info("No bugged NPCs found! Amount of checked NPCs: <highlight>" + allNPCsFound + "</highlight>");

        } else {
            main.getLogManager().info("<YELLOW><highlight>" + buggedNPCsFound + "</highlight> bugged NPCs have been found and removed! Amount of checked NPCs: <highlight>" + allNPCsFound + "</highlight>");

        }
    }




    public void sendCompletedObjectivesAndProgress(final Player player, final ActiveQuest activeQuest) {
        for (ActiveObjective activeObjective : activeQuest.getCompletedObjectives()) {

            final String objectiveDescription = activeObjective.getObjective().getObjectiveDescription();


            player.sendMessage(main.parse(
                    "<strikethrough><GRAY>" + activeObjective.getObjective().getObjectiveID() + ". " + activeObjective.getObjective().getObjectiveFinalName() + ":" + "</strikethrough>"
            ));

            player.sendMessage(main.parse(
                    "    <strikethrough><GRAY>Description: <WHITE>" + objectiveDescription + "</strikethrough>"
            ));

            player.sendMessage(main.parse(
                    getObjectiveTaskDescription(activeObjective.getObjective(), true, player)
            ));
            player.sendMessage(main.parse(
                    "   <strikethrough><GRAY>Progress: <WHITE>" + activeObjective.getCurrentProgress() + " / " + activeObjective.getProgressNeeded() + "</strikethrough>"
            ));
        }
    }


    public final String getObjectiveTaskDescription(final Objective objective, boolean completed, final Player player) {
        String toReturn = "";

        toReturn += objective.getObjectiveTaskDescription(player);

        if (objective.getCompletionNPCID() != -1) {
            if (main.getIntegrationsManager().isCitizensEnabled()) {
                final NPC npc = CitizensAPI.getNPCRegistry().getById(objective.getCompletionNPCID());
                if (npc != null) {
                    toReturn += "\n    <GRAY>To complete: Talk to <highlight>"+ npc.getName();
                } else {
                    toReturn += "\n    <GRAY>To complete: Talk to NPC with ID <highlight>" + objective.getCompletionNPCID() + " <RED>[Currently not available]";
                }
            } else {
                toReturn += "    <RED>Error: Citizens plugin not installed. Contact an admin.";
            }
        }
        if (objective.getCompletionArmorStandUUID() != null) {
            toReturn += "\n    <GRAY>To complete: Talk to <highlight>" + main.getArmorStandManager().getArmorStandName(objective.getCompletionArmorStandUUID());
        }
        if(completed){
            return "<strikethrough>" + toReturn + "</strikethrough>";
        }else{
            return toReturn;
        }
    }

    public void sendActiveObjectivesAndProgress(final Player player, final ActiveQuest activeQuest) {
        for (ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
            sendActiveObjective(player, activeObjective);
        }
    }

    public void sendObjectives(final Player player, final Quest quest) {
        for (final Objective objective : quest.getObjectives()) {
            final String objectiveDescription = objective.getObjectiveDescription();
            player.sendMessage(main.parse(
                    "<GREEN>" + objective.getObjectiveID() + ". <YELLOW>" + objective.getObjectiveFinalName()
            ));


            if (!objectiveDescription.isBlank()) {
                player.sendMessage(main.parse(
                        "   <BLUE>Description: <GOLD>" + objectiveDescription
                ));
            }
            player.sendMessage(main.parse(
                    getObjectiveTaskDescription(objective, false, player)
            ));


        }
    }


    public void sendObjectivesAdmin(final CommandSender sender, final Quest quest) {

        for (final Objective objective : quest.getObjectives()) {

            final String objectiveDescription = objective.getObjectiveDescription();
            sender.sendMessage(main.parse(
                    "<highlight>" + objective.getObjectiveID() + ".</highlight> <main>" + objective.getObjectiveFinalName()
            ));


            if (!objectiveDescription.isBlank()) {
                sender.sendMessage(main.parse(
                        "   <highlight>Description:</highlight> <main>" + objectiveDescription
                ));
            }


            sender.sendMessage(main.parse(
                    "   <highlight>Conditions:"
            ));
            for (final Condition condition : objective.getConditions()) {
                if(sender instanceof Player player){
                    sender.sendMessage(main.parse(
                            "         <highlight>" + condition.getConditionID() + ".</highlight> <main>Condition:</main> <highlight2>" + condition.getConditionDescription(player)
                    ));
                }else{
                    sender.sendMessage(main.parse(
                            "         <highlight>" + condition.getConditionID() + ".</highlight> <main>Condition:</main> <highlight2>" + condition.getConditionDescription(null)
                    ));
                }

            }
            if (objective.getConditions().size() == 0) {
                sender.sendMessage(main.parse(
                        "      <unimportant>No conditions found!"
                ));
            }

            sender.sendMessage(main.parse(getObjectiveTaskDescription(objective, false, null)));

        }
    }


    public void sendActiveObjective(final Player player, ActiveObjective activeObjective) {
        if (activeObjective.isUnlocked()) {
            final String objectiveDescription = activeObjective.getObjective().getObjectiveDescription();
            player.sendMessage(main.parse(
                    main.getLanguageManager().getString("chat.objectives.counter", player, activeObjective.getActiveQuest(), activeObjective)
            ));

            if (!objectiveDescription.isBlank()) {
                player.sendMessage(main.parse(
                        main.getLanguageManager().getString("chat.objectives.description", player, activeObjective.getActiveQuest(), activeObjective)
                                .replace("%OBJECTIVEDESCRIPTION%", activeObjective.getObjective().getObjectiveDescription())
                ));
            }

            player.sendMessage(main.parse(
                    getObjectiveTaskDescription(activeObjective.getObjective(), false, player)
            ));

            player.sendMessage(main.parse(
                    main.getLanguageManager().getString("chat.objectives.progress", player, activeObjective.getActiveQuest(), activeObjective)
            ));
        } else {
            player.sendMessage(main.parse(
                    main.getLanguageManager().getString("chat.objectives.hidden", player, activeObjective, activeObjective)
            ));
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
        if (main.getIntegrationsManager().isCitizensEnabled()) {
            for (NPC npc : getAllNPCsAttachedToQuest(quest)) {
                if (npc == null || npc.getEntity() == null) {
                    main.getLogManager().warn("A quest has an invalid npc attached to it, which should be removed. Report it to an admin. Quest name: <highlight>" + quest.getQuestName() + "</highlight>");
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

    public final ArrayList<Quest> getAllQuestsWithVisibilityEvaluations(final QuestPlayer questPlayer) {
        return getQuestsFromListWithVisibilityEvaluations(questPlayer, getAllQuests());
    }

    public final ArrayList<Quest> getQuestsFromListWithVisibilityEvaluations(final QuestPlayer questPlayer, final ArrayList<Quest> questsList) {
        final ArrayList<Quest> evaluatedQuests = new ArrayList<>();
        questLoop: for(final Quest quest : questsList){
            if(main.getConfiguration().isQuestVisibilityEvaluationAlreadyAccepted() && questPlayer != null){
                for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                    if (activeQuest.getQuest().equals(quest)) {
                        continue questLoop;
                    }
                }
            }


            if(main.getConfiguration().isQuestVisibilityEvaluationMaxAccepts() || main.getConfiguration().isQuestVisibilityEvaluationAcceptCooldown()){
                int completedAmount = 0;

                long mostRecentAcceptTime = 0;
                if(questPlayer != null){
                    for (CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
                        if (completedQuest.getQuest().equals(quest)) {
                            completedAmount += 1;
                            if (completedQuest.getTimeCompleted() > mostRecentAcceptTime) {
                                mostRecentAcceptTime = completedQuest.getTimeCompleted();
                            }
                        }
                    }
                }


                if(main.getConfiguration().isQuestVisibilityEvaluationMaxAccepts()) {
                    if (quest.getMaxAccepts() > -1 && completedAmount >= quest.getMaxAccepts()) {
                        continue questLoop;
                    }
                }

                if(main.getConfiguration().isQuestVisibilityEvaluationAcceptCooldown()) {
                    final long acceptTimeDifference = System.currentTimeMillis() - mostRecentAcceptTime;
                    final long acceptTimeDifferenceMinutes = TimeUnit.MILLISECONDS.toMinutes(acceptTimeDifference);
                    if (acceptTimeDifferenceMinutes < quest.getAcceptCooldown()) {
                        continue questLoop;
                    }
                }
            }

            if(main.getConfiguration().isQuestVisibilityEvaluationConditions()){
                for (final Condition condition : quest.getRequirements()) {
                    if (!condition.check(questPlayer).isBlank()) {
                        continue questLoop;
                    }
                }
            }


            evaluatedQuests.add(quest);

        }
        return evaluatedQuests;
    }
}
