/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.io.IOUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nullable;
import rocks.gravili.notquests.common.managers.LogCategory;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveObjectiveHolder;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.ObjectiveHolder;
import rocks.gravili.notquests.paper.structs.triggers.Trigger;

public class LanguageManager {
    private final NotQuests main;
    /**
     * Configuration objects which contains values from General.yml
     */
    private final Pattern hexPattern = Pattern.compile("<#([A-Fa-f0-9]){6}>");
    File languageFolder = null;
    /**
     * General.yml Configuration
     */
    private File languageConfigFile = null;
    /**
     * General.yml Configuration File
     */
    private FileConfiguration languageConfig;
    private String currentLanguage = "en";
    private FileConfiguration defaultLanguageConfig = null;



    public LanguageManager(final NotQuests main) {
        this.main = main;
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
    public final void loadLanguageConfig(final boolean skipIfLanguageConfigExists) {
        if(skipIfLanguageConfigExists && languageConfig != null){
            main.getLogManager().debug("Skipping loading of language config, as it already exists.");
            return;
        }
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
        main.getLogManager().debug("setupDefaultStrings()");

        if (defaultLanguageConfig == null) {
            main.getDataManager().disablePluginAndSaving("There was an error reading the default.yml language configuration.");
            return false;
        }

        boolean valueChanged = false;
        final ConfigurationSection defaultConfigurationSection = defaultLanguageConfig.getConfigurationSection("");
        if (defaultConfigurationSection != null) {
            // main.getLogManager().debug("All default config keys: " + defaultConfigurationSection.getKeys(true).toString());
            for (final String defaultString : defaultConfigurationSection.getKeys(true)) {

                if (defaultConfigurationSection.isConfigurationSection(defaultString)) {
                    //main.getLogManager().debug("Skipping: <highlight>" + defaultString + "</highlight>");
                    continue;
                }

                if (!getLanguageConfig().contains(defaultString)) {
                    main.getLogManager().info(LogCategory.LANGUAGE, "Updating string: <highlight>" + defaultString + "</highlight>");

                    getLanguageConfig().set(defaultString, defaultConfigurationSection.get(defaultString));
                    valueChanged = true;
                }else{
                    //main.getLogManager().debug("Already is string: " + defaultString);
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
            main.getLogManager().info("getLanguageConfig() is null. Loading language config...");
            loadLanguageConfig(true);
        }
        return languageConfig;
    }


    /*public final ItemStack getComponent(final String languageString, final Player targetPlayer, Object... internalPlaceholderObjects){
        final String material = getString(languageString + ".material", targetPlayer, internalPlaceholderObjects);

        return main.parse(getString(languageString, targetPlayer, internalPlaceholderObjects)).decoration(TextDecoration.ITALIC, false);
    }*/

    public final Component getComponent(final String languageString, final Player targetPlayer, Object... internalPlaceholderObjects){
        return main.parse(getString(languageString, targetPlayer, internalPlaceholderObjects)).decoration(TextDecoration.ITALIC, false);
    }

    //Usually used for GUI
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
                    for(String splitPart : componentPart.split("\n")){
                        components.add(main.parse(splitPart).decoration(TextDecoration.ITALIC, false));
                    }
                }
            } else {
                for(String componentPart : applySpecial(PlaceholderAPI.setPlaceholders(targetPlayer, applyInternalPlaceholders(translatedString, targetPlayer, internalPlaceholderObjects)))) {
                    for(String splitPart : componentPart.split("\n")){
                        components.add(main.parse(splitPart).decoration(TextDecoration.ITALIC, false));
                    }
                }
            }
        }
        return components;
    }

    public final @NonNull Material getMaterialOrAir(final String languageString) {
        if (!getLanguageConfig().isString(languageString)) {
            return Material.AIR;
        } else {
            final Material foundMaterial = Material.getMaterial(getLanguageConfig().getString(languageString, "").toUpperCase(
                Locale.ROOT));

            return foundMaterial != null ? foundMaterial : Material.AIR;
        }
    }
    public final int getInt(final String languageString) {
        if (!getLanguageConfig().isInt(languageString)) {
            return 0;
        } else {
            return getLanguageConfig().getInt(languageString);
        }
    }

    public final String getString(final String languageString, @Nullable final QuestPlayer questPlayer, @Nullable Object... internalPlaceholderObjects) {
        return getString(languageString, questPlayer != null ? questPlayer.getPlayer() : null, internalPlaceholderObjects);
    }

    public final String getString(final String languageString, @Nullable final Player targetPlayer, @Nullable Object... internalPlaceholderObjects) {
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

    public final List<String> getStringList(final String languageString, @Nullable final Player targetPlayer, Object... internalPlaceholderObjects) {
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

    public List<String> applyInternalPlaceholders(List<String> initialMessage, @Nullable final Player player, Object... internalPlaceholderObjects) {
        List<String> toReturn = new ArrayList<>();
        for (String message : initialMessage) {
            toReturn.add(applyInternalPlaceholders(message, player, internalPlaceholderObjects));
        }
        return toReturn;
    }

    public final ActiveQuest findTopLevelActiveQuestOfActiveObjective(final ActiveObjective activeObjective){
        if(activeObjective.getActiveObjectiveHolder() instanceof final ActiveQuest activeQuest){
            return activeQuest;
        }else if(activeObjective.getActiveObjectiveHolder() instanceof final ActiveObjective activeObjective1){
            return findTopLevelActiveQuestOfActiveObjective(activeObjective1);
        }
        return null;
    }

    public final String applyInternalPlaceholders(final String initialMessage, @Nullable final Player player, final @Nullable Object... internalPlaceholderObjects) {
        if (internalPlaceholderObjects == null || internalPlaceholderObjects.length == 0) {
            return initialMessage;
        }

        final Map<String, Supplier<String>> internalPlaceholderReplacements = new HashMap<>(); //With this method probably being used simultaneously, we cannot just have 1 HashMap and clear it. It would get cleared while another thing is processing


        //main.getLogManager().severe("Initial: " + initialMessage);

        //internalPlaceholderReplacements.clear();
        internalPlaceholderReplacements.put("%QUESTPOINTS%", () -> "0");

        Quest foundQuest = null;
        QuestPlayer foundQuestPlayer = null;

        for (final @Nullable Object internalPlaceholderObject : internalPlaceholderObjects) {
            if(internalPlaceholderObject == null){
                continue;
            }

            //main.getLogManager().severe("Object: " + internalPlaceholderObject.toString());
            if (internalPlaceholderObject instanceof final ActiveQuest activeQuest) {
                // main.getLogManager().info("Quest placeholders...");
                internalPlaceholderReplacements.put("%QUESTNAME%", () -> activeQuest.getQuest().getDisplayNameOrIdentifier());
                internalPlaceholderReplacements.put("%QUESTDESCRIPTION%", () -> activeQuest.getQuest().getObjectiveHolderDescription());
                internalPlaceholderReplacements.put("%COMPLETEDOBJECTIVESCOUNT%", () -> "" + activeQuest.getCompletedObjectives().size());
                internalPlaceholderReplacements.put("%ALLOBJECTIVESCOUNT%", () -> "" + activeQuest.getQuest().getObjectives().size());
            } else if (internalPlaceholderObject instanceof final ActiveObjective activeObjective) {

                final ActiveQuest activeQuest = findTopLevelActiveQuestOfActiveObjective(activeObjective);
                if(activeQuest != null){
                    internalPlaceholderReplacements.put("%QUESTNAME%", () -> activeQuest.getQuest().getDisplayNameOrIdentifier());
                    internalPlaceholderReplacements.put("%QUESTDESCRIPTION%", () -> activeQuest.getQuest().getObjectiveHolderDescription());
                }

                internalPlaceholderReplacements.put("%OBJECTIVEID%", () -> "" + activeObjective.getObjective().getObjectiveID());
                internalPlaceholderReplacements.put("%ACTIVEOBJECTIVEID%", () -> "" + activeObjective.getObjective().getObjectiveID());
                internalPlaceholderReplacements.put("%OBJECTIVENAME%", () -> "" + activeObjective.getObjective().getDisplayNameOrIdentifier());
                internalPlaceholderReplacements.put("%ACTIVEOBJECTIVEPROGRESS%", () -> {
                    String formatted = String.format("%.2f", activeObjective.getCurrentProgress());
                    if(formatted.endsWith(".00") || formatted.endsWith(",00")){
                        formatted = formatted.substring(0, formatted.length()-3);
                    }
                    return formatted;
                });
                internalPlaceholderReplacements.put("%OBJECTIVEPROGRESSNEEDED%", () -> {
                    String formatted = String.format("%.2f", activeObjective.getProgressNeeded());
                    if(formatted.endsWith(".00") || formatted.endsWith(",00")){
                        formatted = formatted.substring(0, formatted.length()-3);
                    }
                    return formatted;
                });
                internalPlaceholderReplacements.put("%OBJECTIVEPROGRESSPERCENTAGE%", () -> "" + (int) ((float) ((float) activeObjective.getCurrentProgress() / (float) activeObjective.getProgressNeeded()) * 100));
                internalPlaceholderReplacements.put("%OBJECTIVETASKDESCRIPTION%", () -> main.getQuestManager().getObjectiveTaskDescription(activeObjective.getObjective(), false, main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()), activeObjective));
                internalPlaceholderReplacements.put("%COMPLETEDOBJECTIVETASKDESCRIPTION%", () -> main.getQuestManager().getObjectiveTaskDescription(activeObjective.getObjective(), true, main.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId()), activeObjective));
                internalPlaceholderReplacements.put("%OBJECTIVEDESCRIPTION%", () -> activeObjective.getObjective().getObjectiveHolderDescription());
            } else if (internalPlaceholderObject instanceof final ActiveObjectiveHolder activeObjectiveHolder) {
                // main.getLogManager().info("Quest placeholders...");
                if(activeObjectiveHolder.getObjectiveHolder() instanceof final Quest quest){
                    internalPlaceholderReplacements.put("%QUESTNAME%",
                        quest::getDisplayNameOrIdentifier);
                    internalPlaceholderReplacements.put("%QUESTDESCRIPTION%",
                        quest::getObjectiveHolderDescription);
                }

                internalPlaceholderReplacements.put("%COMPLETEDOBJECTIVESCOUNT%", () -> "" + activeObjectiveHolder.getCompletedObjectives().size());
                internalPlaceholderReplacements.put("%ALLOBJECTIVESCOUNT%", () -> "" + activeObjectiveHolder.getObjectiveHolder().getObjectives().size());
            } else if (internalPlaceholderObject instanceof final Quest quest) {
                //main.getLogManager().info("Applying Quest placeholders...");
                internalPlaceholderReplacements.put("%QUESTNAME%", quest::getDisplayNameOrIdentifier);
                internalPlaceholderReplacements.put("%QUESTDESCRIPTION%", quest::getObjectiveHolderDescription);
                foundQuest = quest;
            }  else if (internalPlaceholderObject instanceof final Objective objective) {
                //main.getLogManager().info("Applying Objective placeholders...");
                if(objective.getObjectiveHolder() instanceof final Quest quest){
                    internalPlaceholderReplacements.put("%QUESTNAME%", () -> quest.getDisplayNameOrIdentifier());
                    internalPlaceholderReplacements.put("%QUESTDESCRIPTION%", () -> quest.getObjectiveHolderDescription());
                }
                internalPlaceholderReplacements.put("%OBJECTIVEID%", () -> "" + objective.getObjectiveID());
                internalPlaceholderReplacements.put("%OBJECTIVENAME%", () -> "" + objective.getDisplayNameOrIdentifier());
            } else if (internalPlaceholderObject instanceof final ObjectiveHolder objectiveHolder) {
                //main.getLogManager().info("Applying Quest placeholders...");
                if(objectiveHolder instanceof final Quest quest){
                    internalPlaceholderReplacements.put("%QUESTNAME%", quest::getDisplayNameOrIdentifier);
                    internalPlaceholderReplacements.put("%QUESTDESCRIPTION%", quest::getObjectiveHolderDescription);
                }

            }else if (internalPlaceholderObject instanceof final Trigger trigger) {
                //main.getLogManager().log(Level.INFO, "Applying Trigger placeholders...");
            } else if (internalPlaceholderObject instanceof final QuestPlayer questPlayer) {
                //main.getLogManager().log(Level.INFO, "Applying QuestPlayer placeholders...");
                internalPlaceholderReplacements.put("%QUESTPOINTS%", () -> "" + questPlayer.getQuestPoints());
                internalPlaceholderReplacements.put("%PROFILENAME%", () -> "" + questPlayer.getProfile());
                foundQuestPlayer = questPlayer;
            } else if (internalPlaceholderObject instanceof final Map providedInternalPlaceholderReplacements) {
                for (final Object key : providedInternalPlaceholderReplacements.keySet()) {
                    internalPlaceholderReplacements.put((String) key, () -> (String) providedInternalPlaceholderReplacements.get(key));
                }
            }

        }

        if (foundQuest != null && foundQuestPlayer != null) {
            final QuestPlayer finalFoundQuestPlayer = foundQuestPlayer;
            final Quest finalFoundQuest = foundQuest;
            internalPlaceholderReplacements.put("%QUESTCOOLDOWNLEFTFORMATTED%", () -> finalFoundQuestPlayer.getCooldownFormatted(finalFoundQuest));
        }
        /*main.getLogManager().warn("initialMessage: " + initialMessage);
        main.getLogManager().warn(" Keys: " + Arrays.toString(internalPlaceholderReplacements.keySet().toArray()));
        main.getLogManager().severe("  Contains %QUESTNAME%: " + internalPlaceholderReplacements.containsKey("%QUESTNAME%"));
        */
        return main.getUtilManager().replaceFromMap(initialMessage, internalPlaceholderReplacements);
    }

    public final List<String> applySpecial(final List<String> initialMessage) {
        final List<String> toReturn = new ArrayList<>();
        for(final String message : initialMessage){
            toReturn.add(applySpecial(message));
        }
        return toReturn;
    }



    public final String applySpecial(String initialMessage) { //TODO: Fix center if that message is later processed for placeholders => process the placeholders here instead
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
