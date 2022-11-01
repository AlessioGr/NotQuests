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

package rocks.gravili.notquests.paper.structs.objectives;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.expressions.NumberExpression;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.PredefinedProgressOrder;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.conditions.Condition;

public abstract class Objective extends ObjectiveHolder{
  protected final NotQuests main;
  private final ArrayList<Condition> unlockConditions;
  private final ArrayList<Condition> progressConditions;
  private final ArrayList<Condition> completeConditions;

  private final ArrayList<Action> rewards;

  private NumberExpression progressNeededExpression;

  private ObjectiveHolder objectiveHolder;
  private int objectiveID = -1;
  private String objectiveDisplayName = "";

  private String taskDescription = "";
  private NQNPC completionNPC = null;
  private boolean showLocation = false;
  private Location location = null;

  public Objective(NotQuests main) {
    this.main = main;
    unlockConditions = new ArrayList<>();
    progressConditions = new ArrayList<>();
    completeConditions = new ArrayList<>();
    rewards = new ArrayList<>();
  }

  public boolean isShowLocation() {
    return showLocation;
  }

  public Location getLocation() {
    return location;
  }

  public void setLocation(Location location, boolean save) {
    this.location = location;
    if (save) {
      objectiveHolder
          .getConfig()
          .set(
              objectiveHolder.getInitialConfigPath() + ".objectives." + getObjectiveID() + ".location",
              location);
      objectiveHolder.saveConfig();
    }
  }

  public void setShowLocation(boolean showLocation, boolean save) {
    this.showLocation = showLocation;
    if (save) {
      objectiveHolder
          .getConfig()
          .set(
              objectiveHolder.getInitialConfigPath()
                  + ".objectives."
                  + getObjectiveID()
                  + ".showLocation",
              showLocation);
      objectiveHolder.saveConfig();
    }
  }

  public final NQNPC getCompletionNPC() {
    return completionNPC;
  }

  public final void setCompletionNPC(final NQNPC completionNPC, final boolean save) {
    this.completionNPC = completionNPC;
    if (save) {
      completionNPC.saveToConfig(objectiveHolder.getConfig(), objectiveHolder.getInitialConfigPath()
          + ".objectives."
          + getObjectiveID()
          + ".completionNPCID");
      objectiveHolder.saveConfig();
    }
  }

  public final int getObjectiveID() {
    return objectiveID;
  }

  public void setObjectiveID(final int objectiveID) {
    this.objectiveID = objectiveID;
  }

  public final NumberExpression getProgressNeededExpression() {
    if (progressNeededExpression == null) {
      progressNeededExpression = new NumberExpression(main, "1");
    }
    return progressNeededExpression;
  }

  public void setProgressNeededExpression(final String progressNeededExpression) {
    this.progressNeededExpression = new NumberExpression(main, progressNeededExpression);
  }

  public final Action getRewardFromID(int id) {
    for (Action action : getRewards()) {
      if (action.getActionID() == id) {
        return action;
      }
    }
    return null;
  }

  public final ArrayList<Action> getRewards() {
    return rewards;
  }

  public void addReward(final Action action, final boolean save) {
    boolean dupeID = false;
    for (Action action1 : rewards) {
      if (action.getActionID() == action1.getActionID()) {
        dupeID = true;
        break;
      }
    }
    if (!dupeID) {
      rewards.add(action);
      if (save) {
        objectiveHolder
            .getConfig()
            .set(
                objectiveHolder.getInitialConfigPath()
                    + ".objectives."
                    + getObjectiveID()
                    + ".rewards."
                    + action.getActionID()
                    + ".actionType",
                action.getActionType());
        if (!action.getActionName().isBlank()) {
          objectiveHolder
              .getConfig()
              .set(
                  objectiveHolder.getInitialConfigPath()
                      + ".objectives."
                      + getObjectiveID()
                      + ".rewards."
                      + action.getActionID()
                      + ".displayName",
                  action.getActionName());
        }
        action.save(
            objectiveHolder.getConfig(),
            objectiveHolder.getInitialConfigPath()
                + ".objectives."
                + getObjectiveID()
                + ".rewards."
                + action.getActionID());
        objectiveHolder.saveConfig();
      }
    } else {
      main.getLogManager()
          .warn(
              "ERROR: Tried to add reward to objective with the ID <highlight>"
                  + getObjectiveID()
                  + "</highlight> with the ID <highlight>"
                  + action.getActionID()
                  + "</highlight> but the ID was a DUPLICATE!");
    }
  }

