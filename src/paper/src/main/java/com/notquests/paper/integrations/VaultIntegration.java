package com.notquests.paper.integrations;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.plugin.RegisteredServiceProvider;

import com.notquests.paper.NotQuests;

public class VaultIntegration {
  private final NotQuests main;

  // Vault
  private Economy econ = null;
  private Permission perms = null;
  private Chat chat = null;

  public VaultIntegration(final NotQuests main) {
    this.main = main;
  }

  /**
   * Sets up the Chat from the Vault plugin.
   *
   * @return if vault chat has been set up successfully
   */
  public boolean setupChat() {
    RegisteredServiceProvider<Chat> rsp =
        main.getMain().getServer().getServicesManager().getRegistration(Chat.class);
    if (rsp != null) {
      chat = rsp.getProvider();
      return true;
    } else {
      return false;
    }
  }

  /**
   * Sets up the Permissions from the Vault plugin.
   *
   * @return if permissions from the vault plugin have been set up successfully
   */
  public boolean setupPermissions() {
    RegisteredServiceProvider<Permission> rsp =
        main.getMain().getServer().getServicesManager().getRegistration(Permission.class);
    if (rsp != null) {
      perms = rsp.getProvider();
      return true;
    } else {
      return false;
    }
  }

  /**
   * Sets up the Economy from the Vault plugin.
   *
   * @return if the economy has been set up successfully and if Vault has been found
   */
  public boolean setupEconomy() {
    if (main.getMain().getServer().getPluginManager().getPlugin("Vault") == null) {
      return false;
    }
    RegisteredServiceProvider<Economy> rsp =
        main.getMain().getServer().getServicesManager().getRegistration(Economy.class);
    if (rsp == null) {
      return false;
    }
    econ = rsp.getProvider();
    return true;
  }

  /**
   * Returns an instance of Economy which has been set up in setupEconomy()
   *
   * @return an instance of Economy which has been set up in setupEconomy()
   */
  public Economy getEconomy() {
    return econ;
  }
}
