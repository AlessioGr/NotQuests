package com.notquests.core.triggers;

import com.notquests.core.registry.NotQuestsRegistry.Triggers;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** A configured quest trigger. */
public final class Trigger implements Triggers.Data, Triggers.Draft {
  private final int id;
  private final String typeId;
  private final Map<String, Object> values;

  public Trigger(final int id, final String typeId, final Triggers.Data data) {
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

  public Trigger data() {
    return this;
  }

  public Map<String, Object> values() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(values));
  }

  @Override
  public void copyTo(final Triggers.Draft trigger) {
    values.forEach(trigger::setValue);
  }

  public Object value(final String name) {
    return values.get(name);
  }

  @Override
  public String text(final String name) {
    final Object value = value(name);
    return value == null ? "" : String.valueOf(value);
  }

  @Override
  public int integer(final String name, final int fallback) {
    final Object value = value(name);
    return value instanceof Number number ? number.intValue() : fallback;
  }

  public int integer(final String name) {
    return integer(name, 0);
  }

  public void setValue(final String name, final Object value) {
    values.put(name, value);
  }
}
