
package notquests.notquests;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitInfo;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import notquests.notquests.Commands.CommandNotQuests;
import notquests.notquests.Commands.CommandNotQuestsAdmin;
import notquests.notquests.Events.CitizensEvents;
import notquests.notquests.Events.QuestEvents;
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
    private DataManager dataManager;
    private QuestManager questManager;
    private QuestPlayerManager questPlayerManager;
    private LanguageManager languageManager;

    //Vault
    private Economy econ = null;
    private Permission perms = null;
    private Chat chat = null;

    //Metrics
    private Metrics metrics;

    //Enabled Features
    private boolean vaultEnabled = false;
    private boolean citizensEnabled = false;


    /**
     * Called when the plugin is enabled. A bunch of stuff is initialized here
     */
    @Override
    public void onEnable() {
        getLogger().log(Level.INFO, "§aNotQuests > NotQuests is starting...");

        //Vault is needed for NotQuests to function. If it's not found, NotQuests will be disabled. EDIT: Now it will just disable some features
        if (!setupEconomy()) {
            getLogger().log(Level.INFO, "§cVault Dependency not found! Some features have been disabled. I recommend you to install Vault for the best experience.");
            //getServer().getPluginManager().disablePlugin(this);
            //return;
        }else{
            setupPermissions();
            setupChat();
            vaultEnabled = true;
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

        //The plugin "Citizens" is currently required for NotQuests to run properly. If it's not found, NotQuests will be disabled. EDIT: Now it will just disable some features
        if (getServer().getPluginManager().getPlugin("Citizens") == null || !Objects.requireNonNull(getServer().getPluginManager().getPlugin("Citizens")).isEnabled()) {
            getLogger().log(Level.INFO, "§cCitizens Dependency not found! Some features regarding NPCs have been disabled. I recommend you to install Citizens for the best experience.");

            //getLogger().log(Level.SEVERE, "§cNotQuests > Citizens 2.0 not found or not enabled");
            //getServer().getPluginManager().disablePlugin(this);
            //return;
        }else{
            citizensEnabled = true;
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

        //Register the Event Listeners in CitizensEvents, if Citizens integration is enabled
        if(isCitizensEnabled()){
            getServer().getPluginManager().registerEvents(new CitizensEvents(this), this);
        }

        //This finally starts loading all Config-, Quest-, and Player Data. Reload = Load
        dataManager.reloadData();

        //This registers all PlaceholderAPI placeholders, if loading is enabled
        if(getDataManager().isLoadingEnabled()){
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new QuestPlaceholders(this).register();
            }

            //Update Checker
            try {
                final UpdateChecker updateChecker = new UpdateChecker(this, 95872);
                if (updateChecker.checkForUpdates()){
                    getLogger().info("§6The version §e" + getDescription().getVersion()
                            + " §6is not the latest version! Please update the plugin here: §bhttps://www.spigotmc.org/resources/95872/");
                }else{
                    getLogger().info("NotQuests seems to be up to date! :)");
                }
            } catch (Exception e) {
                e.printStackTrace();
                getLogger().info("Unable to check for updates ('" + e.getMessage() + "').");
            }

            //bStats statistics
            final int pluginId = 12824; // <-- Replace with the id of your plugin!
            metrics = new Metrics(this, pluginId);
        }






    }

    /**
     * Called when the plugin is disabled or reloaded via ServerUtils / PlugMan
     */
    @Override
    public void onDisable() {
        getLogger().log(Level.INFO, "§aNotQuests > NotQuests is shutting down...");

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
                    getLogger().log(Level.INFO, "§aNotQuests > Removed nquestgiver trait from NPC with the ID §b" + npc.getId());
                }
                traitsToRemove.clear();

            }

            /*
             * Next, the nquestgiver trait itself which is registered via the Citizens API on startup is being
             * de-registered.
             */
            getLogger().log(Level.INFO, "§aNotQuests > Deregistering nquestgiver trait...");
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
            getLogger().log(Level.SEVERE, "§cError: Tried to load Economy when Vault is not enabled. Please report this to the plugin author (and I also recommend you installing Vault for money stuff to work)");

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

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

}
