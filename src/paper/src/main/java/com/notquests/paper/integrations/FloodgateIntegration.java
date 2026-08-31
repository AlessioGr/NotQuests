package com.notquests.paper.integrations;

import org.geysermc.floodgate.api.FloodgateApi;

import com.notquests.paper.NotQuests;

import java.util.UUID;

public class FloodgateIntegration {
  private final NotQuests main;

  public FloodgateIntegration(final NotQuests main) {
    this.main = main;
  }

  public final boolean isPlayerOnFloodgate(final UUID playerUUID) {
    return FloodgateApi.getInstance().isFloodgatePlayer(playerUUID);
  }
}