  public void removeReward(final Action action, final boolean save) {
    int rewardID = action.getActionID();
    rewards.remove(action);
    if (save) {
      objectiveHolder
          .getConfig()
          .set(
              objectiveHolder.getInitialConfigPath()
                  + ".objectives."
                  + getObjectiveID()
                  + ".rewards."
                  + rewardID,
              null);
      objectiveHolder.saveConfig();
    }
  }

  public void clearRewards() {
    rewards.clear();
    objectiveHolder
        .getConfig()
        .set(
            objectiveHolder.getInitialConfigPath() + ".objectives." + getObjectiveID() + ".rewards",
            null);
    objectiveHolder.saveConfig();
  }

  public final String getDisplayName() {
    return objectiveDisplayName;
  }

  @Override
  public final String getDisplayNameOrIdentifier() {
    if (!objectiveDisplayName.isBlank()) {
      return getDisplayName();
    } else {
      return main.getObjectiveManager().getObjectiveType(this.getClass());
    }
  }

  public void setDisplayName(String newObjectiveDisplayName, boolean save) {
    newObjectiveDisplayName =
        main.getUtilManager().replaceLegacyWithMiniMessage(newObjectiveDisplayName);
    this.objectiveDisplayName = newObjectiveDisplayName;
    if (save) {
      objectiveHolder
          .getConfig()
          .set(
              objectiveHolder.getInitialConfigPath() + ".objectives." + getObjectiveID() + ".displayName",
              newObjectiveDisplayName);
      objectiveHolder.saveConfig();
    }
  }

  public void removeDisplayName(boolean save) {
    this.objectiveDisplayName = "";
    if (save) {
      objectiveHolder
          .getConfig()
          .set(
              objectiveHolder.getInitialConfigPath() + ".objectives." + getObjectiveID() + ".displayName",
              null);
      objectiveHolder.saveConfig();
    }
  }


  public final String getTaskDescriptionProvided() { // MiniMessage
    return taskDescription;
  }


  /**
   * Gets the objective description, but also adds line-breaks so the description is not bigger than
   * the screen (useful for the GUI)
   *
   * @param maxLengthPerLine how long the description can be per-line
   * @return the description of the objective with proper line-breaks
   */
  public final String getObjectiveHolderDescription(final int maxLengthPerLine) {
    return main.getUtilManager().wrapText(getObjectiveHolderDescription(), maxLengthPerLine);
  }

  public final List<String> getDescriptionLines(final int maxLengthPerLine) {
    return main.getUtilManager().wrapTextToList(getObjectiveHolderDescription(), maxLengthPerLine);
  }

  public void setDescription(String newObjectiveDescription, boolean save) {
    newObjectiveDescription =
        main.getUtilManager().replaceLegacyWithMiniMessage(newObjectiveDescription);
    setObjectiveHolderDescription(newObjectiveDescription);
    if (save) {
      objectiveHolder
          .getConfig()
          .set(
              objectiveHolder.getInitialConfigPath() + ".objectives." + getObjectiveID() + ".description",
              newObjectiveDescription);
      objectiveHolder.saveConfig();
    }
  }

  public void setTaskDescription(String newObjectiveTaskDescription, boolean save) {
    newObjectiveTaskDescription =
        main.getUtilManager().replaceLegacyWithMiniMessage(newObjectiveTaskDescription);
    this.taskDescription = newObjectiveTaskDescription;
    if (save) {
      objectiveHolder
          .getConfig()
          .set(
              objectiveHolder.getInitialConfigPath() + ".objectives." + getObjectiveID() + ".taskDescription",
              newObjectiveTaskDescription);
      objectiveHolder.saveConfig();
    }
  }

