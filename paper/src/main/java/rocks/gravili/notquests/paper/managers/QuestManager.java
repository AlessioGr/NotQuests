/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
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

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.structs.*;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.actions.BooleanAction;
import rocks.gravili.notquests.paper.structs.actions.ItemStackListAction;
import rocks.gravili.notquests.paper.structs.actions.ListAction;
import rocks.gravili.notquests.paper.structs.actions.NumberAction;
import rocks.gravili.notquests.paper.structs.actions.StringAction;
import rocks.gravili.notquests.paper.structs.conditions.BooleanCondition;
import rocks.gravili.notquests.paper.structs.conditions.Condition;
import rocks.gravili.notquests.paper.structs.conditions.ItemStackListCondition;
import rocks.gravili.notquests.paper.structs.conditions.ListCondition;
import rocks.gravili.notquests.paper.structs.conditions.NumberCondition;
import rocks.gravili.notquests.paper.structs.conditions.StringCondition;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.ObjectiveHolder;
import rocks.gravili.notquests.paper.structs.objectives.ObjectiveObjective;
import rocks.gravili.notquests.paper.structs.triggers.Trigger;


public class QuestManager {

    private final NotQuests main;

    private final ArrayList<Quest> quests;

    private final ArrayList<UUID> debugEnabledPlayers;


    private void loadObjectiveConditionsAndRewards(ObjectiveHolder objectiveHolder, final Category category) {
        main.getLogManager().debug("Loading objective conditions and rewards...");
        for (final Objective objective : objectiveHolder.getObjectives()) { //TODO: Add objective name to error or debug messages to discern from normal requirement loading
            final ConfigurationSection objectiveConditionsUnlockConfigurationSection = objectiveHolder.getConfig().getConfigurationSection(objectiveHolder.getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".conditions.");
            final ConfigurationSection objectiveConditionsProgressConfigurationSection = objectiveHolder.getConfig().getConfigurationSection(objectiveHolder.getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".conditionsProgress.");
            final ConfigurationSection objectiveConditionsCompleteConfigurationSection = objectiveHolder.getConfig().getConfigurationSection(objectiveHolder.getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".conditionsComplete.");

            if (objectiveConditionsUnlockConfigurationSection != null) {
                for (final String objectiveConditionNumber : objectiveConditionsUnlockConfigurationSection.getKeys(false)) {
                    final int conditionID;
                    try {
                        conditionID = Integer.parseInt(objectiveConditionNumber);
                    } catch (java.lang.NumberFormatException ex) {
                        main.getDataManager().disablePluginAndSaving("Error parsing loaded unlock condition ID <highlight>" + objectiveConditionNumber + "</highlight>.", objectiveHolder, objective, category, ex);
                        return;
                    }

                    if (conditionID <= 0) {
                        main.getDataManager().disablePluginAndSaving("Error loading unlock condition with the ID <highlight>" + objectiveConditionNumber + "</highlight>. The condition ID needs to be bigger than 0. However, it's currently <highlight2>" + conditionID, objectiveHolder, category, objective);
                        return;
                    }

                    final Condition condition;

                    try{
                        condition = Condition.loadConditionFromConfig(
                            main,
                            objectiveHolder.getInitialConfigPath()+ ".objectives." + (objective.getObjectiveID()) + ".conditions." + objectiveConditionNumber,
                            objectiveHolder.getConfig(),
                            category,
                            null,
                            conditionID,
                            objectiveHolder,
                            objective);
                    }catch (final Exception e){
                        main.getDataManager().disablePluginAndSaving("Error creating unlock condition with ID <highlight>" + objectiveConditionNumber + "</highlight>. Error: " + e.getMessage(), objectiveHolder, objective, category, e);
                        return;
                    }

                    objective.addUnlockCondition(condition, false);

                }
            }
            if (objectiveConditionsProgressConfigurationSection != null) {
                for (final String objectiveConditionNumber : objectiveConditionsProgressConfigurationSection.getKeys(false)) {
                    final int conditionID;
                    try {
                        conditionID = Integer.parseInt(objectiveConditionNumber);
                    } catch (java.lang.NumberFormatException ex) {
                        main.getDataManager().disablePluginAndSaving("Error parsing loaded progress condition ID <highlight>" + objectiveConditionNumber + "</highlight>.", objectiveHolder, objective, category, ex);
                        return;
                    }

                    if (conditionID <= 0) {
                        main.getDataManager().disablePluginAndSaving("Error loading progress condition with the ID <highlight>" + objectiveConditionNumber + "</highlight>. The condition ID needs to be bigger than 0. However, it's currently <highlight2>" + conditionID, objectiveHolder, category, objective);
                        return;
                    }

                    final Condition condition;

                    try{
                        condition = Condition.loadConditionFromConfig(
                            main,
                            objectiveHolder.getInitialConfigPath()+ ".objectives." + (objective.getObjectiveID()) + ".conditionsProgress." + objectiveConditionNumber,
                            objectiveHolder.getConfig(),
                            category,
                            null,
                            conditionID,
                            objectiveHolder,
                            objective);
                    }catch (final Exception e){
                        main.getDataManager().disablePluginAndSaving("Error creating progress condition with ID <highlight>" + objectiveConditionNumber + "</highlight>. Error: " + e.getMessage(), objectiveHolder, objective, category, e);
                        return;
                    }

                    objective.addProgressCondition(condition, false);

                }
            }
            if (objectiveConditionsCompleteConfigurationSection != null) {
                for (final String objectiveConditionNumber : objectiveConditionsCompleteConfigurationSection.getKeys(false)) {
                    final int conditionID;
                    try {
                        conditionID = Integer.parseInt(objectiveConditionNumber);
                    } catch (java.lang.NumberFormatException ex) {
                        main.getDataManager().disablePluginAndSaving("Error parsing loaded complete condition ID <highlight>" + objectiveConditionNumber + "</highlight>.", objectiveHolder, objective, category, ex);
                        return;
                    }

                    if (conditionID <= 0) {
                        main.getDataManager().disablePluginAndSaving("Error loading complete condition with the ID <highlight>" + objectiveConditionNumber + "</highlight>. The condition ID needs to be bigger than 0. However, it's currently <highlight2>" + conditionID, objectiveHolder, objective, category);
                        return;
                    }

                    final Condition condition;

                    try{
                        condition = Condition.loadConditionFromConfig(
                            main,
                            objectiveHolder.getInitialConfigPath() + ".objectives." + (objective.getObjectiveID()) + ".conditionsComplete." + objectiveConditionNumber,
                            objectiveHolder.getConfig(),
                            category,
                            null,
                            conditionID,
                            objectiveHolder,
                            objective);
                    }catch (final Exception e){
                        main.getDataManager().disablePluginAndSaving("Error creating complete condition with ID <highlight>" + objectiveConditionNumber + "</highlight>. Error: " + e.getMessage(), objectiveHolder, objective, e, category);
                        return;
                    }

                    objective.addCompleteCondition(condition, false);

                }
            }

            final String initialObjectiveRewardsPath = objectiveHolder.getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".rewards.";
            final ConfigurationSection objectiveRewardsConfigurationSection = objectiveHolder.getConfig().getConfigurationSection(initialObjectiveRewardsPath);
            if (objectiveRewardsConfigurationSection != null) {
                for (final String objectiveRewardNumber : objectiveRewardsConfigurationSection.getKeys(false)) {
                    final int rewardID;
                    try {
                        rewardID = Integer.parseInt(objectiveRewardNumber);
                    } catch (java.lang.NumberFormatException ex) {
                        main.getDataManager().disablePluginAndSaving("Error parsing loaded objective reward ID <highlight>" + objectiveRewardNumber + "</highlight>.", objectiveHolder, objective, ex, category);
                        return;
                    }

                    if (rewardID <= 0) {
                        main.getDataManager().disablePluginAndSaving("Error loading Objective reward <highlight>" + objectiveRewardNumber + "</highlight>. The reward ID needs to be bigger than 0. However, it's currently <highlight2>" + rewardID, objectiveHolder, objective, category);
                        return;
                    }

                    final Action action;

                    try{
                        action = Action.loadActionFromConfig(
                            main,
                            initialObjectiveRewardsPath + rewardID,
                            objectiveHolder.getConfig(),
                            category,
                            null,
                            rewardID,
                            objectiveHolder,
                            objective);
                    }catch (final Exception e){
                        main.getDataManager().disablePluginAndSaving("Error parsing objective reward action with ID <highlight>" + objectiveRewardNumber + "</highlight>. Error: " + e.getMessage(), objectiveHolder, objective, category, e);
                        return;
                    }
                    objective.addReward(action, false);

                }
            }
            loadObjectiveConditionsAndRewards(objective, category);
        }
    }


