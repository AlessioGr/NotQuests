package notquests.notquests.Managers;

import net.md_5.bungee.api.ChatColor;
import notquests.notquests.NotQuests;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LanguageManager {
    private final NotQuests main;
    /**
     * Configuration objects which contains values from General.yml
     */
    private final Configuration configuration;
    private final Pattern hexPattern = Pattern.compile("<#([A-Fa-f0-9]){6}>");
    /**
     * General.yml Configuration
     */
    private File languageConfigFile = null;
    /**
     * General.yml Configuration File
     */
    private FileConfiguration languageConfig;
    private String currentLanguage = "en";


    public LanguageManager(final NotQuests main) {
        this.main = main;

        // create an instance of the Configuration object
        configuration = new Configuration();
    }


    /**
     * Load language configs
     */
    public final void loadLanguageConfig() {
        final String languageCode = main.getDataManager().getConfiguration().getLanguageCode();
        main.getLogger().log(Level.INFO, "§5NotQuests > Loading language config §b" + languageCode + ".yml");

        /*
         * If the generalConfigFile Object doesn't exist yet, this will load the file
         * or create a new general.yml file if it does not exist yet and load it into the
         * generalConfig FileConfiguration object.
         */
        if (languageConfigFile == null || !currentLanguage.equals(languageCode)) {

            //Create the Data Folder if it does not exist yet (the NotQuests folder)
            if (!main.getDataFolder().exists()) {
                main.getLogger().log(Level.INFO, "§aNotQuests > Data Folder not found. Creating a new one...");

                if (!main.getDataFolder().mkdirs()) {
                    main.getLogger().log(Level.SEVERE, "§cNotQuests > There was an error creating the NotQuests data folder");
                    main.getDataManager().disablePluginAndSaving("There was an error creating the NotQuests data folder.");
                    return;
                }

            }


            //Create the Language Data Folder if it does not exist yet (the NotQuests/languages folder)
            File languageFolder = new File(main.getDataFolder().getPath() + "/languages/");
            if (!languageFolder.exists()) {
                main.getLogger().log(Level.INFO, "§aNotQuests > Languages Folder not found. Creating a new one...");

                if (!languageFolder.mkdirs()) {
                    main.getLogger().log(Level.SEVERE, "§cNotQuests > There was an error creating the NotQuests languages folder");
                    main.getDataManager().disablePluginAndSaving("There was an error creating the NotQuests languages folder.");
                    return;
                }

            }

            languageConfigFile = new File(languageFolder, main.getDataManager().getConfiguration().getLanguageCode() + ".yml");

            if (!languageConfigFile.exists()) {
                main.getLogger().log(Level.INFO, "§aNotQuests > Language Configuration (" + main.getDataManager().getConfiguration().getLanguageCode() + ".yml) does not exist. Creating a new one...");

                //Does not work yet, since comments are overridden if something is saved
                //saveDefaultConfig();


                try {
                    //Try to create the language.yml config file, and throw an error if it fails.

                    if (!languageConfigFile.createNewFile()) {
                        main.getLogger().log(Level.SEVERE, "§cNotQuests > There was an error creating the " + main.getDataManager().getConfiguration().getLanguageCode() + ".yml language file. (1)");
                        main.getDataManager().disablePluginAndSaving("There was an error creating the " + main.getDataManager().getConfiguration().getLanguageCode() + ".yml language file.");
                        return;

                    }
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    main.getDataManager().disablePluginAndSaving("There was an error creating the " + main.getDataManager().getConfiguration().getLanguageCode() + ".yml config file. (2)");
                    return;
                }
            }

            languageConfig = new YamlConfiguration();
            try {
                languageConfig.load(languageConfigFile);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }
        } else {
            languageConfig = YamlConfiguration.loadConfiguration(languageConfigFile);
        }


        setupDefaultStrings();
        saveLanguageConfig();

        currentLanguage = languageCode;

    }

    public void setupDefaultStrings() {
        //Set default values


        if (!getLanguageConfig().isString("chat.quest-successfully-accepted")) {
            getLanguageConfig().set("chat.quest-successfully-accepted", "&aYou have successfully accepted the Quest &b%QUESTNAME%&a.");
        }
        if (!getLanguageConfig().isString("chat.quest-already-accepted")) {
            getLanguageConfig().set("chat.quest-already-accepted", "&cQuest already accepted.");
        }
        if (!getLanguageConfig().isString("chat.quest-failed")) {
            getLanguageConfig().set("chat.quest-failed", "&eYou have &c&lFAILED &ethe Quest &b%QUESTNAME%&e!");
        }
        if (!getLanguageConfig().isString("chat.quest-aborted")) {
            getLanguageConfig().set("chat.quest-aborted", "&aThe active quest &b%QUESTNAME% &ahas been aborted!");
        }
        if (!getLanguageConfig().isString("chat.quest-completed-and-rewards-given")) {
            getLanguageConfig().set("chat.quest-completed-and-rewards-given", "&aYou have completed the quest &b%QUESTNAME% &aand received your rewards!!!");
        }

        if (!getLanguageConfig().isString("chat.missing-quest-description")) {
            getLanguageConfig().set("chat.missing-quest-description", "&eThis quest has no quest description.");
        }

        if (!getLanguageConfig().isString("chat.objectives-label-after-quest-accepting")) {
            getLanguageConfig().set("chat.objectives-label-after-quest-accepting", "&9Objectives:");
        }

        if (!getLanguageConfig().isString("chat.questpoints")) {
            getLanguageConfig().set("chat.questpoints", "&eYou currently have &b%QUESTPOINTS% &equest points.");
        }


        if (!getLanguageConfig().isString("titles.quest-accepted.title")) {
            getLanguageConfig().set("titles.quest-accepted.title", "&fQuest accepted");
        }
        if (!getLanguageConfig().isString("titles.quest-accepted.subtitle")) {
            getLanguageConfig().set("titles.quest-accepted.subtitle", "<#b617ff>%QUESTNAME%");
        }

        if (!getLanguageConfig().isString("titles.quest-completed.title")) {
            getLanguageConfig().set("titles.quest-completed.title", "&aQuest Completed");
        }
        if (!getLanguageConfig().isString("titles.quest-completed.subtitle")) {
            getLanguageConfig().set("titles.quest-completed.subtitle", "<#b617ff>%QUESTNAME%");
        }

        if (!getLanguageConfig().isString("titles.quest-failed.title")) {
            getLanguageConfig().set("titles.quest-failed.title", "&cQuest Failed");
        }
        if (!getLanguageConfig().isString("titles.quest-failed.subtitle")) {
            getLanguageConfig().set("titles.quest-failed.subtitle", "<#b617ff>%QUESTNAME%");
        }

        //user /q gui - Main
        if (!getLanguageConfig().isString("gui.main.title")) {
            getLanguageConfig().set("gui.main.title", "                &9Quests");
        }
        if (!getLanguageConfig().isString("gui.main.button.questpoints.text")) {
            getLanguageConfig().set("gui.main.button.questpoints.text", "&6Quest Points\n&eCurrent quest points: &b%QUESTPOINTS%");
        }
        if (!getLanguageConfig().isString("gui.main.button.takequest.text")) {
            getLanguageConfig().set("gui.main.button.takequest.text", "&aTake a Quest\n&7Start a new Quest!\n\n&8Some Quests cannot be\n&8started like this.");
        }
        if (!getLanguageConfig().isString("gui.main.button.abortquest.text")) {
            getLanguageConfig().set("gui.main.button.abortquest.text", "&cAbort a Quest\n&7Aborting a Quest may\n&7lead to punishments.");
        }
        if (!getLanguageConfig().isString("gui.main.button.previewquest.text")) {
            getLanguageConfig().set("gui.main.button.previewquest.text", "&9Preview Quest (Quest Info)\n&7Show more information\n&7about a Quest.");
        }
        if (!getLanguageConfig().isString("gui.main.button.activequests.text")) {
            getLanguageConfig().set("gui.main.button.activequests.text", "&3Active Quests\n&7Shows all of your\n&7active Quests.");
        }

    }


    /**
     * This will return the language FileConfiguration object
     */
    public final FileConfiguration getLanguageConfig() {
        if (languageConfig == null) {
            loadLanguageConfig();
        }
        return languageConfig;
    }

    public final String getString(final String languageString) {
        if (!getLanguageConfig().isString(languageString)) {
            return "Language string not found: " + languageString;
        } else {
            return applyColor(getLanguageConfig().getString(languageString));
        }

    }


    public String applyColor(String message) {
        Matcher matcher = hexPattern.matcher(message);
        while (matcher.find()) {
            final ChatColor hexColor = ChatColor.of(matcher.group().substring(1, matcher.group().length() - 1));
            final String before = message.substring(0, matcher.start());
            final String after = message.substring(matcher.end());
            message = before + hexColor + after;
            matcher = hexPattern.matcher(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }


    /**
     * This will try to save the language configuration file with the data which is currently in the
     * languageConfig FileConfiguration object.
     */
    public void saveLanguageConfig() {
        try {
            getLanguageConfig().save(languageConfigFile);

        } catch (IOException ioException) {
            main.getLogger().log(Level.SEVERE, "§cNotQuests > Language Config file could not be saved.");
        }
    }

}
