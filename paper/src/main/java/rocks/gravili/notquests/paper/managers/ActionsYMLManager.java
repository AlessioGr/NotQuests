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

import org.bukkit.configuration.ConfigurationSection;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.conditions.Condition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class ActionsYMLManager {
    private final NotQuests main;
    private final HashMap<String, Action> actionsAndIdentifiers;


    public ActionsYMLManager(final NotQuests main) {
        this.main = main;
        actionsAndIdentifiers = new HashMap<>();

        //setupFiles();
    }



    /*public void setupFiles(final Category category) {
        main.getLogManager().info("Loading actions.yml config");
        if (actionsConfigFile == null) {

            main.getDataManager().prepareDataFolder();

            actionsConfigFile = new File(main.getMain().getDataFolder(), "actions.yml");

            if (!actionsConfigFile.exists()) {
                main.getLogManager().info("Actions Configuration (actions.yml) does not exist. Creating a new one...");
                try {
                    //Try to create the actions.yml config file, and throw an error if it fails.
                    if (!actionsConfigFile.createNewFile()) {
                        main.getDataManager().disablePluginAndSaving("There was an error creating the actions.yml config file.");
                        return;

                    }
                } catch (IOException ioException) {
                    main.getDataManager().disablePluginAndSaving("There was an error creating the actions.yml config file. (2)", ioException);
                    return;
                }
            }

            actionsConfig = new YamlConfiguration();
            try {
                actionsConfig.load(actionsConfigFile);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }


        } else {
            actionsConfig = YamlConfiguration.loadConfiguration(actionsConfigFile);
        }
    }*/

    public void loadActions() {
        ArrayList<String> categoriesStringList = new ArrayList<>();
        for (final Category category : main.getDataManager().getCategories()) {
            categoriesStringList.add(category.getCategoryFullName());
        }
        main.getLogManager().info("Scheduled Actions Data load for following categories: <highlight>" + categoriesStringList.toString() );

        for (final Category category : main.getDataManager().getCategories()) {
            loadActions(category);
            main.getLogManager().info("Loading actions for category <highlight>" + category.getCategoryFullName());
        }
    }

    public void loadActions(final Category category) {
        //First load from actions.yml:
        if(category.getActionsConfig() == null){
            main.getLogManager().severe("Error: Cannot load actions of category <highlight>" + category.getCategoryFullName() + "</highlight>, because it doesn't have an actions config. This category has been skipped.");
            return;
        }

        final ConfigurationSection actionsConfigurationSection = category.getActionsConfig().getConfigurationSection("actions");
        if (actionsConfigurationSection != null) {
            for (final String actionIdentifier : actionsConfigurationSection.getKeys(false)) {
                if (actionsAndIdentifiers.get(actionIdentifier) != null) {
                    main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an error while loading actions.yml actions data: The action " + actionIdentifier + " already exists.");
                    return;
                }
                main.getLogManager().info("Loading action <highlight>" + actionIdentifier);

                Class<? extends Action> actionType = null;
                String actionTypeString = actionsConfigurationSection.getString(actionIdentifier + ".actionType", "");
                /*if (actionTypeString.isBlank()) {
                    actionTypeString = main.getUpdateManager().convertActionsYMLTypeToActionType(actionsConfigurationSection, actionIdentifier);
                }*/

                try {
                    actionType = main.getActionManager().getActionClass(actionTypeString);
                } catch (NullPointerException ex) {
                    main.getDataManager().disablePluginAndSaving("Error parsing actions.yml action Type of action with name <highlight>" + actionIdentifier + "</highlight>.", ex);
                }

                if (!actionIdentifier.isBlank() && actionType != null) {
                    Action action = null;

                    try {
                        action = actionType.getDeclaredConstructor(NotQuests.class).newInstance(main);
                        action.setCategory(category);
                        action.setActionName(actionIdentifier);
                        action.load(category.getActionsConfig(), "actions." + actionIdentifier);

                        loadActionConditions(action);

                    } catch (Exception ex) {
                        main.getDataManager().disablePluginAndSaving("Error parsing action Type of actions.yml action with name <highlight>" + actionIdentifier + "</highlight>.", ex);
                    }


                    if (action != null) {
                        final String actionsDisplayName = actionsConfigurationSection.getString(actionIdentifier + ".displayName", "");
                        if (!actionsDisplayName.isBlank()) {
                            action.setActionName(actionsDisplayName);
                        }
                        actionsAndIdentifiers.put(actionIdentifier, action);
                    } else {
                        main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an error while loading actions.yml actions data.");
                    }
                }

            }
        }

    }

    private void loadActionConditions(Action action) {
        final ConfigurationSection actionsConditionsConfigurationSection = action.getCategory().getActionsConfig().getConfigurationSection("actions." + action.getActionName() + ".conditions.");

        if (actionsConditionsConfigurationSection != null) {
            for (String actionConditionNumber : actionsConditionsConfigurationSection.getKeys(false)) {
                int conditionID = -1;
                boolean validConditionID = true;
                try {
                    conditionID = Integer.parseInt(actionConditionNumber);
                } catch (java.lang.NumberFormatException ex) {
                    validConditionID = false;
                    main.getDataManager().disablePluginAndSaving("Error parsing loaded condition ID <highlight>" + actionConditionNumber + "</highlight>.", action, ex);
                    return;
                }

                Class<? extends Condition> conditionType = null;
                String conditionTypeString = action.getCategory().getActionsConfig().getString("actions." + action.getActionName() + ".conditions." + actionConditionNumber + ".conditionType", "");
                try {
                    conditionType = main.getConditionsManager().getConditionClass(conditionTypeString);
                } catch (java.lang.NullPointerException ex) {
                    main.getDataManager().disablePluginAndSaving("Error parsing condition Type of action with ID <highlight>" + action.getActionName() + "</highlight>.", action, ex);
                    return;
                }

                int progressNeeded = action.getCategory().getActionsConfig().getInt("actions." + action.getActionName() + ".conditions." + actionConditionNumber + ".progressNeeded");
                boolean negated = action.getCategory().getActionsConfig().getBoolean("actions." + action.getActionName() + ".conditions." + actionConditionNumber + ".negated", false);
                String description = action.getCategory().getActionsConfig().getString("actions." + action.getActionName() + ".conditions." + actionConditionNumber + ".description", "");


                if (validConditionID && conditionID > 0 && conditionType != null) {
                    Condition condition = null;

                    try {
                        condition = conditionType.getDeclaredConstructor(NotQuests.class).newInstance(main);
                        condition.setProgressNeeded(progressNeeded);
                        condition.setNegated(negated);
                        condition.setDescription(description);
                        condition.setCategory(action.getCategory());
                        condition.load(action.getCategory().getActionsConfig(), "actions." + action.getActionName() + ".conditions." + actionConditionNumber);
                    } catch (Exception ex) {
                        main.getDataManager().disablePluginAndSaving("Error parsing condition Type of condition with ID <highlight>" + actionConditionNumber + "</highlight>.", action, ex);
                        return;
                    }
                    if (condition != null) {
                        action.addCondition(condition, false, action.getCategory().getActionsConfig(), "actions." + action.getActionName());
                    }

                } else {
                    main.getDataManager().disablePluginAndSaving("Error loading condition. ValidRequirementID: " + validConditionID + " conditionID: " + conditionID + " ConditionTypeNull?" + (conditionType == null) + " ConditionType: " + (conditionType != null ? conditionType.toString() : "null") + " conditionTypeString: " + conditionTypeString, action);
                    return;
                }
            }
        }
    }

    public void saveActions(final Category category) {
        try {
            category.getActionsConfig().save(category.getActionsFile());
            main.getLogManager().info("Saved Data to actions.yml");
        } catch (IOException e) {
            main.getLogManager().severe("Error saving actions. Actions were not saved...");
        }
    }

    /*public final FileConfiguration getActionsConfig() {
        return actionsConfig;
    }*/

    public final HashMap<String, Action> getActionsAndIdentifiers() {
        return actionsAndIdentifiers;
    }

    public final Action getAction(String actionIdentifier) {
        return actionsAndIdentifiers.get(actionIdentifier);
    }


    public final String addAction(String actionIdentifier, Action action) {
        boolean nameAlreadyExists = getActionsAndIdentifiers().get(actionIdentifier) != null;
        action.setActionName(actionIdentifier);

        if (!nameAlreadyExists) {
            actionsAndIdentifiers.put(actionIdentifier, action);

            action.getCategory().getActionsConfig().set("actions." + actionIdentifier + ".actionType", action.getActionType());
            if (!action.getActionName().isBlank()) {
                action.getCategory().getActionsConfig().set("actions." + actionIdentifier + ".displayName", action.getActionName());
            }

            action.save(action.getCategory().getActionsConfig(), "actions." + actionIdentifier);

            saveActions(action.getCategory());
            return ("<success>Action <highlight>" + actionIdentifier + "</highlight> successfully created!");
        } else {
            return ("<error>Action <highlight>" + actionIdentifier + "</highlight> already exists!");
        }
    }


    public String removeAction(String actionToDeleteIdentifier) {
        actionsAndIdentifiers.get(actionToDeleteIdentifier).getCategory().getActionsConfig().set("actions." + actionToDeleteIdentifier, null);
        saveActions(actionsAndIdentifiers.get(actionToDeleteIdentifier).getCategory());
        actionsAndIdentifiers.remove(actionToDeleteIdentifier);

        return "<success>Action <highlight>" + actionToDeleteIdentifier + "</highlight> successfully deleted!";
    }

    public String removeAction(Action actionToDelete) {
        actionToDelete.getCategory().getActionsConfig().set("actions." + actionToDelete.getActionName(), null);
        saveActions(actionToDelete.getCategory());
        actionsAndIdentifiers.remove(actionToDelete.getActionName());

        return "<success>Action <highlight>" + actionToDelete.getActionName() + "</highlight> successfully deleted!";
    }
}
