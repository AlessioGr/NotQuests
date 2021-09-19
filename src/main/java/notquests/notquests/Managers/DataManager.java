package notquests.notquests.Managers;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import notquests.notquests.NotQuests;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * This is the Data Manager which handles loading and saving Player Data, Quest Data and Configurations.
 * The Configuration files 'quests.yml' and 'general.yml' are created here.
 * The MySQL Database is also created here.
 *
 * @author Alessio Gravili
 */
public class DataManager {

    /**
     * Instance of NotQuests is copied over
     */
    private final NotQuests main;
    /**
     * ArrayList for Command Tab Completions. They will be re-used where possible.
     */
    public final List<String> completions = new ArrayList<>();
    public final List<String> standardPlayerCompletions = new ArrayList<>();
    public final List<String> standardEntityTypeCompletions = new ArrayList<>();
    public final List<String> numberCompletions = new ArrayList<>();
    public final List<String> numberPositiveCompletions = new ArrayList<>();
    public final List<String> partialCompletions = new ArrayList<>();
    //MYSQL Database Connection Information
    private String host, port, database, username, password;
    //MYSQL Database Connection Objects
    private Connection connection;
    private Statement statement;

    //Quests.yml Configuration
    private FileConfiguration questsData;
    private File questsDataFile = null;


    /**
     * savingEnabled is true by default. It will be set to false if any error happens when data is loaded from the Database.
     * When this is set to false, no quest or player data will be saved when the plugin is disabled.
     */
    private boolean savingEnabled = true;

    /**
     * If this is set to false, the plugin will try to load NPCs once Citizens is re-loaded or enabled
     */
    private boolean alreadyLoadedNPCs = false;

    //General.yml Configuration
    private File generalConfigFile = null;
    private FileConfiguration generalConfig;

    /**
     * The Data Manager is initialized here. This mainly creates some
     * Array List for generic Tab Completions for various commands.
     * <p>
     * The actual loading of Data doesn't happen here yet.
     *
     * @param main an instance of NotQuests which will be passed over
     */
    public DataManager(NotQuests main) {
        this.main = main;

        /*
         * Fill up the numberCompletions Array List from 0-12 which will be
         * re-used whenever a command accepts a number
         */
        for (int i = -1; i <= 12; i++) {
            numberCompletions.add("" + i);
        }

        /*
         * Same as for numberCompletions, but this one only goes from 1-12
         */
        for (int i = 1; i <= 12; i++) {
            numberPositiveCompletions.add("" + i);
        }

        /*
         * Fill up the standardEntityTypeCompletions Array List with all the
         * Entities which are in the game.
         */
        for (EntityType entityType : EntityType.values()) {
            standardEntityTypeCompletions.add(entityType.toString());
        }

    }

    /**
     * The general.yml configuration file is initialized here. This will create the
     * general.yml config file if it hasn't been created yet. It will also create all
     * the necessary default config values if they are non-existent.
     * <p>
     * This will also load the value from the general.yml config file, like the
     * MySQL Database connection information. If that data is not found, the plugin will
     * stop and throw a warning, since it cannot function without a MySQL database.
     */
    public final void loadGeneralConfig() {
        main.getLogger().log(Level.INFO, "§aNotQuests > Loading general config");

        /*
         * If the generalConfigFile Object doesn't exist yet, this will load the file
         * or create a new general.yml file if it does not exist yet and load it into the
         * generalConfig FileConfiguration object.
         */
        if (generalConfigFile == null) {
            generalConfigFile = new File(main.getDataFolder(), "general.yml");

            if (!generalConfigFile.exists()) {
                try {
                    //Try to create the general.yml config file, and throw an error if it fails.
                    if (!generalConfigFile.createNewFile()) {
                        main.getLogger().log(Level.SEVERE, "§aNotQuests > There was an error creating the general.yml config file.");
                    }
                } catch (IOException ioexception) {
                    ioexception.printStackTrace();
                }
            }

            generalConfig = new YamlConfiguration();
            try {
                generalConfig.load(generalConfigFile);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }
        } else {
            generalConfig = YamlConfiguration.loadConfiguration(generalConfigFile);
        }

        //Load all the MySQL Database Connection information from the general.yml
        host = getGeneralConfig().getString("storage.database.host", "");
        port = getGeneralConfig().getString("storage.database.port", "");
        database = getGeneralConfig().getString("storage.database.database", "");
        username = getGeneralConfig().getString("storage.database.username", "");
        password = getGeneralConfig().getString("storage.database.password", "");

        //Verifies that the loaded information is not empty or null.
        boolean errored = false;

        if (host.equals("")) {
            getGeneralConfig().set("storage.database.host", "");
            errored = true;
        }
        if (port.equals("")) {
            getGeneralConfig().set("storage.database.port", "");
            errored = true;
        }
        if (database.equals("")) {
            getGeneralConfig().set("storage.database.database", "");
            errored = true;
        }
        if (username.equals("")) {
            getGeneralConfig().set("storage.database.username", "");
            errored = true;
        }
        if (password.equals("")) {
            getGeneralConfig().set("storage.database.password", "");
            errored = true;

        }
        saveGeneralConfig();

        //If there was an error loading data from general.yml, the plugin will be disabled
        if (errored) {
            disablePluginAndSaving("Please specify your database information");
        }


    }

