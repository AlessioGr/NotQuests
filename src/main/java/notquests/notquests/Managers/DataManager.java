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


public class DataManager {

    //Completions
    public final List<String> completions = new ArrayList<String>();
    public final List<String> standardPlayerCompletions = new ArrayList<String>();
    public final List<String> standardEntityTypeCompletions = new ArrayList<String>();
    public final List<String> numberCompletions = new ArrayList<String>();
    public final List<String> numberPositiveCompletions = new ArrayList<String>();
    public final List<String> partialCompletions = new ArrayList<String>();
    private final NotQuests main;
    //MySQL Database stuff
    private String host, port, database, username, password;
    private FileConfiguration questsData;
    private File questsDataFile = null;
    private Connection connection;
    private Statement statement;
    private boolean savingEnabled = true;
    private boolean alreadyLoadedNPCs = false;

    //General Config
    private File generalConfigFile = null;
    private FileConfiguration generalConfig;

    public DataManager(NotQuests main) {
        this.main = main;

        for (int i = -1; i <= 12; i++) {
            numberCompletions.add("" + i);
        }
        for (int i = 1; i <= 12; i++) {
            numberPositiveCompletions.add("" + i);
        }
        for (EntityType entityType : EntityType.values()) {
            standardEntityTypeCompletions.add(entityType.toString());
        }

    }

    public final void loadGeneralConfig() {
        main.getLogger().log(Level.INFO, "§aNotQuests > Loading general config");
        if (generalConfigFile == null) {
            generalConfigFile = new File(main.getDataFolder(), "general.yml");

            if (!generalConfigFile.exists()) {
                try {
                    generalConfigFile.createNewFile();

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


        host = getGeneralConfig().getString("storage.database.host", "");
        port = getGeneralConfig().getString("storage.database.port", "");
        database = getGeneralConfig().getString("storage.database.database", "");
        username = getGeneralConfig().getString("storage.database.username", "");
        password = getGeneralConfig().getString("storage.database.password", "");

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
        if (errored) {
            disablePluginAndSaving("Please specify your database information");
        }


    }

    public void saveGeneralConfig() {
        try {
            getGeneralConfig().save(generalConfigFile);

        } catch (IOException ioException) {
            main.getLogger().log(Level.SEVERE, "§cNotQuests > General Config file could not be saved.");
        }
    }

    public void disablePluginAndSaving(final String reason) {
        main.getLogger().log(Level.SEVERE, "§cNotQuests > Plugin and saving disabled. Reason: " + reason);
        main.getDataManager().setSavingEnabled(false);
        main.getServer().getPluginManager().disablePlugin(main);
    }

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


                //Create tables
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


                    boolean foundNPC = false;
                    for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
                        foundNPC = true;
                        break;
                    }
                    if (foundNPC) {
                        loadNPCData();
                    }
                }


            });
        } else {
            try {
                openConnection();
                statement = connection.createStatement();
            } catch (ClassNotFoundException | SQLException e) {
                e.printStackTrace();
            }


            //Create table
            try {
                main.getLogger().log(Level.INFO, "§9NotQuests > §aCreating database table 'QuestPlayerData' if it doesn't exist yet...");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `QuestPlayerData` (`PlayerUUID` varchar(200), `QuestPoints` BIGINT(255), PRIMARY KEY (PlayerUUID))");

                main.getLogger().log(Level.INFO, "§9NotQuests > §aCreating database table 'ActiveQuests' if it doesn't exist yet...");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `ActiveQuests` (`QuestName` varchar(200), `PlayerUUID` varchar(200))");

                main.getLogger().log(Level.INFO, "§9NotQuests > §aCreating database table 'CompletedQuests' if it doesn't exist yet...");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `CompletedQuests` (`QuestName` varchar(200), `PlayerUUID` varchar(200), `TimeCompleted` BIGINT(255))");

                main.getLogger().log(Level.INFO, "§9NotQuests > §aCreating database table 'ActiveObjectives' if it doesn't exist yet...");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS `ActiveObjectives` (`ObjectiveType` varchar(200), `QuestName` varchar(200), `PlayerUUID` varchar(200), `CurrentProgress` BIGINT(255), `ObjectiveID` INT(255), `HasBeenCompleted` BOOLEAN)");

      /*  System.out.println("§9NotQuests > §eCreating database table 'CompletedObjectives' if it doesn't exist yet...");
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS `CompletedObjectives` (`ObjectiveType` varchar(200), `QuestName` varchar(200), `PlayerUUID` varchar(200))");
*/
                // ResultSet res = statement.executeQuery("");
                // res.next();
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

                boolean foundNPC = false;
                for (final NPC npc : CitizensAPI.getNPCRegistry().sorted()) {
                    foundNPC = true;
                    break;
                }
                if (foundNPC) {
                    loadNPCData();
                }

            }
        }


    }


    public final FileConfiguration getQuestsData() {
        if (questsData == null) {
            reloadData();
        }
        return questsData;
    }

    public final FileConfiguration getGeneralConfig() {
        if (generalConfig == null) {
            loadGeneralConfig();
        }
        return generalConfig;
    }


    //MySQL stuff
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

    public final Statement getDatabaseStatement() {
        return statement;
    }

    public final boolean isSavingEnabled() {
        return savingEnabled;
    }

    public void setSavingEnabled(boolean savingEnabled) {
        this.savingEnabled = savingEnabled;
    }

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

    public boolean isAlreadyLoadedNPCs() {
        return alreadyLoadedNPCs;
    }

    public void setAlreadyLoadedNPCs(boolean alreadyLoadedNPCs) {
        this.alreadyLoadedNPCs = alreadyLoadedNPCs;
    }

    public final UUID getOnlineUUID(final String playerName) {
        final Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            return player.getUniqueId();
        } else {
            return null;
        }
    }

    public final UUID getOfflineUUID(final String playerName) {
        return Bukkit.getOfflinePlayer(playerName).getUniqueId();
    }
}
