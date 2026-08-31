package com.notquests.core.conditions;

import com.notquests.core.registry.NotQuestsRegistry.Conditions;
import com.notquests.core.structs.Quest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** A configured condition and its quest-specific behavior. */
public final class Condition implements Conditions.Data, Conditions.Draft {
  private final int id;
  private final String typeId;
  private final Map<String, Object> values;
  private long progressNeeded = 1;
  private boolean negated;
  private String description = "";
  private String hiddenExpression = "";
  private boolean allowProgressDecreaseIfNotFulfilled;

  public Condition(final int id, final String typeId, final Conditions.Data data) {
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

  public Condition data() {
    return this;
  }

  public Map<String, Object> values() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(values));
  }

  @Override
  public void copyTo(final Conditions.Draft condition) {
    values.forEach(condition::setValue);
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
  public void setValue(final String name, final Object value) {
    values.put(name, value);
  }

  public long getProgressNeeded() {
    return progressNeeded;
  }

  public void setProgressNeeded(final long progressNeeded) {
    this.progressNeeded = progressNeeded;
    setValue("progressNeeded", progressNeeded);
  }

  public boolean isNegated() {
    return negated;
  }

  public void setNegated(final boolean negated) {
    this.negated = negated;
    setValue("negated", negated);
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(final String description) {
    this.description = description == null ? "" : description;
  }

  public String getHiddenExpression() {
    return hiddenExpression;
  }

  public void setHiddenExpression(final String hiddenExpression) {
    this.hiddenExpression = hiddenExpression == null ? "" : hiddenExpression;
  }

  public boolean isAllowProgressDecreaseIfNotFulfilled() {
    return allowProgressDecreaseIfNotFulfilled;
  }

  public void setAllowProgressDecreaseIfNotFulfilled(final boolean allow) {
    allowProgressDecreaseIfNotFulfilled = allow;
    setValue("allowProgressDecreaseIfNotFulfilled", allow);
  }

  public void apply(final Quest.ConditionSettings settings) {
    if (settings == null) {
      return;
    }
    setProgressNeeded(settings.progressNeeded());
    setNegated(settings.negated());
    setDescription(settings.description());
    setHiddenExpression(settings.hiddenExpression());
    setAllowProgressDecreaseIfNotFulfilled(settings.allowProgressDecreaseIfNotFulfilled());
  }
}
