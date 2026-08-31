package com.notquests.core.structs;

import com.notquests.core.config.YamlConfig;

import java.util.ArrayList;
import java.util.List;

public class PredefinedProgressOrder {
  private final boolean firstToLast;
  private final boolean lastToFirst;
  private final List<String> custom;
  private PredefinedProgressOrder(final boolean firstToLast, final boolean lastToFirst, final List<String> custom) {
    this.firstToLast = firstToLast;
    this.lastToFirst = lastToFirst;
    this.custom = custom;
  }

  public final boolean isFirstToLast() {
    return firstToLast;
  }

  public final boolean isLastToFirst() {
    return lastToFirst;
  }

  public final List<String> getCustomOrder() {
    return custom;
  }

  public static PredefinedProgressOrder firstToLast() {
    return new PredefinedProgressOrder(true, false, null);
  }
  public static PredefinedProgressOrder lastToFirst() {
    return new PredefinedProgressOrder(false, true, null);
  }

  public static PredefinedProgressOrder custom(final ArrayList<String> customOrder) {
    return new PredefinedProgressOrder(false, false, customOrder);
  }

  public static PredefinedProgressOrder fromString(final String progressOrder) {
    if (progressOrder == null || progressOrder.isBlank()) {
      return null;
    }
    final String trimmed = progressOrder.trim();
    if (trimmed.equalsIgnoreCase("firstToLast")) {
      return firstToLast();
    }
    if (trimmed.equalsIgnoreCase("lastToFirst")) {
      return lastToFirst();
    }
    if (trimmed.regionMatches(true, 0, "custom", 0, "custom".length())) {
      final String customOrder = trimmed.substring("custom".length()).trim();
      if (customOrder.isBlank()) {
        return null;
      }
      final ArrayList<String> entries = new ArrayList<>();
      for (final String entry : customOrder.split("[,\\s]+")) {
        if (!entry.isBlank()) {
          entries.add(entry);
        }
      }
      return entries.isEmpty() ? null : custom(entries);
    }
    return null;
  }

  public static PredefinedProgressOrder fromConfiguration(final YamlConfig configuration, final String initialPath) {
    if(configuration.contains(initialPath + ".firstToLast")) {
      return new PredefinedProgressOrder(configuration.getBoolean(initialPath + ".firstToLast"), false, null);
    } else if(configuration.contains(initialPath + ".lastToFirst")) {
      return new PredefinedProgressOrder(false, configuration.getBoolean(initialPath + ".lastToFirst"), null);
    } else if(configuration.contains(initialPath + ".custom")) {
      return new PredefinedProgressOrder(false, false, configuration.getStringList(initialPath + ".custom"));
    } else {
      return null;
    }
  }
  public void saveToConfiguration(final YamlConfig configuration, final String initialPath){
    configuration.set(initialPath, null);
    if(firstToLast) {
      configuration.set(initialPath + ".firstToLast", true);
    } else if(lastToFirst) {
      configuration.set(initialPath + ".lastToFirst", true);
    } else if(custom != null && custom.size() > 0) {
      configuration.set(initialPath + ".custom", custom);
    }
  }

  public final String getReadableString() {
    return isFirstToLast()
        ? "First to last"
        : (
            isLastToFirst()
                ? "Last to first" : (
                (getCustomOrder() != null && !getCustomOrder().isEmpty())
                    ? "Custom: " + getCustomOrder().toString()
                    : "None (2)"
            )
        )
        ;
  }

  public final String toConfigString() {
    if (isFirstToLast()) {
      return "firstToLast";
    }
    if (isLastToFirst()) {
      return "lastToFirst";
    }
    if (getCustomOrder() != null && !getCustomOrder().isEmpty()) {
      return "custom " + String.join(",", getCustomOrder());
    }
    return "";
  }

  public static String toConfigString(final PredefinedProgressOrder progressOrder) {
    return progressOrder == null ? "" : progressOrder.toConfigString();
  }
}
