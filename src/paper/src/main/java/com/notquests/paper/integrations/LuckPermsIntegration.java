package com.notquests.paper.integrations;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import com.notquests.paper.NotQuests;

import java.util.UUID;

public class LuckPermsIntegration {
  private final LuckPerms luckPerms;

  public LuckPermsIntegration(final NotQuests main) {
    RegisteredServiceProvider<LuckPerms> provider =
        Bukkit.getServicesManager().getRegistration(LuckPerms.class);
    if (provider != null) {
      luckPerms = provider.getProvider();
    } else {
      luckPerms = null;
    }
  }

  public void givePermission(final UUID uuid, final String permissionNode) {
    if (luckPerms != null) {
      luckPerms.getUserManager().modifyUser(
          uuid, user -> user.data().add(Node.builder(permissionNode).value(true).build()));
    }
  }

  public void denyPermission(final UUID uuid, final String permissionNode) {
    if (luckPerms != null) {
      luckPerms.getUserManager().modifyUser(
          uuid, user -> user.data().add(Node.builder(permissionNode).value(false).build()));
    }
  }

  public void unsetPermission(final UUID uuid, final String permissionNode) {
    if (luckPerms != null) {
      luckPerms.getUserManager().modifyUser(
          uuid, user -> user.data().remove(Node.builder(permissionNode).build()));
    }
  }
}
