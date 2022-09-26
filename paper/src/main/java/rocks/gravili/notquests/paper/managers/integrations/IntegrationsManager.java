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

import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.Bukkit;
import org.bukkit.event.server.PluginEnableEvent;
import org.jetbrains.annotations.Nullable;
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
import rocks.gravili.notquests.paper.managers.integrations.betonquest.BetonQuestManager;
import rocks.gravili.notquests.paper.managers.integrations.citizens.CitizensManager;
import rocks.gravili.notquests.paper.placeholders.QuestPlaceholders;

public class IntegrationsManager {
  private final NotQuests main;

  private final CopyOnWriteArrayList<Integration> integrations = new CopyOnWriteArrayList<>();

  private final CopyOnWriteArrayList<Integration> integrationsNotEnabled = new CopyOnWriteArrayList<>();

  private final CopyOnWriteArrayList<EnabledIntegration> enabledIntegrations = new CopyOnWriteArrayList<>();
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

  private boolean floodgateEnabled = false;

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

  private FloodgateManager floodgateManager;


  public IntegrationsManager(final NotQuests main) {
    this.main = main;

    integrations.add(
        new Integration(main, "UltimateJobs")
            .setEnableCondition(() -> main.getConfiguration().isIntegrationUltimateJobsEnabled())
            .setRunWhenEnabled(
                () -> {
                  ultimateJobsEnabled = true;
                  return true;
                })
            .setRunWhenRegisteringEventsOnTime(
                () -> {
                  main.getMain()
                      .getServer()
                      .getPluginManager()
                      .registerEvents(new UltimateJobsEvents(main), main.getMain());
                }));

    integrations.add(
        new Integration(main, "EcoBosses")
            .setEnableCondition(() -> main.getConfiguration().isIntegrationEcoBossesEnabled())
            .setRunWhenEnabled(
                () -> {
                  ecoBossesEnabled = true;
                  main.getLogManager()
                      .info(
                          "EcoBosses found! Enabling EcoBosses support... Bosses will be loaded in 10 seconds, because they are not loaded when the plugin starts. Don't blame me");
                  Bukkit.getScheduler()
                      .scheduleSyncDelayedTask(
                          main.getMain(),
                          () -> {
                            ecoBossesManager = new EcoBossesManager(main);
                            main.getDataManager().loadStandardCompletions();
                          },
                          200L);
                  return true;
                })
            .setRunWhenRegisteringEventsOnTime(
                () -> {
                  main.getMain()
                      .getServer()
                      .getPluginManager()
                      .registerEvents(new EcoBossesEvents(main), main.getMain());
                }));

    integrations.add(
        new Integration(main, "PlaceholderAPI")
            .setEnableCondition(() -> main.getConfiguration().isIntegrationPlaceholderAPIEnabled())
            .setRunWhenEnabled(
                () -> {
                  placeholderAPIEnabled = true;
                  return true;
                })
            .setRunAfterDataLoad(() -> new QuestPlaceholders(main).register()));

    integrations.add(
        new Integration(main, "Vault")
            .setEnableCondition(() -> main.getConfiguration().isIntegrationVaultEnabled())
            .setRunWhenEnabled(
                () -> {
                  vaultManager = new VaultManager(main);
                  if (!vaultManager.setupEconomy()) {
                    main.getLogManager()
                        .warn(
                            "Vault Dependency not found! Some features have been disabled. I recommend you to install Vault for the best experience.");
                    return false;
                  } else {
                    vaultManager.setupPermissions();
                    vaultManager.setupChat();
                    vaultEnabled = true;
                    return true;
                  }
                }));

    integrations.add(
        new Integration(main, "MythicMobs")
            .setEnableCondition(() -> main.getConfiguration().isIntegrationMythicMobsEnabled())
            .setRunWhenEnabled(
                () -> {
                  mythicMobsEnabled = true;
                  mythicMobsManager = new MythicMobsManager(main);
                  return true;
                })
            .setRunAlsoWhenEnabledLate(
                () -> {
                  main.getMain()
                      .getServer()
                      .getPluginManager()
                      .registerEvents(new MythicMobsEvents(main), main.getMain());

                  main.getDataManager().loadStandardCompletions();
                })
            .setRunWhenRegisteringEventsOnTime(
                () -> {
                  main.getMain()
                      .getServer()
                      .getPluginManager()
                      .registerEvents(new MythicMobsEvents(main), main.getMain());
                }));

    integrations.add(
        new Integration(main, "EliteMobs")
            .setEnableCondition(() -> main.getConfiguration().isIntegrationEliteMobsEnabled())
            .setRunWhenEnabled(
                () -> {
                  eliteMobsEnabled = true;
                  return true;
                })
            .setRunWhenRegisteringEventsOnTime(
                () -> {
                  main.getMain()
                      .getServer()
                      .getPluginManager()
                      .registerEvents(new EliteMobsEvents(main), main.getMain());
                }));

    integrations.add(
        new Integration(main, "BetonQuest")
            .setEnableCondition(() -> main.getConfiguration().isIntegrationBetonQuestEnabled())
            .setRunWhenEnabled(
                () -> {
                  betonQuestEnabled = true;
                  betonQuestManager = new BetonQuestManager(main);
                  main.getMain()
                      .getServer()
                      .getPluginManager()
                      .registerEvents(new BetonQuestEvents(main), main.getMain());
                  return true;
                }));

    integrations.add(
        new Integration(main, "WorldEdit")
            .setEnableCondition(() -> main.getConfiguration().isIntegrationWorldEditEnabled())
            .setRunWhenEnabled(
                () -> {
                  worldEditManager = new WorldEditManager(main);
                  worldEditEnabled = true;
                  return true;
                }));

    // Enable 'Citizens' integration. If it's not found, it will just disable some NPC features
    // which can mostly be replaced by armor stands
    integrations.add(
        new Integration(main, "Citizens")
            .setEnableCondition(() -> main.getConfiguration().isIntegrationCitizensEnabled())
            .setRunWhenEnablingFailed(
                () ->
                    main.getLogManager()
                        .info(
                            "Citizens Dependency not found! Congratulations! In NotQuests, you can use armor stands instead of Citizens NPCs"))
            .setRunWhenEnabled(
                () -> {
                  citizensEnabled = true;
                  citizensManager = new CitizensManager(main);
                  return true;
                })
            .setRunAlsoWhenEnabledLate(
                () -> {
                  main.getDataManager().setAlreadyLoadedNPCs(false);
                  main.getMain()
                      .getServer()
                      .getPluginManager()
                      .registerEvents(new CitizensEvents(main), main.getMain());
                  if (!main.getDataManager().isAlreadyLoadedNPCs()) { // Just making sure
                    main.getDataManager().loadNPCData();
                  }
                  citizensManager.registerAnyCitizensCommands();
                })
            .setRunWhenRegisteringEventsOnTime(
                () -> {
                  main.getMain()
                      .getServer()
                      .getPluginManager()
                      .registerEvents(new CitizensEvents(main), main.getMain());
                  citizensManager.registerAnyCitizensCommands();
                }));

    integrations.add(
        new Integration(main, "Slimefun")
            .setEnableCondition(() -> main.getConfiguration().isIntegrationSlimeFunEnabled())
            .setRunWhenEnabled(
                () -> {
                  slimefunEnabled = true;
                  slimefunManager = new SlimefunManager(main);
                  return true;
                })
            .setRunWhenRegisteringEventsOnTime(
                () -> {
                  main.getMain()
                      .getServer()
                      .getPluginManager()
                      .registerEvents(new SlimefunEvents(main), main.getMain());
                }));

    integrations.add(
        new Integration(main, "LuckPerms")
            .setEnableCondition(() -> main.getConfiguration().isIntegrationLuckPermsEnabled())
            .setRunWhenEnabled(
                () -> {
                  luckpermsManager = new LuckpermsManager(main);
                  luckpermsEnabled = true;
                  return true;
                }));

    integrations.add(
        new Integration(main, "UltimateClans")
            .setEnableCondition(() -> main.getConfiguration().isIntegrationUltimateClansEnabled())
            .setRunWhenEnabled(
                () -> {
                  ultimateClansEnabled = true;
                  ultimateClansManager = new UltimateClansManager(main);
                  return true;
                }));

    integrations.add(
        new Integration(main, "Towny")
            .setEnableCondition(() -> main.getConfiguration().isIntegrationTownyEnabled())
            .setRunWhenEnabled(
                () -> {
                  townyEnabled = true;
                  return true;
                })
            .setRunWhenRegisteringEventsOnTime(
                () -> {
                  main.getMain()
                      .getServer()
                      .getPluginManager()
                      .registerEvents(new TownyEvents(main), main.getMain());
                }));

    integrations.add(
        new Integration(main, "Jobs")
            .setEnableCondition(() -> main.getConfiguration().isIntegrationJobsRebornEnabled())
            .setRunWhenEnabled(
                () -> {
                  jobsRebornEnabled = true;
                  return true;
                })
            .setRunWhenRegisteringEventsOnTime(
                () -> {
                  main.getMain()
                      .getServer()
                      .getPluginManager()
                      .registerEvents(new JobsRebornEvents(main), main.getMain());
                }));

    integrations.add(
        new Integration(main, "ProjectKorra")
            .setEnableCondition(() -> main.getConfiguration().isIntegrationProjectKorraEnabled())
            .setRunWhenEnabled(
                () -> {
                  projectKorraManager = new ProjectKorraManager(main);
                  projectKorraEnabled = true;
                  return true;
                })
            .setRunWhenRegisteringEventsOnTime(
                () -> {
                  main.getMain()
                      .getServer()
                      .getPluginManager()
                      .registerEvents(new ProjectKorraEvents(main), main.getMain());
                }));

    integrations.add(
        new Integration(main, "Floodgate")
            .setEnableCondition(() -> main.getConfiguration().isIntegrationFloodgateEnabled())
            .setRunWhenEnabled(
                () -> {
                  floodgateManager = new FloodgateManager(main);
                  floodgateEnabled = true;
                  return true;
                })
    );

    integrationsNotEnabled.addAll(integrations);
  }

