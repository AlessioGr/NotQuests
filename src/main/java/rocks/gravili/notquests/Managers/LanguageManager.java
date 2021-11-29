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

package rocks.gravili.notquests.Managers;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.io.IOUtils;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.NotQuests;

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
    }


    public void loadMissingDefaultLanguageFiles() {
        final ArrayList<String> languageFiles = new ArrayList<>();
        languageFiles.add("en.yml");
        languageFiles.add("de.yml");
        languageFiles.add("zh-CN.yml");
        languageFiles.add("vi.yml");
        languageFiles.add("gr.yml");

        //Create the Language Data Folder if it does not exist yet (the NotQuests/languages folder)
        File languageFolder = new File(main.getDataFolder().getPath() + "/languages/");
        if (!languageFolder.exists()) {
            main.getLogManager().log(Level.INFO, "Languages Folder not found. Creating a new one...");

            if (!languageFolder.mkdirs()) {
                main.getLogManager().log(Level.SEVERE, "There was an error creating the NotQuests languages folder");
                main.getDataManager().disablePluginAndSaving("There was an error creating the NotQuests languages folder.");
                return;
            }

        }

        for (final String fileName : languageFiles) {
            try {
                main.getLogManager().log(Level.INFO, "Creating the <AQUA>" + fileName + "</AQUA> language file...");

                File file = new File(languageFolder, fileName);

                if (!file.exists()) {
                    if (!file.createNewFile()) {
                        main.getLogManager().log(Level.SEVERE, "There was an error creating the " + fileName + " language file. (3)");
                        main.getDataManager().disablePluginAndSaving("There was an error creating the " + fileName + " language file. (3)");
                        return;
                    }

                    InputStream inputStream = main.getResource("translations/" + fileName);
                    //Instead of creating a new language file, we will copy the one from inside of the plugin jar into the plugin folder:
                    if (inputStream != null) {


                        try (OutputStream outputStream = new FileOutputStream(file)) {
                            IOUtils.copy(inputStream, outputStream);
                        } catch (Exception e) {
                            main.getLogManager().log(Level.SEVERE, "There was an error creating the " + fileName + " language file. (4)");
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
        main.getLogManager().log(Level.INFO, LogCategory.LANGUAGE, "Loading language config <AQUA>" + languageCode + ".yml");

        /*
         * If the generalConfigFile Object doesn't exist yet, this will load the file
         * or create a new general.yml file if it does not exist yet and load it into the
         * generalConfig FileConfiguration object.
         */
        if (languageConfigFile == null || !currentLanguage.equals(languageCode)) {

            //Create the Data Folder if it does not exist yet (the NotQuests folder)
            if (!main.getDataFolder().exists()) {
                main.getLogManager().log(Level.INFO, "Data Folder not found. Creating a new one...");

                if (!main.getDataFolder().mkdirs()) {
                    main.getLogManager().log(Level.SEVERE, "There was an error creating the NotQuests data folder");
                    main.getDataManager().disablePluginAndSaving("There was an error creating the NotQuests data folder.");
                    return;
                }

            }


            //Create the Language Data Folder if it does not exist yet (the NotQuests/languages folder)
            File languageFolder = new File(main.getDataFolder().getPath() + "/languages/");
            if (!languageFolder.exists()) {
                main.getLogManager().log(Level.INFO, "Languages Folder not found. Creating a new one...");

                if (!languageFolder.mkdirs()) {
                    main.getLogManager().log(Level.SEVERE, "There was an error creating the NotQuests languages folder");
                    main.getDataManager().disablePluginAndSaving("There was an error creating the NotQuests languages folder.");
                    return;
                }

            }

            languageConfigFile = new File(languageFolder, main.getDataManager().getConfiguration().getLanguageCode() + ".yml");

            if (!languageConfigFile.exists()) {
                main.getLogManager().log(Level.INFO, "Language Configuration (" + main.getDataManager().getConfiguration().getLanguageCode() + ".yml) does not exist. Creating a new one...");

                //Does not work yet, since comments are overridden if something is saved
                //saveDefaultConfig();


                try {
                    //Try to create the language.yml config file, and throw an error if it fails.

                    if (!languageConfigFile.createNewFile()) {
                        main.getLogManager().log(Level.SEVERE, "There was an error creating the " + main.getDataManager().getConfiguration().getLanguageCode() + ".yml language file. (1)");
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
            main.getLogManager().info("<DARK_PURPLE>Language Configuration <AQUA>" + languageCode + ".yml <DARK_PURPLE>was updated with new values! Saving it...");
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
            getLanguageConfig().set("chat.quest-completed-and-rewards-given", "\n<CENTER>&a[Quest Completed]\n&b&l%QUESTNAME%\n\n");
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
        if (!getLanguageConfig().isString("chat.quest-not-active-error")) {
            getLanguageConfig().set("chat.quest-not-active-error", "&cError: &b%QUESTNAME% &cis not an active Quest!");
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

        //Chat Objectives
        if (!getLanguageConfig().isString("chat.objectives.counter")) {
            getLanguageConfig().set("chat.objectives.counter", "&e%OBJECTIVEID%. %OBJECTIVENAME%:");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.objectives.hidden")) {
            getLanguageConfig().set("chat.objectives.hidden", "&e%OBJECTIVEID%. &7&l[HIDDEN]");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.objectives.description")) {
            getLanguageConfig().set("chat.objectives.description", "   &9Description: &6%OBJECTIVEDESCRIPTION%");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.objectives.progress")) {
            getLanguageConfig().set("chat.objectives.progress", "   §7Progress: §f%ACTIVEOBJECTIVEPROGRESS% &f/ %OBJECTIVEPROGRESSNEEDED%");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.objectives.successfully-completed")) {
            getLanguageConfig().set("chat.objectives.successfully-completed", "&aYou have successfully completed the objective &e%OBJECTIVENAME% &a for quest &b%QUESTNAME%&a!");
            valueChanged = true;
        }
        //Chat Objectives Task Descriptions
        if (!getLanguageConfig().isString("chat.objectives.taskDescription.breakBlocks.base")) {
            getLanguageConfig().set("chat.objectives.taskDescription.breakBlocks.base", "    &8└─ &7%EVENTUALCOLOR%Block to break: &f%EVENTUALCOLOR%%BLOCKTOBREAK%");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.objectives.taskDescription.breed.base")) {
            getLanguageConfig().set("chat.objectives.taskDescription.breed.base", "    &8└─ &7%EVENTUALCOLOR%Mob to breed: &f%EVENTUALCOLOR%%ENTITYTOBREED%");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.objectives.taskDescription.collectItems.base")) {
            getLanguageConfig().set("chat.objectives.taskDescription.collectItems.base", "    &8└─ &7%EVENTUALCOLOR%Items to collect: &f%EVENTUALCOLOR%%ITEMTOCOLLECTTYPE% (%ITEMTOCOLLECTNAME%)");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.objectives.taskDescription.consumeItems.base")) {
            getLanguageConfig().set("chat.objectives.taskDescription.consumeItems.base", "    &8└─ &7%EVENTUALCOLOR%Items to consume: &f%EVENTUALCOLOR%%ITEMTOCONSUMETYPE% (%ITEMTOCONSUMENAME%)");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.objectives.taskDescription.craftItems.base")) {
            getLanguageConfig().set("chat.objectives.taskDescription.craftItems.base", "    &8└─ &7%EVENTUALCOLOR%Items to craft: &f%EVENTUALCOLOR%%ITEMTOCRAFTTYPE% (%ITEMTOCRAFTNAME%)");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.objectives.taskDescription.deliverItems.base")) {
            getLanguageConfig().set("chat.objectives.taskDescription.deliverItems.base", "    &8└─ &7%EVENTUALCOLOR%Items to deliver: &f%EVENTUALCOLOR%%ITEMTODELIVERTYPE% (%ITEMTODELIVERNAME%)");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.objectives.taskDescription.escortNPC.base")) {
            getLanguageConfig().set("chat.objectives.taskDescription.escortNPC.base", "    &8└─ &7%EVENTUALCOLOR%Escort &f%EVENTUALCOLOR%%NPCNAME% &7%EVENTUALCOLOR%to &f%EVENTUALCOLOR%%DESTINATIONNPCNAME%");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.objectives.taskDescription.killMobs.base")) {
            getLanguageConfig().set("chat.objectives.taskDescription.killMobs.base", "    &8└─ &7%EVENTUALCOLOR%Mobs to kill: &f%EVENTUALCOLOR%%MOBTOKILL%");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.objectives.taskDescription.otherQuest.base")) {
            getLanguageConfig().set("chat.objectives.taskDescription.otherQuest.base", "    &8└─ &7%EVENTUALCOLOR%Quest completion: &f%EVENTUALCOLOR%%OTHERQUESTNAME%");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.objectives.taskDescription.placeBlocks.base")) {
            getLanguageConfig().set("chat.objectives.taskDescription.placeBlocks.base", "    &8└─ &7%EVENTUALCOLOR%Block to place: &f%EVENTUALCOLOR%%BLOCKTOPLACE%");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.objectives.taskDescription.reachLocation.base")) {
            getLanguageConfig().set("chat.objectives.taskDescription.reachLocation.base", "    &8└─ &7%EVENTUALCOLOR%Reach Location: &f%EVENTUALCOLOR%%LOCATIONNAME%");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.objectives.taskDescription.talkToNPC.base")) {
            getLanguageConfig().set("chat.objectives.taskDescription.talkToNPC.base", "    &8└─ &7%EVENTUALCOLOR%Talk to &f%EVENTUALCOLOR%%NAME%");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.objectives.taskDescription.triggerCommand.base")) {
            getLanguageConfig().set("chat.objectives.taskDescription.triggerCommand.base", "    &8└─ &7%EVENTUALCOLOR%Goal: &f%EVENTUALCOLOR%%TRIGGERNAME%");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.objectives.taskDescription.killEliteMobs.base")) {
            getLanguageConfig().set("chat.objectives.taskDescription.killEliteMobs.base", "    &8└─ &7%EVENTUALCOLOR%Kill Elite Mob: &f%EVENTUALCOLOR%%ELITEMOBNAME%");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.objectives.taskDescription.killEliteMobs.any")) {
            getLanguageConfig().set("chat.objectives.taskDescription.killEliteMobs.any", "    &8└─ &7%EVENTUALCOLOR%Kill any Elite Mob!");
            valueChanged = true;
        }

        if (!getLanguageConfig().isString("chat.objectives.taskDescription.runCommand.base")) {
            getLanguageConfig().set("chat.objectives.taskDescription.runCommand.base", "    &8└─ &7%EVENTUALCOLOR%Run command: &f%EVENTUALCOLOR%%COMMANDTORUN%");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.objectives.taskDescription.interact.base")) {
            getLanguageConfig().set("chat.objectives.taskDescription.interact.base", "    &8└─ &7%EVENTUALCOLOR%%INTERACTTYPE% Location: &f%EVENTUALCOLOR%%COORDINATES% &f%EVENTUALCOLOR%in world%EVENTUALCOLOR% %WORLDNAME%");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.objectives.taskDescription.interact.taskDescriptionProvided")) {
            getLanguageConfig().set("chat.objectives.taskDescription.interact.taskDescriptionProvided", "    &8└─ &7%EVENTUALCOLOR%%TASKDESCRIPTION%");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.objectives.taskDescription.jump.base")) {
            getLanguageConfig().set("chat.objectives.taskDescription.jump.base", "    &8└─ &7%EVENTUALCOLOR%Jump &f%EVENTUALCOLOR%%AMOUNTOFJUMPS% &7%EVENTUALCOLOR%times");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("chat.objectives.taskDescription.smelt.base")) {
            getLanguageConfig().set("chat.objectives.taskDescription.smelt.base", "    &8└─ &7%EVENTUALCOLOR%Items to smelt: &f%EVENTUALCOLOR%%ITEMTOSMELTTYPE% (%ITEMTOSMELTNAME%)");
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


        //user /q abort questname
        if (!getLanguageConfig().isString("gui.abortQuest.title")) {
            getLanguageConfig().set("gui.abortQuest.title", "         &cAbort Confirmation");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.abortQuest.button.confirmAbort.text")) {
            getLanguageConfig().set("gui.abortQuest.button.confirmAbort.text", "&b%QUESTNAME% &a[ACTIVE]\n&7Progress: &a%COMPLETEDOBJECTIVESCOUNT% &f/ %ALLOBJECTIVESCOUNT%\n&cClick to abort the quest &b%QUESTNAME%&c!");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.abortQuest.button.cancelAbort.text")) {
            getLanguageConfig().set("gui.abortQuest.button.cancelAbort.text", "&aClick to NOT abort this quest\n&aand cancel this action");
            valueChanged = true;
        }

        //user /q preview questname
        if (!getLanguageConfig().isString("gui.previewQuest.title")) {
            getLanguageConfig().set("gui.previewQuest.title", "&9Preview for Quest &b%QUESTNAME%");
            valueChanged = true;
        }

        if (!getLanguageConfig().isString("gui.previewQuest.button.description.text")) {
            getLanguageConfig().set("gui.previewQuest.button.description.text", "&eDescription\n&8%QUESTDESCRIPTION%");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.previewQuest.button.description.empty")) {
            getLanguageConfig().set("gui.previewQuest.button.description.empty", "???");
            valueChanged = true;
        }

        if (!getLanguageConfig().isString("gui.previewQuest.button.rewards.text")) {
            getLanguageConfig().set("gui.previewQuest.button.rewards.text", "&aRewards\n&f%QUESTREWARDS%");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.previewQuest.button.rewards.empty")) {
            getLanguageConfig().set("gui.previewQuest.button.rewards.empty", "&8None or hidden");
            valueChanged = true;
        }

        if (!getLanguageConfig().isString("gui.previewQuest.button.requirements.text")) {
            getLanguageConfig().set("gui.previewQuest.button.requirements.text", "&cRequirements\n&f%QUESTREQUIREMENTS%");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.previewQuest.button.requirements.empty")) {
            getLanguageConfig().set("gui.previewQuest.button.requirements.empty", "&8-");
            valueChanged = true;
        }

        if (!getLanguageConfig().isString("gui.previewQuest.button.confirmTake.text")) {
            getLanguageConfig().set("gui.previewQuest.button.confirmTake.text", "&b%QUESTNAME%\n&aClick to take the quest &b%QUESTNAME%&a!");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.previewQuest.button.cancelTake.text")) {
            getLanguageConfig().set("gui.previewQuest.button.cancelTake.text", "&cClick to NOT take this quest\n&cand cancel this action");
            valueChanged = true;
        }


        //user /q progress questname
        if (!getLanguageConfig().isString("gui.progress.title")) {
            getLanguageConfig().set("gui.progress.title", "&9Details for Quest &b%QUESTNAME%");
            valueChanged = true;
        }

        if (!getLanguageConfig().isString("gui.progress.button.unlockedObjective.text")) {
            getLanguageConfig().set("gui.progress.button.unlockedObjective.text", "&e%ACTIVEOBJECTIVEID%. &b%OBJECTIVENAME%\n&6&lACTIVE\n&8%OBJECTIVEDESCRIPTION%\n%ACTIVEOBJECTIVEDESCRIPTION%\n\n&7Progress: &a%ACTIVEOBJECTIVEPROGRESS% &f/ %OBJECTIVEPROGRESSNEEDED%");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.progress.button.unlockedObjective.description-empty")) {
            getLanguageConfig().set("gui.progress.button.unlockedObjective.description-empty", "No Description");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.progress.button.lockedObjective.text")) {
            getLanguageConfig().set("gui.progress.button.lockedObjective.text", "&e%ACTIVEOBJECTIVEID%. &7&l[HIDDEN]\n&eThis objective has not yet\n&ebeen unlocked!");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.progress.button.completedObjective.text")) {
            getLanguageConfig().set("gui.progress.button.completedObjective.text", "&a&m%ACTIVEOBJECTIVEID%. &2&m%OBJECTIVENAME%\n&a&lCOMPLETED\n&8&m%OBJECTIVEDESCRIPTION%\n%COMPLETEDOBJECTIVEDESCRIPTION%\n\n&7&mProgress: &a&m%ACTIVEOBJECTIVEPROGRESS% &f&m/ %OBJECTIVEPROGRESSNEEDED%");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.progress.button.completedObjective.description-empty")) {
            getLanguageConfig().set("gui.progress.button.completedObjective.description-empty", "No Description");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.progress.button.previousPage.text")) {
            getLanguageConfig().set("gui.progress.button.previousPage.text", "Go to previous page (%prevpage%)");
            valueChanged = true;
        }
        if (!getLanguageConfig().isString("gui.progress.button.nextPage.text")) {
            getLanguageConfig().set("gui.progress.button.nextPage.text", "Go to next page (%nextpage%)");
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

    public final String getString(final String languageString, final Player targetPlayer) {
        if (!getLanguageConfig().isString(languageString)) {
            return "Language string not found: " + languageString;
        } else {
            final String translatedString = getLanguageConfig().getString(languageString);
            if(translatedString == null){
                return "Language string not found: " + languageString;
            }
            if(!main.getDataManager().getConfiguration().supportPlaceholderAPIInTranslationStrings || !main.isPlaceholderAPIEnabled() || targetPlayer == null){
                return applySpecial(applyColor(translatedString));
            }else{
                return applySpecial(applyColor(PlaceholderAPI.setPlaceholders(targetPlayer, translatedString)));
            }
        }

    }

    public String applySpecial(String initialMessage) { //TODO: Fix center if that message is later processed for placeholders => process the placeholders here instead
        initialMessage = initialMessage.replaceAll("<EMPTY>", " ");


        final StringBuilder finalMessage = new StringBuilder();

        final String[] splitMessages = initialMessage.split("\n");
        for (int index = 0; index < splitMessages.length; index++) {
            if (splitMessages[index].contains("<CENTER>")) {
                finalMessage.append(main.getUtilManager().getCenteredMessage(splitMessages[index].replaceAll("<CENTER>", "")));
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
        return LegacyComponentSerializer.builder().hexColors().build().serialize(MiniMessage.miniMessage().parse(message)).replace("&", "§");
    }


    /**
     * This will try to save the language configuration file with the data which is currently in the
     * languageConfig FileConfiguration object.
     */
    public void saveLanguageConfig() {
        try {
            getLanguageConfig().save(languageConfigFile);

        } catch (IOException ioException) {
            main.getLogManager().log(Level.SEVERE, "Language Config file could not be saved.");
        }
    }

}
