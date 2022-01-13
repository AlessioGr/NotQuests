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

package rocks.gravili.notquests.paper.managers;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.io.IOUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.triggers.Trigger;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LanguageManager {
    private final NotQuests main;
    /**
     * Configuration objects which contains values from General.yml
     */
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

    File languageFolder = null;

    private FileConfiguration defaultLanguageConfig = null;

    final Map<String, String> internalPlaceholderReplacements;


    public LanguageManager(final NotQuests main) {
        this.main = main;
        internalPlaceholderReplacements = new HashMap<>();
    }


    public void loadMissingDefaultLanguageFiles() {
        //Create the Language Data Folder if it does not exist yet (the NotQuests/languages folder)
        languageFolder = new File(main.getMain().getDataFolder().getPath() + "/languages/");


        final ArrayList<String> languageFiles = new ArrayList<>();
        languageFiles.add("af-ZA.yml");
        languageFiles.add("ar-SA.yml");
        languageFiles.add("ca-ES.yml");
        languageFiles.add("cs-CZ.yml");
        languageFiles.add("da-DK.yml");
        languageFiles.add("de-DE.yml");
        languageFiles.add("el-GR.yml");
        languageFiles.add("en-US.yml");
        languageFiles.add("es-ES.yml");
        languageFiles.add("fi-FI.yml");
        languageFiles.add("fr-FR.yml");
        languageFiles.add("he-IL.yml");
        languageFiles.add("hu-HU.yml");
        languageFiles.add("id-ID.yml");
        languageFiles.add("it-IT.yml");
        languageFiles.add("ja-JP.yml");
        languageFiles.add("ko-KR.yml");
        languageFiles.add("nl-NL.yml");
        languageFiles.add("no-NO.yml");
        languageFiles.add("pl-PL.yml");
        languageFiles.add("pt-PT.yml");
        languageFiles.add("ro-RO.yml");
        languageFiles.add("ru-RU.yml");
        languageFiles.add("sr-Cyrl.yml");
        languageFiles.add("sr-Latn.yml");
        languageFiles.add("sv-SE.yml");
        languageFiles.add("tr-TR.yml");
        languageFiles.add("uk-UA.yml");
        languageFiles.add("vi-VN.yml");
        languageFiles.add("zh-CN.yml");
        languageFiles.add("zh-TW.yml");


        if (!languageFolder.exists()) {
            main.getLogManager().info(LogCategory.LANGUAGE, "Languages Folder not found. Creating a new one...");
            if (!languageFolder.mkdirs()) {
                main.getDataManager().disablePluginAndSaving("There was an error creating the NotQuests languages folder.");
                return;
            }
        }

        for (final String fileName : languageFiles) {
            try {
                File file = new File(languageFolder, fileName);


                if (!file.exists()) {
                    main.getLogManager().info(LogCategory.LANGUAGE, "Creating the <highlight>" + fileName + "</highlight> language file...");
                    if (!file.createNewFile()) {
                        main.getDataManager().disablePluginAndSaving("There was an error creating the " + fileName + " language file. (3)");
                        return;
                    }

                    InputStream inputStream = main.getMain().getResource("translations/" + fileName);
                    //Instead of creating a new language file, we will copy the one from inside of the plugin jar into the plugin folder:
                    if (inputStream != null) {
                        try (OutputStream outputStream = new FileOutputStream(file)) {
                            IOUtils.copy(inputStream, outputStream);
                        } catch (Exception e) {
                            main.getDataManager().disablePluginAndSaving("There was an error creating the " + fileName + " language file. (4)", e);
                            return;
                        }

                    }
                }


                //Doesn't matter if the en-US.yml exists in the plugin folder or not, because we're reading it from the internal resources folder
                if (fileName.equals("en-US.yml")) {
                    //Copy to default.yml
                    main.getLogManager().info(LogCategory.LANGUAGE, "Creating or updating default.yml...");
                    File defaultFile = new File(languageFolder, "default.yml");

                    InputStream inputStream = main.getMain().getResource("translations/en-US.yml");
                    //Instead of creating a new language file, we will copy the one from inside of the plugin jar into the plugin folder:
                    if (inputStream != null) {
                        try (OutputStream defaultOutputStream = new FileOutputStream(defaultFile)) {
                            IOUtils.copy(inputStream, defaultOutputStream);
                            //Put into fileConfiguration

                            if (!defaultFile.exists()) {
                                main.getDataManager().disablePluginAndSaving("There was an error reading the default.yml language file. (5)");
                                return;
                            }
                            defaultLanguageConfig = new YamlConfiguration();
                            defaultLanguageConfig.load(defaultFile);

                        } catch (Exception e) {
                            main.getDataManager().disablePluginAndSaving("There was an error creating the default.yml language file. (6)", e);
                            return;
                        }
                    } else {
                        main.getDataManager().disablePluginAndSaving("There was an error creating the default.yml language file. (7)");
                        return;
                    }


                }


            } catch (IOException ioException) {
                main.getDataManager().disablePluginAndSaving("There was an error creating the " + fileName + " language file. (3)", ioException);
                return;
            }
        }


    }


    /**
     * Load language configs
     */
    public final void loadLanguageConfig() {
        loadMissingDefaultLanguageFiles();

        final String languageCode = main.getConfiguration().getLanguageCode();
        main.getLogManager().info(LogCategory.LANGUAGE, "Loading language config <highlight>" + languageCode + ".yml");

        /*
         * If the generalConfigFile Object doesn't exist yet, this will load the file
         * or create a new general.yml file if it does not exist yet and load it into the
         * generalConfig FileConfiguration object.
         */
        if (languageConfigFile == null || !currentLanguage.equals(languageCode)) {

            //Create the Data Folder if it does not exist yet (the NotQuests folder)
            main.getDataManager().prepareDataFolder();


            if (languageFolder == null) {
                languageFolder = new File(main.getMain().getDataFolder().getPath() + "/languages/");
            }

            if (!languageFolder.exists()) {
                main.getLogManager().info(LogCategory.LANGUAGE, "Languages Folder not found. Creating a new one...");

                if (!languageFolder.mkdirs()) {
                    main.getDataManager().disablePluginAndSaving("There was an error creating the NotQuests languages folder.");
                    return;
                }

            }

            languageConfigFile = new File(languageFolder, main.getConfiguration().getLanguageCode() + ".yml");

            if (!languageConfigFile.exists()) {
                main.getLogManager().info(LogCategory.LANGUAGE, "Language Configuration (" + main.getConfiguration().getLanguageCode() + ".yml) does not exist. Creating a new one...");

                //Does not work yet, since comments are overridden if something is saved
                //saveDefaultConfig();


                try {
                    //Try to create the language.yml config file, and throw an error if it fails.

                    if (!languageConfigFile.createNewFile()) {
                        main.getDataManager().disablePluginAndSaving("There was an error creating the " + main.getConfiguration().getLanguageCode() + ".yml language file.");
                        return;

                    }
                } catch (IOException ioException) {
                    main.getDataManager().disablePluginAndSaving("There was an error creating the " + main.getConfiguration().getLanguageCode() + ".yml config file. (2)", ioException);
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
            main.getLogManager().info("<DARK_PURPLE>Language Configuration <highlight>" + languageCode + ".yml <DARK_PURPLE>was updated with new values! Saving it...");
            saveLanguageConfig();
        }


        currentLanguage = languageCode;

    }

    public boolean setupDefaultStrings() {
        //Set default values

        if (defaultLanguageConfig == null) {
            main.getDataManager().disablePluginAndSaving("There was an error reading the default.yml language configuration.");
            return false;
        }

        boolean valueChanged = false;
        final ConfigurationSection defaultConfigurationSection = defaultLanguageConfig.getConfigurationSection("");
        if (defaultConfigurationSection != null) {
            for (final String defaultString : defaultConfigurationSection.getKeys(true)) {

                if (!defaultConfigurationSection.isString(defaultString)) {
                    //main.getLogManager().log(Level.INFO, "Skipping: <highlight>" + defaultString + "</highlight>");
                    continue;
                }

                if (!getLanguageConfig().isString(defaultString)) {
                    main.getLogManager().info(LogCategory.LANGUAGE, "Updating string: <highlight>" + defaultString + "</highlight>");

                    getLanguageConfig().set(defaultString, defaultConfigurationSection.getString(defaultString));
                    valueChanged = true;
                }
            }
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

    public final Component getComponent(final String languageString, final Player targetPlayer, Object... internalPlaceholderObjects){
        return main.parse(getString(languageString, targetPlayer, internalPlaceholderObjects)).decoration(TextDecoration.ITALIC, false);
    }

    public final List<Component> getComponentList(final String languageString, final Player targetPlayer, Object... internalPlaceholderObjects){
        List<Component> components = new ArrayList<>();
        if (!getLanguageConfig().isList(languageString)) {
            return Collections.singletonList(Component.text("Language string not found: " + languageString));
        } else {
            final List<String> translatedString = getLanguageConfig().getStringList(languageString);
            if (translatedString.isEmpty()) {
                return Collections.singletonList(Component.text("Language string not found: " + languageString));
            }

            if (!main.getConfiguration().supportPlaceholderAPIInTranslationStrings || !main.getIntegrationsManager().isPlaceholderAPIEnabled() || targetPlayer == null) {
                for(String componentPart : applySpecial(applyInternalPlaceholders(translatedString, targetPlayer, internalPlaceholderObjects))){
                    components.add(main.parse(componentPart).decoration(TextDecoration.ITALIC, false));
                }
            } else {
                for(String componentPart : applySpecial(PlaceholderAPI.setPlaceholders(targetPlayer, applyInternalPlaceholders(translatedString, targetPlayer, internalPlaceholderObjects)))) {
                    components.add(main.parse(componentPart).decoration(TextDecoration.ITALIC, false));
                }
            }
        }
        return components;
    }

    public final int getInt(final String languageString) {
        if (!getLanguageConfig().isInt(languageString)) {
            return 0;
        } else {
            return getLanguageConfig().getInt(languageString);
        }
    }

    public final String getString(final String languageString, final Player targetPlayer, Object... internalPlaceholderObjects) {
        if (!getLanguageConfig().isString(languageString)) {
            return "Language string not found: " + languageString;
        } else {
            final String translatedString = getLanguageConfig().getString(languageString);
            if (translatedString == null) {
                return "Language string not found: " + languageString;
            }
            if (!main.getConfiguration().supportPlaceholderAPIInTranslationStrings || !main.getIntegrationsManager().isPlaceholderAPIEnabled() || targetPlayer == null) {
                return applySpecial(applyInternalPlaceholders(translatedString, targetPlayer, internalPlaceholderObjects)); //Removed applyColor( for minimessage support
            } else {
                return applySpecial(PlaceholderAPI.setPlaceholders(targetPlayer, applyInternalPlaceholders(translatedString, targetPlayer, internalPlaceholderObjects)));
            }
        }
    }

    public final List<String> getStringList(final String languageString, final Player targetPlayer, Object... internalPlaceholderObjects) {
        if (!getLanguageConfig().isList(languageString)) {
            return Collections.singletonList("Language string list not found: " + languageString);
        } else {
            final List<String> translatedString = getLanguageConfig().getStringList(languageString);
            if (translatedString.isEmpty()) {
                return Collections.singletonList("Language string list not found: " + languageString);
            }
            if (!main.getConfiguration().supportPlaceholderAPIInTranslationStrings || !main.getIntegrationsManager().isPlaceholderAPIEnabled() || targetPlayer == null) {
                return applySpecial(applyInternalPlaceholders(translatedString, targetPlayer, internalPlaceholderObjects)); //Removed applyColor( for minimessage support
            } else {
                return applySpecial(PlaceholderAPI.setPlaceholders(targetPlayer, applyInternalPlaceholders(translatedString, targetPlayer, internalPlaceholderObjects)));
            }
        }
    }

    public List<String> applyInternalPlaceholders(List<String> initialMessage,final Player player, Object... internalPlaceholderObjects) {
        List<String> toReturn = new ArrayList<>();
        for(String message : initialMessage){
            toReturn.add(applyInternalPlaceholders(message, player, internalPlaceholderObjects));
        }
        return toReturn;
    }

    public String applyInternalPlaceholders(String initialMessage, final Player player, Object... internalPlaceholderObjects) {
        if (internalPlaceholderObjects.length == 0) {
            return initialMessage;
        }
        internalPlaceholderReplacements.clear();
        for (Object internalPlaceholderObject : internalPlaceholderObjects) {
            if (internalPlaceholderObject instanceof ActiveQuest activeQuest) {
                //main.getLogManager().log(Level.INFO, "Applying ActiveQuest placeholders...");
                internalPlaceholderReplacements.put("%QUESTNAME%", activeQuest.getQuest().getQuestFinalName());
                internalPlaceholderReplacements.put("%QUESTDESCRIPTION%", activeQuest.getQuest().getQuestDescription());
                internalPlaceholderReplacements.put("%COMPLETEDOBJECTIVESCOUNT%", "" + activeQuest.getCompletedObjectives().size());
                internalPlaceholderReplacements.put("%ALLOBJECTIVESCOUNT%", "" + activeQuest.getQuest().getObjectives().size());
            } else if (internalPlaceholderObject instanceof Quest quest) {
                //main.getLogManager().log(Level.INFO, "Applying Quest placeholders...");
                internalPlaceholderReplacements.put("%QUESTNAME%", quest.getQuestFinalName());
                internalPlaceholderReplacements.put("%QUESTDESCRIPTION%", quest.getQuestDescription());
            } else if (internalPlaceholderObject instanceof ActiveObjective activeObjective) {
                //main.getLogManager().log(Level.INFO, "Applying ActiveObjective placeholders...");
                internalPlaceholderReplacements.put("%OBJECTIVEID%", "" + activeObjective.getObjective().getObjectiveID());
                internalPlaceholderReplacements.put("%ACTIVEOBJECTIVEID%", "" + activeObjective.getObjective().getObjectiveID());
                internalPlaceholderReplacements.put("%OBJECTIVENAME%", "" + activeObjective.getObjective().getObjectiveFinalName());
                internalPlaceholderReplacements.put("%ACTIVEOBJECTIVEPROGRESS%", "" + activeObjective.getCurrentProgress());
                internalPlaceholderReplacements.put("%OBJECTIVEPROGRESSNEEDED%", "" + activeObjective.getProgressNeeded());
                internalPlaceholderReplacements.put("%ACTIVEOBJECTIVEDESCRIPTION%", main.getQuestManager().getObjectiveTaskDescription(activeObjective.getObjective(), false, player));
                internalPlaceholderReplacements.put("%COMPLETEDOBJECTIVEDESCRIPTION%", main.getQuestManager().getObjectiveTaskDescription(activeObjective.getObjective(), true, player));
            } else if (internalPlaceholderObject instanceof Objective objective) {
                //main.getLogManager().log(Level.INFO, "Applying Objective placeholders...");
                internalPlaceholderReplacements.put("%OBJECTIVEID%", "" + objective.getObjectiveID());
                internalPlaceholderReplacements.put("%OBJECTIVENAME%", "" + objective.getObjectiveFinalName());
            } else if (internalPlaceholderObject instanceof Trigger trigger) {
                //main.getLogManager().log(Level.INFO, "Applying Trigger placeholders...");
            } else if (internalPlaceholderObject instanceof QuestPlayer questPlayer) {
                //main.getLogManager().log(Level.INFO, "Applying QuestPlayer placeholders...");
                internalPlaceholderReplacements.put("%QUESTPOINTS%", "" + questPlayer.getQuestPoints());
            }

        }
        return main.getUtilManager().replaceFromMap(initialMessage, internalPlaceholderReplacements);
    }

    public List<String> applySpecial(List<String> initialMessage) {
        List<String> toReturn = new ArrayList<>();
        for(String message : initialMessage){
            toReturn.add(applySpecial(message));
        }
        return toReturn;
    }



        public String applySpecial(String initialMessage) { //TODO: Fix center if that message is later processed for placeholders => process the placeholders here instead
        initialMessage = initialMessage.replace("<EMPTY>", " ");


        final StringBuilder finalMessage = new StringBuilder();

        final String[] splitMessages = initialMessage.split("\n");
        for (int index = 0; index < splitMessages.length; index++) {
            if (splitMessages[index].contains("<CENTER>")) {
                finalMessage.append(main.getUtilManager().getCenteredMessage(splitMessages[index].replace("<CENTER>", "")));
            } else {
                finalMessage.append(splitMessages[index]);
            }
            if (index < splitMessages.length - 1) {
                finalMessage.append("\n");
            }
        }


        return finalMessage.toString();
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
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', LegacyComponentSerializer.builder().hexColors().build().serialize(main.parse(message)));
    }


    /**
     * This will try to save the language configuration file with the data which is currently in the
     * languageConfig FileConfiguration object.
     */
    public void saveLanguageConfig() {
        try {
            getLanguageConfig().save(languageConfigFile);

        } catch (IOException ioException) {
            ioException.printStackTrace();
            main.getLogManager().severe("Language Config file could not be saved.");
        }
    }

}
