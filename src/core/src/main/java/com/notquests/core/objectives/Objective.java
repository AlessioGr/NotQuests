package com.notquests.core.objectives;

import com.notquests.core.actions.Action;
import com.notquests.core.conditions.Condition;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.structs.Quest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A configured objective, including its child objectives, conditions, and rewards. */
public final class Objective implements Objectives.Data, Objectives.Draft {
  private final int id;
  private final String typeId;
  private final Map<String, Object> values;
  private String displayName = "";
  private String description = "";
  private String taskDescription = "";
  private String completionNpc = "";
  private String childObjectiveProgressOrder = "";
  private NQLocation location;
  private boolean locationEnabled;
  private final List<Objective> childObjectives = new ArrayList<>();
  private final List<Condition> unlockConditions = new ArrayList<>();
  private final List<Condition> progressConditions = new ArrayList<>();
  private final List<Condition> completeConditions = new ArrayList<>();
  private final List<Action> rewards = new ArrayList<>();

  public Objective(final int id, final String typeId, final Objectives.Data data) {
    this.id = id;
    this.typeId = typeId == null ? "" : typeId;
    this.values = new LinkedHashMap<>();
    if (data != null) {
      data.copyTo(this);
    }
  }

  public int id() {
    return id;
  }

  public String typeId() {
    return typeId;
  }

  public Objective data() {
    return this;
  }