  public final CopyOnWriteArrayList<Integration> getIntegrations() {
    return integrations;
  }

  public final CopyOnWriteArrayList<Integration> getDisabledIntegrations() {
    return integrationsNotEnabled;
  }

  public final CopyOnWriteArrayList<EnabledIntegration> getEnabledIntegrations() {
    return enabledIntegrations;
  }

  public final String getEnabledIntegrationDiscordString() {
    final StringBuilder enabledIntegrationsString = new StringBuilder();
    for (final EnabledIntegration enabledIntegration : getEnabledIntegrations()) {
      enabledIntegrationsString.append("\n- ");

      enabledIntegrationsString
          .append(enabledIntegration.getExactName())
          .append(" *(")
          .append(enabledIntegration.getVersionString())
          .append(")*");
    }

    return enabledIntegrationsString.toString();
  }

  public final String getEnabledIntegrationString() {
    final StringBuilder enabledIntegrationsString = new StringBuilder();
    int counter = 0;
    for (final EnabledIntegration enabledIntegration : getEnabledIntegrations()) {
      counter++;
      if (counter > 1) {
        enabledIntegrationsString.append("<veryUnimportant>,</veryUnimportant> ");
      }
      enabledIntegrationsString
          .append(enabledIntegration.getExactName())
          .append(" <unimportant>(")
          .append(enabledIntegration.getVersionString())
          .append(")</unimportant>");
    }

    return enabledIntegrationsString.toString();
  }

