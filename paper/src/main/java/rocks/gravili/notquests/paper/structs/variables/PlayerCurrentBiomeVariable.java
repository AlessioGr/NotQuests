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

package rocks.gravili.notquests.paper.structs.variables;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.block.Biome;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;

public class PlayerCurrentBiomeVariable extends Variable<String> {
  public PlayerCurrentBiomeVariable(NotQuests main) {
    super(main);
  }

  @Override
  public String getValueInternally(QuestPlayer questPlayer, Object... objects) {
    if (questPlayer != null) {
      final Biome biome = questPlayer.getPlayer().getLocation().getBlock().getBiome();
      return RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME)
          .getKeyOrThrow(biome).getKey();
    } else {
      return null;
    }
  }

  @Override
  public boolean setValueInternally(String newValue, QuestPlayer questPlayer, Object... objects) {
    return false;
  }

  @Override
  public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
    List<String> possibleValues = new ArrayList<>();
    final var biomeRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);
    for (Biome biome : biomeRegistry) {
      possibleValues.add(biomeRegistry.getKeyOrThrow(biome).getKey());
    }
    return possibleValues;
  }

  @Override
  public String getPlural() {
    return "Biomes";
  }

  @Override
  public String getSingular() {
    return "Biome";
  }
}
