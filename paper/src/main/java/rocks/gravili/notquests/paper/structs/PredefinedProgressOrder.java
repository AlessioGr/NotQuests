package rocks.gravili.notquests.paper.structs;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

public class PredefinedProgressOrder {
  private final boolean firstToLast;
  private final boolean lastToFirst;
  private final List<Integer> custom;
  private PredefinedProgressOrder(final boolean firstToLast, final boolean lastToFirst, final List<Integer> custom) {
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

  public final @Nullable List<Integer> getCustomOrder() {
    return custom;
  }

  public static PredefinedProgressOrder firstToLast() {
    return new PredefinedProgressOrder(true, false, null);
  }
  public static PredefinedProgressOrder lastToFirst() {
    return new PredefinedProgressOrder(false, true, null);
  }

  public static PredefinedProgressOrder custom(final ArrayList<Integer> customOrder) {
    return new PredefinedProgressOrder(false, false, customOrder);
  }

  public static PredefinedProgressOrder fromConfiguration(final FileConfiguration fileConfiguration, final String initialPath) {
    if(fileConfiguration.contains(initialPath + ".firstToLast")) {
      return new PredefinedProgressOrder(fileConfiguration.getBoolean(initialPath + ".firstToLast"), false, null);
    } else if(fileConfiguration.contains(initialPath + ".lastToFirst")) {
      return new PredefinedProgressOrder(false, fileConfiguration.getBoolean(initialPath + ".lastToFirst"), null);
    } else if(fileConfiguration.contains(initialPath + ".custom")) {
      return new PredefinedProgressOrder(false, false, fileConfiguration.getIntegerList(initialPath + ".custom"));
    } else {
      return null;
    }
  }
  public void saveToConfiguration(final FileConfiguration fileConfiguration, final String initialPath){
    fileConfiguration.set(initialPath, null);
    if(firstToLast) {
      fileConfiguration.set(initialPath + ".firstToLast", true);
    } else if(lastToFirst) {
      fileConfiguration.set(initialPath + ".lastToFirst", true);
    } else if(custom != null && custom.size() > 0) {
      fileConfiguration.set(initialPath + ".custom", custom);
    }
  }
}