  public void enableIntegrations() {

    for (final Integration integration : integrations) {
      final @Nullable EnabledIntegration enabledIntegration = integration.enable();
      if (enabledIntegration == null) {
        continue;
      }
      enabledIntegrations.add(enabledIntegration);
      integrationsNotEnabled.remove(integration);
    }
  }

  public void onDisable() {
    if (isCitizensEnabled()) {
      citizensManager.onDisable();
    }
  }

  public void enableIntegrationsAfterDataLoad() {
    for (final Integration disabledIntegration : getDisabledIntegrations()) {
      final @Nullable EnabledIntegration enabledIntegration = disabledIntegration.enable();
      if (enabledIntegration == null) {
        continue;
      }
      enabledIntegrations.add(enabledIntegration);
      integrationsNotEnabled.remove(disabledIntegration);
    }

    for (final EnabledIntegration enabledIntegration : getEnabledIntegrations()) {
      enabledIntegration.dataLoaded();
    }
  }

  public void registerEvents() {
    for (final EnabledIntegration enabledIntegration : getEnabledIntegrations()) {
      enabledIntegration.registeringEventsOnTime();
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

  public boolean isFloodgateEnabled() {
    return floodgateEnabled;
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

  public final FloodgateManager getFloodgateManager() {
    return floodgateManager;
  }

  public final UltimateClansManager getUltimateClansManager() {
    return ultimateClansManager;
  }

  public final VaultManager getVaultManager() {
    return vaultManager;
  }

  public void onPluginEnable(final PluginEnableEvent event) {
    for (final Integration disabledIntegration : getDisabledIntegrations()) {
      if (!event.getPlugin().getName().equals(disabledIntegration.getExactName())) {
        continue;
      }
      final @Nullable EnabledIntegration enabledIntegration =
          disabledIntegration.enableLate(); // This also runs runWhenEnabledLate at the end as well
      if (enabledIntegration == null) {
        continue;
      }

      enabledIntegrations.add(enabledIntegration);
      integrationsNotEnabled.remove(disabledIntegration);
    }
  }
}
