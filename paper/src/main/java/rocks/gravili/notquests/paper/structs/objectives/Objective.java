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
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.expressions.NumberExpression;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.conditions.Condition;

public abstract class Objective {
  protected final NotQuests main;
  private final ArrayList<Condition> unlockConditions;
  private final ArrayList<Condition> progressConditions;
  private final ArrayList<Condition> completeConditions;

  private final ArrayList<Action> rewards;

  private NumberExpression progressNeededExpression;

  private Quest quest;
  private int objectiveID = -1;
  private String objectiveDisplayName = "";
  private String objectiveDescription = "";

  private String taskDescription = "";
  private NQNPC completionNPC = null;
  private UUID completionArmorStandUUID = null;

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
      quest
          .getCategory()
          .getQuestsConfig()
          .set(
              "quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".location",
              location);
      quest.getCategory().saveQuestsConfig();
    }
  }

  public void setShowLocation(boolean showLocation, boolean save) {
    this.showLocation = showLocation;
    if (save) {
      quest
          .getCategory()
          .getQuestsConfig()
          .set(
              "quests."
                  + quest.getQuestName()
                  + ".objectives."
                  + getObjectiveID()
                  + ".showLocation",
              showLocation);
      quest.getCategory().saveQuestsConfig();
    }
  }

  public final NQNPC getCompletionNPC() {
    return completionNPC;
  }

  public final UUID getCompletionArmorStandUUID() {
    return completionArmorStandUUID;
  }

  public final void setCompletionNPC(final NQNPC completionNPC, final boolean save) {
    this.completionNPC = completionNPC;
    if (save) {
      completionNPC.saveToConfig(quest.getCategory()
          .getQuestsConfig(), "quests."
          + quest.getQuestName()
          + ".objectives."
          + getObjectiveID()
          + ".completionNPCID");
      quest.getCategory().saveQuestsConfig();
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
        quest
            .getCategory()
            .getQuestsConfig()
            .set(
                "quests."
                    + quest.getQuestName()
                    + ".objectives."
                    + getObjectiveID()
                    + ".rewards."
                    + action.getActionID()
                    + ".actionType",
                action.getActionType());
        if (!action.getActionName().isBlank()) {
          quest
              .getCategory()
              .getQuestsConfig()
              .set(
                  "quests."
                      + quest.getQuestName()
                      + ".objectives."
                      + getObjectiveID()
                      + ".rewards."
                      + action.getActionID()
                      + ".displayName",
                  action.getActionName());
        }
        action.save(
            quest.getCategory().getQuestsConfig(),
            "quests."
                + quest.getQuestName()
                + ".objectives."
                + getObjectiveID()
                + ".rewards."
                + action.getActionID());
        quest.getCategory().saveQuestsConfig();
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
      quest
          .getCategory()
          .getQuestsConfig()
          .set(
              "quests."
                  + quest.getQuestName()
                  + ".objectives."
                  + getObjectiveID()
                  + ".rewards."
                  + rewardID,
              null);
      quest.getCategory().saveQuestsConfig();
    }
  }

  public void clearRewards() {
    rewards.clear();
    quest
        .getCategory()
        .getQuestsConfig()
        .set(
            "quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".rewards",
            null);
    quest.getCategory().saveQuestsConfig();
  }

  public final String getDisplayName() {
    return objectiveDisplayName;
  }

  public final String getFinalName() {
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
      quest
          .getCategory()
          .getQuestsConfig()
          .set(
              "quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".displayName",
              newObjectiveDisplayName);
      quest.getCategory().saveQuestsConfig();
    }
  }

  public void removeDisplayName(boolean save) {
    this.objectiveDisplayName = "";
    if (save) {
      quest
          .getCategory()
          .getQuestsConfig()
          .set(
              "quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".displayName",
              null);
      quest.getCategory().saveQuestsConfig();
    }
  }

  public final String getDescription() { // MiniMessage
    return objectiveDescription;
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
  public final String getDescription(final int maxLengthPerLine) {
    return main.getUtilManager().wrapText(getDescription(), maxLengthPerLine);
  }

  public final List<String> getDescriptionLines(final int maxLengthPerLine) {
    return main.getUtilManager().wrapTextToList(getDescription(), maxLengthPerLine);
  }

  public void setDescription(String newObjectiveDescription, boolean save) {
    newObjectiveDescription =
        main.getUtilManager().replaceLegacyWithMiniMessage(newObjectiveDescription);
    this.objectiveDescription = newObjectiveDescription;
    if (save) {
      quest
          .getCategory()
          .getQuestsConfig()
          .set(
              "quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".description",
              newObjectiveDescription);
      quest.getCategory().saveQuestsConfig();
    }
  }

  public void setTaskDescription(String newObjectiveTaskDescription, boolean save) {
    newObjectiveTaskDescription =
        main.getUtilManager().replaceLegacyWithMiniMessage(newObjectiveTaskDescription);
    this.taskDescription = newObjectiveTaskDescription;
    if (save) {
      quest
          .getCategory()
          .getQuestsConfig()
          .set(
              "quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".taskDescription",
              newObjectiveTaskDescription);
      quest.getCategory().saveQuestsConfig();
    }
  }

  public void removeDescription(boolean save) {
    this.objectiveDescription = "";
    if (save) {
      quest
          .getCategory()
          .getQuestsConfig()
          .set(
              "quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".description",
              null);
      quest.getCategory().saveQuestsConfig();
    }
  }

  public void removeTaskDescription(boolean save) {
    this.taskDescription = "";
    if (save) {
      quest
          .getCategory()
          .getQuestsConfig()
          .set(
              "quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".taskDescription",
              null);
      quest.getCategory().saveQuestsConfig();
    }
  }

  public final Quest getQuest() {
    return quest;
  }

  public void setQuest(final Quest quest) {
    this.quest = quest;
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
        quest
            .getCategory()
            .getQuestsConfig()
            .set(
                "quests."
                    + quest.getQuestName()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditions."
                    + condition.getConditionID()
                    + ".conditionType",
                condition.getConditionType());
        quest
            .getCategory()
            .getQuestsConfig()
            .set(
                "quests."
                    + quest.getQuestName()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditions."
                    + condition.getConditionID()
                    + ".progressNeeded",
                condition.getProgressNeeded());
        quest
            .getCategory()
            .getQuestsConfig()
            .set(
                "quests."
                    + quest.getQuestName()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditions."
                    + condition.getConditionID()
                    + ".negated",
                condition.isNegated());
        quest
            .getCategory()
            .getQuestsConfig()
            .set(
                "quests."
                    + quest.getQuestName()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditions."
                    + condition.getConditionID()
                    + ".description",
                condition.getDescription());

        condition.save(
            quest.getCategory().getQuestsConfig(),
            "quests."
                + quest.getQuestName()
                + ".objectives."
                + getObjectiveID()
                + ".conditions."
                + condition.getConditionID());
        quest.getCategory().saveQuestsConfig();
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
      quest
          .getCategory()
          .getQuestsConfig()
          .set(
              "quests."
                  + quest.getQuestName()
                  + ".objectives."
                  + getObjectiveID()
                  + ".conditions."
                  + conditionID,
              null);
      quest.getCategory().saveQuestsConfig();
    }
  }

  public void clearUnlockConditions() {
    unlockConditions.clear();
    quest
        .getCategory()
        .getQuestsConfig()
        .set(
            "quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".conditions",
            null);
    quest.getCategory().saveQuestsConfig();
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
        quest
            .getCategory()
            .getQuestsConfig()
            .set(
                "quests."
                    + quest.getQuestName()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditionsProgress."
                    + condition.getConditionID()
                    + ".conditionType",
                condition.getConditionType());
        quest
            .getCategory()
            .getQuestsConfig()
            .set(
                "quests."
                    + quest.getQuestName()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditionsProgress."
                    + condition.getConditionID()
                    + ".progressNeeded",
                condition.getProgressNeeded());
        quest
            .getCategory()
            .getQuestsConfig()
            .set(
                "quests."
                    + quest.getQuestName()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditionsProgress."
                    + condition.getConditionID()
                    + ".negated",
                condition.isNegated());
        quest
            .getCategory()
            .getQuestsConfig()
            .set(
                "quests."
                    + quest.getQuestName()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditionsProgress."
                    + condition.getConditionID()
                    + ".description",
                condition.getDescription());

        quest
            .getCategory()
            .getQuestsConfig()
            .set(
                "quests."
                    + quest.getQuestName()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditionsProgress."
                    + condition.getConditionID()
                    + ".allowProgressDecreaseIfNotFulfilled",
                condition.isObjectiveConditionSpecific_allowProgressDecreaseIfNotFulfilled());

        condition.save(
            quest.getCategory().getQuestsConfig(),
            "quests."
                + quest.getQuestName()
                + ".objectives."
                + getObjectiveID()
                + ".conditionsProgress."
                + condition.getConditionID());
        quest.getCategory().saveQuestsConfig();
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
      quest
          .getCategory()
          .getQuestsConfig()
          .set(
              "quests."
                  + quest.getQuestName()
                  + ".objectives."
                  + getObjectiveID()
                  + ".conditionsProgress."
                  + conditionID,
              null);
      quest.getCategory().saveQuestsConfig();
    }
  }

  public void clearProgressConditions() {
    progressConditions.clear();
    quest
        .getCategory()
        .getQuestsConfig()
        .set(
            "quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".conditionsProgress",
            null);
    quest.getCategory().saveQuestsConfig();
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
        quest
            .getCategory()
            .getQuestsConfig()
            .set(
                "quests."
                    + quest.getQuestName()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditionsComplete."
                    + condition.getConditionID()
                    + ".conditionType",
                condition.getConditionType());
        quest
            .getCategory()
            .getQuestsConfig()
            .set(
                "quests."
                    + quest.getQuestName()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditionsComplete."
                    + condition.getConditionID()
                    + ".progressNeeded",
                condition.getProgressNeeded());
        quest
            .getCategory()
            .getQuestsConfig()
            .set(
                "quests."
                    + quest.getQuestName()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditionsComplete."
                    + condition.getConditionID()
                    + ".negated",
                condition.isNegated());
        quest
            .getCategory()
            .getQuestsConfig()
            .set(
                "quests."
                    + quest.getQuestName()
                    + ".objectives."
                    + getObjectiveID()
                    + ".conditionsComplete."
                    + condition.getConditionID()
                    + ".description",
                condition.getDescription());

        condition.save(
            quest.getCategory().getQuestsConfig(),
            "quests."
                + quest.getQuestName()
                + ".objectives."
                + getObjectiveID()
                + ".conditionsComplete."
                + condition.getConditionID());
        quest.getCategory().saveQuestsConfig();
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
      quest
          .getCategory()
          .getQuestsConfig()
          .set(
              "quests."
                  + quest.getQuestName()
                  + ".objectives."
                  + getObjectiveID()
                  + ".conditionsComplete."
                  + conditionID,
              null);
      quest.getCategory().saveQuestsConfig();
    }
  }

  public void clearCompleteConditions() {
    completeConditions.clear();
    quest
        .getCategory()
        .getQuestsConfig()
        .set(
            "quests." + quest.getQuestName() + ".objectives." + getObjectiveID() + ".conditionsComplete",
            null);
    quest.getCategory().saveQuestsConfig();
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
}
