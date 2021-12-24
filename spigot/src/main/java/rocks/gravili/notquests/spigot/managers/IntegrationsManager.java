package rocks.gravili.notquests.spigot.managers;

import org.bukkit.Bukkit;
import rocks.gravili.notquests.spigot.events.hooks.*;
import rocks.gravili.notquests.spigot.managers.integrations.*;
import rocks.gravili.notquests.spigot.managers.integrations.betonquest.BetonQuestManager;
import rocks.gravili.notquests.spigot.managers.integrations.citizens.CitizensManager;
import rocks.gravili.notquests.spigot.placeholders.QuestPlaceholders;
import rocks.gravili.notquests.spigot.NotQuests;
import rocks.gravili.notquestsspigot.events.hooks.*;
import rocks.gravili.notquestsspigot.managers.integrations.*;

import java.util.Objects;

public class IntegrationsManager {
    private final NotQuests main;
    //Booleans
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
    //Managers
    private VaultManager vaultManager;
    private MythicMobsManager mythicMobsManager;
    private CitizensManager citizensManager;
    private BetonQuestManager betonQuestManager;
    private WorldEditManager worldEditManager;
    private SlimefunManager slimefunManager;
    private LuckpermsManager luckpermsManager;
    private ProjectKorraManager projectKorraManager;
    private UltimateClansManager ultimateClansManager;

    public IntegrationsManager(final NotQuests main) {
        this.main = main;
    }

