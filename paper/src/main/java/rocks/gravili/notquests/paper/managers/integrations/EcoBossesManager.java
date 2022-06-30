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

import com.willfp.ecobosses.EcoBossesPlugin;
import com.willfp.ecobosses.bosses.Bosses;
import com.willfp.ecobosses.bosses.EcoBoss;
import java.util.ArrayList;
import java.util.Collection;
import org.bukkit.Location;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.actions.SpawnMobAction;

public class EcoBossesManager {
  private final NotQuests main;
  private final ArrayList<String> bossNames;
  private final EcoBossesPlugin ecoBossesPlugin;

  public EcoBossesManager(final NotQuests main) {
    this.main = main;
    ecoBossesPlugin = EcoBossesPlugin.getInstance();

    bossNames = new ArrayList<>();

    try {
      for (EcoBoss ecoBoss : Bosses.values()) {
        try {
          // bossNames.add(ecoBoss.getId());
          // main.getLogManager().info("Registered EcoBoss: <highlight>" + ecoBoss.getId());
          final String id = ecoBoss.getId();

          bossNames.add(id);
          main.getLogManager().info("Registered EcoBoss: <highlight>" + id);
        } catch (Exception e) {
          final String id = (String) ecoBoss.getClass().getMethod("getName").invoke(ecoBoss);
          bossNames.add(id);
          main.getLogManager().info("Registered EcoBoss: <highlight>" + id);
        }
      }
      main.getLogManager()
          .info("Registered <highlight>" + Bosses.values().size() + "</highlight> EcoBosses.");

    } catch (Exception ignored) {
      main.getLogManager().warn("Failed to add EcoBosses mobs. Are you on the latest version?");
    }
  }

  public final Collection<String> getBossNames() {
    return bossNames;
  }

  public final boolean isEcoBoss(final String bossToSpawnType) {
    return Bosses.getByID(bossToSpawnType) != null;
  }

  public void spawnMob(
      String mobToSpawnType, Location location, int amount, final SpawnMobAction spawnMobAction) {
    EcoBoss foundEcoBoss = Bosses.getByID(mobToSpawnType);
    if (foundEcoBoss == null) {
      main.getLogManager()
          .warn("Tried to spawn EcoBoss, but the spawn " + mobToSpawnType + " was not found.");
      return;
    }
    if (location == null) {
      main.getLogManager().warn("Tried to spawn EcoBoss, but the spawn location is invalid.");
      return;
    }
    if (location.getWorld() == null) {
      main.getLogManager().warn("Tried to spawn EcoBoss, but the spawn location world is invalid.");
      return;
    }

    try {
      for (int i = 0; i < amount; i++) {
        foundEcoBoss.spawn(spawnMobAction.getRandomLocationWithRadius(location));
      }
    } catch (NoSuchMethodError e) {
      try {
        for (int i = 0; i < amount; i++) {
          foundEcoBoss
              .getClass()
              .getMethod("spawn", Location.class)
              .invoke(foundEcoBoss, spawnMobAction.getRandomLocationWithRadius(location));
        }
      } catch (Exception ignored) {
        main.getLogManager().warn("Failed to add EcoBosses mobs. Are you on the latest version?");
      }
    }
  }
}