  public Map<String, Object> values() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(values));
  }

  @Override
  public void copyTo(final Objectives.Draft objective) {
    values.forEach(objective::setValue);
  }

  @Override
  public Object value(final String name) {
    return values.get(name);
  }

  @Override
  public String text(final String name) {
    final Object value = value(name);
    return value == null ? "" : String.valueOf(value);
  }

  @Override
  public boolean flag(final String name) {
    return Boolean.TRUE.equals(value(name));
  }

  @Override
  public int integer(final String name, final int fallback) {
    final Object value = value(name);
    return value instanceof Number number ? number.intValue() : fallback;
  }

  @Override
  public double number(final String name, final double fallback) {
    final Object value = value(name);
    return value instanceof Number number ? number.doubleValue() : fallback;
  }

  @Override
  public NQLocation location(final String name) {
    return value(name) instanceof NQLocation location ? location : null;
  }

  @Override
  public ItemSelection itemSelection(final String name) {
    return value(name) instanceof ItemSelection selection ? selection : null;
  }

  public void setValue(final String name, final Object value) {
    values.put(name, value);
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(final String displayName) {
    this.displayName = clean(displayName);
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = clean(description);
  }

  public String getTaskDescription() {
    return taskDescription;
  }

  public void setTaskDescription(final String taskDescription) {
    this.taskDescription = clean(taskDescription);
  }

  public String getCompletionNPC() {
    return completionNpc;
  }

  public void setCompletionNpc(final String completionNpc) {
    this.completionNpc = clean(completionNpc);
  }

  public String getChildObjectiveProgressOrder() {
    return childObjectiveProgressOrder;
  }

  public void setChildObjectiveProgressOrder(final String childObjectiveProgressOrder) {
    this.childObjectiveProgressOrder = clean(childObjectiveProgressOrder);
  }

  public void clearChildObjectiveProgressOrder() {
    childObjectiveProgressOrder = "";
  }

  public NQLocation getLocation() {
    return location;
  }

  public void setLocation(final NQLocation location) {
    this.location = location;
  }

  public boolean isLocationEnabled() {
    return locationEnabled;
  }

  public void setLocationEnabled(final boolean locationEnabled) {
    this.locationEnabled = locationEnabled;
  }

  public void apply(final Quest.ObjectiveSettings settings) {
    if (settings == null) {
      return;
    }
    setDisplayName(settings.displayName());
    setDescription(settings.description());
    setTaskDescription(settings.taskDescription());
    setChildObjectiveProgressOrder(settings.childObjectiveProgressOrder());
    setLocationEnabled(settings.locationEnabled());
    setCompletionNpc(settings.completionNpc());
    setLocation(settings.location());
  }

  public synchronized Objective addChildObjective(
      final String typeId,
      final Objectives.Data data,
      final String taskDescription) {
    return addChildObjective(nextObjectiveId(), typeId, data, taskDescription);
  }

  public synchronized Objective addChildObjective(
      final int id,
      final String typeId,
      final Objectives.Data data,
      final String taskDescription) {
    final Objective objective = new Objective(id, typeId, data);
    objective.setTaskDescription(taskDescription);
    childObjectives.add(objective);
    return objective;
  }

  public synchronized List<Objective> getObjectives() {
    return List.copyOf(childObjectives);
  }

  public synchronized Objective getObjectiveFromID(final int id) {
    return childObjectives.stream().filter(objective -> objective.id() == id).findFirst().orElse(null);
  }

  public synchronized int getFreeObjectiveID() {
    return firstFreeObjectiveId();
  }

  public synchronized int clearChildObjectives() {
    final int size = childObjectives.size();
    childObjectives.clear();
    return size;
  }

  public synchronized boolean removeChildObjective(final int id) {
    return childObjectives.removeIf(objective -> objective.id() == id);
  }

  public synchronized Condition addCondition(
      final String group,
      final String typeId,
      final Conditions.Data data) {
    final List<Condition> conditions = conditionGroup(group);
    return addCondition(group, nextConditionId(conditions), typeId, data);
  }

  public synchronized Condition addCondition(
      final String group,
      final int id,
      final String typeId,
      final Conditions.Data data) {
    final Condition condition = new Condition(id, typeId, data);
    conditionGroup(group).add(condition);
    return condition;
  }

  public synchronized List<Condition> getConditions(final String group) {
    return List.copyOf(conditionGroup(group));
  }

  public synchronized Condition getConditionFromID(final String group, final int id) {
    return conditionGroup(group).stream().filter(condition -> condition.id() == id).findFirst().orElse(null);
  }

  public synchronized int getFreeConditionID(final String group) {
    return firstFreeConditionId(conditionGroup(group));
  }

  public synchronized int clearConditions(final String group) {
    final List<Condition> conditions = conditionGroup(group);
    final int size = conditions.size();
    conditions.clear();
    return size;
  }

  public synchronized boolean removeCondition(final String group, final int id) {
    return conditionGroup(group).removeIf(condition -> condition.id() == id);
  }

  public synchronized Action addReward(
      final String typeId,
      final Actions.Data data) {
    return addReward(nextActionId(), typeId, data);
  }

  public synchronized Action addReward(
      final int id,
      final String typeId,
      final Actions.Data data) {
    final Action action = new Action(id, typeId, data);
    rewards.add(action);
    return action;
  }

  public synchronized List<Action> getRewards() {
    return List.copyOf(rewards);
  }

  public synchronized Action getRewardFromID(final int id) {
    return rewards.stream().filter(action -> action.id() == id).findFirst().orElse(null);
  }

  public synchronized int getFreeRewardID() {
    return firstFreeActionId();
  }

  public synchronized int clearRewards() {
    final int size = rewards.size();
    rewards.clear();
    return size;
  }

  public synchronized boolean removeReward(final int id) {
    return rewards.removeIf(action -> action.id() == id);
  }

  private List<Condition> conditionGroup(final String group) {
    if ("progress".equalsIgnoreCase(group)) {
      return progressConditions;
    }
    if ("complete".equalsIgnoreCase(group)) {
      return completeConditions;
    }
    return unlockConditions;
  }

  private int nextObjectiveId() {
    return childObjectives.stream().mapToInt(Objective::id).max().orElse(0) + 1;
  }

  private int firstFreeObjectiveId() {
    for (int id = 1; id < Integer.MAX_VALUE; id++) {
      final int candidate = id;
      if (childObjectives.stream().noneMatch(objective -> objective.id() == candidate)) {
        return candidate;
      }
    }
    return childObjectives.size() + 1;
  }

  private static int nextConditionId(final List<Condition> conditions) {
    return conditions.stream().mapToInt(Condition::id).max().orElse(0) + 1;
  }

  private static int firstFreeConditionId(final List<Condition> conditions) {
    for (int id = 1; id < Integer.MAX_VALUE; id++) {
      final int candidate = id;
      if (conditions.stream().noneMatch(condition -> condition.id() == candidate)) {
        return candidate;
      }
    }
    return conditions.size() + 1;
  }

  private int nextActionId() {
    return rewards.stream().mapToInt(Action::id).max().orElse(0) + 1;
  }

  private int firstFreeActionId() {
    for (int id = 1; id < Integer.MAX_VALUE; id++) {
      final int candidate = id;
      if (rewards.stream().noneMatch(action -> action.id() == candidate)) {
        return candidate;
      }
    }
    return rewards.size() + 1;
  }

  private static String clean(final String value) {
    return value == null ? "" : value;
  }
}
