
/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021 Alessio Gravili
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

package rocks.gravili.notquests;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.lumine.xikage.mythicmobs.MythicMobs;
import io.papermc.lib.PaperLib;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitInfo;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import rocks.gravili.notquests.Conversation.ConversationEvents;
import rocks.gravili.notquests.Conversation.ConversationManager;
import rocks.gravili.notquests.Events.ArmorStandEvents;
import rocks.gravili.notquests.Events.InventoryEvents;
import rocks.gravili.notquests.Events.QuestEvents;
import rocks.gravili.notquests.Events.TriggerEvents;
import rocks.gravili.notquests.Events.hooks.*;
import rocks.gravili.notquests.Hooks.BetonQuest.BetonQuestIntegration;
import rocks.gravili.notquests.Hooks.Citizens.CitizensManager;
import rocks.gravili.notquests.Managers.*;
import rocks.gravili.notquests.Managers.Registering.ObjectiveManager;
import rocks.gravili.notquests.Managers.Registering.RequirementManager;
import rocks.gravili.notquests.Managers.Registering.RewardManager;
import rocks.gravili.notquests.Managers.Registering.TriggerManager;
import rocks.gravili.notquests.Placeholders.QuestPlaceholders;

import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Level;

/**
 * This is the entry point of NotQuests. All kinds of managers, commands and other shit is reistered here.
 *
 * @author Alessio Gravili
 */
public final class NotQuests extends JavaPlugin {

    private static NotQuests instance;

    //Managers
    private UtilManager utilManager;
    private LogManager logManager;
    private DataManager dataManager;
    private QuestManager questManager;
    private QuestPlayerManager questPlayerManager;
    private LanguageManager languageManager;
    private ArmorStandManager armorStandManager;
    private PerformanceManager performanceManager;
    private CommandManager commandManager;
    private ConversationManager conversationManager;

    private CitizensManager citizensManager;

    //Registering Managers
    private ObjectiveManager objectiveManager;
    private RequirementManager requirementManager;
    private RewardManager rewardManager;
    private TriggerManager triggerManager;


    //Vault
    private Economy econ = null;
    private Permission perms = null;
    private Chat chat = null;

    //Metrics
    private Metrics metrics;

    //Enabled Features
    private boolean vaultEnabled = false;
    private boolean citizensEnabled = false;
    private boolean slimefunEnabled = false;

    //Enabled Hooks
    private boolean mythicMobsEnabled = false;
    private MythicMobs mythicMobs;

    //Enabled Hooks
    private boolean eliteMobsEnabled = false;
    private boolean placeholderAPIEnabled = false;
    private boolean betonQuestEnabled = false;
    private BetonQuestIntegration betonQuestIntegration;

    private boolean worldEditEnabled = false;
    private WorldEditPlugin worldEditPlugin;
    private WorldEditHook worldEditHook;

    private Slimefun slimefun;


    private BukkitAudiences adventure;