    public void enableIntegrations() {
        //Vault Hook
        if (main.getConfiguration().isIntegrationVaultEnabled()) {
            if (main.getMain().getServer().getPluginManager().getPlugin("Vault") != null) {
                vaultManager = new VaultManager(main);
                if (!vaultManager.setupEconomy()) {
                    main.getLogManager().warn("Vault Dependency not found! Some features have been disabled. I recommend you to install Vault for the best experience.");
                } else {
                    vaultManager.setupPermissions();
                    vaultManager.setupChat();
                    vaultEnabled = true;
                    main.getLogManager().info("Vault found! Enabling Vault support...");
                }
            }

        }

        //MythicMobs Hook
        if (main.getConfiguration().isIntegrationMythicMobsEnabled()) {
            if (main.getMain().getServer().getPluginManager().getPlugin("MythicMobs") != null && Objects.requireNonNull(main.getMain().getServer().getPluginManager().getPlugin("MythicMobs")).isEnabled()) {
                mythicMobsEnabled = true;
                main.getLogManager().info("MythicMobs found! Enabling MythicMobs support...");
                mythicMobsManager = new MythicMobsManager(main);
            }
        }


        //EliteMobs Hook
        if (main.getConfiguration().isIntegrationEliteMobsEnabled()) {
            if (main.getMain().getServer().getPluginManager().getPlugin("EliteMobs") != null && Objects.requireNonNull(main.getMain().getServer().getPluginManager().getPlugin("EliteMobs")).isEnabled()) {
                eliteMobsEnabled = true;
                main.getLogManager().info("EliteMobs found! Enabling EliteMobs support...");
            }
        }

        //BetonQuest Hook
        if (main.getConfiguration().isIntegrationBetonQuestEnabled()) {
            if (main.getMain().getServer().getPluginManager().getPlugin("BetonQuest") != null && Objects.requireNonNull(main.getMain().getServer().getPluginManager().getPlugin("BetonQuest")).isEnabled()) {
                betonQuestEnabled = true;
                main.getLogManager().info("BetonQuest found! Enabling BetonQuest support...");
                betonQuestManager = new BetonQuestManager(main);
            }
        }


        //WorldEdit
        if (main.getConfiguration().isIntegrationWorldEditEnabled()) {
            if (main.getMain().getServer().getPluginManager().getPlugin("WorldEdit") != null) {
                worldEditManager = new WorldEditManager(main);
                worldEditEnabled = false;
                main.getLogManager().info("WorldEdit found! Enabling WorldEdit support...");
                worldEditEnabled = true;
            }
        }


        //Enable 'Citizens' integration. If it's not found, it will just disable some NPC features which can mostly be replaced by armor stands
        if (main.getConfiguration().isIntegrationCitizensEnabled()) {
            if (main.getMain().getServer().getPluginManager().getPlugin("Citizens") == null || !Objects.requireNonNull(main.getMain().getServer().getPluginManager().getPlugin("Citizens")).isEnabled()) {
                main.getLogManager().info("Citizens Dependency not found! Congratulations! In NotQuests, you can use armor stands instead of Citizens NPCs");

            } else {
                citizensManager = new CitizensManager(main);
                citizensEnabled = true;
                main.getLogManager().info("Citizens found! Enabling Citizens support...");
            }
        }

        //Enable 'SlimeFun' integration.
        if (main.getConfiguration().isIntegrationSlimeFunEnabled()) {
            if (main.getMain().getServer().getPluginManager().getPlugin("Slimefun") == null || !Objects.requireNonNull(main.getMain().getServer().getPluginManager().getPlugin("Slimefun")).isEnabled()) {
                slimefunEnabled = false;
            } else {
                slimefunManager = new SlimefunManager(main);
                main.getLogManager().info("SlimeFun found! Enabling SlimeFun support...");
                slimefunEnabled = true;
            }
        }

        //LuckPerms
        if (main.getConfiguration().isIntegrationLuckPermsEnabled()) {
            if (main.getMain().getServer().getPluginManager().getPlugin("LuckPerms") != null) {
                luckpermsManager = new LuckpermsManager(main);
                luckpermsEnabled = true;
                main.getLogManager().info("LuckPerms found! Enabling LuckPerms support...");
            }

        }

        //UltimateClans
        if (main.getConfiguration().isIntegrationUltimateClansEnabled()) {
            if (main.getMain().getServer().getPluginManager().getPlugin("UltimateClans") != null) {
                ultimateClansEnabled = true;
                ultimateClansManager = new UltimateClansManager(main);
                main.getLogManager().info("UltimateClans found! Enabling UltimateClans support...");
            }
        }

        //Towny
        if (main.getConfiguration().isIntegrationTownyEnabled()) {
            if (main.getMain().getServer().getPluginManager().getPlugin("Towny") != null) {
                townyEnabled = true;
                main.getLogManager().info("Towny found! Enabling Towny support...");
            }
        }

        //JobsReborn
        if (main.getConfiguration().isIntegrationJobsRebornEnabled()) {
            if (main.getMain().getServer().getPluginManager().getPlugin("Jobs") != null) {
                jobsRebornEnabled = true;
                main.getLogManager().info("Jobs Reborn found! Enabling Jobs Reborn support...");
            }
        }

        //Project Korra
        if (main.getConfiguration().isIntegrationJobsRebornEnabled()) {
            if (main.getMain().getServer().getPluginManager().getPlugin("ProjectKorra") != null) {
                projectKorraManager = new ProjectKorraManager(main);
                projectKorraEnabled = true;
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
        if (main.getConfiguration().isIntegrationPlaceholderAPIEnabled()) {
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                placeholderAPIEnabled = true;
                main.getLogManager().info("PlaceholderAPI found! Enabling PlaceholderAPI support...");
                new QuestPlaceholders(main).register();
            }
        }
    }

    public void registerEvents() {
        if (isCitizensEnabled()) {
            main.getMain().getServer().getPluginManager().registerEvents(new CitizensEvents(main), main.getMain());
        }

        if (isMythicMobsEnabled()) {
            main.getMain().getServer().getPluginManager().registerEvents(new MythicMobsEvents(main), main.getMain());
        }

        if (isEliteMobsEnabled()) {
            main.getMain().getServer().getPluginManager().registerEvents(new EliteMobsEvents(main), main.getMain());
        }

        if (isTownyEnabled()) {
            main.getMain().getServer().getPluginManager().registerEvents(new TownyEvents(main), main.getMain());
        }

        if (isJobsRebornEnabled()) {
            main.getMain().getServer().getPluginManager().registerEvents(new JobsRebornEvents(main), main.getMain());
        }

        if (isProjectKorraEnabled()) {
            main.getMain().getServer().getPluginManager().registerEvents(new ProjectKorraEvents(main), main.getMain());
        }

        if (isSlimefunEnabled()) {
            main.getMain().getServer().getPluginManager().registerEvents(new SlimefunEvents(main), main.getMain());
        }
    }


    public void enableMythicMobs() {
        if (mythicMobsManager == null && main.getConfiguration().isIntegrationMythicMobsEnabled()) {
            mythicMobsEnabled = true;
            main.getLogManager().info("MythicMobs found! Enabling MythicMobs support (late)...");
            mythicMobsManager = new MythicMobsManager(main);
            main.getMain().getServer().getPluginManager().registerEvents(new MythicMobsEvents(main), main.getMain());

            main.getDataManager().loadStandardCompletions();
        }
    }

    public void enableCitizens() {
        if (main.getConfiguration().isIntegrationCitizensEnabled()) {
            if (citizensManager == null) {
                citizensManager = new CitizensManager(main);
            }
            citizensEnabled = true;
            main.getLogManager().info("Citizens found! Enabling Citizens support (late)...");
            main.getDataManager().setAlreadyLoadedNPCs(false);
            main.getMain().getServer().getPluginManager().registerEvents(new CitizensEvents(main), main.getMain());
            if (!main.getDataManager().isAlreadyLoadedNPCs()) { //Just making sure
                main.getDataManager().loadNPCData();
            }
        }
    }


    /**
     * Returns if Vault integration is enabled or not. It's usually disabled when Vault is not found on the Server.
     *
     * @return if Vault is enabled
     */
    public boolean isVaultEnabled() {
        return vaultEnabled;
    }

    /**
     * Returns if Citizens integration is enabled or not. It's usually disabled when Citizens is not found on the Server.
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


    public final MythicMobsManager getMythicMobsManager() {
        return mythicMobsManager;
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
}
