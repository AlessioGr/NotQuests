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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.conditions.Condition;

public class ActionsYMLManager {
  private final NotQuests main;
  private final HashMap<String, Action> actionsAndIdentifiers;

  public ActionsYMLManager(final NotQuests main) {
    this.main = main;
    actionsAndIdentifiers = new HashMap<>();
  }

  public void loadActions() {
    final ArrayList<String> categoriesStringList = new ArrayList<>();
    for (final Category category : main.getDataManager().getCategories()) {
      categoriesStringList.add(category.getCategoryFullName());
    }
    main.getLogManager()
        .info(
            "Scheduled Actions Data load for following categories: <highlight>"
                + categoriesStringList + "</highlight>...");

    for (final Category category : main.getDataManager().getCategories()) {
      main.getLogManager()
              .info("Loading actions for category <highlight>" + category.getCategoryFullName());
      loadActions(category);
    }
  }

  public void loadActions(final Category category) {
    // First load from actions.yml:
    if (category.getActionsConfig() == null) {
      main.getLogManager()
          .severe(
              "Error: Cannot load actions of category <highlight>"
                  + category.getCategoryFullName()
                  + "</highlight>, because it doesn't have an actions config. This category has been skipped.");
      return;
    }

    final ConfigurationSection actionsConfigurationSection =
        category.getActionsConfig().getConfigurationSection("actions");
    if (actionsConfigurationSection != null) {
      for (final String actionIdentifier : actionsConfigurationSection.getKeys(false)) {
        if (actionsAndIdentifiers.get(actionIdentifier) != null) {
          main.getDataManager()
              .disablePluginAndSaving(
                  "Plugin disabled, because there was an error while loading actions.yml actions data: The action "
                      + actionIdentifier
                      + " already exists.");
          return;
        }
        if (main.getConfiguration().isVerboseStartupMessages()) {
          main.getLogManager().info("Loading action <highlight>" + actionIdentifier);
        }


        if (actionIdentifier.isBlank()) {
          main.getLogManager()
              .warn(
                  "Skipping loading the action of category <highlight2>"
                      + category.getCategoryFullName()
                      + "</highlight2> because the action identifier is empty.");
          continue;
        }

        final Action action;

        try{
          action = Action.loadActionFromConfig(
              main,
              "actions." + actionIdentifier,
              category.getActionsConfig(),
              category,
              actionIdentifier,
              -1,
              null,
              null);
        }catch (final Exception e){
          main.getDataManager()
              .disablePluginAndSaving(
                  "Error parsing action of actions.yml with name <highlight>"
                      + actionIdentifier
                      + "</highlight>. Error: " + e.getMessage(),
                  e,
                  category);
          return;
        }

        loadActionConditions(action);

        actionsAndIdentifiers.put(actionIdentifier, action);
      }
    }
  }

  private void loadActionConditions(final Action action) {
    final ConfigurationSection actionsConditionsConfigurationSection =
        action
            .getCategory()
            .getActionsConfig()
            .getConfigurationSection("actions." + action.getActionName() + ".conditions.");

    if (actionsConditionsConfigurationSection != null) {
      for (final String actionConditionNumber :
          actionsConditionsConfigurationSection.getKeys(false)) {
        final int conditionID;
        try {
          conditionID = Integer.parseInt(actionConditionNumber);
        } catch (java.lang.NumberFormatException ex) {
          main.getDataManager()
              .disablePluginAndSaving(
                  "Error parsing loaded condition ID <highlight>"
                      + actionConditionNumber
                      + "</highlight>.",
                  action,
                  ex);
          return;
        }

        if (conditionID <= 0) {
          main.getDataManager()
              .disablePluginAndSaving(
                  "Error parsing condition ID of action with ID <highlight>"
                      + action.getActionName()
                      + "</highlight>. Reason: Invalid condition ID - it needs to be bigger than 0: "
                      + conditionID,
                  action);
          return;
        }

        final Condition condition;

        try{
          condition = Condition.loadConditionFromConfig(
              main,
              "actions."+ action.getActionName()+ ".conditions."+ actionConditionNumber,
              action.getCategory().getActionsConfig(),
              action.getCategory(),
              null,
              conditionID,
              null,
              null);
        }catch (final Exception e){
          main.getDataManager()
              .disablePluginAndSaving(
                  "Error parsing condition of action with ID <highlight>"
                      + action.getActionName()
                      + "</highlight>. Condition type: <highlight2>. Error: "
                      + e.getMessage(),
                  action,
                  e);
          return;
        }


        action.addCondition(
            condition,
            false,
            action.getCategory().getActionsConfig(),
            "actions." + action.getActionName());
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

  public final Action getAction(final @NotNull String actionIdentifier) {
    return actionsAndIdentifiers.get(actionIdentifier);
  }

  public final String addAction(final String actionIdentifier, final Action action) {
    final boolean nameAlreadyExists = getActionsAndIdentifiers().get(actionIdentifier) != null;
    action.setActionName(actionIdentifier);

    if (actionIdentifier.contains(".")) {
      return ("<error>Action <highlight>"
          + actionIdentifier
          + "</highlight> cannot contain a dot in its name!");
    }

    if (!nameAlreadyExists) {
      actionsAndIdentifiers.put(actionIdentifier, action);

      action
          .getCategory()
          .getActionsConfig()
          .set("actions." + actionIdentifier + ".actionType", action.getActionType());
      if (!action.getActionName().isBlank()) {
        action
            .getCategory()
            .getActionsConfig()
            .set("actions." + actionIdentifier + ".displayName", action.getActionName());
      }

      if (action.getExecutionDelay() != -1) {
        action
            .getCategory()
            .getActionsConfig()
            .set("actions." + actionIdentifier + ".executionDelay", action.getExecutionDelay());
      }

      action.save(action.getCategory().getActionsConfig(), "actions." + actionIdentifier);

      saveActions(action.getCategory());

      return ("<success>"
          + main.getActionManager().getActionType(action.getClass())
          + " Action with the name <highlight>"
          + actionIdentifier
          + "</highlight> has been created successfully!");
    } else {
      return ("<error>Action <highlight>" + actionIdentifier + "</highlight> already exists!");
    }
  }

  public final String removeAction(String actionToDeleteIdentifier) {
    actionsAndIdentifiers
        .get(actionToDeleteIdentifier)
        .getCategory()
        .getActionsConfig()
        .set("actions." + actionToDeleteIdentifier, null);
    saveActions(actionsAndIdentifiers.get(actionToDeleteIdentifier).getCategory());
    actionsAndIdentifiers.remove(actionToDeleteIdentifier);

    return "<success>Action <highlight>"
        + actionToDeleteIdentifier
        + "</highlight> successfully deleted!";
  }

  public final String removeAction(Action actionToDelete) {
    actionToDelete
        .getCategory()
        .getActionsConfig()
        .set("actions." + actionToDelete.getActionName(), null);
    saveActions(actionToDelete.getCategory());
    actionsAndIdentifiers.remove(actionToDelete.getActionName());

    return "<success>Action <highlight>"
        + actionToDelete.getActionName()
        + "</highlight> successfully deleted!";
  }
}