  public void removeDescription(boolean save) {
    setObjectiveHolderDescription("");
    if (save) {
      objectiveHolder
          .getConfig()
          .set(
              objectiveHolder.getInitialConfigPath() + ".objectives." + getObjectiveID() + ".description",
              null);
      objectiveHolder.saveConfig();
    }
  }

  public void removeTaskDescription(boolean save) {
    this.taskDescription = "";
    if (save) {
      objectiveHolder
          .getConfig()
          .set(
              objectiveHolder.getInitialConfigPath() + ".objectives." + getObjectiveID() + ".taskDescription",
              null);
      objectiveHolder.saveConfig();
    }
  }

  public final ObjectiveHolder getObjectiveHolder() {
    return objectiveHolder;
  }

  public void setObjectiveHolder(final ObjectiveHolder objectiveHolder) {
    this.objectiveHolder = objectiveHolder;
  }

  public final String getTaskDescription(final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective){
    final String taskDescriptionToReturn = (getTaskDescriptionProvided() == null || getTaskDescriptionProvided().isBlank())
        ? getTaskDescriptionInternal(questPlayer, activeObjective)
        : getTaskDescriptionProvided();

    return main.getLanguageManager().getString("chat.objectives.taskDescription.global.prefix", questPlayer, activeObjective)
        + taskDescriptionToReturn.replace("    <veryUnimportant>└─ <unimportant>", "") //Convert old to new
        + main.getLanguageManager().getString("chat.objectives.taskDescription.global.suffix", questPlayer, activeObjective);
  }

  protected abstract String getTaskDescriptionInternal(
      final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective);

  public abstract void save(final FileConfiguration configuration, final String initialPath);

  public abstract void load(final FileConfiguration configuration, final String initialPath);

  public abstract void onObjectiveUnlock(
      final ActiveObjective activeObjective,
      final boolean unlockedDuringPluginStartupQuestLoadingProcess);

  public abstract void onObjectiveCompleteOrLock(
      final ActiveObjective activeObjective,
      final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
      final boolean completed);

  public final int getFreeRewardID() {
    for (int i = 1; i < Integer.MAX_VALUE; i++) {
      if (getRewardFromID(i) == null) {
        return i;
      }
    }
    return getRewards().size() + 1;
  }

  /*
   * Unlock Conditions
   */
  public final ArrayList<Condition> getUnlockConditions() {
    return unlockConditions;
  }

  public void addUnlockCondition(final Condition condition, final boolean save) {
    boolean dupeID = false;
    for (final Condition condition1 : unlockConditions) {
      if (condition.getConditionID() == condition1.getConditionID()) {
        dupeID = true;
        break;
      }
    }
    if (!dupeID) {
      unlockConditions.add(condition);
      if (save) {
        objectiveHolder
            .getConfig()
            .set(
                objectiveHolder.getInitialConfigPath()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditions."
                    + condition.getConditionID()
                    + ".conditionType",
                condition.getConditionType());
        objectiveHolder
            .getConfig()
            .set(
                objectiveHolder.getInitialConfigPath()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditions."
                    + condition.getConditionID()
                    + ".progressNeeded",
                condition.getProgressNeeded());
        objectiveHolder
            .getConfig()
            .set(
                objectiveHolder.getInitialConfigPath()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditions."
                    + condition.getConditionID()
                    + ".negated",
                condition.isNegated());
        objectiveHolder
            .getConfig()
            .set(
                objectiveHolder.getInitialConfigPath()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditions."
                    + condition.getConditionID()
                    + ".description",
                condition.getDescription());

        condition.save(
            objectiveHolder.getConfig(),
            objectiveHolder.getInitialConfigPath()
                + ".objectives."
                + getObjectiveID()
                + ".conditions."
                + condition.getConditionID());
        objectiveHolder.saveConfig();
      }
    } else {
      main.getLogManager()
          .warn(
              "ERROR: Tried to add condition to objective with the ID <highlight>"
                  + getObjectiveID()
                  + "</highlight> with the ID <highlight>"
                  + condition.getConditionID()
                  + "</highlight> but the ID was a DUPLICATE!");
    }
  }

