/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.paper.managers.integrations;

import com.willfp.ecomobs.mob.EcoMob;
import com.willfp.ecomobs.mob.EcoMobs;
import com.willfp.ecomobs.mob.SpawnReason;
import java.util.ArrayList;
import java.util.Collection;
import org.bukkit.Location;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.actions.SpawnMobAction;

/**
 * Integration with EcoMobs (formerly EcoBosses). EcoMobs registers its mobs in the
 * {@link EcoMobs#INSTANCE} registry; we cache their ids for tab-completion and spawn them via the
 * {@link SpawnMobAction}.
 */
public class EcoMobsManager {
  private final NotQuests main;
  private final ArrayList<String> mobNames;

  public EcoMobsManager(final NotQuests main) {
    this.main = main;
    mobNames = new ArrayList<>();

    try {
      for (final EcoMob ecoMob : EcoMobs.INSTANCE.values()) {
        final String id = ecoMob.getID();
        mobNames.add(id);
        main.getLogManager().info("Registered EcoMob: <highlight>" + id);
      }
      main.getLogManager()
          .info("Registered <highlight>" + EcoMobs.INSTANCE.values().size() + "</highlight> EcoMobs.");
    } catch (final Exception ignored) {
      main.getLogManager().warn("Failed to load EcoMobs mobs. Are you on the latest version?");
    }
  }

  public final Collection<String> getMobNames() {
    return mobNames;
  }

  public final boolean isEcoMob(final String mobToSpawnType) {
    return EcoMobs.INSTANCE.getByID(mobToSpawnType) != null;
  }

  public void spawnMob(
      final String mobToSpawnType,
      final Location location,
      final int amount,
      final SpawnMobAction spawnMobAction) {
    final EcoMob foundEcoMob = EcoMobs.INSTANCE.getByID(mobToSpawnType);
    if (foundEcoMob == null) {
      main.getLogManager()
          .warn("Tried to spawn EcoMob, but the spawn " + mobToSpawnType + " was not found.");
      return;
    }
    if (location == null) {
      main.getLogManager().warn("Tried to spawn EcoMob, but the spawn location is invalid.");
      return;
    }
    if (location.getWorld() == null) {
      main.getLogManager().warn("Tried to spawn EcoMob, but the spawn location world is invalid.");
      return;
    }

    try {
      for (int i = 0; i < amount; i++) {
        foundEcoMob.spawn(
            spawnMobAction.getRandomLocationWithRadius(location), SpawnReason.COMMAND);
      }
    } catch (final Exception ignored) {
      main.getLogManager().warn("Failed to spawn EcoMob. Are you on the latest version?");
    }
  }
}
