
package notquests.notquests;

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
import notquests.notquests.Commands.CommandNotQuests;
import notquests.notquests.Commands.CommandNotQuestsAdmin;
import notquests.notquests.Events.ArmorStandEvents;
import notquests.notquests.Events.QuestEvents;
import notquests.notquests.Events.hooks.CitizensEvents;
import notquests.notquests.Events.hooks.EliteMobsEvents;
import notquests.notquests.Events.hooks.MythicMobsEvents;
import notquests.notquests.Managers.*;
import notquests.notquests.Placeholders.QuestPlaceholders;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Level;

/**
 * This is the entry point of NotQuests. All kinds of managers, commands and other shit is reistered here.
 *
 * @author Alessio Gravili
 */
public final class NotQuests extends JavaPlugin {

    //Managers
    private UtilManager utilManager;
    private LogManager logManager;
    private DataManager dataManager;
    private QuestManager questManager;
    private QuestPlayerManager questPlayerManager;
    private LanguageManager languageManager;
    private ArmorStandManager armorStandManager;
    private PerformanceManager performanceManager;

    //Vault
    private Economy econ = null;
    private Permission perms = null;
    private Chat chat = null;

    //Metrics
    private Metrics metrics;

    //Enabled Features
    private boolean vaultEnabled = false;
    private boolean citizensEnabled = false;

    //Enabled Hooks
    private boolean mythicMobsEnabled = false;
    private MythicMobs mythicMobs;

    //Enabled Hooks
    private boolean eliteMobsEnabled = false;
    private BukkitAudiences adventure;

    public final BukkitAudiences adventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return this.adventure;
    }

    /**
     * Called when the plugin is enabled. A bunch of stuff is initialized here
     */
    @Override
    public void onEnable() {
        // Initialize an audiences instance for the plugin
        this.adventure = BukkitAudiences.create(this);

        //PaperLib for paper-specific methods (like getting TPS)
        PaperLib.suggestPaper(this);

        //Create a new instance of the Util Manager which will be re-used everywhere
        utilManager = new UtilManager(this);

        //Create a new instance of the Log Manager which will be re-used everywhere
        logManager = new LogManager(this);

        //Create a new instance of the Performance Manager which will be re-used everywhere
        performanceManager = new PerformanceManager(this);


        getLogManager().log(Level.INFO, "NotQuests is starting...");

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

        //MythicMobs Hook
        if (getServer().getPluginManager().getPlugin("MythicMobs") != null && Objects.requireNonNull(getServer().getPluginManager().getPlugin("MythicMobs")).isEnabled()) {
            mythicMobsEnabled = true;
            getLogManager().info("MythicMobs found! Enabling MythicMobs support...");
            this.mythicMobs = MythicMobs.inst();
        }

        //EliteMobs Hook
        if (getServer().getPluginManager().getPlugin("EliteMobs") != null && Objects.requireNonNull(getServer().getPluginManager().getPlugin("EliteMobs")).isEnabled()) {
            eliteMobsEnabled = true;
            getLogManager().info("EliteMobs found! Enabling EliteMobs support...");
        }


        //Create a new instance of the Data Manager which will be re-used everywhere
        dataManager = new DataManager(this);

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

        //The plugin "Citizens" is currently required for NotQuests to run properly. If it's not found, NotQuests will be disabled. EDIT: Now it will just disable some features
        if (getServer().getPluginManager().getPlugin("Citizens") == null || !Objects.requireNonNull(getServer().getPluginManager().getPlugin("Citizens")).isEnabled()) {
            getLogManager().log(Level.INFO, "Citizens Dependency not found! Congratulations! In NotQuests, you can use armor stands instead of Citizens NPCs");

        } else {
            citizensEnabled = true;
            getLogManager().info("Citizens found! Enabling Citizens support...");
        }


        //Registering the nquestgiver Trait here has been commented out. I think I'm currently doing that somewhere else atm. So, this isn't needed at the moment.
        //net.citizensnpcs.api.CitizensAPI.getTraitFactory().registerTrait(net.citizensnpcs.api.trait.TraitInfo.create(QuestGiverNPCTrait.class).withName("nquestgiver"));


        //Register the notquestsadmin command & tab completer. This command will be used by Admins
        final PluginCommand notQuestsAdminCommand = getCommand("notquestsadmin");
        if (notQuestsAdminCommand != null) {
            final CommandNotQuestsAdmin commandNotQuestsAdmin = new CommandNotQuestsAdmin(this);
            notQuestsAdminCommand.setTabCompleter(commandNotQuestsAdmin);
            notQuestsAdminCommand.setExecutor(commandNotQuestsAdmin);
        }


        //Register the notquests command & tab completer. This command will be used by Players
        final PluginCommand notQuestsCommand = getCommand("notquests");
        if (notQuestsCommand != null) {
            final CommandNotQuests commandNotQuests = new CommandNotQuests(this);
            notQuestsCommand.setExecutor(commandNotQuests);
            notQuestsCommand.setTabCompleter(commandNotQuests);
        }


        //Register the Event Listeners in QuestEvents
        getServer().getPluginManager().registerEvents(new QuestEvents(this), this);

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

        //This finally starts loading all Config-, Quest-, and Player Data. Reload = Load
        dataManager.reloadData();

        //This registers all PlaceholderAPI placeholders, if loading is enabled
        if (getDataManager().isLoadingEnabled()) {
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new QuestPlaceholders(this).register();
            }

            //Update Checker
            try {
                final UpdateChecker updateChecker = new UpdateChecker(this, 95872);
                if (updateChecker.checkForUpdates()) {
                    getLogManager().info("<GOLD>The version <Yellow>" + getDescription().getVersion()
                            + " <GOLD>is not the latest version (<Green>" + updateChecker.getLatestVersion() + "<GOLD>)! Please update the plugin here: <Aqua>https://www.spigotmc.org/resources/95872/ <DARK_GRAY>(If your version is newer, the spigot API might not be updated yet).");
                }else{
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
                    getLogManager().log(Level.INFO, "Removed nquestgiver trait from NPC with the ID §b" + npc.getId());
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

    public MythicMobs getMythicMobs() {
        return mythicMobs;
    }


}
