package com.notquests.paper.integrations;

import io.lumine.mythic.api.MythicPlugin;
import io.lumine.mythic.api.MythicProvider;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;
import org.bukkit.Location;

import com.notquests.paper.NotQuests;

import java.util.Collection;
import java.util.Optional;

public class MythicMobsIntegration {
  private final NotQuests main;
  private final MythicPlugin mythicPlugin;

  public MythicMobsIntegration(final NotQuests main) {
    this.main = main;
    this.mythicPlugin = MythicProvider.get();
  }

  public final MythicPlugin getMythicPlugin() {
    return mythicPlugin;
  }

  public final Collection<String> getMobNames() {
    return mythicPlugin.getMobManager().getMobNames();
  }

  public boolean spawnOneMob(final String mobToSpawnType, final Location location) {
    final Optional<MythicMob> foundMythicMob =
        mythicPlugin.getMobManager().getMythicMob(mobToSpawnType);
    if (foundMythicMob.isEmpty() || location == null) {
      return false;
    }
    foundMythicMob.get().spawn(BukkitAdapter.adapt(location), 1);
    return true;
  }

  public final boolean isMythicMob(final String mobToSpawnType) {
    return mythicPlugin.getMobManager().getMythicMob(mobToSpawnType).isPresent();
  }
}
