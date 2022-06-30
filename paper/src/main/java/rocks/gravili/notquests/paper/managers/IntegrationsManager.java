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

package rocks.gravili.notquests.paper.managers;

import java.util.ArrayList;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.event.server.PluginEnableEvent;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.events.hooks.BetonQuestEvents;
import rocks.gravili.notquests.paper.events.hooks.CitizensEvents;
import rocks.gravili.notquests.paper.events.hooks.EcoBossesEvents;
import rocks.gravili.notquests.paper.events.hooks.EliteMobsEvents;
import rocks.gravili.notquests.paper.events.hooks.JobsRebornEvents;
import rocks.gravili.notquests.paper.events.hooks.MythicMobsEvents;
import rocks.gravili.notquests.paper.events.hooks.ProjectKorraEvents;
import rocks.gravili.notquests.paper.events.hooks.SlimefunEvents;
import rocks.gravili.notquests.paper.events.hooks.TownyEvents;
import rocks.gravili.notquests.paper.events.hooks.UltimateJobsEvents;
import rocks.gravili.notquests.paper.managers.integrations.EcoBossesManager;
import rocks.gravili.notquests.paper.managers.integrations.LuckpermsManager;
import rocks.gravili.notquests.paper.managers.integrations.MythicMobsManager;
import rocks.gravili.notquests.paper.managers.integrations.ProjectKorraManager;
import rocks.gravili.notquests.paper.managers.integrations.SlimefunManager;
import rocks.gravili.notquests.paper.managers.integrations.UltimateClansManager;
import rocks.gravili.notquests.paper.managers.integrations.VaultManager;
import rocks.gravili.notquests.paper.managers.integrations.WorldEditManager;
import rocks.gravili.notquests.paper.managers.integrations.betonquest.BetonQuestManager;
import rocks.gravili.notquests.paper.managers.integrations.citizens.CitizensManager;
import rocks.gravili.notquests.paper.placeholders.QuestPlaceholders;

public class IntegrationsManager {
  private final NotQuests main;
  private final ArrayList<String> enabledIntegrations = new ArrayList<>();
  // Booleans
  private boolean vaultEnabled = false;
  private boolean citizensEnabled = false;
  private boolean slimefunEnabled = false;
  private boolean townyEnabled = false;
  private boolean jobsRebornEnabled = false;
  private boolean projectKorraEnabled = false;
  private boolean ultimateClansEnabled = false;
  private boolean luckpermsEnabled = false;
  private boolean worldEditEnabled = false;
  private boolean eliteMobsEnabled = false;
  private boolean placeholderAPIEnabled = false;
  private boolean betonQuestEnabled = false;
  private boolean mythicMobsEnabled = false;
  private boolean ecoBossesEnabled = false;
  private boolean ultimateJobsEnabled = false;
  // Managers
  private VaultManager vaultManager;
  private MythicMobsManager mythicMobsManager;
  private CitizensManager citizensManager;
  private BetonQuestManager betonQuestManager;
  private WorldEditManager worldEditManager;
  private SlimefunManager slimefunManager;
  private LuckpermsManager luckpermsManager;
  private ProjectKorraManager projectKorraManager;
  private UltimateClansManager ultimateClansManager;
  private EcoBossesManager ecoBossesManager;

  public IntegrationsManager(final NotQuests main) {
    this.main = main;
  }

  public final ArrayList<String> getEnabledIntegrations() {
    return enabledIntegrations;
  }

  public void enableIntegrations() {

    // UltimateJobs
    if (main.getConfiguration().isIntegrationUltimateJobsEnabled()) {
      if (Bukkit.getPluginManager().getPlugin("UltimateJobs") != null) {
        ultimateJobsEnabled = true;
        enabledIntegrations.add("UltimateJobs");
        main.getLogManager().info("UltimateJobs found! Enabling UltimateJobs support...");
      }
    }

    // EcoBosses
    if (main.getConfiguration().isIntegrationEcoBossesEnabled()) {
      if (Bukkit.getPluginManager().getPlugin("EcoBosses") != null) {
        ecoBossesEnabled = true;
        enabledIntegrations.add("EcoBosses");
        main.getLogManager()
            .info(
                "EcoBosses found! Enabling EcoBosses support... Bosses will be loaded in 10 seconds, because they are not loaded when the plugin starts. Don't blame me");
        Bukkit.getScheduler()
            .scheduleSyncDelayedTask(
                main.getMain(),
                new Runnable() {
                  @Override
                  public void run() {
                    ecoBossesManager = new EcoBossesManager(main);
                    main.getDataManager().loadStandardCompletions();
                  }
                },
                200L);
      }
    }

    // PlaceholderAPI
    if (main.getConfiguration().isIntegrationPlaceholderAPIEnabled()) {
      if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
        placeholderAPIEnabled = true;
        enabledIntegrations.add("PlaceholderAPI");
        main.getLogManager().info("PlaceholderAPI found! Enabling PlaceholderAPI support...");
      }
    }

