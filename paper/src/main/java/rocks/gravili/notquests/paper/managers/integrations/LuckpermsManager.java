/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
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

import java.util.UUID;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import rocks.gravili.notquests.paper.NotQuests;

public class LuckpermsManager {
  private final NotQuests main;
  private final LuckPerms luckPerms;

  public LuckpermsManager(final NotQuests main) {
    this.main = main;
    RegisteredServiceProvider<LuckPerms> provider =
        Bukkit.getServicesManager().getRegistration(LuckPerms.class);
    if (provider != null) {
      luckPerms = provider.getProvider();
    } else {
      luckPerms = null;
    }
  }

  public void givePermission(final UUID uuid, final String permissionNode) {
    if (!permissionNode.isBlank()) {
      luckPerms
          .getUserManager()
          .modifyUser(
              uuid,
              user -> {
                // Add the permission
                user.data().add(Node.builder(permissionNode).value(true).build());
              });
    }
  }

  public void denyPermission(final UUID uuid, final String permissionNode) {
    if (!permissionNode.isBlank()) {
      luckPerms
          .getUserManager()
          .modifyUser(
              uuid,
              user -> {
                // Add the permission
                user.data().add(Node.builder(permissionNode).value(false).build());
              });
    }
  }

  public void unsetPermission(final UUID uuid, final String permissionNode) {
    if (!permissionNode.isBlank()) {
      luckPerms
          .getUserManager()
          .modifyUser(
              uuid,
              user -> {
                // Add the permission
                user.data().remove(Node.builder(permissionNode).build());
              });
    }
  }
}
