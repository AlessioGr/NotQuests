package rocks.gravili.notquests.paper.managers.integrations;

import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nullable;
import rocks.gravili.notquests.paper.NotQuests;

public class Integration {

  protected final NotQuests main;
  private final String exactName;

  private Supplier<Boolean> runWhenEnabled; //Return false -> cancel the enabling
  private Runnable runWhenEnabledLate; //This runs ONLY when enabled late via the plugin enable event

  private Runnable runAfterDataLoad; //Only runs if enabled

  private Runnable runWhenRegisteringEventsOnTime;

  private Supplier<Boolean> enableCondition; //Return false -> dont enable and dont run runWhenEnabled

  private Runnable runWhenEnableConditionFalse; //Runs only if the enableCondition when checked is false
  private Runnable runWhenEnablingFailed; //Runs only if the plugin is not found or not enabled when trying to enable it

  public Integration(final NotQuests main, final String exactName) {
    this.main = main;
    this.exactName = exactName;
  }

  public Integration(NotQuests main, String exactName, final Integration integration) {
    this.main = main;
    this.exactName = exactName;
    this.runWhenEnabled = integration.runWhenEnabled;
    this.runWhenEnabledLate = integration.runWhenEnabledLate;
    this.runAfterDataLoad = integration.runAfterDataLoad;
    this.runWhenRegisteringEventsOnTime = integration.runWhenRegisteringEventsOnTime;
    this.enableCondition = integration.enableCondition;
    this.runWhenEnableConditionFalse = integration.runWhenEnableConditionFalse;
    this.runWhenEnablingFailed = integration.runWhenEnablingFailed;
  }

  public final String getExactName() {
    return exactName;
  }

  public final Integration setEnableCondition(final Supplier<Boolean> enableCondition) {
    this.enableCondition = enableCondition;
    return this;
  }

  public final Integration setRunWhenEnabled(final Supplier<Boolean> runWhenEnabled) {
    this.runWhenEnabled = runWhenEnabled;
    return this;
  }

  /**
   * This runs ADDITIONALLY and afterwards of runWhenEnabled.
   */
  public final Integration setRunAlsoWhenEnabledLate(final Runnable runWhenEnabledLate) {
    this.runWhenEnabledLate = runWhenEnabledLate;
    return this;
  }

  public final Integration setRunWhenRegisteringEventsOnTime(final Runnable runWhenRegisteringEvents) {
    this.runWhenRegisteringEventsOnTime = runWhenRegisteringEvents;
    return this;
  }
  protected final Runnable getRunWhenRegisteringEventsOnTime() {
    return runWhenRegisteringEventsOnTime;
  }

  protected final Runnable getRunWhenEnabledLate() {
    return runWhenEnabledLate;
  }

  public final Integration setRunWhenEnableConditionFalse(final Runnable runWhenEnableConditionFalse) {
    this.runWhenEnableConditionFalse = runWhenEnableConditionFalse;
    return this;
  }

  public final Integration setRunWhenEnablingFailed(final Runnable runWhenEnablingFailed) {
    this.runWhenEnablingFailed = runWhenEnablingFailed;
    return this;
  }

  public final Integration setRunAfterDataLoad(final Runnable runAfterDataLoad) {
    this.runAfterDataLoad = runAfterDataLoad;
    return this;
  }

  public final boolean checkEnableCondition() {
    if (enableCondition != null) {
      return enableCondition.get();
    }
    return true;
  }

  /**
   * This does the same as enable() but runs the runWhenEnabledLate runnable at the end as well.
   *
   * @return the EnabledIntegration instance (can be null if enabling failed)
   */
  public final @Nullable EnabledIntegration enableLate() {
    final Plugin plugin = Bukkit.getPluginManager().getPlugin(getExactName());
    if(plugin == null) {
      if(runWhenEnablingFailed != null) {
        runWhenEnablingFailed.run();
      }
      return null;
    }
    return enable(plugin, true);
  }
  public final @Nullable EnabledIntegration enable() {
    final Plugin plugin = Bukkit.getPluginManager().getPlugin(getExactName());
    if(plugin == null) {
      if(runWhenEnablingFailed != null) {
        runWhenEnablingFailed.run();
      }
      return null;
    }
    return enable(plugin, false);
  }

  public final @Nullable EnabledIntegration enable(@NonNull final Plugin plugin, final boolean late) {
    if(!plugin.isEnabled()) {
      if(runWhenEnablingFailed != null) {
        runWhenEnablingFailed.run();
      }
      return null;
    }
    if (checkEnableCondition()) {
      if (runWhenEnabled != null) {
        if(!runWhenEnabled.get()) {
          return null;
        }
      }
      if(late) {
        if(runWhenEnabledLate != null) {
          runWhenEnabledLate.run();
        }
        main.getLogManager().info( getExactName() + " found. Enabled " + getExactName() + " support (late)!");
      } else {
        main.getLogManager().info( getExactName() + " found. Enabled " + getExactName() + " support!");
      }

      return new EnabledIntegration(main, exactName, plugin, this);
    } else {
      if (runWhenEnableConditionFalse != null) {
        runWhenEnableConditionFalse.run();
      }
    }
    return null;
  }

  protected final Runnable getRunAfterDataLoad() {
    return runAfterDataLoad;
  }
}