    // Vault Hook
    if (main.getConfiguration().isIntegrationVaultEnabled()) {
      if (main.getMain().getServer().getPluginManager().getPlugin("Vault") != null) {
        vaultManager = new VaultManager(main);
        if (!vaultManager.setupEconomy()) {
          main.getLogManager()
              .warn(
                  "Vault Dependency not found! Some features have been disabled. I recommend you to install Vault for the best experience.");
        } else {
          vaultManager.setupPermissions();
          vaultManager.setupChat();
          vaultEnabled = true;
          enabledIntegrations.add("Vault");
          main.getLogManager().info("Vault found! Enabling Vault support...");
        }
      }
    }

    // MythicMobs Hook
    if (main.getConfiguration().isIntegrationMythicMobsEnabled()) {
      if (main.getMain().getServer().getPluginManager().getPlugin("MythicMobs") != null
          && Objects.requireNonNull(
                  main.getMain().getServer().getPluginManager().getPlugin("MythicMobs"))
              .isEnabled()) {
        mythicMobsEnabled = true;
        enabledIntegrations.add("MythicMobs");
        main.getLogManager().info("MythicMobs found! Enabling MythicMobs support...");
        mythicMobsManager = new MythicMobsManager(main);
      }
    }

    // EliteMobs Hook
    if (main.getConfiguration().isIntegrationEliteMobsEnabled()) {
      if (main.getMain().getServer().getPluginManager().getPlugin("EliteMobs") != null
          && Objects.requireNonNull(
                  main.getMain().getServer().getPluginManager().getPlugin("EliteMobs"))
              .isEnabled()) {
        eliteMobsEnabled = true;
        enabledIntegrations.add("EliteMobs");
        main.getLogManager().info("EliteMobs found! Enabling EliteMobs support...");
      }
    }

    // BetonQuest Hook
    if (main.getConfiguration().isIntegrationBetonQuestEnabled()) {
      if (main.getMain().getServer().getPluginManager().getPlugin("BetonQuest") != null
          && Objects.requireNonNull(
                  main.getMain().getServer().getPluginManager().getPlugin("BetonQuest"))
              .isEnabled()) {
        betonQuestEnabled = true;
        enabledIntegrations.add("BetonQuest");
        main.getLogManager().info("BetonQuest found! Enabling BetonQuest support...");
        betonQuestManager = new BetonQuestManager(main);
        main.getMain()
            .getServer()
            .getPluginManager()
            .registerEvents(new BetonQuestEvents(main), main.getMain());
      }
    }

    // WorldEdit
    if (main.getConfiguration().isIntegrationWorldEditEnabled()) {
      if (main.getMain().getServer().getPluginManager().getPlugin("WorldEdit") != null) {
        worldEditManager = new WorldEditManager(main);
        main.getLogManager().info("WorldEdit found! Enabling WorldEdit support...");
        worldEditEnabled = true;
        enabledIntegrations.add("WorldEdit");
      }
    }

    // Enable 'Citizens' integration. If it's not found, it will just disable some NPC features
    // which can mostly be replaced by armor stands
    if (main.getConfiguration().isIntegrationCitizensEnabled()) {
      if (main.getMain().getServer().getPluginManager().getPlugin("Citizens") == null
          || !Objects.requireNonNull(
                  main.getMain().getServer().getPluginManager().getPlugin("Citizens"))
              .isEnabled()) {
        main.getLogManager()
            .info(
                "Citizens Dependency not found! Congratulations! In NotQuests, you can use armor stands instead of Citizens NPCs");

      } else {
        citizensEnabled = true;
        enabledIntegrations.add("Citizens");
        citizensManager = new CitizensManager(main);
        main.getLogManager().info("Citizens found! Enabling Citizens support...");
      }
    }

    // Enable 'SlimeFun' integration.
    if (main.getConfiguration().isIntegrationSlimeFunEnabled()) {
      if (main.getMain().getServer().getPluginManager().getPlugin("Slimefun") == null
          || !Objects.requireNonNull(
                  main.getMain().getServer().getPluginManager().getPlugin("Slimefun"))
              .isEnabled()) {
        slimefunEnabled = false;
      } else {
        slimefunEnabled = true;
        enabledIntegrations.add("Slimefun");
        slimefunManager = new SlimefunManager(main);
        main.getLogManager().info("SlimeFun found! Enabling SlimeFun support...");
      }
    }

