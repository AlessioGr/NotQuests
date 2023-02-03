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

package rocks.gravili.notquests.paper.structs.conditions;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.managers.expressions.NumberExpression;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.ObjectiveHolder;

public abstract class Condition {
  protected final NotQuests main;
  private String conditionName = "";
  private long progressNeeded = 1;
  private ObjectiveHolder objectiveHolder;
  private Objective objective;
  private boolean negated = false;
  private Category category;

  /** Custom Condition description */
  private String description = "";

  private NumberExpression hidden;

  private int conditionID = -1;

  private boolean objectiveConditionSpecific_allowProgressDecreaseIfNotFulfilled = false;

  public Condition(NotQuests main) {
    this.main = main;
    category = main.getDataManager().getDefaultCategory();
    main.allConditions.add(this); // For bStats
  }



  public final int getConditionID() {
    return conditionID;
  }

  public void setConditionID(int conditionID) {
    this.conditionID = conditionID;
  }

  public final String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description;
  }

  public void removeDescription() {
    this.description = "";
  }

  public final Category getCategory() {
    return category;
  }

  public void setCategory(final Category category) {
    this.category = category;
  }

  public final String getConditionType() {
    return main.getConditionsManager().getConditionType(this.getClass());
  }

  public long getProgressNeeded() {
    return progressNeeded;
  }

  public void setProgressNeeded(final long progressNeeded) {
    this.progressNeeded = progressNeeded;
  }

  public final ObjectiveHolder getObjectiveHolder() {
    return objectiveHolder;
  }

  public void setObjectiveHolder(final ObjectiveHolder objectiveHolder) {
    this.objectiveHolder = objectiveHolder;
  }

  public final Objective getObjective() {
    return objective;
  }

  public void setObjective(final Objective objective) {
    this.objective = objective;
  }

  public final String getConditionName() {
    return conditionName;
  }

  public void setConditionName(final String conditionName) {
    this.conditionName = conditionName;
  }

  public final String getConditionIdentifier() {
    return "Name: "
        + conditionName
        + " Type: "
        + getConditionType()
        + " Description: "
        + getDescription()
        + " Negated: "
        + negated;
  }

  /**
   * @return String if the condition is not fulfilled. Empty string if the condition is fulfilled.
   *     The String should say the still-required condition.
   */
  protected abstract String checkInternally(final QuestPlayer questPlayer);

  public final boolean canCheckAsync() { // TODO: Make some conditions work async
    return false;
  }

  public final ConditionResult check(final QuestPlayer questPlayer) {
    final String result;
    if(Bukkit.isPrimaryThread() || canCheckAsync()){
      result = checkInternally(questPlayer);
    }else {

      try {
        result = Bukkit.getScheduler().callSyncMethod(main.getMain(), () -> checkInternally(questPlayer)).get();
        main.getLogManager().info("Async result: "+ result);
      }catch (Exception e){
        e.printStackTrace();
        return new ConditionResult(false, "An error occurred while checking the condition (from async thread). Please report this to the developer!");
      }
    }


    if (!isNegated()) {
      if (result.isBlank()) {
        return new ConditionResult(true, "");
      } else {
        if (description.isBlank()) {
          return new ConditionResult(false, result);

        } else {
          return new ConditionResult(false, "<YELLOW>" + description);
        }
      }
    } else {
      if (result.isBlank()) {
        return new ConditionResult(false, "<YELLOW>You cannot fulfill this condition: <unimportant>"
            + getConditionDescription(questPlayer));
      } else {
        return new ConditionResult(true, "");
      }
    }
  }

  public String getConditionDescription(QuestPlayer questPlayer, Object... objects) {
    if (description.isBlank()) {
      return getConditionDescriptionInternally(questPlayer, objects);
    } else {
      return "<GRAY>" + description;
    }
  }

  protected abstract String getConditionDescriptionInternally(
      QuestPlayer questPlayer, Object... objects);

  public abstract void save(final FileConfiguration configuration, final String initialPath);

  public abstract void load(final FileConfiguration configuration, final String initialPath);

  public final boolean isNegated() {
    return negated;
  }

  public void setNegated(boolean negated) {
    this.negated = negated;
  }

  public abstract void deserializeFromSingleLineString(final ArrayList<String> arguments);

  public void switchCategory(final Category category) {

    final ConfigurationSection conditionsConfigurationSection =
        getCategory()
            .getConditionsConfig()
            .getConfigurationSection("conditions." + getConditionName());

    getCategory().getConditionsConfig().set("conditions." + getConditionName(), null);
    getCategory().saveConditionsConfig();

    setCategory(category);

    category
        .getConditionsConfig()
        .set("conditions." + getConditionName(), conditionsConfigurationSection);
    category.saveConditionsConfig();
  }

  @Override
  public String toString() {
    return "Condition{"
        + "conditionName='"
        + conditionName
        + '\''
        + ", progressNeeded="
        + progressNeeded
        + ", negated="
        + negated
        + ", category="
        + category
        + ", description='"
        + description
        + '\''
        + ", conditionID="
        + conditionID
        + '}';
  }

  public final boolean isObjectiveConditionSpecific_allowProgressDecreaseIfNotFulfilled() {
    return objectiveConditionSpecific_allowProgressDecreaseIfNotFulfilled;
  }

  public void setObjectiveConditionSpecific_allowProgressDecreaseIfNotFulfilled(
      final boolean objectiveConditionSpecific_allowProgressDecreaseIfNotFulfilled) {
    this.objectiveConditionSpecific_allowProgressDecreaseIfNotFulfilled = objectiveConditionSpecific_allowProgressDecreaseIfNotFulfilled;
  }


  public final boolean isHidden(final QuestPlayer questPlayer) {
    return hidden != null && hidden.calculateBooleanValue(questPlayer);
  }
  public final NumberExpression getHiddenExpression() {
    if (hidden == null) {
      hidden = new NumberExpression(main, "0");
    }
    return hidden;
  }
  public void setHidden(final NumberExpression hidden) {
    this.hidden = hidden;
  }

  public record ConditionResult(boolean fulfilled, String message) {

  }

  public static Condition loadConditionFromConfig(final NotQuests main, final String initialPath, final FileConfiguration config, final Category category, final @Nullable String conditionName, final int conditionID, final @Nullable ObjectiveHolder objectiveHolder, final @Nullable Objective objective)
      throws Exception {
    final String conditionTypeString =
        config
            .getString(
                initialPath
                    + ".conditionType",
                "");

    final Class<? extends Condition> conditionType =
        main.getConditionsManager().getConditionClass(conditionTypeString);

    if (conditionType == null) {
      throw new Exception("Condition type " + conditionTypeString + " could not be parsed.");
    }

    final int progressNeeded =
        config
            .getInt(
                initialPath
                    + ".progressNeeded");
    final boolean negated =
        config
            .getBoolean(
                initialPath
                    + ".negated",
                false);
    final String description =
        config
            .getString(
                initialPath
                    + ".description",
                "");

    final String hiddenStatusExpression =
        config
            .getString(
                initialPath
                    + ".hiddenStatusExpression",
                "");

    final Condition condition;

    condition = conditionType.getDeclaredConstructor(NotQuests.class).newInstance(main);
    condition.setProgressNeeded(progressNeeded);
    condition.setNegated(negated);
    condition.setDescription(description);
    condition.setConditionID(conditionID);
    if(conditionName != null) {
      condition.setConditionName(conditionName);
    }
    if(category != null) {
      condition.setCategory(category);
    }
    if(objectiveHolder != null){
      condition.setObjectiveHolder(objectiveHolder);
    }
    if(objective != null){
      condition.setObjective(objective);
    }
    condition.setHidden(new NumberExpression(main, hiddenStatusExpression.isBlank() ? "0" : hiddenStatusExpression));

    condition.load(
        config,
        initialPath);

    return condition;
  }
}