  public void removeUnlockCondition(final Condition condition, final boolean save) {
    int conditionID = condition.getConditionID();
    unlockConditions.remove(condition);
    if (save) {
      objectiveHolder
          .getConfig()
          .set(
              objectiveHolder.getInitialConfigPath()
                  + ".objectives."
                  + getObjectiveID()
                  + ".conditions."
                  + conditionID,
              null);
      objectiveHolder.saveConfig();
    }
  }

  public void clearUnlockConditions() {
    unlockConditions.clear();
    objectiveHolder
        .getConfig()
        .set(
            objectiveHolder.getInitialConfigPath() + ".objectives." + getObjectiveID() + ".conditions",
            null);
    objectiveHolder.saveConfig();
  }

  public final Condition getUnlockConditionFromID(int id) {
    for (final Condition condition : getUnlockConditions()) {
      if (condition.getConditionID() == id) {
        return condition;
      }
    }
    return null;
  }

  public final int getFreeUnlockConditionID() {
    for (int i = 1; i < Integer.MAX_VALUE; i++) {
      if (getUnlockConditionFromID(i) == null) {
        return i;
      }
    }
    return getUnlockConditions().size() + 1;
  }

  /*
   * Progress Conditions
   */

  public final ArrayList<Condition> getProgressConditions() {
    return progressConditions;
  }

  public void addProgressCondition(final Condition condition, final boolean save) {
    boolean dupeID = false;
    for (final Condition condition1 : progressConditions) {
      if (condition.getConditionID() == condition1.getConditionID()) {
        dupeID = true;
        break;
      }
    }
    if (!dupeID) {
      progressConditions.add(condition);
      if (save) {
        objectiveHolder
            .getConfig()
            .set(
                objectiveHolder.getInitialConfigPath()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditionsProgress."
                    + condition.getConditionID()
                    + ".conditionType",
                condition.getConditionType());
        objectiveHolder
            .getConfig()
            .set(
                objectiveHolder.getInitialConfigPath()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditionsProgress."
                    + condition.getConditionID()
                    + ".progressNeeded",
                condition.getProgressNeeded());
        objectiveHolder
            .getConfig()
            .set(
                objectiveHolder.getInitialConfigPath()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditionsProgress."
                    + condition.getConditionID()
                    + ".negated",
                condition.isNegated());
        objectiveHolder
            .getConfig()
            .set(
                objectiveHolder.getInitialConfigPath()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditionsProgress."
                    + condition.getConditionID()
                    + ".description",
                condition.getDescription());

        objectiveHolder
            .getConfig()
            .set(
                objectiveHolder.getInitialConfigPath()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditionsProgress."
                    + condition.getConditionID()
                    + ".allowProgressDecreaseIfNotFulfilled",
                condition.isObjectiveConditionSpecific_allowProgressDecreaseIfNotFulfilled());

        condition.save(
            objectiveHolder.getConfig(),
            objectiveHolder.getInitialConfigPath()
                + ".objectives."
                + getObjectiveID()
                + ".conditionsProgress."
                + condition.getConditionID());
        objectiveHolder.saveConfig();
      }
    } else {
      main.getLogManager()
          .warn(
              "ERROR: Tried to add progress condition to objective with the ID <highlight>"
                  + getObjectiveID()
                  + "</highlight> with the ID <highlight>"
                  + condition.getConditionID()
                  + "</highlight> but the ID was a DUPLICATE!");
    }
  }