    // LuckPerms
    if (main.getConfiguration().isIntegrationLuckPermsEnabled()) {
      if (main.getMain().getServer().getPluginManager().getPlugin("LuckPerms") != null) {
        luckpermsManager = new LuckpermsManager(main);
        luckpermsEnabled = true;
        enabledIntegrations.add("LuckPerms");
        main.getLogManager().info("LuckPerms found! Enabling LuckPerms support...");
      }
    }

    // UltimateClans
    if (main.getConfiguration().isIntegrationUltimateClansEnabled()) {
      if (main.getMain().getServer().getPluginManager().getPlugin("UltimateClans") != null) {
        ultimateClansEnabled = true;
        enabledIntegrations.add("UltimateClans");
        ultimateClansManager = new UltimateClansManager(main);
        main.getLogManager().info("UltimateClans found! Enabling UltimateClans support...");
      }
    }

    // Towny
    if (main.getConfiguration().isIntegrationTownyEnabled()) {
      if (main.getMain().getServer().getPluginManager().getPlugin("Towny") != null) {
        townyEnabled = true;
        enabledIntegrations.add("Towny");
        main.getLogManager().info("Towny found! Enabling Towny support...");
      }
    }

    // JobsReborn
    if (main.getConfiguration().isIntegrationJobsRebornEnabled()) {
      if (main.getMain().getServer().getPluginManager().getPlugin("Jobs") != null) {
        jobsRebornEnabled = true;
        enabledIntegrations.add("JobsReborn");
        main.getLogManager().info("Jobs Reborn found! Enabling Jobs Reborn support...");
      }
    }