    public final BukkitAudiences adventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return this.adventure;
    }

    public static NotQuests getInstance() {
        return instance;
    }

    /**
     * Called when the plugin is enabled. A bunch of stuff is initialized here
     */
    @Override
    public void onEnable() {
        instance = this;

        // Initialize an audiences instance for the plugin
        this.adventure = BukkitAudiences.create(this);

        //PaperLib for paper-specific methods (like getting TPS)
        PaperLib.suggestPaper(this);


        //Create a new instance of the Util Manager which will be re-used everywhere
        utilManager = new UtilManager(this);


        //Create a new instance of the Log Manager which will be re-used everywhere
        logManager = new LogManager(this);

        getLogManager().log(Level.INFO, "NotQuests is starting...");


        //Create a new instance of the Performance Manager which will be re-used everywhere
        performanceManager = new PerformanceManager(this);

        //Create a new instance of the Data Manager which will be re-used everywhere
        dataManager = new DataManager(this);

        //Load general config first, because we'll need it for the integrations
        dataManager.loadGeneralConfig();


        //Vault Hook
        if (getDataManager().getConfiguration().isIntegrationVaultEnabled()) {
            //Vault is needed for NotQuests to function. If it's not found, NotQuests will be disabled. EDIT: Now it will just disable some features
            if (!setupEconomy()) {
                getLogManager().log(Level.WARNING, "Vault Dependency not found! Some features have been disabled. I recommend you to install Vault for the best experience.");
                //getServer().getPluginManager().disablePlugin(this);
                //return;
            } else {
                setupPermissions();
                setupChat();
                vaultEnabled = true;
                getLogManager().info("Vault found! Enabling Vault support...");
            }
        }

        //MythicMobs Hook
        if (getDataManager().getConfiguration().isIntegrationMythicMobsEnabled()) {
            if (getServer().getPluginManager().getPlugin("MythicMobs") != null && Objects.requireNonNull(getServer().getPluginManager().getPlugin("MythicMobs")).isEnabled()) {
                mythicMobsEnabled = true;
                getLogManager().info("MythicMobs found! Enabling MythicMobs support...");
                this.mythicMobs = MythicMobs.inst();
            }
        }


        //EliteMobs Hook
        if (getDataManager().getConfiguration().isIntegrationEliteMobsEnabled()) {
            if (getServer().getPluginManager().getPlugin("EliteMobs") != null && Objects.requireNonNull(getServer().getPluginManager().getPlugin("EliteMobs")).isEnabled()) {
                eliteMobsEnabled = true;
                getLogManager().info("EliteMobs found! Enabling EliteMobs support...");
            }
        }

        //BetonQuest Hook
        if (getDataManager().getConfiguration().isIntegrationBetonQuestEnabled()) {
            if (getServer().getPluginManager().getPlugin("BetonQuest") != null && Objects.requireNonNull(getServer().getPluginManager().getPlugin("BetonQuest")).isEnabled()) {
                betonQuestEnabled = true;
                getLogManager().info("BetonQuest found! Enabling BetonQuest support...");
                betonQuestIntegration = new BetonQuestIntegration(this);
            }
        }


        //WorldEdit
        if (getDataManager().getConfiguration().isIntegrationWorldEditEnabled()) {
            worldEditPlugin = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
            if (worldEditPlugin == null) {
                worldEditEnabled = false;

            } else {
                getLogManager().info("WorldEdit found! Enabling WorldEdit support...");
                worldEditEnabled = true;
                worldEditHook = new WorldEditHook(this);
            }
        }


        //Enable 'Citizens' integration. If it's not found, it will just disable some NPC features which can mostly be replaced by armor stands
        if (getDataManager().getConfiguration().isIntegrationCitizensEnabled()) {
            if (getServer().getPluginManager().getPlugin("Citizens") == null || !Objects.requireNonNull(getServer().getPluginManager().getPlugin("Citizens")).isEnabled()) {
                getLogManager().log(Level.INFO, "Citizens Dependency not found! Congratulations! In NotQuests, you can use armor stands instead of Citizens NPCs");

            } else {
                citizensManager = new CitizensManager(this);

                citizensEnabled = true;
                getLogManager().info("Citizens found! Enabling Citizens support...");
            }
        }

        //Enable 'SlimeFun' integration.
        if (getDataManager().getConfiguration().isIntegrationSlimeFunEnabled()) {
            if (getServer().getPluginManager().getPlugin("Slimefun") == null || !Objects.requireNonNull(getServer().getPluginManager().getPlugin("Slimefun")).isEnabled()) {
                slimefunEnabled = false;
            } else {
                slimefun = Slimefun.instance();
                if (slimefun == null) {
                    slimefunEnabled = false;

                } else {
                    getLogManager().info("SlimeFun found! Enabling SlimeFun support...");
                    slimefunEnabled = true;
                }
            }

        }


        dataManager.loadStandardCompletions();


        //Create a new instance of the Quest Manager which will be re-used everywhere
        questManager = new QuestManager(this);


        languageManager = new LanguageManager(this);

        /*
         * Tell the Data Manager: Hey, NPCs have not been loaded yet. If this is set to false, the plugin will
         * try to load NPCs once Citizens is re-loaded or enabled
         */
        getDataManager().setAlreadyLoadedNPCs(false);

        //Create a new instance of the QuestPlayer Manager which will be re-used everywhere
        questPlayerManager = new QuestPlayerManager(this);

        //Create a new instance of the Armorstand Manager which will be re-used everywhere
        armorStandManager = new ArmorStandManager(this);

        armorStandManager.loadAllArmorStandsFromLoadedChunks();


        //Registering the nquestgiver Trait here has been commented out. I think I'm currently doing that somewhere else atm. So, this isn't needed at the moment.
        //net.citizensnpcs.api.CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(QuestGiverNPCTrait.class).withName("nquestgiver"));


        //Register the notquestsadmin command & tab completer. This command will be used by Admins
        commandManager = new CommandManager(this);
        commandManager.preSetupCommands();

        //Registering Managers
        objectiveManager = new ObjectiveManager(this);
        requirementManager = new RequirementManager(this);
        rewardManager = new RewardManager(this);
        triggerManager = new TriggerManager(this);

        commandManager.setupCommands();


        //Register the Event Listeners in QuestEvents
        getServer().getPluginManager().registerEvents(new QuestEvents(this), this);

        //Register the Event Listeners in InventoryEvents
        getServer().getPluginManager().registerEvents(new InventoryEvents(this), this);

        //Register the Event Listeners in TriggerEvents
        getServer().getPluginManager().registerEvents(new TriggerEvents(this), this);

        //Register the Event Listeners in ArmorStandEvents
        getServer().getPluginManager().registerEvents(new ArmorStandEvents(this), this);


        //Register the Event Listeners in CitizensEvents, if Citizens integration is enabled
        if (isCitizensEnabled()) {
            getServer().getPluginManager().registerEvents(new CitizensEvents(this), this);
        }

        //Register the Event Listeners in MythicMobsEvents, if MythicMobs integration is enabled
        if (isMythicMobsEnabled()) {
            getServer().getPluginManager().registerEvents(new MythicMobsEvents(this), this);
        }

        //Register the Event Listeners in EliteMobsEvents, if EliteMobs integration is enabled
        if (isEliteMobsEnabled()) {
            getServer().getPluginManager().registerEvents(new EliteMobsEvents(this), this);
        }

        //Register the Event Listeners in SlimefunEvents, if Slimefun integration is enabled
        if (isSlimefunEnabled()) {
            getServer().getPluginManager().registerEvents(new SlimefunEvents(this), this);
        }

        //This finally starts loading all Config-, Quest-, and Player Data. Reload = Load
        dataManager.reloadData();

        //This registers all PlaceholderAPI placeholders, if loading is enabled
        if (getDataManager().isLoadingEnabled()) {

            if (getDataManager().getConfiguration().isIntegrationPlaceholderAPIEnabled()) {
                if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                    placeholderAPIEnabled = true;
                    getLogManager().info("PlaceholderAPI found! Enabling PlaceholderAPI support...");
                    new QuestPlaceholders(this).register();
                }
            }


            //Update Checker
            try {
                final UpdateChecker updateChecker = new UpdateChecker(this, 95872);
                if (updateChecker.checkForUpdates()) {
                    getLogManager().info("<GOLD>The version <Yellow>" + getDescription().getVersion()
                            + " <GOLD>is not the latest version (<Green>" + updateChecker.getLatestVersion() + "<GOLD>)! Please update the plugin here: <Aqua>https://www.spigotmc.org/resources/95872/ <DARK_GRAY>(If your version is newer, the spigot API might not be updated yet).");
                } else {
                    getLogManager().info("NotQuests seems to be up to date! :)");
                }
            } catch (Exception e) {
                e.printStackTrace();
                getLogManager().info("Unable to check for updates ('" + e.getMessage() + "').");
            }

            //bStats statistics
            final int pluginId = 12824; // <- Plugin ID (on bstats)
            metrics = new Metrics(this, pluginId);
        }


        conversationManager = new ConversationManager(this);
        //Register the Event Listeners in ConversationEvents
        getServer().getPluginManager().registerEvents(new ConversationEvents(this, conversationManager), this);

        commandManager.setupAdminConversationCommands(conversationManager);

    }


    /**
     * Called when the plugin is disabled or reloaded via ServerUtils / PlugMan
     */
    @Override
    public void onDisable() {
        getLogManager().log(Level.INFO, "NotQuests is shutting down...");

        //Save all kinds of data
        dataManager.saveData();



        /* This is kind of useful for compatibility with ServerUtils or Plugman.
         * If this is false, the plugin will try to load NPCs again if the Citizens plugin is reloaded or enabled.
         * Might not be necessary.
         */
        getDataManager().setAlreadyLoadedNPCs(false);


        //Do Citizens stuff if the Citizens integration is enabled
        if(isCitizensEnabled()){
            /*
             * All Citizen NPCs which have quests attached to them have the Citizens NPC trait "nquestgiver".
             * When the plugin is disabled right here, this piece of code will try removing this trait from all+
             * NPCs which currently have this trait.
             */
            final ArrayList<Trait> traitsToRemove = new ArrayList<>();
            for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
                for (final Trait trait : npc.getTraits()) {
                    if (trait.getName().equalsIgnoreCase("nquestgiver")) {
                        traitsToRemove.add(trait);

                    }
                }
                for (final Trait traitToRemove : traitsToRemove) {
                    npc.removeTrait(traitToRemove.getClass());
                    getLogManager().log(Level.INFO, "Removed nquestgiver trait from NPC with the ID <AQUA>" + npc.getId());
                }
                traitsToRemove.clear();

            }

            /*
             * Next, the nquestgiver trait itself which is registered via the Citizens API on startup is being
             * de-registered.
             */
            getLogManager().log(Level.INFO, "Deregistering nquestgiver trait...");
            final ArrayList<TraitInfo> toDeregister = new ArrayList<>();
            for (final TraitInfo traitInfo : net.citizensnpcs.api.CitizensAPI.getTraitFactory().getRegisteredTraits()) {
                if (traitInfo.getTraitName().equals("nquestgiver")) {
                    toDeregister.add(traitInfo);

                }
            }
            //Actual nquestgiver trait de-registering happens here, to prevent a ConcurrentModificationException
            for (final TraitInfo traitInfo : toDeregister) {
                net.citizensnpcs.api.CitizensAPI.getTraitFactory().deregisterTrait(traitInfo);
            }
        }


        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }


    }


    /**
     * Returns an instance of the QuestManager which handles the saving and loading of configured Quests
     *
     * @return an instance of the Quest Manager
     */
    public QuestManager getQuestManager() {
        return questManager;
    }

    /**
     * Returns an instance of the QuestPlayer Manager which handles the saving and loading of player data
     *
     * @return an instance of the QuestPlayer Manager
     */
    public QuestPlayerManager getQuestPlayerManager() {
        return questPlayerManager;
    }

    /**
     * Returns an instance of the Data Manager which handles all kinds of MySQL & configuration loading and saving
     *
     * @return an instance of the Data Manager
     */
    public DataManager getDataManager() {
        return dataManager;
    }

    /**
     * Sets up the Economy from the Vault plugin.
     *
     * @return if the economy has been set up successfully and if Vault has been found
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return true;
    }


    /**
     * Sets up the Chat from the Vault plugin.
     *
     * @return if vault chat has been set up successfully
     */
    private boolean setupChat() {
        RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
        if(rsp != null){
            chat = rsp.getProvider();
            return true;
        }else{
            return false;
        }

    }

    /**
     * Sets up the Permissions from the Vault plugin.
     *
     * @return if permissions from the vault plugin have been set up successfully
     */
    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        if(rsp != null){
            perms = rsp.getProvider();
            return true;
        }else{
            return false;
        }

    }

    /**
     * Returns an instance of Economy which has been set up in setupEconomy()
     *
     * @return an instance of Economy which has been set up in setupEconomy()
     */
    public Economy getEconomy() {
        if(!isVaultEnabled()){
            getLogManager().log(Level.SEVERE, "§cError: Tried to load Economy when Vault is not enabled. Please report this to the plugin author (and I also recommend you installing Vault for money stuff to work)");

            return null;
        }
        return econ;
    }


    /**
     * Returns an instance of the bStats Metrics object
     *
     * @return bStats Metrics object
     */
    public Metrics getMetrics() {
        return metrics;
    }

    /**
     * Returns if Vault integration is enabled or not. It's usually disabled when Vault is not found on the Server.
     *
     * @return if Vault is enabled
     */
    public boolean isVaultEnabled(){
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

    private boolean isSlimefunEnabled() {
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


    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public UtilManager getUtilManager() {
        return utilManager;
    }

    public ArmorStandManager getArmorStandManager() {
        return armorStandManager;
    }

    public PerformanceManager getPerformanceManager() {
        return performanceManager;
    }

    public ObjectiveManager getObjectiveManager() {
        return objectiveManager;
    }

    public RequirementManager getRequirementManager() {
        return requirementManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public TriggerManager getTriggerManager() {
        return triggerManager;
    }

    public MythicMobs getMythicMobs() {
        return mythicMobs;
    }


    public BetonQuestIntegration getBetonQuestIntegration() {
        return betonQuestIntegration;
    }

    public WorldEditPlugin getWorldEdit() {
        return worldEditPlugin;
    }

    public Slimefun getSlimefun() {
        return slimefun;
    }

    public WorldEditHook getWorldEditHook() {
        return worldEditHook;
    }

    public void enableMythicMobs() {
        if (getDataManager().getConfiguration().isIntegrationMythicMobsEnabled()) {
            mythicMobsEnabled = true;
            getLogManager().info("MythicMobs found! Enabling MythicMobs support (late)...");
            this.mythicMobs = MythicMobs.inst();
            getServer().getPluginManager().registerEvents(new MythicMobsEvents(this), this);

            dataManager.loadStandardCompletions();
        }
    }

    public void enableCitizens() {
        if (getDataManager().getConfiguration().isIntegrationCitizensEnabled()) {
            citizensEnabled = true;
            getLogManager().info("Citizens found! Enabling Citizens support (late)...");
            getDataManager().setAlreadyLoadedNPCs(false);
            getServer().getPluginManager().registerEvents(new CitizensEvents(this), this);
            getDataManager().loadNPCData();
        }

    }

    public CommandManager getCommandManager() {
        return this.commandManager;
    }

    public ConversationManager getConversationManager() {
        return this.conversationManager;
    }

    public CitizensManager getCitizensManager() {
        return citizensManager;
    }
}