  public void removeProgressCondition(final Condition condition, final boolean save) {
    int conditionID = condition.getConditionID();
    progressConditions.remove(condition);
    if (save) {
      objectiveHolder
          .getConfig()
          .set(
              objectiveHolder.getInitialConfigPath()
                  + ".objectives."
                  + getObjectiveID()
                  + ".conditionsProgress."
                  + conditionID,
              null);
      objectiveHolder.saveConfig();
    }
  }

  public void clearProgressConditions() {
    progressConditions.clear();
    objectiveHolder
        .getConfig()
        .set(
            objectiveHolder.getInitialConfigPath() + ".objectives." + getObjectiveID() + ".conditionsProgress",
            null);
    objectiveHolder.saveConfig();
  }

  public final Condition getProgressConditionFromID(int id) {
    for (final Condition condition : getProgressConditions()) {
      if (condition.getConditionID() == id) {
        return condition;
      }
    }
    return null;
  }

  public final int getFreeProgressConditionID() {
    for (int i = 1; i < Integer.MAX_VALUE; i++) {
      if (getProgressConditionFromID(i) == null) {
        return i;
      }
    }
    return getProgressConditions().size() + 1;
  }

  /*
   * Complete Conditions
   */

  public final ArrayList<Condition> getCompleteConditions() {
    return completeConditions;
  }

  public void addCompleteCondition(final Condition condition, final boolean save) {
    boolean dupeID = false;
    for (final Condition condition1 : completeConditions) {
      if (condition.getConditionID() == condition1.getConditionID()) {
        dupeID = true;
        break;
      }
    }
    if (!dupeID) {
      completeConditions.add(condition);
      if (save) {
        objectiveHolder
            .getConfig()
            .set(
                objectiveHolder.getInitialConfigPath()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditionsComplete."
                    + condition.getConditionID()
                    + ".conditionType",
                condition.getConditionType());
        objectiveHolder
            .getConfig()
            .set(
                objectiveHolder.getInitialConfigPath()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditionsComplete."
                    + condition.getConditionID()
                    + ".progressNeeded",
                condition.getProgressNeeded());
        objectiveHolder
            .getConfig()
            .set(
                objectiveHolder.getInitialConfigPath()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditionsComplete."
                    + condition.getConditionID()
                    + ".negated",
                condition.isNegated());
        objectiveHolder
            .getConfig()
            .set(
                objectiveHolder.getInitialConfigPath()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditionsComplete."
                    + condition.getConditionID()
                    + ".description",
                condition.getDescription());

        condition.save(
            objectiveHolder.getConfig(),
            objectiveHolder.getInitialConfigPath()
                + ".objectives."
                + getObjectiveID()
                + ".conditionsComplete."
                + condition.getConditionID());
        objectiveHolder.saveConfig();
      }
    } else {
      main.getLogManager()
          .warn(
              "ERROR: Tried to add complete condition to objective with the ID <highlight>"
                  + getObjectiveID()
                  + "</highlight> with the ID <highlight>"
                  + condition.getConditionID()
                  + "</highlight> but the ID was a DUPLICATE!");
    }
  }

  public void removeCompleteCondition(final Condition condition, final boolean save) {
    int conditionID = condition.getConditionID();
    completeConditions.remove(condition);
    if (save) {
      objectiveHolder
          .getConfig()
          .set(
              objectiveHolder.getInitialConfigPath()
                  + ".objectives."
                  + getObjectiveID()
                  + ".conditionsComplete."
                  + conditionID,
              null);
      objectiveHolder.saveConfig();
    }
  }

  public void clearCompleteConditions() {
    completeConditions.clear();
    objectiveHolder
        .getConfig()
        .set(
            objectiveHolder.getInitialConfigPath() + ".objectives." + getObjectiveID() + ".conditionsComplete",
            null);
    objectiveHolder.saveConfig();
  }

  public final Condition getCompleteConditionFromID(int id) {
    for (final Condition condition : getCompleteConditions()) {
      if (condition.getConditionID() == id) {
        return condition;
      }
    }
    return null;
  }