    // Project Korra
    if (main.getConfiguration().isIntegrationJobsRebornEnabled()) {
      if (main.getMain().getServer().getPluginManager().getPlugin("ProjectKorra") != null) {
        projectKorraManager = new ProjectKorraManager(main);
        projectKorraEnabled = true;
        enabledIntegrations.add("ProjectKorra");
        main.getLogManager().info("Project Korra found! Enabling Project Korra support...");
      }
    }
  }

  public void onDisable() {
    if (isCitizensEnabled()) {
      citizensManager.onDisable();
    }
  }

  public void enableIntegrationsAfterDataLoad() {
    if (main.getConfiguration().isIntegrationPlaceholderAPIEnabled()
        && !isPlaceholderAPIEnabled()) {
      if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
        placeholderAPIEnabled = true;
        enabledIntegrations.add("PlaceholderAPI");
        main.getLogManager().info("PlaceholderAPI found! Enabling PlaceholderAPI support...");
        new QuestPlaceholders(main).register();
      }
    } else if (main.getConfiguration().isIntegrationPlaceholderAPIEnabled()) {
      new QuestPlaceholders(main).register();
    }
  }

  public void registerEvents() {
    if (isCitizensEnabled()) {
      main.getMain()
          .getServer()
          .getPluginManager()
          .registerEvents(new CitizensEvents(main), main.getMain());
      citizensManager.registerAnyCitizensCommands();
    }

    if (isMythicMobsEnabled()) {
      main.getMain()
          .getServer()
          .getPluginManager()
          .registerEvents(new MythicMobsEvents(main), main.getMain());
    }

    if (isEliteMobsEnabled()) {
      main.getMain()
          .getServer()
          .getPluginManager()
          .registerEvents(new EliteMobsEvents(main), main.getMain());
    }

    if (isTownyEnabled()) {
      main.getMain()
          .getServer()
          .getPluginManager()
          .registerEvents(new TownyEvents(main), main.getMain());
    }

    if (isJobsRebornEnabled()) {
      main.getMain()
          .getServer()
          .getPluginManager()
          .registerEvents(new JobsRebornEvents(main), main.getMain());
    }

    if (isProjectKorraEnabled()) {
      main.getMain()
          .getServer()
          .getPluginManager()
          .registerEvents(new ProjectKorraEvents(main), main.getMain());
    }

    if (isSlimefunEnabled()) {
      main.getMain()
          .getServer()
          .getPluginManager()
          .registerEvents(new SlimefunEvents(main), main.getMain());
    }

    if (isEcoBossesEnabled()) {
      main.getMain()
          .getServer()
          .getPluginManager()
          .registerEvents(new EcoBossesEvents(main), main.getMain());
    }

    if (isUltimateJobsEnabled()) {
      main.getMain()
          .getServer()
          .getPluginManager()
          .registerEvents(new UltimateJobsEvents(main), main.getMain());
    }
  }

  public void enableMythicMobs() {
    if (mythicMobsManager == null && main.getConfiguration().isIntegrationMythicMobsEnabled()) {
      mythicMobsEnabled = true;
      enabledIntegrations.add("MythicMobs");
      main.getLogManager().info("MythicMobs found! Enabling MythicMobs support (late)...");
      mythicMobsManager = new MythicMobsManager(main);
      main.getMain()
          .getServer()
          .getPluginManager()
          .registerEvents(new MythicMobsEvents(main), main.getMain());

      main.getDataManager().loadStandardCompletions();
    }
  }

  public void enableCitizens() {
    if (main.getConfiguration().isIntegrationCitizensEnabled()) {
      if (citizensManager == null) {
        citizensManager = new CitizensManager(main);
      }
      citizensEnabled = true;
      enabledIntegrations.add("Citizens");
      main.getLogManager().info("Citizens found! Enabling Citizens support (late)...");
      main.getDataManager().setAlreadyLoadedNPCs(false);
      main.getMain()
          .getServer()
          .getPluginManager()
          .registerEvents(new CitizensEvents(main), main.getMain());
      if (!main.getDataManager().isAlreadyLoadedNPCs()) { // Just making sure
        main.getDataManager().loadNPCData();
      }
      citizensManager.registerAnyCitizensCommands();

      // Aand the commands
      // final Command.Builder<CommandSender> citizensNPCsBuilder = editBuilder.literal("npcs");
      // main.getCommandManager().getAdminEditCommands().handleCitizensNPCs();
    }
  }

  /**
   * Returns if Vault integration is enabled or not. It's usually disabled when Vault is not found
   * on the Server.
   *
   * @return if Vault is enabled
   */
  public boolean isVaultEnabled() {
    return vaultEnabled;
  }

  /**
   * Returns if Citizens integration is enabled or not. It's usually disabled when Citizens is not
   * found on the Server.
   *
   * @return if Citizens is enabled
   */
  public boolean isCitizensEnabled() {
    return citizensEnabled;
  }

  public boolean isMythicMobsEnabled() {
    return mythicMobsEnabled;
  }

  public boolean isEliteMobsEnabled() {
    return eliteMobsEnabled;
  }

  public boolean isSlimefunEnabled() {
    return slimefunEnabled;
  }

  public boolean isPlaceholderAPIEnabled() {
    return placeholderAPIEnabled;
  }

  public boolean isBetonQuestEnabled() {
    return betonQuestEnabled;
  }

  public boolean isWorldEditEnabled() {
    return worldEditEnabled;
  }

  public boolean isLuckpermsEnabled() {
    return luckpermsEnabled;
  }

  public boolean isUltimateClansEnabled() {
    return ultimateClansEnabled;
  }

  public boolean isTownyEnabled() {
    return townyEnabled;
  }

  public boolean isJobsRebornEnabled() {
    return jobsRebornEnabled;
  }

  public boolean isProjectKorraEnabled() {
    return projectKorraEnabled;
  }

  public boolean isEcoBossesEnabled() {
    return ecoBossesEnabled;
  }

  public boolean isUltimateJobsEnabled() {
    return ultimateJobsEnabled;
  }

  public final MythicMobsManager getMythicMobsManager() {
    return mythicMobsManager;
  }

  public final EcoBossesManager getEcoBossesManager() {
    return ecoBossesManager;
  }

  public final BetonQuestManager getBetonQuestManager() {
    return betonQuestManager;
  }

  public final WorldEditManager getWorldEditManager() {
    return worldEditManager;
  }

  public final LuckpermsManager getLuckPermsManager() {
    return luckpermsManager;
  }

  public final CitizensManager getCitizensManager() {
    return citizensManager;
  }

  public final ProjectKorraManager getProjectKorraManager() {
    return projectKorraManager;
  }

  public final UltimateClansManager getUltimateClansManager() {
    return ultimateClansManager;
  }

  public final VaultManager getVaultManager() {
    return vaultManager;
  }

  public void onPluginEnable(final PluginEnableEvent event) {
    if (event.getPlugin().getName().equals("MythicMobs")
        && !main.getIntegrationsManager().isMythicMobsEnabled()) {
      // Turn on support for the plugin
      main.getIntegrationsManager().enableMythicMobs();
    } else if (event.getPlugin().getName().equals("Citizens")
        && !main.getIntegrationsManager().isCitizensEnabled()) {
      // Turn on support for the plugin
      main.getIntegrationsManager().enableCitizens();
    }
  }
}
