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

import io.lumine.mythic.api.MythicPlugin;
import io.lumine.mythic.api.MythicProvider;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import org.bukkit.Location;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.actions.SpawnMobAction;

public class MythicMobsManager {
  private final NotQuests main;
  private final MythicPlugin mythicPlugin;

  public MythicMobsManager(final NotQuests main) {
    this.main = main;
    this.mythicPlugin = MythicProvider.get();
  }

  public final MythicPlugin getMythicPlugin() {
    return mythicPlugin;
  }

  public final Collection<String> getMobNames() {
    return mythicPlugin.getMobManager().getMobNames();
  }

  public final Collection<String> getFactionNames(final String prefix) {
    final Collection<String> factionNames = new ArrayList<>();
    for(final MythicMob mobType : mythicPlugin.getMobManager().getMobTypes()){
      if(!factionNames.contains(mobType.getFaction())) {
        if(mobType.getFaction() == null){
          factionNames.add(prefix+ "none");
        }else {
          factionNames.add(prefix+ mobType.getFaction());
        }
      }
    }
    return factionNames;
  }

  public void spawnMob(
      final String mobToSpawnType,
      final Location location,
      final int amount,
      final SpawnMobAction spawnMobAction) {
    final Optional<MythicMob> foundMythicMob =
        mythicPlugin.getMobManager().getMythicMob(mobToSpawnType);
    if (foundMythicMob.isEmpty()) {
      main.getLogManager()
          .warn(
              "Tried to spawn mythic mob, but the mythic mob "
                  + mobToSpawnType
                  + " was not found.");
      return;
    }
    if (location == null) {
      main.getLogManager().warn("Tried to spawn mythic mob, but the spawn location is invalid.");
      return;
    }
    if (location.getWorld() == null) {
      main.getLogManager()
          .warn("Tried to spawn mythic mob, but the spawn location world is invalid.");
      return;
    }

    for (int i = 0; i < amount; i++) {
      foundMythicMob
          .get()
          .spawn(BukkitAdapter.adapt(spawnMobAction.getRandomLocationWithRadius(location)), 1);
    }
  }

  public final boolean isMythicMob(final String mobToSpawnType) {
    return mythicPlugin.getMobManager().getMythicMob(mobToSpawnType).isPresent();
  }
}
