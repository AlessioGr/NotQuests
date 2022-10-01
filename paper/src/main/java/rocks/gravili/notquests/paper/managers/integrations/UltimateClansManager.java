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

import me.ulrich.clans.interfaces.UClans;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;

public class UltimateClansManager {
  private final NotQuests main;
  private final UClans api;

  public UltimateClansManager(final NotQuests main) {
    this.main = main;
    api = (UClans) Bukkit.getPluginManager().getPlugin("UltimateClans");
  }

  public final UClans getApi() {
    return api;
  }

  public final boolean isInClanWithMinLevel(final Player player, final long minLevel) {
    return api.getPlayerAPI().getPlayerClan(player.getUniqueId()) != null
        && getClanLevel(player) >= minLevel;
  }

  public final int getClanLevel(final Player player) {
    if (api.getPlayerAPI().getPlayerClan(player.getUniqueId()) == null) {
      return 0;
    }
    return api.getPlayerAPI().getPlayerClan(player.getUniqueId()).getLevel();
  }

  public final void setClanLevel(final Player player, final int newLevel) {
    if (api.getPlayerAPI().getPlayerClan(player.getUniqueId()) == null) {
      return;
    }
    api.getPlayerAPI().getPlayerClan(player.getUniqueId()).setLevel(newLevel);
  }
}