    private void loadObjectives(final ObjectiveHolder objectiveHolder, final String initialPath /* quests." + questName + ".objectives */, final Category category) {
        final ConfigurationSection objectivesConfigurationSection = objectiveHolder.getConfig().getConfigurationSection(initialPath);
        if(objectivesConfigurationSection == null) {
            return;
        }
        for (final String objectiveNumber : objectivesConfigurationSection.getKeys(false)) {
            final ConfigurationSection config = objectivesConfigurationSection.getConfigurationSection(objectiveNumber);

            if(config == null){
                continue;
            }

            String objectiveTypeString = config.getString( "objectiveType", "");
            //Migrate from 5.8.3 => 5.8.4
            if(objectiveTypeString.equals("CollectItems")){
                main.getLogManager().info("Migrating old CollectItems objectives to PickupItems objectives...");
                objectiveTypeString = "PickupItems";
                config.set("objectiveType", "PickupItems");
                objectiveHolder.saveConfig();
            }

            final Class<? extends Objective> objectiveType = main.getObjectiveManager().getObjectiveClass(objectiveTypeString);

            if (objectiveType == null) {
                main.getDataManager().disablePluginAndSaving("Error parsing objective Type of objective with ID <highlight>" + objectiveNumber + "</highlight>. Objective type: <highlight2>" + objectiveTypeString + ". Objective class lookup returned null. This would mean, that the objective type you're trying to use in notquests is not registered.", objectiveHolder, category);
                return;
            }

            final String progressNeededExpression;
            //Convert old progressNeeded to progressNeededExpression
            if(config.contains("progressNeeded")){
                progressNeededExpression = ""+config.getInt("progressNeeded", 1);
                config.set("progressNeeded", null);
                config.set("progressNeededExpression", progressNeededExpression);
                objectiveHolder.saveConfig();
            }else {
                progressNeededExpression = config.getString("progressNeededExpression", "1");
            }

            main.getLogManager().debug("ProgressNeededExpression: " + progressNeededExpression);

            final Location location = config.getLocation("location", null);
            final boolean showLocation = config.getBoolean("showLocation", false);

            final int objectiveID;
            try {
                objectiveID = Integer.parseInt(objectiveNumber);
            } catch (NumberFormatException ex) {
                main.getDataManager().disablePluginAndSaving("Error parsing loaded objective ID <highlight>" + objectiveNumber + "</highlight>.", objectiveHolder, category, ex);
                return;
            }

            if (objectiveID <= 0) {
                main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an error while loading quests objective data (2). Objective ID: <highlight>" + objectiveNumber + "</highlight> Reason: Invalid objective ID - it needs to be bigger than 0: " + objectiveID, category, objectiveHolder);
                return;
            }

            final Objective objective;

            try {
                objective = objectiveType.getDeclaredConstructor(NotQuests.class).newInstance(main);
                objective.setObjectiveHolder(objectiveHolder);
                objective.setObjectiveID(objectiveID);
                objective.setProgressNeededExpression(progressNeededExpression);
                objective.setLocation(location, false);
                objective.setShowLocation(showLocation, false);

                objective.load(objectiveHolder.getConfig(), initialPath + "." + objectiveNumber);

            } catch (Exception ex) {
                main.getDataManager().disablePluginAndSaving("Error parsing objective of objective with ID <highlight>" + objectiveNumber + "</highlight>. Objective type: <highlight2>" + objectiveTypeString + "</highlight2>. Reason: an error occurred during objective loading,", objectiveHolder, category, ex);
                return;
            }


            final String objectiveDisplayName = config.getString("displayName", "");
            final String objectiveDescription = config.getString("description", "");
            final String objectiveTaskDescription = config.getString("taskDescription", "");
            final NQNPC completionNPC = NQNPC.fromConfig(main, objectiveHolder.getConfig(), initialPath + "."+ objectiveNumber + ".completionNPC");


            objective.setDescription(objectiveDescription.replace("\\n", "\n"), false);
            objective.setTaskDescription(objectiveTaskDescription.replace("\\n", "\n"), false);
            objective.setDisplayName(objectiveDisplayName.replace("\\n", "\n"), false);


            objective.setCompletionNPC(completionNPC, false);
            objectiveHolder.addObjective(objective, false);

            //Sub-Objectives
            if(config.contains("objectives")){
                loadObjectives(objective, initialPath + "." + objectiveNumber + ".objectives", category);
            }
        }
    }




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