    /**
     * This will try to save the general.yml configuration file with the data which is currently in the
     * generalConfig FileConfiguration object.
     */
    public void saveGeneralConfig() {
        try {
            getGeneralConfig().save(generalConfigFile);

        } catch (IOException ioException) {
            main.getLogger().log(Level.SEVERE, "§cNotQuests > General Config file could not be saved.");
        }
    }

    /**
     * This will set saving to false, so the plugin will not try to save any kind of data anymore. After that, it
     * disables the plugin.
     *
     * @param reason the reason for disabling saving and the plugin. Will be shown in the console error message
     */
    public void disablePluginAndSaving(final String reason) {
        main.getLogger().log(Level.SEVERE, "§cNotQuests > Plugin and saving disabled. Reason: " + reason);
        main.getDataManager().setSavingEnabled(false);
        main.getServer().getPluginManager().disablePlugin(main);
    }

    /**
     * If saving is enabled, this will try to save the following data:
     * (1) Player Data into the MySQL Database
     * (2) The quests.yml Quest Configuration file
     * <p>
     * This will not save the general.yml Configuration file
     */
    public void saveData() {
        if (isSavingEnabled()) {
            main.getLogger().log(Level.INFO, "§aNotQuests > Citizens nquestgiver trait has been registered!");
            main.getLogger().log(Level.INFO, "§aNotQuests > Saving player data...");
            main.getQuestPlayerManager().savePlayerData();

            if (questsData == null || questsDataFile == null) {
                main.getLogger().log(Level.SEVERE, "§cNotQuests > Could not save data to quests.yml");
                return;
            }
            try {
                getQuestsData().save(questsDataFile);
                main.getLogger().log(Level.INFO, "§aNotQuests > Saved Data to quests.yml");
            } catch (IOException e) {
                main.getLogger().log(Level.SEVERE, "§cNotQuests > Could not save config to §b" + questsDataFile + "§c. Stacktrace:");
                e.printStackTrace();
            }
        } else {
            main.getLogger().log(Level.WARNING, "§eNotQuests > Saving is disabled => no data has been saved.");
        }


    }

    /**
     * If the plugin has been running for some time, the MySQL Database connection is
     * sometimes interrupted which causes errors when it tries to save data once the plugin
     * is disabled.
     * <p>
     * This is especially bad, because if the plugin has been running for a while, that data will be lost.
     * <p>
     * This method will try to re-open the database connection statement, so data can be saved to the database
     * safely again.
     *
     * @param newTask sets if the plugin should force an asynchronous thread to re-open the database connection. If
     *                set to false, it will do it in whatever thread this method is run in.
     */
    public void refreshDatabaseConnection(final boolean newTask) {
        if (newTask) {
            if (Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTaskAsynchronously(main, () -> {
                    try {
                        openConnection();
                        statement = connection.createStatement();
                    } catch (ClassNotFoundException | SQLException e) {
                        e.printStackTrace();
                    }


                });
            } else {
                try {
                    openConnection();
                    statement = connection.createStatement();
                } catch (ClassNotFoundException | SQLException e) {
                    e.printStackTrace();
                }


            }
        } else {
            try {
                openConnection();
                statement = connection.createStatement();
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }
        }

    }


