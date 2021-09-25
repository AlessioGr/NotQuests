package notquests.notquests.Managers;

import net.md_5.bungee.api.ChatColor;
import notquests.notquests.NotQuests;
import org.apache.commons.io.IOUtils;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.ArrayList;
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


    public void loadMissingDefaultLanguageFiles() {
        final ArrayList<String> languageFiles = new ArrayList<>();
        languageFiles.add("en.yml");
        languageFiles.add("de.yml");

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

        for (final String fileName : languageFiles) {
            try {
                main.getLogger().log(Level.INFO, "§aCreating the §b" + fileName + " §alanguage file...");

                File file = new File(languageFolder, fileName);

                if (!file.exists()) {
                    if (!file.createNewFile()) {
                        main.getLogger().log(Level.SEVERE, "§cNotQuests > There was an error creating the " + fileName + " language file. (3)");
                        main.getDataManager().disablePluginAndSaving("There was an error creating the " + fileName + " language file. (3)");
                        return;
                    }

                    InputStream inputStream = main.getResource("translations/" + fileName);
                    //Instead of creating a new language file, we will copy the one from inside of the plugin jar into the plugin folder:
                    if (inputStream != null) {


                        try (OutputStream outputStream = new FileOutputStream(file)) {
                            IOUtils.copy(inputStream, outputStream);
                        } catch (Exception e) {
                            main.getLogger().log(Level.SEVERE, "§cNotQuests > There was an error creating the " + fileName + " language file. (4)");
                            main.getDataManager().disablePluginAndSaving("There was an error creating the " + fileName + " language file. (4)");
                            return;
                        }


                    }
                }


            } catch (IOException ioException) {
                ioException.printStackTrace();
                main.getDataManager().disablePluginAndSaving("There was an error creating the " + fileName + " language file. (3)");
                return;
            }
        }


    }


    /**
     * Load language configs
     */
    public final void loadLanguageConfig() {
        loadMissingDefaultLanguageFiles();

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


        if (setupDefaultStrings()) {
            main.getLogger().info("§5Language Configuration §b" + languageCode + ".yml §5was updated with new values! Saving it...");
            saveLanguageConfig();
        }


        currentLanguage = languageCode;

    }

    public boolean setupDefaultStrings() {
        //Set default values

        boolean valueChanged = false;

        if (!getLanguageConfig().isString("chat.wrong-command-usage")) {
            getLanguageConfig().set("chat.wrong-command-usage", "&cWrong command usage!");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.missing-permission")) {
            getLanguageConfig().set("chat.missing-permission", "&cNo permission! Required permission node: &e%PERMISSION%");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.too-many-arguments")) {
            getLanguageConfig().set("chat.too-many-arguments", "&cToo many arguments!");
            valueChanged = true;
        }

        if (!getLanguageConfig().isString("chat.quest-successfully-accepted")) {
            getLanguageConfig().set("chat.quest-successfully-accepted", "&aYou have successfully accepted the Quest &b%QUESTNAME%&a.");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.quest-description")) {
            getLanguageConfig().set("chat.quest-description", "&eQuest description: &7%QUESTDESCRIPTION%");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.quest-already-accepted")) {
            getLanguageConfig().set("chat.quest-already-accepted", "&cQuest already accepted.");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.quest-failed")) {
            getLanguageConfig().set("chat.quest-failed", "&eYou have &c&lFAILED &ethe Quest &b%QUESTNAME%&e!");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.quest-aborted")) {
            getLanguageConfig().set("chat.quest-aborted", "&aThe active quest &b%QUESTNAME% &ahas been aborted!");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.quest-completed-and-rewards-given")) {
            getLanguageConfig().set("chat.quest-completed-and-rewards-given", "&aYou have completed the quest &b%QUESTNAME% &aand received your rewards!!!");
            valueChanged = true;
        }

        if (!getLanguageConfig().isString("chat.missing-quest-description")) {
            getLanguageConfig().set("chat.missing-quest-description", "&eThis quest has no quest description.");
            valueChanged = true;
        }

        if (!getLanguageConfig().isString("chat.objectives-label-after-quest-accepting")) {
            getLanguageConfig().set("chat.objectives-label-after-quest-accepting", "&9Objectives:");
            valueChanged = true;
        }

        if (!getLanguageConfig().isString("chat.questpoints.query")) {
            getLanguageConfig().set("chat.questpoints.query", "&eYou currently have &b%QUESTPOINTS% &equest points.");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.questpoints.none")) {
            getLanguageConfig().set("chat.questpoints.none", "&cSeems like you don't have any quest points!");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.no-quests-accepted")) {
            getLanguageConfig().set("chat.no-quests-accepted", "&cSeems like you don't have any active quests!");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.quest-does-not-exist")) {
            getLanguageConfig().set("chat.quest-does-not-exist", "&cQuest &b%QUESTNAME% &cdoes not exist!");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.quest-not-found-or-does-not-exist")) {
            getLanguageConfig().set("chat.quest-not-found-or-does-not-exist", "&cQuest &b%QUESTNAME% &cnot found or not active!");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.active-quests-label")) {
            getLanguageConfig().set("chat.active-quests-label", "&eActive quests:");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.take-disabled-preview")) {
            getLanguageConfig().set("chat.take-disabled-preview", "&cPreviewing the quest &b%QUESTNAME% &cis disabled with the &e/nquests preview &ccommand.");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.take-disabled-accept")) {
            getLanguageConfig().set("chat.take-disabled-accept", "&cAccepting the quest &b%QUESTNAME% &cis disabled with the &e/nquests take &ccommand.");
            valueChanged = true;
        }


        if (!getLanguageConfig().isString("titles.quest-accepted.title")) {
            getLanguageConfig().set("titles.quest-accepted.title", "&fQuest accepted");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("titles.quest-accepted.subtitle")) {
            getLanguageConfig().set("titles.quest-accepted.subtitle", "<#b617ff>%QUESTNAME%");
            valueChanged = true;
        }

        if (!getLanguageConfig().isString("titles.quest-completed.title")) {
            getLanguageConfig().set("titles.quest-completed.title", "&aQuest Completed");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("titles.quest-completed.subtitle")) {
            getLanguageConfig().set("titles.quest-completed.subtitle", "<#b617ff>%QUESTNAME%");
            valueChanged = true;
        }

        if (!getLanguageConfig().isString("titles.quest-failed.title")) {
            getLanguageConfig().set("titles.quest-failed.title", "&cQuest Failed");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("titles.quest-failed.subtitle")) {
            getLanguageConfig().set("titles.quest-failed.subtitle", "<#b617ff>%QUESTNAME%");
            valueChanged = true;
        }

        //user /q gui - Main
        if (!getLanguageConfig().isString("gui.main.title")) {
            getLanguageConfig().set("gui.main.title", "                &9Quests");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.main.button.questpoints.text")) {
            getLanguageConfig().set("gui.main.button.questpoints.text", "&6Quest Points\n&eCurrent quest points: &b%QUESTPOINTS%");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.main.button.takequest.text")) {
            getLanguageConfig().set("gui.main.button.takequest.text", "&aTake a Quest\n&7Start a new Quest!\n\n&8Some Quests cannot be\n&8started like this.");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.main.button.abortquest.text")) {
            getLanguageConfig().set("gui.main.button.abortquest.text", "&cAbort a Quest\n&7Aborting a Quest may\n&7lead to punishments.");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.main.button.previewquest.text")) {
            getLanguageConfig().set("gui.main.button.previewquest.text", "&9Preview Quest (Quest Info)\n&7Show more information\n&7about a Quest.");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.main.button.activequests.text")) {
            getLanguageConfig().set("gui.main.button.activequests.text", "&3Active Quests\n&7Shows all of your\n&7active Quests.");
            valueChanged = true;
        }


        //user /q activeQuests
        if (!getLanguageConfig().isString("gui.activeQuests.title")) {
            getLanguageConfig().set("gui.activeQuests.title", "             &9Active Quests");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.activeQuests.button.activeQuestButton.text")) {
            getLanguageConfig().set("gui.activeQuests.button.activeQuestButton.text", "&b%QUESTNAME% &a[ACTIVE]\n&7Progress: &a%COMPLETEDOBJECTIVESCOUNT% &f/ %ALLOBJECTIVESCOUNT%\n&fClick to see details!");
            valueChanged = true;
        }

        //user /q take
        if (!getLanguageConfig().isString("gui.takeQuestChoose.title")) {
            getLanguageConfig().set("gui.takeQuestChoose.title", "&9Which Quest do you want to take?");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.takeQuestChoose.button.questPreview.acceptedSuffix")) {
            getLanguageConfig().set("gui.takeQuestChoose.button.questPreview.acceptedSuffix", " &a[ACCEPTED]");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.takeQuestChoose.button.questPreview.questNamePrefix")) {
            getLanguageConfig().set("gui.takeQuestChoose.button.questPreview.questNamePrefix", "&b");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.takeQuestChoose.button.questPreview.questDescriptionPrefix")) {
            getLanguageConfig().set("gui.takeQuestChoose.button.questPreview.questDescriptionPrefix", "&8");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.takeQuestChoose.button.questPreview.bottomText")) {
            getLanguageConfig().set("gui.takeQuestChoose.button.questPreview.bottomText", "&aClick to preview Quest!");
            valueChanged = true;
        }

        //user /q abort
        if (!getLanguageConfig().isString("gui.abortQuestChoose.title")) {
            getLanguageConfig().set("gui.abortQuestChoose.title", "  &9Active Quests you can abort");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.abortQuestChoose.button.abortQuestPreview.text")) {
            getLanguageConfig().set("gui.abortQuestChoose.button.abortQuestPreview.text", "&b%QUESTNAME% &a[ACTIVE]\n&7Progress: &a%COMPLETEDOBJECTIVESCOUNT% &f/ %ALLOBJECTIVESCOUNT%\n&cClick to abort Quest!");
            valueChanged = true;
        }

        //user /q preview
        if (!getLanguageConfig().isString("gui.previewQuestChoose.title")) {
            getLanguageConfig().set("gui.previewQuestChoose.title", "     &9Choose Quest to preview");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.previewQuestChoose.button.questPreview.acceptedSuffix")) {
            getLanguageConfig().set("gui.previewQuestChoose.button.questPreview.acceptedSuffix", " &a[ACCEPTED]");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.previewQuestChoose.button.questPreview.questNamePrefix")) {
            getLanguageConfig().set("gui.previewQuestChoose.button.questPreview.questNamePrefix", "&b");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.previewQuestChoose.button.questPreview.questDescriptionPrefix")) {
            getLanguageConfig().set("gui.previewQuestChoose.button.questPreview.questDescriptionPrefix", "&8");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.previewQuestChoose.button.questPreview.bottomText")) {
            getLanguageConfig().set("gui.previewQuestChoose.button.questPreview.bottomText", "&aClick to preview Quest!");
            valueChanged = true;
        }


        return valueChanged;

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
