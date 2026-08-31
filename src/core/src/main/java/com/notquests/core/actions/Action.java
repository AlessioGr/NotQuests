package com.notquests.core.actions;

import com.notquests.core.conditions.Condition;
import com.notquests.core.items.ItemSelection;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A configured action belonging to a quest, objective, or saved action chain. */
public final class Action implements Actions.Data, Actions.Draft {
  private final int id;
  private final String typeId;
  private final Map<String, Object> values;
  private String displayName = "";
  private String description = "";
  private final List<Condition> conditions = new ArrayList<>();

  public Action(final int id, final String typeId, final Actions.Data data) {
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

  public Action data() {
    return this;
  }

  public Map<String, Object> values() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(values));
  }

  @Override
  public void copyTo(final Actions.Draft action) {
    values.forEach(action::setValue);
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
  public int integer(final String name) {
    return integer(name, 0);
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
  public boolean flag(final String name) {
    return Boolean.TRUE.equals(value(name));
  }

  @Override
  public ItemSelection itemSelection(final String name) {
    return value(name) instanceof ItemSelection selection ? selection : null;
  }

  @Override
  public Duration duration(final String name, final Duration fallback) {
    return value(name) instanceof Duration duration ? duration : fallback;
  }

  @Override
  public NQLocation location(final String name) {
    return value(name) instanceof NQLocation location ? location : null;
  }

  @Override
  public void setValue(final String name, final Object value) {
    values.put(name, value);
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(final String displayName) {
    this.displayName = displayName == null ? "" : displayName;
  }

  public void clearDisplayName() {
    displayName = "";
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description == null ? "" : description;
  }

  public synchronized Condition addCondition(
      final int id,
      final String typeId,
      final Conditions.Data data) {
    final Condition condition = new Condition(id, typeId, data);
    conditions.add(condition);
    return condition;
  }

  public synchronized List<Condition> getConditions() {
    return List.copyOf(conditions);
  }
}