    public final String createQuest(final String questName) {
        if (getQuest(questName) == null) {
            if(questName.contains("°")){
                return ("<error>The symbol <highlight>°</highlight> cannot be used, because it's used for some important, plugin-internal stuff.");
            }
            final Quest newQuest = new Quest(main, questName);
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
            if (quest.getIdentifier().equalsIgnoreCase(questName)) {
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
        main.getLogManager().info("Scheduled Quest Data load for following categories: <highlight>" + categoriesStringList);

        quests.clear();
        for (final Category category : main.getDataManager().getCategories()) {
            loadQuestsFromConfig(category);
        }

    }

    public void loadQuestsFromConfig(final Category category) {
        try {
            main.getLogManager().info("Loading Quests data from <highlight>" + category.getCategoryName() + "</highlight> category...");

            if(category.getQuestsConfig() == null){
                main.getLogManager().severe("Error: Cannot load quests of category <highlight>" + category.getCategoryFullName() + "</highlight>, because it doesn't have a quests config. This category has been skipped.");
                return;
            }

            //Quests
            final ConfigurationSection questsConfigurationSection = category.getQuestsConfig().getConfigurationSection("quests");


            if (questsConfigurationSection != null) {
                for (final String questName : questsConfigurationSection.getKeys(false)) {
                    if (main.getConfiguration().isVerboseStartupMessages()) {
                        main.getLogManager().info("   Loading Quest <highlight>" + questName + "</highlight>...");
                    }

                    final Quest quest = new Quest(main, questName, category);
                    if(category.getQuestsConfig().isInt("quests." + questName + ".maxAccepts")){ // Convert
                        final int oldMaxAccepts = category.getQuestsConfig().getInt("quests." + questName + ".maxAccepts", -1);
                        category.getQuestsConfig().set("quests." + questName + ".maxAccepts", null);
                        category.getQuestsConfig().set("quests." + questName + ".limits.completions", oldMaxAccepts);
                        category.saveQuestsConfig();
                    }
                    quest.setMaxCompletions(category.getQuestsConfig().getInt("quests." + questName + ".limits.completions", -1), false);
                    quest.setMaxAccepts(category.getQuestsConfig().getInt("quests." + questName + ".limits.accepts", -1), false);
                    quest.setMaxFails(category.getQuestsConfig().getInt("quests." + questName + ".limits.fails", -1), false);
                    quest.setTakeEnabled(category.getQuestsConfig().getBoolean("quests." + questName + ".takeEnabled", true), false);
                    quest.setAbortEnabled(category.getQuestsConfig().getBoolean("quests." + questName + ".abortEnabled", true), false);
                    if(category.getQuestsConfig().isInt("quests." + questName + ".acceptCooldown")){ // Convert
                        final int oldCooldown = category.getQuestsConfig().getInt("quests." + questName + ".acceptCooldown", -1);
                        category.getQuestsConfig().set("quests." + questName + ".acceptCooldown", null);
                        category.getQuestsConfig().set("quests." + questName + ".acceptCooldown.complete", oldCooldown);
                        category.saveQuestsConfig();
                    }
                    quest.setAcceptCooldownComplete(category.getQuestsConfig().getLong("quests." + questName + ".acceptCooldown.complete", -1), false);

                    quest.setPredefinedProgressOrder(PredefinedProgressOrder.fromConfiguration(category.getQuestsConfig(), "quests." + questName + ".predefinedProgressOrder"), false);


                    quest.setQuestDescription(
                            category.getQuestsConfig().getString("quests." + questName + ".description", "")
                                    .replace("\\n", "\n"), false
                    );
                    quest.setQuestDisplayName(
                            category.getQuestsConfig().getString("quests." + questName + ".displayName", "")
                                    .replace("\\n", "\n"), false
                    );

                    //Objectives:
                    loadObjectives(quest, "quests." + questName + ".objectives", category);


                    //Quest Requirements:
                    final ConfigurationSection requirementsConfigurationSection = category.getQuestsConfig().getConfigurationSection("quests." + questName + ".requirements");
                    if (requirementsConfigurationSection != null) {
                        for (final String requirementNumber : requirementsConfigurationSection.getKeys(false)) {

                            final int requirementID;
                            try {
                                requirementID = Integer.parseInt(requirementNumber);
                            } catch (java.lang.NumberFormatException ex) {
                                main.getDataManager().disablePluginAndSaving("Error parsing loaded requirement ID <highlight>" + requirementNumber + "</highlight>.", quest, category, ex);
                                return;
                            }

                            if (requirementID <= 0) {
                                main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an error while loading quests requirement data. The requirement ID needs to be bigger than 0, but it's currently <highlight>" + requirementID, quest, category);
                                return;
                            }

                            final Condition condition;

                            try{
                                condition = Condition.loadConditionFromConfig(
                                    main,
                                    "quests." + questName + ".requirements." + requirementNumber,
                                    category.getQuestsConfig(),
                                    category,
                                    null,
                                    requirementID,
                                    quest,
                                    null);
                            }catch (final Exception e){
                                main.getDataManager().disablePluginAndSaving("Error parsing Quest condition with ID <highlight>" + requirementNumber + "</highlight>. Error: " + e.getMessage(), quest, category, e);
                                return;
                            }
                            quest.addRequirement(condition, false);

                        }
                    }


                    //Rewards for Quests:
                    final ConfigurationSection rewardsConfigurationSection = category.getQuestsConfig().getConfigurationSection("quests." + questName + ".rewards");
                    if (rewardsConfigurationSection != null) {
                        for (final String rewardNumber : rewardsConfigurationSection.getKeys(false)) {

                            final int rewardID;
                            try {
                                rewardID = Integer.parseInt(rewardNumber);
                            } catch (java.lang.NumberFormatException ex) {
                                main.getDataManager().disablePluginAndSaving("Error parsing loaded reward ID <highlight>" + rewardNumber + "</highlight>.", quest, category, ex);
                                return;
                            }
                            if (rewardID <= 0) {
                                main.getDataManager().disablePluginAndSaving("Error loading Quest reward <highlight>" + rewardNumber + "</highlight>. The reward ID needs to be bigger than 0, but it's currently <highlight2>" + rewardID, quest, category);
                                return;
                            }

                            final Action action;

                            try{
                                action = Action.loadActionFromConfig(
                                    main,
                                    "quests." + questName + ".rewards." + rewardID,
                                    category.getQuestsConfig(),
                                    category,
                                    null,
                                    rewardID,
                                    quest,
                                    null);
                            }catch (final Exception e){
                                main.getDataManager().disablePluginAndSaving("Error parsing action with ID <highlight>" + rewardNumber + "</highlight>. Error: " + e.getMessage(), quest, category, e);
                                return;
                            }


                            quest.addReward(action, false);


                        }
                    }


                    //Triggers:
                    final ConfigurationSection triggersConfigurationSection = category.getQuestsConfig().getConfigurationSection("quests." + questName + ".triggers");
                    if (triggersConfigurationSection != null) {
                        for (final String triggerNumber : triggersConfigurationSection.getKeys(false)) {


                            final int triggerID;
                            try {
                                triggerID = Integer.parseInt(triggerNumber);
                            } catch (java.lang.NumberFormatException ex) {
                                main.getDataManager().disablePluginAndSaving("Error parsing loaded trigger ID ID <highlight>" + triggerNumber + "</highlight>.", quest, ex, category);
                                return;
                            }

                            if (triggerID <= 0) {
                                main.getDataManager().disablePluginAndSaving("Error loading trigger with the triggerNumber <highlight>" + triggerNumber + " </highlight>: The trigger ID needs to be bigger than 0. However, it's currently <highlight2>" + triggerID, quest, category);
                                return;
                            }

                            final String triggerTypeString = category.getQuestsConfig().getString("quests." + questName + ".triggers." + triggerNumber + ".triggerType", "");

                            final Class<? extends Trigger> triggerType = main.getTriggerManager().getTriggerClass(triggerTypeString);

                            if (triggerType == null) {
                                main.getDataManager().disablePluginAndSaving("Error parsing trigger Type of trigger with ID <highlight>" + triggerNumber + "</highlight>. Trigger type: <highlight2>" + triggerTypeString, quest, category);
                                return;
                            }


                            final String triggerActionName = category.getQuestsConfig().getString("quests." + questName + ".triggers." + triggerNumber + ".triggerActionName", "");
                            final long amountNeeded = category.getQuestsConfig().getLong("quests." + questName + ".triggers." + triggerNumber + ".amountNeeded", 1);

                            final int applyOn = category.getQuestsConfig().getInt("quests." + questName + ".triggers." + triggerNumber + ".applyOn");
                            final String worldName = category.getQuestsConfig().getString("quests." + questName + ".triggers." + triggerNumber + ".worldName", "ALL");

                            if (!worldName.equalsIgnoreCase("ALL") && Bukkit.getWorld(worldName) == null) {
                                main.getLogManager().warn("The world of the trigger <highlight>" + triggerNumber + "</highlight> of Quest <highlight>" + questName + "</highlight> was not found. World name: <highlight>" + worldName + "</highlight>");
                            }


                            final Action foundAction = main.getActionsYMLManager().getAction(triggerActionName);

                            if (foundAction == null) {
                                main.getDataManager().disablePluginAndSaving("ERROR when loading trigger with the triggerNumber <highlight>" + triggerNumber + " </highlight>: The action <highlight2>" + triggerActionName + "</highlight2> was not found!", quest, category);
                                return;
                            }

                            final Trigger trigger;

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
                                main.getDataManager().disablePluginAndSaving("Error creating the trigger with ID <highlight>" + triggerNumber + "</highlight>.", quest, category, ex);
                                return;
                            }
                            quest.addTrigger(trigger, false);


                        }
                    }


                    //Convert old dependencies
                    //main.getUpdateManager().convertObjectiveDependenciesToNewObjectiveConditions(quest);


                    //Objective Conditions and Rewards
                    loadObjectiveConditionsAndRewards(quest, category);


                    //TakeItem:
                    quest.setTakeItem(category.getQuestsConfig().getItemStack("quests." + questName + ".takeItem"), false);

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
            final PersistentDataContainer armorstandPDB = armorstand.getPersistentDataContainer();
            final NamespacedKey attachedShowingQuestsKey = new NamespacedKey(main.getMain(), "notquests-attachedQuests-showing");
            final NamespacedKey attachedNonShowingQuestsKey = new NamespacedKey(main.getMain(), "notquests-attachedQuests-nonshowing");

            //Showing
            if (armorstandPDB.has(attachedShowingQuestsKey, PersistentDataType.STRING)) {
                final String existingAttachedQuests = armorstandPDB.get(attachedShowingQuestsKey, PersistentDataType.STRING);
                if (existingAttachedQuests != null && existingAttachedQuests.length() >= 1) {
                    for (final String split : existingAttachedQuests.split("°")) {
                        final Quest foundQuest = getQuest(split);
                        if (foundQuest != null) {
                            add(foundQuest);
                        }
                    }
                }
            }

            //Nonshowing
            if (armorstandPDB.has(attachedNonShowingQuestsKey, PersistentDataType.STRING)) {
                final String existingAttachedQuests = armorstandPDB.get(attachedNonShowingQuestsKey, PersistentDataType.STRING);
                if (existingAttachedQuests != null && existingAttachedQuests.length() >= 1) {
                    for (final String split : existingAttachedQuests.split("°")) {
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
            final PersistentDataContainer armorstandPDB = armorstand.getPersistentDataContainer();
            final NamespacedKey attachedQuestsKey = new NamespacedKey(main.getMain(), "notquests-attachedQuests-showing");

            if (armorstandPDB.has(attachedQuestsKey, PersistentDataType.STRING)) {
                final String existingAttachedQuests = armorstandPDB.get(attachedQuestsKey, PersistentDataType.STRING);
                if (existingAttachedQuests != null && existingAttachedQuests.length() >= 1) {
                    for (final String split : existingAttachedQuests.split("°")) {
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

    public final ArrayList<Quest> getAllQuestsAttachedToNPC(final NQNPC npc) {
        final ArrayList<Quest> questsAttached = new ArrayList<>();
        for (final Quest quest : quests) {
            main.getLogManager().debug("getAttachedNPCsWithQuestShowing size: " + quest.getAttachedNPCsWithoutQuestShowing().size());
            main.getLogManager().debug("getAttachedNPCsWithQuestShowing: " + quest.getAttachedNPCsWithoutQuestShowing());
            main.getLogManager().debug("npc: " + npc);

            if (quest.getAttachedNPCsWithQuestShowing().contains(npc) || quest.getAttachedNPCsWithoutQuestShowing().contains(npc)) {
                questsAttached.add(quest);
            }
        }
        // System.out.println("§esize: " + questsattached.size());
        return questsAttached;
    }

    public final ArrayList<Quest> getQuestsAttachedToNPCWithShowing(final NQNPC npc) {
        final ArrayList<Quest> questsAttached = new ArrayList<>();
        for (final Quest quest : quests) {
            if (quest.getAttachedNPCsWithQuestShowing().contains(npc)) {
                questsAttached.add(quest);
            }
        }
        // System.out.println("§esize: " + questsattached.size());
        return questsAttached;
    }


    public final ArrayList<Quest> getQuestsAttachedToNPCWithoutShowing(final NQNPC npc) {
        final ArrayList<Quest> questsattached = new ArrayList<>();
        for (Quest quest : quests) {
            if (quest.getAttachedNPCsWithoutQuestShowing().contains(npc)) {
                questsattached.add(quest);
            }
        }
        // System.out.println("§esize: " + questsattached.size());
        return questsattached;
    }

    public final CopyOnWriteArrayList<NQNPC> getAllNPCsAttachedToQuest(final Quest quest) {
        final CopyOnWriteArrayList<NQNPC> npcsAttached = new CopyOnWriteArrayList<>();
        npcsAttached.addAll(quest.getAttachedNPCsWithQuestShowing());
        npcsAttached.addAll(quest.getAttachedNPCsWithoutQuestShowing());
        return npcsAttached;
    }

    public final CopyOnWriteArrayList<NQNPC> getNPCsAttachedToQuestWithShowing(final Quest quest) {
        return quest.getAttachedNPCsWithQuestShowing();
    }

    public final CopyOnWriteArrayList<NQNPC> getNPCsAttachedToQuestWithoutShowing(final Quest quest) {
        return quest.getAttachedNPCsWithoutQuestShowing();
    }


    public boolean sendQuestsPreviewOfQuestShownArmorstands(ArmorStand armorStand, QuestPlayer questPlayer) {
        final ArrayList<Quest> questsAttachedToNPC = getQuestsAttachedToArmorstandWithShowing(armorStand);

        //No quests attached or all quests are set to not showing (more likely). THen nothing should show. That should make it work with Interactions plugin and takeEnabled = false.
        if (questsAttachedToNPC.size() == 0) {
            return false;
        }
        final Player player = questPlayer.getPlayer();

        if (main.getConfiguration().isQuestPreviewUseGUI()) {

            main.getGuiManager().showTakeQuestsGUI(questPlayer, questsAttachedToNPC);
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
                            player.chat("/notquests preview " + quest.getIdentifier() );
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
            main.getLogManager().info("All quest count: <highlight>" + quests.size() + "</highlight>");

            player.sendMessage(Component.empty());
            player.sendMessage(main.parse(
                    "<BLUE>" + questsAttachedToNPC.size() + " Available Quests:"
            ));
            int counter = 1;

            for (Quest quest : questsAttachedToNPC) {

                Component acceptComponent = main.parse("<GREEN>[CHOOSE]")
                        .clickEvent(ClickEvent.runCommand("/nquests preview " + quest.getIdentifier()))
                        .hoverEvent(HoverEvent.showText(main.parse("<GREEN>Click to preview/choose the quest <highlight>" + quest.getDisplayNameOrIdentifier())));

                Component component = main.parse("<YELLOW>" + counter + ". <highlight>" + quest.getDisplayNameOrIdentifier() + " ")
                        .append(acceptComponent);

                player.sendMessage(component);


                counter++;
            }
            //getQuestsAttachedToNPC(npc);
        }

        return true;

    }


    public void sendQuestsPreviewOfQuestShownNPCs(final NQNPC npc, final QuestPlayer questPlayer) {
        questPlayer.sendDebugMessage("Sending quests preview...");

        if(npc == null){
            questPlayer.sendDebugMessage("NPC is null");
            return;
        }

        final ArrayList<Quest> questsAttachedToNPC = getQuestsAttachedToNPCWithShowing(npc);

        //No quests attached or all quests are set to not showing (more likely). THen nothing should show. That should make it work with Interactions plugin and takeEnabled = false.
        if (questsAttachedToNPC.isEmpty()) {
            questPlayer.sendDebugMessage("No quests attached to NPC " + npc.getID() + " and name " + npc.getName());
            return;
        }

        final Player player = questPlayer.getPlayer();
        if (main.getConfiguration().isQuestPreviewUseGUI()) {
            main.getGuiManager().showTakeQuestsGUI(questPlayer, questsAttachedToNPC);
        } else {
            main.getLogManager().info("All quest count: <highlight>" + quests.size() + "</highlight>");

            player.sendMessage(Component.empty());
            player.sendMessage(main.parse(
                    "<BLUE>" + questsAttachedToNPC.size() + " Availahle Quests:"
            ));
            int counter = 1;

            for (final Quest quest : questsAttachedToNPC) {

                Component acceptComponent = main.parse("<GREEN>[CHOOSE]")
                        .clickEvent(ClickEvent.runCommand("/nquests preview " + quest.getIdentifier()))
                        .hoverEvent(HoverEvent.showText(main.parse("<GREEN>Click to preview/choose the quest <highlight>" + quest.getDisplayNameOrIdentifier())));

                Component component = main.parse("<YELLOW>" + counter + ". <highlight>" + quest.getDisplayNameOrIdentifier() + " ")
                        .append(acceptComponent);

                player.sendMessage(component);

                counter++;
            }
            //getQuestsAttachedToNPC(npc);
        }

    }

    public final String getQuestRequirements(final Quest quest, final QuestPlayer questPlayer) {
        StringBuilder requirements = new StringBuilder();
        int counter = 1;
        for (final Condition condition : quest.getRequirements()) {
            if(condition.isHidden(questPlayer)){
                continue;
            }

            if (counter != 1) {
                requirements.append("\n");
            }
            requirements.append("<GREEN>").append(counter).append(". <YELLOW>").append(condition.getConditionType()).append("\n");

            requirements.append(condition.getConditionDescription(questPlayer)).append("\n");


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


        if(condition instanceof final NumberCondition numberCondition){
            conditionType = numberCondition.getVariableName();
        }else if(condition instanceof final StringCondition stringCondition){
            conditionType = stringCondition.getVariableName();
        }else if(condition instanceof final BooleanCondition booleanCondition){
            conditionType = booleanCondition.getVariableName();
        }else if(condition instanceof final ListCondition listCondition){
            conditionType = listCondition.getVariableName();
        }else if(condition instanceof final ItemStackListCondition itemStackListCondition){
            conditionType = itemStackListCondition.getVariableName();
        }
        return conditionType;
    }


    //Not for admin
    public final ArrayList<String> getQuestRequirementsList(final Quest quest, final QuestPlayer questPlayer) {
        final ArrayList<String> requirements = new ArrayList<>();
        int counter = 1;
        for (final Condition condition : quest.getRequirements()) {
            if(condition.isHidden(questPlayer)){
                continue;
            }
            requirements.add("<GREEN>" + counter + ". <YELLOW>" + getDisplayConditionType(condition));
            requirements.add(condition.getConditionDescription(questPlayer, quest));

            counter += 1;
        }
        return requirements;
    }

    public final String getQuestRewards(final Quest quest, final QuestPlayer questPlayer) {
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
                    rewards.append("<GREEN>").append(counter).append(main.getLanguageManager().getString("gui.reward-hidden-text", questPlayer, quest, reward)).append("</GREEN>");
                } else {
                    rewards.append("<GREEN>").append(counter).append(". <BLUE>").append(reward.getActionDescription(questPlayer)).append("</GREEN>");
                }

            }
            counter += 1;

        }
        return rewards.toString();
    }

    public final ArrayList<String> getQuestRewardsList(final Quest quest, final QuestPlayer questPlayer) { //TODO: Customizability for colors
        ArrayList<String> rewards = new ArrayList<>();
        int counter = 1;
        for (final Action reward : quest.getRewards()) {
            if (!reward.getActionName().isBlank()) {
                rewards.add("<GREEN>" + counter + ". <BLUE>" + reward.getActionName() + "</GREEN>");
            } else {
                if (main.getConfiguration().hideRewardsWithoutName) {
                    rewards.add("<GREEN>" + counter + main.getLanguageManager().getString("gui.reward-hidden-text", questPlayer, quest, reward) + "</GREEN>");
                } else {
                    rewards.add("<GREEN>" + counter + ". <BLUE>" + reward.getActionDescription(questPlayer) + "</GREEN>");
                }
            }
            counter += 1;

        }
        return rewards;
    }

    public void sendSingleQuestPreview(QuestPlayer questPlayer, Quest quest) {
        final Player player = questPlayer.getPlayer();
        player.sendMessage(Component.empty());
        player.sendMessage(main.parse("<GRAY>-----------------------------------"));
        player.sendMessage(main.parse(
                "<BLUE>Quest Preview for Quest <highlight>" + quest.getDisplayNameOrIdentifier() + "</highlight>:"
        ));


        if (quest.getObjectiveHolderDescription().length() >= 1) {
            player.sendMessage(main.parse(
                    "<YELLOW>Quest description: <GRAY>" + quest.getObjectiveHolderDescription()
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
                getQuestRequirements(quest, questPlayer)
        ));

        player.sendMessage(main.parse(
                "<BLUE>Quest Rewards:"
        ));
        player.sendMessage(main.parse(
                getQuestRewards(quest, questPlayer)
        ));

        Component acceptComponent = main.parse("<GREEN>**[ACCEPT THIS QUEST]")
                .clickEvent(ClickEvent.runCommand("/nquests take " + quest.getIdentifier()))
                .hoverEvent(HoverEvent.showText(main.parse("<GREEN>Click to accept the Quest <highlight>" + quest.getDisplayNameOrIdentifier())));

        player.sendMessage(Component.empty());
        player.sendMessage(acceptComponent);
        player.sendMessage(main.parse(
                "<GRAY>-----------------------------------"
        ));


    }




    public void sendCompletedObjectivesAndProgress(final QuestPlayer questPlayer, final ActiveQuest activeQuest) {
        final Player player = questPlayer.getPlayer();

        for (ActiveObjective activeObjective : activeQuest.getCompletedObjectives()) {

            final String objectiveDescription = activeObjective.getObjective().getObjectiveHolderDescription();


            player.sendMessage(main.parse(
                    "<strikethrough><GRAY>" + activeObjective.getObjective().getObjectiveID() + ". " + activeObjective.getObjective().getDisplayNameOrIdentifier() + ":" + "</strikethrough>"
            ));

            player.sendMessage(main.parse(
                    "    <strikethrough><GRAY>Description: <WHITE>" + objectiveDescription + "</strikethrough>"
            ));

            player.sendMessage(main.parse(
                    getObjectiveTaskDescription(activeObjective.getObjective(), true, questPlayer, activeObjective)
            ));
            player.sendMessage(main.parse(
                    "   <strikethrough><GRAY>Progress: <WHITE>" + activeObjective.getCurrentProgress() + " / " + activeObjective.getProgressNeeded() + "</strikethrough>"
            ));
        }
    }

    public final String getObjectiveTaskDescription(final Objective objective, boolean completed, final QuestPlayer questPlayer) {
        return getObjectiveTaskDescription(objective, completed, questPlayer, null);
    }
    public final String getObjectiveTaskDescription(final Objective objective, boolean completed, final QuestPlayer questPlayer, @Nullable final ActiveObjective activeObjective) {
        String toReturn = "";

        toReturn += objective.getTaskDescription(questPlayer, activeObjective);

        if (objective.getCompletionNPC() != null) {
            final String npcName = objective.getCompletionNPC().getName();
            if(npcName != null){
                toReturn += "\n    <GRAY>To complete: Talk to <highlight>" + npcName;
            }else{
                toReturn += "\n    <GRAY>To complete: Talk to NPC with ID <highlight>" + objective.getCompletionNPC().getID() + " <RED>[Currently not available]";
            }
        }
        if(completed){
            return "<strikethrough>" + toReturn + "</strikethrough>";
        }else{
            return toReturn;
        }
    }


    public void sendActiveObjectivesAndProgress(final QuestPlayer questPlayer, final ActiveObjectiveHolder activeObjectiveHolder, final int level) {
        for (ActiveObjective activeObjective : activeObjectiveHolder.getActiveObjectives()) {
            if(activeObjective.isCompleted(null)){
                continue;
            }
            sendActiveObjective(questPlayer, activeObjective, level);
            if(!activeObjective.getActiveObjectives().isEmpty()){
                sendActiveObjectivesAndProgress(questPlayer, activeObjective, level+1);
            }
        }
    }


    //TODO: Make this work for sub-objectives:
    public void sendObjectivesAdmin(final CommandSender sender, final ObjectiveHolder quest) {

        for (final Objective objective : quest.getObjectives()) {

            final String objectiveDescription = objective.getObjectiveHolderDescription();
            sender.sendMessage(main.parse(
                    "<highlight>" + objective.getObjectiveID() + ".</highlight> <main>" + objective.getDisplayNameOrIdentifier()
            ));


            if (!objectiveDescription.isBlank()) {
                sender.sendMessage(main.parse(
                        "   <highlight>Description:</highlight> <main>" + objectiveDescription
                ));
            }


            {
                sender.sendMessage(main.parse(
                    "   <highlight>Unlock Conditions:"
                ));
                for (final Condition condition : objective.getUnlockConditions()) {
                    if(sender instanceof Player player){
                        sender.sendMessage(main.parse(
                            "         <highlight>" + condition.getConditionID() + ".</highlight> <main>Condition:</main> <highlight2>" + condition.getConditionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))
                        ));
                    }else{
                        sender.sendMessage(main.parse(
                            "         <highlight>" + condition.getConditionID() + ".</highlight> <main>Condition:</main> <highlight2>" + condition.getConditionDescription(null)
                        ));
                    }

                }
                if (objective.getUnlockConditions().size() == 0) {
                    sender.sendMessage(main.parse(
                        "      <unimportant>No unlock conditions found!"
                    ));
                }
            }
            {
                sender.sendMessage(main.parse(
                    "   <highlight>Progress Conditions:"
                ));
                for (final Condition condition : objective.getProgressConditions()) {
                    if(sender instanceof Player player){
                        sender.sendMessage(main.parse(
                            "         <highlight>" + condition.getConditionID() + ".</highlight> <main>Condition:</main> <highlight2>" + condition.getConditionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))
                        ));
                    }else{
                        sender.sendMessage(main.parse(
                            "         <highlight>" + condition.getConditionID() + ".</highlight> <main>Condition:</main> <highlight2>" + condition.getConditionDescription(null)
                        ));
                    }

                }
                if (objective.getProgressConditions().size() == 0) {
                    sender.sendMessage(main.parse(
                        "      <unimportant>No progress conditions found!"
                    ));
                }
            }
            {
                sender.sendMessage(main.parse(
                    "   <highlight>Complete Conditions:"
                ));
                for (final Condition condition : objective.getCompleteConditions()) {
                    if(sender instanceof Player player){
                        sender.sendMessage(main.parse(
                            "         <highlight>" + condition.getConditionID() + ".</highlight> <main>Condition:</main> <highlight2>" + condition.getConditionDescription(main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()))
                        ));
                    }else{
                        sender.sendMessage(main.parse(
                            "         <highlight>" + condition.getConditionID() + ".</highlight> <main>Condition:</main> <highlight2>" + condition.getConditionDescription(null)
                        ));
                    }

                }
                if (objective.getCompleteConditions().size() == 0) {
                    sender.sendMessage(main.parse(
                        "      <unimportant>No complete conditions found!"
                    ));
                }
            }


            sender.sendMessage(main.parse(getObjectiveTaskDescription(objective, false, null)));

        }
    }


    public void sendActiveObjective(final QuestPlayer questPlayer, ActiveObjective activeObjective, final int level) {
        final Player player = questPlayer.getPlayer();

        String prefix = "";
        ActiveObjective lastActiveObjective = activeObjective;

        String counterWithSubId = ""+lastActiveObjective.getObjectiveID();
        final String prefixAppend = main.getLanguageManager().getString("chat.objectives.subObjectivePrefixAppend", questPlayer, activeObjective.getObjectiveHolder(), activeObjective);
        for(int i = 0; i < level; i++){
            prefix += prefixAppend;
            if(lastActiveObjective.getActiveObjectiveHolder() instanceof final ActiveObjective parentActiveObjective){
                lastActiveObjective = parentActiveObjective;
                counterWithSubId = lastActiveObjective.getObjectiveID() + "."+counterWithSubId;
            }
        }

        if (activeObjective.isUnlocked()) {
            if(activeObjective.getObjective() instanceof final ObjectiveObjective objectiveObjective){
                final String objectiveDescription = activeObjective.getObjective().getObjectiveHolderDescription();
                player.sendMessage(main.parse(
                    prefix+main.getLanguageManager().getString("chat.objectives.counterForObjectiveObjective", player, activeObjective.getActiveObjectiveHolder(), activeObjective,
                        Map.of(
                            "%OBJECTIVEIDWITHSUBID%", counterWithSubId,
                            "%OBJECTIVEHOLDERNAME%", objectiveObjective.getObjectiveHolderName()
                            )
                    )
                ));
                if (!objectiveDescription.isBlank()) {
                    player.sendMessage(main.parse(
                        prefix+main.getLanguageManager().getString("chat.objectives.description", player, activeObjective.getActiveObjectiveHolder(), activeObjective))
                    );
                }
                return;
            }
            final String objectiveDescription = activeObjective.getObjective().getObjectiveHolderDescription();
            player.sendMessage(main.parse(
                prefix+main.getLanguageManager().getString("chat.objectives.counter", player, activeObjective.getActiveObjectiveHolder(), activeObjective,
                    Map.of("%OBJECTIVEIDWITHSUBID%", counterWithSubId)
                )
            ));

            if (!objectiveDescription.isBlank()) {
                player.sendMessage(main.parse(
                    prefix+main.getLanguageManager().getString("chat.objectives.description", player, activeObjective.getActiveObjectiveHolder(), activeObjective))
                );
            }

            player.sendMessage(main.parse(
                prefix+getObjectiveTaskDescription(activeObjective.getObjective(), false, questPlayer, activeObjective)
            ));

            player.sendMessage(main.parse(
                prefix+main.getLanguageManager().getString("chat.objectives.progress", player, activeObjective.getActiveObjectiveHolder(), activeObjective)
            ));
        } else {
            player.sendMessage(main.parse(
                prefix+main.getLanguageManager().getString("chat.objectives.hidden", player, activeObjective.getActiveObjectiveHolder(), activeObjective)
            ));
        }


    }


    /**
     * Checks if the player is close to a Citizens NPC or Armor Stand which has the specified Quest attached to it
     *
     * @param questPlayer the QuestPlayer who should be close to the Citizens NPC or Armor Stand
     * @param quest       the Quest which needs to be attached to the Citizens NPC or Armor Stand
     * @return if the player is close to a Citizens NPC or Armor Stand which has the specified Quest attached to it
     */
    public final boolean isPlayerCloseToCitizenOrArmorstandWithQuest(final QuestPlayer questPlayer, final Quest quest) {
        final int closenessCheckDistance = 6;
        final Player player = questPlayer.getPlayer();

        //First check Armor stands since I think that check is probably faster - especially if the user has a lot of Citizen NPCs
        for (final Entity entity : player.getNearbyEntities(closenessCheckDistance, closenessCheckDistance, closenessCheckDistance)) {
            if (entity instanceof ArmorStand armorStand) {
                if (getAllQuestsAttachedToArmorstand(armorStand).contains(quest)) {
                    return true;
                }
            }
        }


        //TODO: Only applicable for Citizens or not?
        for (NQNPC npc : getAllNPCsAttachedToQuest(quest)) {
            if (npc == null || npc.getEntity() == null) {
                main.getLogManager().warn("A quest has an invalid npc attached to it, which should be removed. Report it to an admin. Quest name: <highlight>" + quest.getIdentifier() + "</highlight>");
                continue;
            }
            final Location npcLocation = npc.getEntity().getLocation();
            if (npcLocation.getWorld() != null && npcLocation.getWorld().equals(player.getWorld())) {
                if (npcLocation.distance(player.getLocation()) < closenessCheckDistance) {
                    return true;
                }
            }
        }

        return false;


    }

    public final ArrayList<UUID> getDebugEnabledPlayers() {
        return debugEnabledPlayers;
    }

    public void addDebugEnabledPlayer(final UUID uuid) {
        this.debugEnabledPlayers.add(uuid);
    }

    public void removeDebugEnabledPlayer(final UUID uuid) {
        this.debugEnabledPlayers.remove(uuid);
    }

    public final boolean isDebugEnabledPlayer(final UUID uuid) {
        return this.debugEnabledPlayers.contains(uuid);
    }

    public final ArrayList<Quest> getAllQuestsWithVisibilityEvaluations(final QuestPlayer questPlayer) {
        return getQuestsFromListWithVisibilityEvaluations(questPlayer, getAllQuests());
    }

    public final ArrayList<Quest> getQuestsFromListWithVisibilityEvaluations(final QuestPlayer questPlayer, final ArrayList<Quest> questsList) {
        final ArrayList<Quest> evaluatedQuests = new ArrayList<>();
        questLoop:
        for (final Quest quest : questsList) {
            if (main.getConfiguration().isQuestVisibilityEvaluationAlreadyAccepted() && questPlayer != null){
                for (ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                    if (activeQuest.getQuest().equals(quest)) {
                        continue questLoop;
                    }
                }
            }


            if(main.getConfiguration().isQuestVisibilityEvaluationLimits() || main.getConfiguration().isQuestVisibilityEvaluationAcceptCooldown()){
                int completedAmount = 0;
                long mostRecentCompleteTime = 0;

                int failedAmount = 0;
                long mostRecentFailTime = 0;

                int acceptedAmount = 0;
                if(questPlayer != null){
                    for (final CompletedQuest completedQuest : questPlayer.getCompletedQuests()) {
                        if (completedQuest.getQuest().equals(quest)) {
                            completedAmount += 1;
                            acceptedAmount += 1;
                            if (completedQuest.getTimeCompleted() > mostRecentCompleteTime) {
                                mostRecentCompleteTime = completedQuest.getTimeCompleted();
                            }
                        }
                    }
                    for (final FailedQuest failedQuest : questPlayer.getFailedQuests()) {
                        if (failedQuest.getQuest().equals(quest)) {
                            failedAmount += 1;
                            acceptedAmount += 1;
                            if (failedQuest.getTimeFailed() > mostRecentFailTime) {
                                mostRecentFailTime = failedQuest.getTimeFailed();
                            }
                        }
                    }
                    for (final ActiveQuest activeQuest : questPlayer.getActiveQuests()) {
                        if (activeQuest.getQuest().equals(quest)) {
                            acceptedAmount += 1;
                        }
                    }
                }


                if(main.getConfiguration().isQuestVisibilityEvaluationLimits()) {
                    if (quest.getMaxCompletions() > -1 && completedAmount >= quest.getMaxCompletions()) {
                        continue questLoop;
                    }
                    if (quest.getMaxAccepts() > -1 && acceptedAmount >= quest.getMaxAccepts()) {
                        continue questLoop;
                    }
                    if (quest.getMaxFails() > -1 && failedAmount >= quest.getMaxFails()) {
                        continue questLoop;
                    }
                }

                if(main.getConfiguration().isQuestVisibilityEvaluationAcceptCooldown()) { // TODO: Cooldown for failed completed or accepted quests and potentially use questplayer.getcooldownformatted
                    final long acceptTimeDifference = System.currentTimeMillis() - mostRecentCompleteTime;
                    final long acceptTimeDifferenceMinutes = TimeUnit.MILLISECONDS.toMinutes(acceptTimeDifference);
                    if (acceptTimeDifferenceMinutes < quest.getAcceptCooldownComplete()) {
                        continue questLoop;
                    }
                }
            }

            if(main.getConfiguration().isQuestVisibilityEvaluationConditions()){
                for (final Condition condition : quest.getRequirements()) {
                    if (!condition.check(questPlayer).fulfilled()) {
                        continue questLoop;
                    }
                }
            }


            evaluatedQuests.add(quest);

        }
        return evaluatedQuests;
    }
}
