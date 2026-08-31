package com.notquests.paper.integrations;

import com.willfp.ecomobs.mob.EcoMob;
import com.willfp.ecomobs.mob.EcoMobs;
import com.willfp.ecomobs.mob.SpawnReason;
import org.bukkit.Location;

import java.util.Collection;

/**
 * Integration with EcoMobs (formerly EcoBosses). EcoMobs registers its mobs in the
 * {@link EcoMobs#INSTANCE} registry; we cache their ids for tab-completion and spawn them via the
 * the action's configured location/radius values.
 */
public class EcoMobsIntegration {
  public final Collection<String> getMobNames() {
    return EcoMobs.INSTANCE.values().stream().map(EcoMob::getID).toList();
  }

  public final boolean isEcoMob(final String mobToSpawnType) {
    return EcoMobs.INSTANCE.getByID(mobToSpawnType) != null;
  }

  public boolean spawnOneMob(final String mobToSpawnType, final Location location) {
    final EcoMob foundEcoMob = EcoMobs.INSTANCE.getByID(mobToSpawnType);
    if (foundEcoMob == null || location == null) {
      return false;
    }
    foundEcoMob.spawn(location, SpawnReason.COMMAND);
    return true;
  }
}
