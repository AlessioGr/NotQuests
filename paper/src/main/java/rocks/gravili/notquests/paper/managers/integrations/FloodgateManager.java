package rocks.gravili.notquests.paper.managers.integrations;

import java.util.UUID;
import org.geysermc.floodgate.api.FloodgateApi;
import rocks.gravili.notquests.paper.NotQuests;

public class FloodgateManager {
  private final NotQuests main;

  public FloodgateManager(final NotQuests main) {
    this.main = main;
  }

  public final boolean isPlayerOnFloodgate(final UUID playerUUID) {
    return FloodgateApi.getInstance().isFloodgatePlayer(playerUUID);
  }
}
