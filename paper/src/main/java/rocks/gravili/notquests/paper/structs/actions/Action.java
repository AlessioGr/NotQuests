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

package rocks.gravili.notquests.paper.structs.actions;

import java.util.ArrayList;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.conditions.Condition;
import rocks.gravili.notquests.paper.structs.objectives.Objective;

public abstract class Action {

  protected final NotQuests main;
  private final ArrayList<Condition> conditions;
  private String actionName = "";
  private Quest quest;
  private Objective objective;
  private Category category;
  private int actionID = -1;
  private long executionDelay = -1; // Cooldown in milliseconds. -1 or smaller => no cooldown.


  public Action(NotQuests main) {
    this.main = main;
    conditions = new ArrayList<>();
    category = main.getDataManager().getDefaultCategory();
    main.allActions.add(this); // For bStats
  }

  public final int getActionID() {
    return actionID;
  }

  public void setActionID(int actionID) {
    this.actionID = actionID;
  }

  public final Category getCategory() {
    return category;
  }

  public void setCategory(final Category category) {
    this.category = category;
  }

  public final String getActionType() {
    return main.getActionManager().getActionType(this.getClass());
  }

  public final String getActionName() {
    return actionName;
  }

  public void setActionName(final String actionName) {
    this.actionName = actionName;
  }

  public void removeActionName() {
    this.actionName = "";
  }

  public final Quest getQuest() {
    return quest;
  }

  public void setQuest(final Quest quest) {
    this.quest = quest;
  }

  public final Objective getObjective() {
    return objective;
  }

  public void setObjective(final Objective objective) {
    this.objective = objective;
  }

  public abstract String getActionDescription(
      final QuestPlayer questPlayer, final Object... objects);

  public abstract void executeInternally(final QuestPlayer questPlayer, Object... objects);

  public void execute(final QuestPlayer questPlayer, Object... objects) {
    if (main.getDataManager().isDisabled()) {
      return;
    }
    if (questPlayer != null) {
      questPlayer.sendDebugMessage("Executing action " + getActionName());
    }
    if(getExecutionDelay() == -1){
      executeInternally(questPlayer, objects);
    }else{
      Bukkit.getScheduler().runTaskLater(main.getMain(), () -> executeInternally(questPlayer, objects), getExecutionDelay()/50);
    }
  }

  public abstract void save(final FileConfiguration configuration, final String initialPath);

  public abstract void load(final FileConfiguration configuration, final String initialPath);

  public final ArrayList<Condition> getConditions() {
    return conditions;
  }

  public void addCondition(
      final Condition condition,
      final boolean save,
      final FileConfiguration configuration,
      final String initialPath) {
    conditions.add(condition);
    if (save) {
      configuration.set(
          initialPath + ".conditions." + conditions.size() + ".conditionType",
          condition.getConditionType());
      configuration.set(
          initialPath + ".conditions." + conditions.size() + ".progressNeeded",
          condition.getProgressNeeded());
      configuration.set(
          initialPath + ".conditions." + conditions.size() + ".negated", condition.isNegated());
      configuration.set(
          initialPath + ".conditions." + conditions.size() + ".description",
          condition.getDescription());

      condition.save(configuration, initialPath + ".conditions." + conditions.size());
    }
  }

  public final long getExecutionDelay() {
    return executionDelay;
  }

  public void setExecutionDelay(final long executionDelay) {
    this.executionDelay = executionDelay;
  }


  public void removeCondition(
      final Condition condition,
      final boolean save,
      final FileConfiguration configuration,
      final String initialPath) {
    int conditionID = conditions.indexOf(condition) + 1;
    conditions.remove(condition);
    if (save) {
      configuration.set(initialPath + ".conditions." + conditionID, null);
    }
  }

  public void clearConditions(final FileConfiguration configuration, final String initialPath) {
    conditions.clear();
    configuration.set(initialPath + ".conditions", null);
  }

  public abstract void deserializeFromSingleLineString(final ArrayList<String> arguments);

  public void switchCategory(final Category category) {

    final ConfigurationSection actionsConfigurationSection =
        getCategory().getActionsConfig().getConfigurationSection("actions." + getActionName());

    getCategory().getActionsConfig().set("actions." + getActionName(), null);
    getCategory().saveActionsConfig();

    setCategory(category);

    category.getActionsConfig().set("actions." + getActionName(), actionsConfigurationSection);
    category.saveActionsConfig();
  }

  @Override
  public String toString() {
    return "Action{"
        + "actionName='"
        + actionName
        + '\''
        + ", quest="
        + quest
        + ", objective="
        + objective
        + ", conditions="
        + conditions
        + ", category="
        + category
        + ", actionID="
        + actionID
        + '}';
  }

  public static Action loadActionFromConfig(final NotQuests main, final String initialPath, final FileConfiguration config, final Category category, final @Nullable String actionName, final int actionID, final @Nullable Quest quest, final @Nullable Objective objective)
      throws Exception {

    final String actionTypeString =
        config.getString(initialPath + ".actionType", "");

    final Class<? extends Action> actionType =
        main.getActionManager().getActionClass(actionTypeString);

    if (actionType == null) {
      throw new Exception("Action type " + actionTypeString + " could not be parsed.");
    }

    final Action action;


    action = actionType.getDeclaredConstructor(NotQuests.class).newInstance(main);
    action.setCategory(category);
    action.setActionID(actionID);
    if(actionName != null){
      action.setActionName(actionName);
    }
    if(quest != null){
      action.setQuest(quest);
    }
    if(objective != null){
      action.setObjective(objective);
    }

    final String actionsDisplayName =
        config.getString(initialPath + ".displayName", "");
    if (!actionsDisplayName.isBlank()) {
      action.setActionName(actionsDisplayName);
    }

    final long actionExecutionDelay =
        config.getLong(initialPath + ".executionDelay", -1);
    action.setExecutionDelay(actionExecutionDelay);


    action.load(config, initialPath);

    return action;
  }
}