  public final int getFreeCompleteConditionID() {
    for (int i = 1; i < Integer.MAX_VALUE; i++) {
      if (getCompleteConditionFromID(i) == null) {
        return i;
      }
    }
    return getCompleteConditions().size() + 1;
  }



  //For ObjectiveHolder interface:
  @Override
  public FileConfiguration getConfig() {
    return getObjectiveHolder().getConfig();
  }

  @Override
  public void saveConfig() {
    getObjectiveHolder().saveConfig();
  }

  @Override
  public String getInitialConfigPath() {
    return getObjectiveHolder().getInitialConfigPath() + ".objectives." + getObjectiveID();
  }

  @Override
  public String getIdentifier() {
    return getDisplayNameOrIdentifier();
  }

  @Override
  public void setPredefinedProgressOrder(final PredefinedProgressOrder predefinedProgressOrder, final boolean save) {
    super.predefinedProgressOrder = predefinedProgressOrder;
    if (save) {
      if(predefinedProgressOrder != null) {
        predefinedProgressOrder.saveToConfiguration(getObjectiveHolder().getConfig(),   objectiveHolder.getInitialConfigPath() + ".objectives." + getObjectiveID()
            + ".predefinedProgressOrder");
      }else{
        getObjectiveHolder().getConfig()
            .set(
                objectiveHolder.getInitialConfigPath() + ".objectives." + getObjectiveID() + ".predefinedProgressOrder",
                null);
      }
      getObjectiveHolder().saveConfig();
    }
  }

  @Override
  public void clearObjectives() {
    super.getObjectives().clear();
    getObjectiveHolder().getConfig().set(getObjectiveHolder().getInitialConfigPath() + ".objectives." + getObjectiveID() + ".objectives", null);
    getObjectiveHolder().saveConfig();
  }

  @Override
  public final Objective getObjectiveFromID(final int objectiveID) {
    for (final Objective objective : super.getObjectives()) {
      if (objective.getObjectiveID() == objectiveID) {
        return objective;
      }
    }
    return null;
  }

  @Override
  public void removeObjective(final Objective objective) {
    getObjectiveHolder().getConfig()
        .set(getObjectiveHolder().getInitialConfigPath() + ".objectives." + getObjectiveID() + ".objectives." + objective.getObjectiveID(), null);
    getObjectiveHolder().saveConfig();
    super.getObjectives().remove(objective);
  }


  public void addObjective(Objective objective, boolean save) {
    boolean dupeID = false;
    for (Objective objective1 : super.getObjectives()) {
      if (objective.getObjectiveID() == objective1.getObjectiveID()) {
        dupeID = true;
        break;
      }
    }
    if (!dupeID) {
      super.getObjectives().add(objective);
      if (save) {
        getObjectiveHolder().getConfig()
            .set(
                getObjectiveHolder().getInitialConfigPath() + ".objectives." + getObjectiveID() + ".objectives."
                    + objective.getObjectiveID()
                    + ".objectiveType",
                main.getObjectiveManager().getObjectiveType(objective.getClass()));
        getObjectiveHolder().getConfig()
            .set(
                getObjectiveHolder().getInitialConfigPath() + ".objectives." + getObjectiveID() + ".objectives."
                    + objective.getObjectiveID()
                    + ".progressNeededExpression",
                objective.getProgressNeededExpression().getRawExpression());

        objective.save(
            getObjectiveHolder().getConfig(),
            getObjectiveHolder().getInitialConfigPath() + ".objectives." + getObjectiveID() + ".objectives." + objective.getObjectiveID());
        getObjectiveHolder().saveConfig();
      }
    } else {
      main.getLogManager()
          .warn(
              "ERROR: Tried to add objective to quest <highlight>"
                  + getDisplayNameOrIdentifier()
                  + "</highlight> with the ID <highlight>"
                  + objective.getObjectiveID()
                  + "</highlight> but the ID was a DUPLICATE!");
    }
  }



}
