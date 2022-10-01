package rocks.gravili.notquests.paper.managers.integrations;

import org.bukkit.plugin.Plugin;
import rocks.gravili.notquests.paper.NotQuests;

public class EnabledIntegration extends Integration {

  private final String versionString;

  public EnabledIntegration(final NotQuests main, final String exactName, final Plugin plugin) {
    super(main, exactName);
    this.versionString = plugin.getDescription().getVersion();
  }

  public EnabledIntegration(final NotQuests main, final String exactName, final Plugin plugin, final Integration integration) {
    super(main, exactName, integration);
    this.versionString = plugin.getDescription().getVersion();
  }

  public final String getVersionString() {
    return versionString;
  }

  public void dataLoaded() {
    if (super.getRunAfterDataLoad() != null) {
      super.getRunAfterDataLoad().run();
    }
  }

  /**
   * This runs when registering events (ON TIME). Doesn't run when it's registered late.
   **/
  public void registeringEventsOnTime() {
    if (super.getRunWhenRegisteringEventsOnTime() != null) {
      main.getLogManager().info("Registering events for " + getExactName() + " (on time)...");
      super.getRunWhenRegisteringEventsOnTime().run();
    }
  }
 }