    /**
     * THis will LOAD the following data:
     * (1) general.yml General Configuration - in whatever Thread
     * (2) CREATE all the necessary MySQL Database tables if they don't exist yet - in an asynchronous Thread (forced)
     * (3) Load all the Quests Data from the quests.yml - in an asynchronous Thread (forced)
     * (4) AFTER THAT load the Player Data from the MySQL Database - in an asynchronous Thread (forced)
     * (5) Then it will try to load the Data from Citizens NPCs
     */
    public void reloadData() {

        loadGeneralConfig();
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(main, () -> {
                try {
                    openConnection();
                    statement = connection.createStatement();
                } catch (ClassNotFoundException | SQLException e) {
                    e.printStackTrace();
                }


                //Create Database tables if they don't exist yet
                try {
                    main.getLogger().log(Level.INFO, "§9NotQuests > §aCreating database table 'QuestPlayerData' if it doesn't exist yet...");
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS `QuestPlayerData` (`PlayerUUID` varchar(200), `QuestPoints` BIGINT(255), PRIMARY KEY (PlayerUUID))");

                    main.getLogger().log(Level.INFO, "§9NotQuests > §aCreating database table 'ActiveQuests' if it doesn't exist yet...");
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS `ActiveQuests` (`QuestName` varchar(200), `PlayerUUID` varchar(200))");

                    main.getLogger().log(Level.INFO, "§9NotQuests > §aCreating database table 'CompletedQuests' if it doesn't exist yet...");
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS `CompletedQuests` (`QuestName` varchar(200), `PlayerUUID` varchar(200), `TimeCompleted` BIGINT(255))");

                    main.getLogger().log(Level.INFO, "§9NotQuests > §aCreating database table 'ActiveObjectives' if it doesn't exist yet...");
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS `ActiveObjectives` (`ObjectiveType` varchar(200), `QuestName` varchar(200), `PlayerUUID` varchar(200), `CurrentProgress` BIGINT(255), `ObjectiveID` INT(255), `HasBeenCompleted` BOOLEAN)");

                    main.getLogger().log(Level.INFO, "§9NotQuests > §aCreating database table 'ActiveTriggers' if it doesn't exist yet...");

                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS `ActiveTriggers` (`TriggerType` varchar(200), `QuestName` varchar(200), `PlayerUUID` varchar(200), `CurrentProgress` BIGINT(255), `TriggerID` INT(255))");


                } catch (SQLException e) {
                    main.getLogger().log(Level.SEVERE, "§9NotQuests > §cThere was an error while trying to load MySQL database tables! This is the stacktrace:");

                    e.printStackTrace();
                    main.getLogger().log(Level.SEVERE, "§cNotQuests > Plugin disabled, because there was an error while initializing tables.");
                    main.getDataManager().setSavingEnabled(false);
                    main.getServer().getPluginManager().disablePlugin(main);
                }

                if (isSavingEnabled()) {
                    main.getLogger().log(Level.INFO, "§aNotQuests > Loaded player data");

                    if (questsDataFile == null) {
                        questsDataFile = new File(main.getDataFolder(), "quests.yml");
                        questsData = YamlConfiguration.loadConfiguration(questsDataFile);
                        main.getLogger().log(Level.INFO, "§aNotQuests > First load of quests.yml. Loading data from it...");
                        main.getQuestManager().loadData();

                    } else {
                        questsData = YamlConfiguration.loadConfiguration(questsDataFile);
                        main.getLogger().log(Level.INFO, "§aNotQuests > Loading Data from existing quests.yml...");
                        main.getQuestManager().loadData();

                    }

                    main.getQuestPlayerManager().loadPlayerData();

                    //IF an NPC exist, try to load NPC data.
                    boolean foundNPC = false;
                    for (final NPC ignored : CitizensAPI.getNPCRegistry().sorted()) {
                        foundNPC = true;
                        break;
                    }
                    if (foundNPC) {
                        loadNPCData();
                    }
                }


            });
        } else { //If this is already an asynchronous thread, this else{ thingy does not try to create a new asynchronous thread for better performance. The contents of this else section is identical.2
            try {
                openConnection();
                statement = connection.createStatement();
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }


            //Create Database tables if they don't exist yet
            try {
                main.getLogger().log(Level.INFO, "§9NotQuests > §aCreating database table 'QuestPlayerData' if it doesn't exist yet...");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `QuestPlayerData` (`PlayerUUID` varchar(200), `QuestPoints` BIGINT(255), PRIMARY KEY (PlayerUUID))");

                main.getLogger().log(Level.INFO, "§9NotQuests > §aCreating database table 'ActiveQuests' if it doesn't exist yet...");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `ActiveQuests` (`QuestName` varchar(200), `PlayerUUID` varchar(200))");

                main.getLogger().log(Level.INFO, "§9NotQuests > §aCreating database table 'CompletedQuests' if it doesn't exist yet...");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `CompletedQuests` (`QuestName` varchar(200), `PlayerUUID` varchar(200), `TimeCompleted` BIGINT(255))");

                main.getLogger().log(Level.INFO, "§9NotQuests > §aCreating database table 'ActiveObjectives' if it doesn't exist yet...");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `ActiveObjectives` (`ObjectiveType` varchar(200), `QuestName` varchar(200), `PlayerUUID` varchar(200), `CurrentProgress` BIGINT(255), `ObjectiveID` INT(255), `HasBeenCompleted` BOOLEAN)");

            } catch (SQLException e) {
                main.getLogger().log(Level.SEVERE, "§9NotQuests > §cThere was an error while trying to load MySQL database tables! This is the stacktrace:");

                e.printStackTrace();
                main.getLogger().log(Level.SEVERE, "§cNotQuests > Plugin disabled, because there was an error while initializing tables.");
                main.getDataManager().setSavingEnabled(false);
                main.getServer().getPluginManager().disablePlugin(main);
            }

            if (isSavingEnabled()) {

                main.getLogger().log(Level.INFO, "§aNotQuests > Loaded player data");

                if (questsDataFile == null) {
                    questsDataFile = new File(main.getDataFolder(), "quests.yml");
                    questsData = YamlConfiguration.loadConfiguration(questsDataFile);
                    main.getQuestManager().loadData();
                    main.getLogger().log(Level.INFO, "§aNotQuests > First load of quests.yml");
                } else {
                    questsData = YamlConfiguration.loadConfiguration(questsDataFile);
                    main.getQuestManager().loadData();
                    main.getLogger().log(Level.INFO, "§aNotQuests > Loading Data from existing quests.yml");
                }

                main.getQuestPlayerManager().loadPlayerData();

                //IF an NPC exist, try to load NPC data.
                boolean foundNPC = false;
                for (final NPC ignored : CitizensAPI.getNPCRegistry().sorted()) {
                    foundNPC = true;
                    break;
                }
                if (foundNPC) {
                    loadNPCData();
                }

            }
        }


    }

    /**
     * This will return the quests.yml Configuration FileConfiguration object.
     * If it does not exist, it will try to load ALL the data again in reloadData()
     * which should also create the quests.yml file
     *
     * @return the quests.yml Configuration FileConfiguration object
     */
    public final FileConfiguration getQuestsData() {
        if (questsData == null) {
            reloadData();
        }
        return questsData;
    }

    /**
     * This will return the general.yml Configuration FileConfiguration object.
     * If it does not exist, it will try to load / create it.
     *
     * @return the general.yml Configuration FileConfiguration object
     */
    public final FileConfiguration getGeneralConfig() {
        if (generalConfig == null) {
            loadGeneralConfig();
        }
        return generalConfig;
    }


    /**
     * This will open a MySQL connection statement which is needed to save and load
     * to the MySQL Database.
     * <p>
     * This is where it tries to log-in to the Database.
     *
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public void openConnection() throws SQLException,
            ClassNotFoundException {
        if (connection != null && !connection.isClosed()) {
            return;
        }
        // Class.forName("com.mysql.jdbc.Driver"); - Use this with old version of the Driver
        Class.forName("com.mysql.cj.jdbc.Driver");
        connection = DriverManager.getConnection("jdbc:mysql://"
                        + this.host + ":" + this.port + "/" + this.database + "?autoReconnect=true",
                this.username, this.password);
    }

    /**
     * @return the MySQL Database Statement
     */
    public final Statement getDatabaseStatement() {
        return statement;
    }

    /**
     * @return if the plugin will try to save data once it's disabled.
     * This should be true unless a severe error occurred during data
     * loading-
     */
    public final boolean isSavingEnabled() {
        return savingEnabled;
    }

    /**
     * @param savingEnabled sets if data saving should be enabled or disabled
     */
    public void setSavingEnabled(boolean savingEnabled) {
        this.savingEnabled = savingEnabled;
    }


    /**
     * This will load the Data from Citizens NPCs asynchronously. It will also make sure
     * that the quests.yml configuration object is valid first.
     * <p>
     * The actual loading of Citizens NPC data will happen in the loadNPCData() function of
     * the Quest Manager. In that function, most of that will run synchronously as that's required
     * for some operations with the Citizens API.
     */
    public void loadNPCData() {
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(main, () -> {

                if (questsDataFile == null) {
                    questsDataFile = new File(main.getDataFolder(), "quests.yml");
                    questsData = YamlConfiguration.loadConfiguration(questsDataFile);
                    main.getQuestManager().loadNPCData();
                    main.getLogger().log(Level.INFO, "§aNotQuests > First load of quests.yml");

                } else {
                    questsData = YamlConfiguration.loadConfiguration(questsDataFile);
                    main.getQuestManager().loadNPCData();
                    main.getLogger().log(Level.INFO, "§aNotQuests > Loading Data from existing quests.yml");

                }
            });
        } else {

            if (questsDataFile == null) {
                questsDataFile = new File(main.getDataFolder(), "quests.yml");
                questsData = YamlConfiguration.loadConfiguration(questsDataFile);
                main.getQuestManager().loadNPCData();
                main.getLogger().log(Level.INFO, "§aNotQuests > First load of quests.yml");

            } else {
                questsData = YamlConfiguration.loadConfiguration(questsDataFile);
                main.getQuestManager().loadNPCData();
                main.getLogger().log(Level.INFO, "§aNotQuests > Loading Data from existing quests.yml");

            }

        }

    }

    /**
     * @return if Citizen NPCs have been already successfully loaded by the plugin
     */
    public boolean isAlreadyLoadedNPCs() {
        return alreadyLoadedNPCs;
    }

    /**
     * @param alreadyLoadedNPCs sets if Citizen NPCs have been already successfully loaded by the plugin
     */
    public void setAlreadyLoadedNPCs(boolean alreadyLoadedNPCs) {
        this.alreadyLoadedNPCs = alreadyLoadedNPCs;
    }

    /**
     * Utility function: Returns the UUID of an online player. If the player is
     * offline, it will return null.
     *
     * @param playerName the name of the online player you want to get the UUID from
     * @return the UUID of the specified, online player
     */
    public final UUID getOnlineUUID(final String playerName) {
        final Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            return player.getUniqueId();
        } else {
            return null;
        }
    }

    /**
     * Utility function: Tries to return the UUID of an offline player (can also be online)
     * via some weird Bukkit function. This probably makes calls to the Minecraft API, I don't
     * know for sure. It's definitely slower.
     *
     * @param playerName the name of the player you want to get the UUID from
     * @return the UUID from the player based on his current username.
     */
    public final UUID getOfflineUUID(final String playerName) {
        return Bukkit.getOfflinePlayer(playerName).getUniqueId();
    }
}
