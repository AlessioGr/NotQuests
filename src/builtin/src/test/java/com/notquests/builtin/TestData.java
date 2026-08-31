package com.notquests.builtin;

import com.notquests.core.items.ItemSelection;
import com.notquests.core.platform.NQLocation;
import com.notquests.core.registry.NotQuestsRegistry.Actions;
import com.notquests.core.registry.NotQuestsRegistry.Conditions;
import com.notquests.core.registry.NotQuestsRegistry.Objectives;
import com.notquests.core.registry.NotQuestsRegistry.Triggers;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Map-backed fixture for tests that construct several kinds of configured data. */
public final class TestData
    implements Actions.Data,
        Conditions.Data,
        Objectives.Data,
        Triggers.Data {
  private final Map<String, Object> values;

  public TestData(final Map<String, ?> values) {
    this.values = new LinkedHashMap<>();
    if (values != null) {
      values.forEach(this.values::put);
    }
  }

  public void setValue(final String name, final Object value) { values.put(name, value); }

  @Override public Object value(final String name) { return values.get(name); }
  @Override public <V> V value(final String name, final Class<V> type) { final Object value = value(name); return type.isInstance(value) ? type.cast(value) : null; }
  @Override public String text(final String name) { final Object value = value(name); return value == null ? "" : String.valueOf(value); }
  @Override public int integer(final String name) { return integer(name, 0); }
  @Override public int integer(final String name, final int fallback) { final Object value = value(name); return value instanceof Number number ? number.intValue() : fallback; }
  @Override public double number(final String name, final double fallback) { final Object value = value(name); return value instanceof Number number ? number.doubleValue() : fallback; }
  @Override public boolean flag(final String name) { return Boolean.TRUE.equals(value(name)); }
  @Override public ItemSelection itemSelection(final String name) { return value(name) instanceof ItemSelection selection ? selection : null; }
  @Override public Duration duration(final String name, final Duration fallback) { return value(name) instanceof Duration duration ? duration : fallback; }
  @Override public NQLocation location(final String name) { return value(name) instanceof NQLocation location ? location : null; }
  @Override public void copyTo(final Actions.Draft action) { values.forEach(action::setValue); }
  @Override public void copyTo(final Conditions.Draft condition) { values.forEach(condition::setValue); }
  @Override public void copyTo(final Objectives.Draft objective) { values.forEach(objective::setValue); }
  @Override public void copyTo(final Triggers.Draft trigger) { values.forEach(trigger::setValue); }
}
