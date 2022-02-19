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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.actions.Action;
import rocks.gravili.notquests.paper.structs.objectives.Objective;

import java.io.*;
import java.sql.*;
import java.util.*;


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
     * ArrayList for Command Tab Completions. They will be re-used where possible. This is sort of like a buffer for completions.
     * It does not return the real completions, but it's for example used in ObjectivesAdminCommand handleCompletions() which is
     * called by the real Tab Completer CommandNotQuestsAdmin to split it up a little.
     */
    public final List<String> completions = new ArrayList<>();
    /**
     * ArrayList for Command Tab Completions for players. They will be re-used where possible.
     */
    public final List<String> standardPlayerCompletions = new ArrayList<>();
    /**
     * ArrayList for Command Tab Completions for entity types. They will be initialized on startup be re-used where possible.
     */
    public final List<String> standardEntityTypeCompletions = new ArrayList<>();
    /**
     * ArrayList for Command Tab Completions for numbers from -1 to 12. They will be initialized on startup be re-used where possible.
     */
    public final List<String> numberCompletions = new ArrayList<>();
    /**
     * ArrayList for Command Tab Completions for numbers from 1 to 12. They will be initialized on startup be re-used where possible.
     */
    public final List<String> numberPositiveCompletions = new ArrayList<>();
    /**
     * ArrayList for Command Tab Completions. They will be re-used where possible.
     */
    public final List<String> partialCompletions = new ArrayList<>();

    /**
     * ArrayList for Command Tab Completions for elitemob entity types. They will be initialized on startup if the elitemobs integration is enabled and will be re-used where possible.
     */
    public final List<String> standardEliteMobNamesCompletions = new ArrayList<>();



    /*
     * Quests.yml Configuration File
     *
    private File questsConfigFile = null;*/
    /*
     * Quests.yml Configuration

    private FileConfiguration questsConfig;*/

    /**
     * savingEnabled is true by default. It will be set to false if any error happens when data is loaded from the Database.
     * When this is set to false, no quest or player data will be saved when the plugin is disabled.
     */
    private boolean savingEnabled = true;

    /**
     * loadingEnabled is true by default. It will be set to false if any error happens when data is loaded from the Database.
     * When this is set to false, no data will be loaded. This is to prevent trying to load data while the plugin is shutting down.
     */
    private boolean loadingEnabled = true;

    /**
     * If this is set to false, the plugin will try to load NPCs once Citizens is re-loaded or enabled
     */
    private boolean alreadyLoadedNPCs = false;

    private boolean alreadyLoadedGeneral = false;
    private boolean alreadyLoadedQuests = false;
    private boolean currentlyLoading = true;

    /**
     * General.yml Configuration
     */
    private File generalConfigFile = null;
    /**
     * General.yml Configuration File
     */
    private FileConfiguration generalConfig;


    /**
     * Configuration objects which contains values from General.yml
     */
    private final Configuration configuration;

    private boolean disabled = false;


    /*
     * ItemStack Cache used for 'storing ItemStacks to PDBs' (used for attaching Objectives To Armor Stands)
     */
    private final HashMap<Integer, Object> itemStackCache;


    private final ArrayList<Category> categories, topLevelOnlyCategories;
    private Category defaultCategory;

    private boolean valueChanged = false;

    private ArrayList<String> criticalErrors;


    //HikariCP
    private HikariConfig hikariConfig;
    private HikariDataSource hikariDataSource;


    public final boolean isDisabled(){
        return disabled;
    }


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

        criticalErrors = new ArrayList<>();

        itemStackCache = new HashMap<>();
        // create an instance of the Configuration object
        configuration = new Configuration();

        categories = new ArrayList<>();
        topLevelOnlyCategories = new ArrayList<>();
    }

    public void prepareDataFolder() {
        //Create the Data Folder if it does not exist yet (the NotQuests folder)
        if (!main.getMain().getDataFolder().exists()) {
            main.getLogManager().info("Data Folder not found. Creating a new one...");
            if (!main.getMain().getDataFolder().mkdirs()) {
                disablePluginAndSaving("There was an error creating the NotQuests data folder.");
            }
        }
    }

    public final ArrayList<Category> getCategories() {
        return categories;
    }

    public final ArrayList<Category> getTopLevelOnlyCategories() {
        return topLevelOnlyCategories;
    }

    public final Category getDefaultCategory() {
        return defaultCategory;
    }

    public void loadCategories(final Category parent) {

        File parentCategoryFolder = parent != null ? parent.getCategoryFolder() : main.getMain().getDataFolder();

        main.getLogManager().info("Checking folder for categories: <highlight>" + parentCategoryFolder.getName());


        for (final File categoryFolder : main.getUtilManager().listFoldersWithoutLanguagesOrBackups(parentCategoryFolder)) {
            if (categoryFolder == null) {
                continue;
            }

            //1. Check if there is a category.yml

            File[] fileList = categoryFolder.listFiles();
            if (fileList == null) {
                continue;
            }

            File categoryYMLFile = null;
            File questsFile = null;
            File actionsFile = null;
            File conditionsFile = null;
            File tagsFile = null;
            File itemsFile = null;


            File conversationsFolder = null;

            for (File file : fileList) {
                if (file.isFile()) {
                    if (file.getName().equalsIgnoreCase("category.yml")) {
                        categoryYMLFile = file;
                    } else if (file.getName().equalsIgnoreCase("quests.yml")) {
                        questsFile = file;
                    } else if (file.getName().equalsIgnoreCase("actions.yml")) {
                        actionsFile = file;
                    } else if (file.getName().equalsIgnoreCase("conditions.yml")) {
                        conditionsFile = file;
                    } else if (file.getName().equalsIgnoreCase("tags.yml")) {
                        tagsFile = file;
                    } else if (file.getName().equalsIgnoreCase("items.yml")) {
                        itemsFile = file;
                    }
                } else {
                    if (file.getName().equalsIgnoreCase("conversations")) {
                        conversationsFolder = file;
                    }
                }

            }
            if (categoryYMLFile == null) {
                continue; //No real category, just a random folder. skip.
            }

            //Create new tags.yml if doesn't exist:
            try{
                if (tagsFile == null || !tagsFile.exists()) {
                    tagsFile = new File(categoryFolder, "tags.yml");
                    if(!tagsFile.createNewFile()){
                        main.getLogManager().warn("Couldn't create a (1) tags.yml file for category <highlight>" + categoryFolder.getName());
                    }
                }
            }catch (Exception e){
                main.getLogManager().warn("Couldn't create a (2) tags.yml file for category <highlight>" + categoryFolder.getName());
            }

            //Create new items.yml if doesn't exist:
            try{
                if (itemsFile == null || !itemsFile.exists()) {
                    itemsFile = new File(categoryFolder, "items.yml");
                    if(!itemsFile.createNewFile()){
                        main.getLogManager().warn("Couldn't create a (1) items.yml file for category <highlight>" + categoryFolder.getName());
                    }
                }
            }catch (Exception e){
                main.getLogManager().warn("Couldn't create a (2) items.yml file for category <highlight>" + categoryFolder.getName());
            }



            final Category category = new Category(main, categoryFolder.getName(), categoryFolder);
            category.setCategoryFile(categoryYMLFile);
            category.setQuestsFile(questsFile);
            category.setActionsFile(actionsFile);
            category.setConditionsFile(conditionsFile);
            category.setConversationsFolder(conversationsFolder);
            category.setTagsFile(tagsFile);
            category.setItemsFile(itemsFile);
            category.loadDataFromCategoryConfig();

            main.getLogManager().info("  Loading real category: <highlight>" + category.getCategoryFullName());


            category.initializeConfigurations();

            if (parent != null) {
                category.setParentCategory(parent);
            } else {
                topLevelOnlyCategories.add(category);
                if (category.getCategoryName().equalsIgnoreCase("default")) {
                    defaultCategory = category;
                }
            }

            categories.add(category);


            loadCategories(category);
        }
    }


    public final void loadCategories() {
        if(!categories.isEmpty()){
            return;
        }
        prepareDataFolder();

        main.getLogManager().info("Loading categories and configurations...");
        loadCategories(null);

        if (defaultCategory == null) {
            defaultCategory = createCategory("default", null);
        }
    }

    @Deprecated
    public final Category getCategoryBySimpleName(final String categoryName) {
        for (final Category category : getCategories()) {
            if (category.getCategoryName().equalsIgnoreCase(categoryName)) {
                return category;
            }
        }
        return null;
    }

    public final Category getCategory(final String fullCategoryName) {
        for (final Category category : getCategories()) {
            if (category.getCategoryFullName().equalsIgnoreCase(fullCategoryName)) {
                return category;
            }
        }
        return null;
    }

    public void addCategory(final Category category){
        categories.add(category);
        if(category.getParentCategory() != null){
            topLevelOnlyCategories.add(category);
        }
    }

    public final Category createCategory(final String categoryName, final Category parentCategory) {
        main.getLogManager().info("Creating <highlight>" + categoryName + "</highlight> category...");
        //Create new default category with default folder, conversations folder inside and following files inside: category.yml, quests.yml, actions.yml, conditions.yml
        File categoryFolder;
        if (parentCategory == null) {
            categoryFolder = new File(main.getMain().getDataFolder(), categoryName);
        } else {
            categoryFolder = new File(parentCategory.getCategoryFolder(), categoryName);
        }

        if (!categoryFolder.exists()) {
            main.getLogManager().info(categoryName + " category does not exist. Creating a new one...");

            try {
                if (!categoryFolder.exists()) {
                    if (!categoryFolder.mkdirs()) {
                        disablePluginAndSaving("There was an error creating the category folder.");
                    }
                }

                File categoryFile = new File(categoryFolder, "category.yml");
                if (!categoryFile.exists() && !categoryFile.createNewFile()) {
                    disablePluginAndSaving("There was an error creating the " + categoryName + " category category.yml...");
                    return null;
                }
                File questsFile = new File(categoryFolder, "quests.yml");
                if (!questsFile.exists() && !questsFile.createNewFile()) {
                    disablePluginAndSaving("There was an error creating the " + categoryName + " category quests.yml...");
                    return null;
                }
                File actionsFile = new File(categoryFolder, "actions.yml");
                if (!actionsFile.exists() && !actionsFile.createNewFile()) {
                    disablePluginAndSaving("There was an error creating the " + categoryName + " category actions.yml...");
                    return null;
                }
                File conditionsFile = new File(categoryFolder, "conditions.yml");
                if (!conditionsFile.exists() && !conditionsFile.createNewFile()) {
                    disablePluginAndSaving("There was an error creating the " + categoryName + " category conditions.yml...");
                    return null;
                }

                File tagsFile = new File(categoryFolder, "tags.yml");
                if (!tagsFile.exists() && !tagsFile.createNewFile()) {
                    disablePluginAndSaving("There was an error creating the " + categoryName + " category tags.yml...");
                    return null;
                }

                File itemsFile = new File(categoryFolder, "items.yml");
                if (!itemsFile.exists() && !itemsFile.createNewFile()) {
                    disablePluginAndSaving("There was an error creating the " + categoryName + " category items.yml...");
                    return null;
                }

                File conversationsFolder = new File(categoryFolder, "conversations");
                if (!conversationsFolder.exists() && !conversationsFolder.mkdir()) {
                    disablePluginAndSaving("There was an error creating the " + categoryName + " category conversations folder..");
                    return null;
                }

                Category category = new Category(main, categoryName, categoryFolder);
                category.setCategoryFile(categoryFile);
                category.setQuestsFile(questsFile);
                category.setActionsFile(actionsFile);
                category.setConditionsFile(conditionsFile);
                category.setConversationsFolder(conversationsFolder);
                category.setTagsFile(tagsFile);
                category.setItemsFile(itemsFile);


                if (parentCategory != null) {
                    category.setParentCategory(parentCategory);
                }

                category.initializeConfigurations();


                return category;

            } catch (IOException ioException) {
                disablePluginAndSaving("There was an error creating the " + categoryName + " category.", ioException);
                return null;
            }
        }
        return null;
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
        main.getLogManager().info("Loading general config");
        /*
         * If the generalConfigFile Object doesn't exist yet, this will load the file
         * or create a new general.yml file if it does not exist yet and load it into the
         * generalConfig FileConfiguration object.
         */
        if (generalConfigFile == null) {
            //Create the Data Folder if it does not exist yet (the NotQuests folder)
            prepareDataFolder();

            generalConfigFile = new File(main.getMain().getDataFolder(), "general.yml");

            if (!generalConfigFile.exists()) {
                main.getLogManager().info("General Configuration (general.yml) does not exist. Creating a new one...");
                try {
                    //Try to create the general.yml config file, and throw an error if it fails.
                    if (!generalConfigFile.createNewFile()) {
                        disablePluginAndSaving("There was an error creating the general.yml config file (1).");
                        return;
                    }
                    main.getLogManager().info("Loading default <highlight>general.yml</highlight>...");

                    //Instead of creating a new general.yml file, we will copy the one from inside of the plugin jar into the plugin folder:
                    InputStream inputStream = main.getMain().getResource("general.yml");
                    if (inputStream != null) {
                        try (OutputStream outputStream = new FileOutputStream(generalConfigFile)) {
                            IOUtils.copy(inputStream, outputStream);
                        } catch (Exception e) {
                            disablePluginAndSaving("There was an error creating the general.yml config file (2).", e);
                            return;
                        }
                    }
                } catch (IOException ioException) {
                    disablePluginAndSaving("There was an error creating the general.yml config file. (2)", ioException);
                    return;
                }
            }

        }

        //Now load the either new (or existing) general config file...
        try {
            generalConfig = loadYAMLConfiguration(generalConfigFile);
        } catch (IOException | InvalidConfigurationException e) {
            disablePluginAndSaving("There was an error loading the general configuration file. It either doesn't exist or is invalid.", e);
            return;
        }


        updateAndReadGeneralConfig();
        setAlreadyLoadedGeneral(true);
    }

    public void updateAndReadGeneralConfig() {

        //Load all the MySQL Database Connection information from the general.yml
        configuration.setMySQLEnabled(getGeneralConfig().getBoolean("storage.database.enabled", false));
        configuration.setDatabaseHost(getGeneralConfig().getString("storage.database.host", ""));
        configuration.setDatabasePort(getGeneralConfig().getInt("storage.database.port", -1));
        configuration.setDatabaseName(getGeneralConfig().getString("storage.database.database", ""));
        configuration.setDatabaseUsername(getGeneralConfig().getString("storage.database.username", ""));
        configuration.setDatabasePassword(getGeneralConfig().getString("storage.database.password", ""));

        //Verifies that the loaded information is not empty or null.
        boolean errored = false;
        //For upgrades from older versions who didn't have the enable flag but still used MySQL
        boolean mysqlstorageenabledbooleannotloadedyet = false;

        valueChanged = false;

        //Storage Stuff
        {
            String key = "storage.database.enabled";
            if (!getGeneralConfig().isBoolean(key)) {
                getGeneralConfig().set(key, false);
                mysqlstorageenabledbooleannotloadedyet = true;
                valueChanged = true;
            }
            key = "storage.database.host";
            if (!getGeneralConfig().isString(key)) {
                getGeneralConfig().set(key, "");
                errored = true;
                valueChanged = true;
            }
            key = "storage.database.port";
            if (!getGeneralConfig().isInt(key)) {
                getGeneralConfig().set(key, 3306);
                //errored = true;
                configuration.setDatabasePort(3306);
                valueChanged = true;
            }
            key = "storage.database.database";
            if (!getGeneralConfig().isString(key)) {
                getGeneralConfig().set(key, "");
                errored = true;
                valueChanged = true;
            }
            key = "storage.database.username";
            if (!getGeneralConfig().isString(key)) {
                getGeneralConfig().set(key, "");
                errored = true;
                valueChanged = true;
            }
            key = "storage.database.password";
            if (!getGeneralConfig().isString(key)) {
                getGeneralConfig().set(key, "");
                errored = true;
                valueChanged = true;
            }

            //For upgrades from older versions who didn't have the enable flag but still used MySQL
            if (mysqlstorageenabledbooleannotloadedyet && !errored) {
                configuration.setMySQLEnabled(true);
                getGeneralConfig().set("storage.database.enabled", true);
            }

            if (!configuration.isMySQLEnabled()) {
                //No need to error previous stuff, since SQLite will be used
                errored = false;
            }
        }


        //Other Stuff


        configuration.setConfigurationVersion(getGeneralConfigString(
                "config-version-do-not-edit",
                main.getMain().getDescription().getVersion()
        ));

        configuration.setDebug(getGeneralConfigBoolean(
                "debug",
                false
        ));

        configuration.setLoadPlayerData(getGeneralConfigBoolean(
                "storage.load-playerdata",
                true
        ));

        configuration.setSavePlayerData(getGeneralConfigBoolean(
                "storage.save-playerdata",
                true
        ));

        configuration.setLoadPlayerDataOnJoin(getGeneralConfigBoolean(
                "storage.load-playerdata-on-join",
                true
        ));

        configuration.setSavePlayerDataOnQuit(getGeneralConfigBoolean(
                "storage.save-playerdata-on-quit",
                true
        ));

        configuration.setStorageCreateBackupsWhenServerShutsDown(getGeneralConfigBoolean(
                "storage.backups.create-when-server-shuts-down",
                true
        ));

        configuration.setMaxActiveQuestsPerPlayer(getGeneralConfigInt(
                "general.max-active-quests-per-player",
                -1
        ));

        configuration.setLanguageCode(getGeneralConfigString(
                "visual.language",
                "en-US"
        ));

        //NPC particles Citizens
        configuration.setCitizensNPCQuestGiverIndicatorParticleEnabled(getGeneralConfigBoolean(
                "visual.citizensnpc.quest-giver-indicator-particle.enabled",
                true
        ));

        configuration.setCitizensNPCQuestGiverIndicatorParticleType(Particle.valueOf(getGeneralConfigString(
                "visual.citizensnpc.quest-giver-indicator-particle.type",
                "VILLAGER_ANGRY"
        )));

        configuration.setCitizensNPCQuestGiverIndicatorParticleSpawnInterval(getGeneralConfigInt(
                "visual.citizensnpc.quest-giver-indicator-particle.spawn-interval",
                10
        ));

        configuration.setCitizensNPCQuestGiverIndicatorParticleCount(getGeneralConfigInt(
                "visual.citizensnpc.quest-giver-indicator-particle.count",
                1
        ));

        configuration.setCitizensNPCQuestGiverIndicatorParticleDisableIfTPSBelow(getGeneralConfigDouble(
                "visual.citizensnpc.quest-giver-indicator-particle.disable-if-tps-below",
                -1d
        ));
        //

        configuration.setHideRewardsWithoutName(getGeneralConfigBoolean(
                "visual.hide-rewards-without-name",
                true
        ));

        configuration.setShowRewardsAfterQuestCompletion(getGeneralConfigBoolean(
                "visual.show-rewards-after-quest-completion",
                true
        ));

        configuration.setShowRewardsAfterObjectiveCompletion(getGeneralConfigBoolean(
                "visual.show-rewards-after-objective-completion",
                true
        ));


        //Prevent armorstand editing
        configuration.setArmorStandPreventEditing(getGeneralConfigBoolean(
                "visual.armorstands.prevent-editing",
                true
        ));

        //Particles ArmorStands
        configuration.setArmorStandQuestGiverIndicatorParticleEnabled(getGeneralConfigBoolean(
                "visual.armorstands.quest-giver-indicator-particle.enabled",
                true
        ));

        configuration.setArmorStandQuestGiverIndicatorParticleType(Particle.valueOf(getGeneralConfigString(
                "visual.armorstands.quest-giver-indicator-particle.type",
                "VILLAGER_ANGRY"
        )));

        configuration.setArmorStandQuestGiverIndicatorParticleSpawnInterval(getGeneralConfigInt(
                "visual.armorstands.quest-giver-indicator-particle.spawn-interval",
                10
        ));

        configuration.setArmorStandQuestGiverIndicatorParticleCount(getGeneralConfigInt(
                "visual.armorstands.quest-giver-indicator-particle.count",
                1
        ));

        configuration.setArmorStandQuestGiverIndicatorParticleDisableIfTPSBelow(getGeneralConfigDouble(
                "visual.armorstands.quest-giver-indicator-particle.disable-if-tps-below",
                -1d
        ));

        //Visual Colors
        configuration.setColorsMain(getGeneralConfigStringList(
                "visual.colors.main",
                Arrays.asList("#1985ff", "#2bc7ff")
        ));

        configuration.setColorsHighlight(getGeneralConfigStringList(
                "visual.colors.highlight",
                Arrays.asList("#00fffb", "#00ffc3")
        ));

        configuration.setColorsHighlight2(getGeneralConfigStringList(
                "visual.colors.highlight2",
                Arrays.asList("#ff2465", "#ff24a0")
        ));

        configuration.setColorsError(getGeneralConfigStringList(
                "visual.colors.error",
                Arrays.asList("#ff004c", "#a80000")
        ));

        configuration.setColorsSuccess(getGeneralConfigStringList(
                "visual.colors.success",
                Arrays.asList("#54b2ff", "#ff5ecc")
        ));

        configuration.setColorsUnimportant(getGeneralConfigStringList(
                "visual.colors.unimportant",
                Arrays.asList("#9c9c9c", "#858383")
        ));

        configuration.setColorsVeryUnimportant(getGeneralConfigStringList(
                "visual.colors.veryUnimportant",
                Arrays.asList("#5c5c5c", "#454545")
        ));

        configuration.setColorsWarn(getGeneralConfigStringList(
                "visual.colors.warn",
                Arrays.asList("#fff700", "#ffa629")
        ));

        configuration.setColorsPositive(getGeneralConfigStringList(
                "visual.colors.positive",
                Arrays.asList("#73ff00", "#00ffd0")
        ));

        configuration.setColorsNegative(getGeneralConfigStringList(
                "visual.colors.negative",
                Arrays.asList("#ff006f", "#ff002f")
        ));


        //Visual More
        configuration.setVisualTitleQuestSuccessfullyAccepted_enabled(getGeneralConfigBoolean(
                "visual.titles.quest-successfully-accepted.enabled",
                true
        ));

        configuration.setVisualTitleQuestFailed_enabled(getGeneralConfigBoolean(
                "visual.titles.quest-failed.enabled",
                true
        ));

        configuration.setVisualTitleQuestCompleted_enabled(getGeneralConfigBoolean(
                "visual.titles.quest-completed.enabled",
                true
        ));

        configuration.setVisualObjectiveTrackingShowProgressInActionBar(getGeneralConfigBoolean(
                "visual.objective-tracking.actionbar.enabled",
                true
        ));

        configuration.setVisualObjectiveTrackingShowProgressInBossBar(getGeneralConfigBoolean(
                "visual.objective-tracking.bossbar.enabled",
                true
        ));

        configuration.setVisualObjectiveTrackingBossBarTimer(getGeneralConfigInt(
                "visual.objective-tracking.bossbar.show-time",
                10
        ));

        configuration.setVisualObjectiveTrackingShowProgressInBossBarIfObjectiveCompleted(getGeneralConfigBoolean(
                "visual.objective-tracking.bossbar.show-if-objective-is-completed",
                false
        ));


        //GUI
        configuration.setQuestVisibilityEvaluationAlreadyAccepted(getGeneralConfigBoolean(
                "gui.quest-visibility-evaluations.already-accepted.enabled",
                true
        ));

        configuration.setQuestVisibilityEvaluationMaxAccepts(getGeneralConfigBoolean(
                "gui.quest-visibility-evaluations.max-accepts.enabled",
                true
        ));
        configuration.setQuestVisibilityEvaluationAcceptCooldown(getGeneralConfigBoolean(
                "gui.quest-visibility-evaluations.accept-cooldown.enabled",
                false
        ));
        configuration.setQuestVisibilityEvaluationConditions(getGeneralConfigBoolean(
                "gui.quest-visibility-evaluations.conditions.enabled",
                false
        ));

        configuration.setQuestPreviewUseGUI(getGeneralConfigBoolean(
                "gui.questpreview.enabled",
                true
        ));

        //Description
        configuration.setGuiQuestPreviewDescription_enabled(getGeneralConfigBoolean(
                "gui.questpreview.description.enabled",
                true
        ));


       /* key = "gui.questpreview.description.slot";
        if (!getGeneralConfig().isString("gui.questpreview.description.slot")) {
            getGeneralConfig().set(key, '1');
            valueChanged = true;
        }
        configuration.setGuiQuestPreviewDescription_slot(getGeneralConfig().getString(key, "1").charAt(0));*/

        configuration.setShowQuestItemAmount(getGeneralConfigBoolean(
                "gui.show-quest-item-amount",
                false
        ));

        configuration.setShowObjectiveItemAmount(getGeneralConfigBoolean(
                "gui.show-objective-item-amount",
                true
        ));

        configuration.setGuiQuestDescriptionMaxLineLength(getGeneralConfigInt(
                "gui.quest-description-max-line-length",
                50
        ));

        configuration.setGuiObjectiveDescriptionMaxLineLength(getGeneralConfigInt(
                "gui.objective-description-max-line-length",
                50
        ));

        configuration.setWrapLongWords(getGeneralConfigBoolean(
                "gui.wrap-long-words",
                false
        ));


        //Rewards
        configuration.setGuiQuestPreviewRewards_enabled(getGeneralConfigBoolean(
                "gui.questpreview.rewards.enabled",
                true
        ));


        /*key = "gui.questpreview.rewards.slot";
        if (!getGeneralConfig().isString(key)) {
            getGeneralConfig().set(key, '3');
            valueChanged = true;
        }
        configuration.setGuiQuestPreviewRewards_slot(getGeneralConfig().getString(key, "3").charAt(0));*/

        //Requirements
        configuration.setGuiQuestPreviewRequirements_enabled(getGeneralConfigBoolean(
                "gui.questpreview.requirements.enabled",
                true
        ));


        /*key = "gui.questpreview.requirements.slot";
        if (!getGeneralConfig().isString(key)) {
            getGeneralConfig().set(key, '5');
            valueChanged = true;
        }
        configuration.setGuiQuestPreviewRequirements_slot(getGeneralConfig().getString(key, "5").charAt(0));*/


        configuration.setUserCommandsUseGUI(getGeneralConfigBoolean(
                "gui.usercommands.enabled",
                true
        ));


        configuration.setSupportPlaceholderAPIInTranslationStrings(getGeneralConfigBoolean(
                "placeholders.support_placeholderapi_in_translation_strings",
                false
        ));


        configuration.setPlaceholder_player_active_quests_list_horizontal_separator(getGeneralConfigString(
                "placeholders.player_active_quests_list_horizontal.separator",
                " | "
        ));

        configuration.setPlaceholder_player_active_quests_list_horizontal_limit(getGeneralConfigInt(
                "placeholders.player_active_quests_list_horizontal.limit",
                -1
        ));

        configuration.setPlaceholder_player_active_quests_list_vertical_limit(getGeneralConfigInt(
                "placeholders.player_active_quests_list_vertical.limit",
                -1
        ));

        configuration.setPlaceholder_player_active_quests_list_horizontal_use_displayname_if_available(getGeneralConfigBoolean(
                "placeholders.player_active_quests_list_horizontal.use-displayname-if-available",
                true
        ));

        configuration.setPlaceholder_player_active_quests_list_vertical_use_displayname_if_available(getGeneralConfigBoolean(
                "placeholders.player_active_quests_list_vertical.use-displayname-if-available",
                true
        ));


        configuration.setIntegrationCitizensEnabled(getGeneralConfigBoolean(
                "integrations.citizens.enabled",
                true
        ));

        configuration.setIntegrationVaultEnabled(getGeneralConfigBoolean(
                "integrations.vault.enabled",
                true
        ));

        configuration.setIntegrationPlaceholderAPIEnabled(getGeneralConfigBoolean(
                "integrations.placeholderapi.enabled",
                true
        ));

        configuration.setIntegrationMythicMobsEnabled(getGeneralConfigBoolean(
                "integrations.mythicmobs.enabled",
                true
        ));

        configuration.setIntegrationEliteMobsEnabled(getGeneralConfigBoolean(
                "integrations.elitemobs.enabled",
                true
        ));

        configuration.setIntegrationBetonQuestEnabled(getGeneralConfigBoolean(
                "integrations.betonquest.enabled",
                true
        ));

        configuration.setIntegrationWorldEditEnabled(getGeneralConfigBoolean(
                "integrations.worldedit.enabled",
                true
        ));

        configuration.setIntegrationSlimeFunEnabled(getGeneralConfigBoolean(
                "integrations.slimefun.enabled",
                true
        ));

        configuration.setIntegrationLuckPermsEnabled(getGeneralConfigBoolean(
                "integrations.luckperms.enabled",
                true
        ));

        configuration.setIntegrationUltimateClansEnabled(getGeneralConfigBoolean(
                "integrations.ultimateclans.enabled",
                true
        ));

        configuration.setIntegrationTownyEnabled(getGeneralConfigBoolean(
                "integrations.towny.enabled",
                true
        ));

        configuration.setIntegrationJobsRebornEnabled(getGeneralConfigBoolean(
                "integrations.jobs-reborn.enabled",
                true
        ));

        configuration.setIntegrationProjectKorraEnabled(getGeneralConfigBoolean(
                "integrations.project-korra.enabled",
                true
        ));

        configuration.setIntegrationEcoBossesEnabled(getGeneralConfigBoolean(
                "integrations.ecoBosses.enabled",
                true
        ));

        configuration.setIntegrationUltimateJobsEnabled(getGeneralConfigBoolean(
                "integrations.ultimatejobs.enabled",
                true
        ));

        configuration.setActionBarFancyCommandCompletionEnabled(getGeneralConfigBoolean(
                "visual.fancy-command-completion.actionbar-enabled",
                true
        ));

        configuration.setTitleFancyCommandCompletionEnabled(getGeneralConfigBoolean(
                "visual.fancy-command-completion.title-enabled",
                false
        ));

        configuration.setBossBarFancyCommandCompletionEnabled(getGeneralConfigBoolean(
                "visual.fancy-command-completion.bossbar-enabled",
                false
        ));

        configuration.setFancyCommandCompletionMaxPreviousArgumentsDisplayed(getGeneralConfigInt(
                "visual.fancy-command-completion.max-previous-arguments-displayed",
                2
        ));


        configuration.setMoveEventEnabled(getGeneralConfigBoolean(
                "general.enable-move-event",
                true
        ));

        configuration.setJournalItemEnabledWorlds(getGeneralConfigStringList(
                "general.journal-item.enabled-worlds",
                List.of("")
        ));

        configuration.setJournalInventorySlot(getGeneralConfigInt(
                "general.journal-item.inventory-slot",
                8
        ));

        ItemStack journal = new ItemStack(Material.ENCHANTED_BOOK, 1);
        ItemMeta im = journal.getItemMeta();
        ArrayList<Component> lore = new ArrayList<>();
        lore.add(main.parse("<GRAY>A book containing all your quest information").decoration(TextDecoration.ITALIC, false));
        if (im != null) {
            im.displayName(main.parse(
                    "<BLUE><ITALIC>Journal"
            ));
            im.lore(lore);
        }
        journal.setItemMeta(im);
        configuration.setJournalItem(getGeneralConfigItemStack(
                "general.journal-item.item",
                journal
        ));


        configuration.setPacketMagic(getGeneralConfigBoolean(
                "general.packet-magic.enabled",
                true
        ));

        configuration.setUsePacketEvents(getGeneralConfigString(
                "general.packet-magic.mode",
                "internal"
        ).equalsIgnoreCase("packetevents"));


        configuration.setPacketMagicUnsafeDisregardVersion(getGeneralConfigBoolean(
                "general.packet-magic.unsafe-disregard-version",
                false
        ));


        main.getLogManager().info("Detected version: " + Bukkit.getBukkitVersion() + " <highlight>(Paper)");

        if (!Bukkit.getBukkitVersion().contains("1.18") && !Bukkit.getBukkitVersion().contains("1.17")) {
            if (configuration.isPacketMagicUnsafeDisregardVersion()) {
                configuration.setPacketMagic(false);
                main.getLogManager().info("Packet magic has been disabled, because you are using an unsupported bukkit version...");
            } else {
                main.getLogManager().info("You are using an unsupported version for packet magic. However, because the unsafe-disregard-version flag which is set to true in your config, we will try to do some packet magic anyways. Let's hope it works - good luck!");
            }
        }

        configuration.setDeletePreviousConversations(getGeneralConfigBoolean(
                "general.packet-magic.conversations.delete-previous",
                true
        ));

        configuration.setPreviousConversationsHistorySize(getGeneralConfigInt(
                "general.packet-magic.conversations.history-size",
                20
        ));

        configuration.setUpdateCheckerNotifyOpsInChat(getGeneralConfigBoolean(
                "general.update-checker.notify-ops-in-chat",
                true
        ));




        //Do potential data updating here
        /////
        //Now update config version value, assuming everything is updated
        if (!getGeneralConfig().isString("config-version-do-not-edit") ||
           (getGeneralConfig().isString("config-version-do-not-edit") && !getGeneralConfig().getString("config-version-do-not-edit", "").equalsIgnoreCase(main.getMain().getDescription().getVersion()) )) {
            getGeneralConfig().set("config-version-do-not-edit", main.getMain().getDescription().getVersion());
            valueChanged = true;
        }
        configuration.setConfigurationVersion(getGeneralConfig().getString("config-version-do-not-edit"));



        if (valueChanged) {
            main.getLogManager().info("<highlight>General.yml</highlight> Configuration was updated with new values! Saving it...");
            saveGeneralConfig();
        }


        //If there was an error loading data from general.yml, the plugin will be disabled
        if (errored) {
            disablePluginAndSaving("Please specify your database information");
        }
    }

    public String getGeneralConfigString(String key, String defaultValue){
        if (!getGeneralConfig().isString(key)) {
            getGeneralConfig().set(key, defaultValue);
            valueChanged = true;
        }
        return getGeneralConfig().getString(key);
    }
    public boolean getGeneralConfigBoolean(String key, boolean defaultValue){
        if (!getGeneralConfig().isBoolean(key)) {
            getGeneralConfig().set(key, defaultValue);
            valueChanged = true;
        }
        return getGeneralConfig().getBoolean(key);
    }
    public int getGeneralConfigInt(String key, int defaultValue){
        if (!getGeneralConfig().isInt(key)) {
            getGeneralConfig().set(key, defaultValue);
            valueChanged = true;
        }
        return getGeneralConfig().getInt(key);
    }
    public double getGeneralConfigDouble(String key, double defaultValue){
        if (!getGeneralConfig().isDouble(key)) {
            getGeneralConfig().set(key, defaultValue);
            valueChanged = true;
        }
        return getGeneralConfig().getDouble(key);
    }
    public ItemStack getGeneralConfigItemStack(String key, ItemStack defaultValue){
        if (!getGeneralConfig().isItemStack(key)) {
            getGeneralConfig().set(key, defaultValue);
            valueChanged = true;
        }
        return getGeneralConfig().getItemStack(key);
    }

    public List<String> getGeneralConfigStringList(String key, List<String> defaultValue){
        if (!getGeneralConfig().isList(key)) {
            getGeneralConfig().set(key, defaultValue);
            valueChanged = true;
        }
        return getGeneralConfig().getStringList(key);
    }


    /**
     * This will try to save the general.yml configuration file with the data which is currently in the
     * generalConfig FileConfiguration object.
     */
    public void saveGeneralConfig() {
        try {
            getGeneralConfig().save(generalConfigFile);
        } catch (IOException ioException) {
            main.getLogManager().severe("General Config file could not be saved.");
        }
    }
    /*public void saveQuestsConfig() {
        if (isCurrentlyLoading()) {
            main.getLogManager().warn("Quest data saving has been skipped, because the plugin is currently loading.");
            return;
        }
        if (isSavingEnabled()) {
            if (questsConfig == null || questsConfigFile == null) {
                main.getLogManager().severe("Could not save data to quests.yml");
                return;
            }
            try {
                if (getConfiguration().storageCreateBackupsWhenSavingQuests) {
                    main.getBackupManager().backupQuests();
                }
                getQuestsConfig().save(questsConfigFile);
                main.getLogManager().info("Saved Data to quests.yml");
            } catch (IOException e) {
                main.getLogManager().severe("Could not save quests config to <highlight>" + questsConfigFile + "</highlight>. Stacktrace:");
                e.printStackTrace();
            }
        } else {
            main.getLogManager().info("Quest data saving has been skipped, because saving has been disabled. This usually happens when something goes wrong during Quest data loading, to prevent data loss.");
        }
    }*/

    public void backupQuests() {
        if (!getConfiguration().isStorageCreateBackupsWhenServerShutsDown()) {
            return;
        }
        if (isCurrentlyLoading()) {
            main.getLogManager().warn("Quest data backup has been skipped, because the plugin is currently loading.");
            return;
        }
        for(Category category : getCategories()){
            main.getBackupManager().backupQuests(category);
        }
    }

    /**
     * This will set saving to false, so the plugin will not try to save any kind of data anymore. After that, it
     * disables the plugin.
     *
     * @param reason the reason for disabling saving and the plugin. Will be shown in the console error message
     */
    public void disablePluginAndSaving(final String reason) {
        main.getLogManager().severe("Plugin, saving and loading has been disabled. Reason: " + reason);
        setSavingEnabled(false);
        setLoadingEnabled(false);
        disabled = true;

        criticalErrors.add(reason);

        //main.getMain().getServer().getPluginManager().disablePlugin(main.getMain());
    }

    public void disablePluginAndSaving(final String reason, Object... objects) {

        main.getLogManager().severe("Plugin, saving and loading has been disabled. Reason: " + reason);

        String reasonWithObjects = reason;

        for (Object object : objects) {
            if (object instanceof Exception e) {
                main.getLogManager().severe("Error message:");
                e.printStackTrace();

                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                reasonWithObjects += "\n" + "Error message:" + "\n" + sw.toString();
            }else if (object instanceof Throwable throwable) {
                main.getLogManager().severe("Error message:");
                throwable.printStackTrace();

                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                reasonWithObjects += "\n" + "Error message:" + "\n" + sw.toString();
            } else if (object instanceof Quest quest) {
                main.getLogManager().severe("  <DARK_GRAY>└─</DARK_GRAY> Quest: <highlight>"+ quest.getQuestName());
                reasonWithObjects += "\n" + "  <DARK_GRAY>└─</DARK_GRAY> Quest: <highlight>"+ quest.getQuestName();
            } else if (object instanceof Objective objective) {
                main.getLogManager().severe("  <DARK_GRAY>└─</DARK_GRAY> Objective ID: <highlight>" + objective.getObjectiveID() + "</highlight> of Quest: <highlight2>" + ((Objective) object).getQuest().getQuestName());
                reasonWithObjects += "\n" + "  <DARK_GRAY>└─</DARK_GRAY> Objective ID: <highlight>" + objective.getObjectiveID() + "</highlight> of Quest: <highlight2>" + ((Objective) object).getQuest().getQuestName();
            } else if (object instanceof Action action) {
                main.getLogManager().severe("  <DARK_GRAY>└─</DARK_GRAY> Action Name: <highlight>" + action.getActionName() + "</highlight> of Type: <highlight2>" + action.getActionType());
                reasonWithObjects += "\n" + "  <DARK_GRAY>└─</DARK_GRAY> Action Name: <highlight>" + action.getActionName() + "</highlight> of Type: <highlight2>" + action.getActionType();
            }
        }
        setSavingEnabled(false);
        setLoadingEnabled(false);

        disabled = true;

        criticalErrors.add(reasonWithObjects);
        //main.getMain().getServer().getPluginManager().disablePlugin(main.getMain());
    }

    public void enablePluginAndSaving(final String reason) {
        main.getLogManager().severe("Plugin, saving and loading has been enabled. Reason: " + reason);
        setSavingEnabled(true);
        setLoadingEnabled(true);
        disabled = false;

        //main.getMain().getServer().getPluginManager().disablePlugin(main.getMain());
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
            if(!main.getConfiguration().isSavePlayerDataOnQuit()){
                main.getTagManager().saveAllOnlinePlayerTags();
                main.getQuestPlayerManager().savePlayerData();
            }else{
                for(QuestPlayer questPlayer : new ArrayList<>(main.getQuestPlayerManager().getQuestPlayers())) {
                    main.getTagManager().onQuit(questPlayer, questPlayer.getPlayer());
                    main.getQuestPlayerManager().saveSinglePlayerData(questPlayer.getPlayer());
                }
            }
            backupQuests();
            //saveQuestsConfig();
        } else {
            main.getLogManager().warn("Saving is disabled => no data has been saved.");
        }
    }




    /**
     * This will LOAD the following data:
     * (1) general.yml General Configuration - in whatever Thread
     * (2) CREATE all the necessary MySQL Database tables if they don't exist yet - in an asynchronous Thread (forced)
     * (3) Load all the Quests Data from the quests.yml - in an asynchronous Thread (forced)
     * (4) AFTER THAT load the Player Data from the MySQL Database - in an asynchronous Thread (forced)
     * (5) Then it will try to load the Data from Citizens NPCs
     */
    public void reloadData() {
        if(isLoadingEnabled()){

            main.getLanguageManager().loadLanguageConfig();

            //Check for isLoadingEnabled again, in case it changed during loading of the general config
            if(!isLoadingEnabled()){
                main.getLogManager().severe("Data loading has been skipped, because it has been disabled. This is because there was an error loading from the general config.");
                return;
            }

            if (Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTaskAsynchronously(main.getMain(), () -> {
                    reloadData2();
                    currentlyLoading = false;
                });
            } else { //If this is already an asynchronous thread, this else{ thingy does not try to create a new asynchronous thread for better performance. The contents of this else section is identical.2
                reloadData2();
                currentlyLoading = false;
            }
        } else {
            main.getLogManager().severe("Data loading has been skipped, because it has been disabled. This might be caused because of an error during plugin startup earlier.");
        }
    }

    private void reloadData2() {
        openConnection();


        //Create Database tables if they don't exist yet
        try (Connection connection = getConnection();
             PreparedStatement createQuestPlayerDataTableStatement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `QuestPlayerData` (`PlayerUUID` varchar(200), `QuestPoints` BIGINT(255), PRIMARY KEY (PlayerUUID))");
             PreparedStatement createActiveQuestsDataTableStatement = connection.prepareStatement( "CREATE TABLE IF NOT EXISTS `ActiveQuests` (`QuestName` varchar(200), `PlayerUUID` varchar(200))" );
             PreparedStatement createCompletedQuestsDataTableStatement = connection.prepareStatement( "CREATE TABLE IF NOT EXISTS `CompletedQuests` (`QuestName` varchar(200), `PlayerUUID` varchar(200), `TimeCompleted` BIGINT(255))" );
             PreparedStatement createActiveObjectivesDataTableStatement = connection.prepareStatement( "CREATE TABLE IF NOT EXISTS `ActiveObjectives` (`ObjectiveType` varchar(200), `QuestName` varchar(200), `PlayerUUID` varchar(200), `CurrentProgress` BIGINT(255), `ObjectiveID` INT(255), `HasBeenCompleted` BOOLEAN)" );
             PreparedStatement createActiveTriggersDataTableStatement = connection.prepareStatement( "CREATE TABLE IF NOT EXISTS `ActiveTriggers` (`TriggerType` varchar(200), `QuestName` varchar(200), `PlayerUUID` varchar(200), `CurrentProgress` BIGINT(255), `TriggerID` INT(255))" );
        ) {
            main.getLogManager().info(LogCategory.DATA, "Creating database table 'QuestPlayerData' if it doesn't exist yet...");
            createQuestPlayerDataTableStatement.executeUpdate();

            main.getLogManager().info(LogCategory.DATA, "Creating database table 'ActiveQuests' if it doesn't exist yet...");
            createActiveQuestsDataTableStatement.executeUpdate();

            main.getLogManager().info(LogCategory.DATA, "Creating database table 'CompletedQuests' if it doesn't exist yet...");
            createCompletedQuestsDataTableStatement.executeUpdate();

            main.getLogManager().info(LogCategory.DATA, "Creating database table 'ActiveObjectives' if it doesn't exist yet...");
            createActiveObjectivesDataTableStatement.executeUpdate();

            main.getLogManager().info(LogCategory.DATA, "Creating database table 'ActiveTriggers' if it doesn't exist yet...");
            createActiveTriggersDataTableStatement.executeUpdate();

        } catch (SQLException e) {
            disablePluginAndSaving("Plugin disabled, because there was an error while trying to load MySQL database tables", e);
            return;
        }




        if (isSavingEnabled()) {
            main.getLogManager().info("Loaded player data");

            if (!isAlreadyLoadedQuests()) {
                loadCategories();
                main.getQuestManager().loadQuestsFromConfig();

            } else {
                main.getLogManager().info("Skipped loading Quests data, because they're already loaded (apparently).");
               /* main.getLogManager().info("Loading Data from existing quests.yml... - reloadData");
                try {
                    questsConfig = loadYAMLConfiguration(questsConfigFile);
                } catch (Exception e) {
                    disablePluginAndSaving("Plugin disabled, because there was an error loading data from quests.yml.", e);
                    return;
                }
                main.getQuestManager().loadQuestsFromConfig();*///TODO: Check up on that. Can I just comment it out?

            }

            if(!main.getConfiguration().isLoadPlayerDataOnJoin()){
                main.getQuestPlayerManager().loadPlayerData();
            }else{
                if (Bukkit.isPrimaryThread()) {
                    Bukkit.getScheduler().runTaskAsynchronously(main.getMain(), () -> {
                        for(final Player player : Bukkit.getOnlinePlayers()){
                            main.getQuestPlayerManager().loadSinglePlayerData(player);
                        }
                    });
                }else{
                    for(final Player player : Bukkit.getOnlinePlayers()){
                        main.getQuestPlayerManager().loadSinglePlayerData(player);
                    }                }

            }

            //Citizens stuff if Citizens is enabled
            if (main.getIntegrationsManager().isCitizensEnabled()) {
                //IF an NPC exist, try to load NPC data.
                boolean foundNPC = false;
                try{
                    for (final NPC ignored : CitizensAPI.getNPCRegistry().sorted()) {
                        foundNPC = true;
                        break;
                    }
                    if (foundNPC && !isAlreadyLoadedNPCs()) {
                        loadNPCData();
                    }
                }catch (Exception e){
                    if(main.getConfiguration().isDebug()){
                        e.printStackTrace();
                    }
                }

            }

        }
    }


    public boolean isCurrentlyLoading() {
        return currentlyLoading;
    }

    /*
     * This will return the quests.yml Configuration FileConfiguration object.
     * If it does not exist, it will try to load ALL the data again in reloadData()
     * which should also create the quests.yml file
     *
     * @return the quests.yml Configuration FileConfiguration object

    public final FileConfiguration getQuestsConfig() {
        if (!isAlreadyLoadedQuests() && !isCurrentlyLoading()) {
            reloadData();
        }
        return questsConfig;
    }*/

    /**
     * This will return the general.yml Configuration FileConfiguration object.
     * If it does not exist, it will try to load / create it.
     *
     * @return the general.yml Configuration FileConfiguration object
     */
    public final FileConfiguration getGeneralConfig() {
        if (!isAlreadyLoadedGeneral() && !isCurrentlyLoading()) {
            loadGeneralConfig();
        }
        return generalConfig;
    }






    /**
     * @return if the plugin will try to save data once it's disabled.
     * This should be true unless a severe error occurred during data
     * loading
     */
    public final boolean isSavingEnabled() {
        return savingEnabled;
    }

    /**
     * @param savingEnabled sets if data saving should be enabled or disabled
     */
    public void setSavingEnabled(final boolean savingEnabled) {
        this.savingEnabled = savingEnabled;
    }

    /**
     * @return if the plugin will try to load data.
     * This should be true unless a severe error occurred during data
     * loading
     */
    public final boolean isLoadingEnabled() {
        return loadingEnabled;
    }

    /**
     * @param loadingEnabled sets if data loading should be enabled or disabled
     */
    public void setLoadingEnabled(final boolean loadingEnabled) {
        this.loadingEnabled = loadingEnabled;
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
            Bukkit.getScheduler().runTaskAsynchronously(main.getMain(), () -> {
                if (!isAlreadyLoadedQuests()) {
                    loadCategories();
                }
                main.getQuestManager().loadNPCData();
            });
        } else {
            if (!isAlreadyLoadedQuests()) {
                loadCategories();
            }
            main.getQuestManager().loadNPCData();
        }

    }


    /**
     * @return if Citizen NPCs have been already successfully loaded by the plugin
     */
    public boolean isAlreadyLoadedNPCs() {
        return alreadyLoadedNPCs;
    }

    public boolean isAlreadyLoadedGeneral() {
        return alreadyLoadedGeneral && generalConfig != null && generalConfigFile != null;
    }

    public boolean isAlreadyLoadedQuests() {
        if (!alreadyLoadedQuests) {
            return false;
        }
        for (Category category : categories) {
            if (category.getQuestsConfig() == null || category.getQuestsConfig() == null) {
                main.getLogManager().info("Returned false for already loaded quests due to categories not loaded.");
                return false;
            }
        }
        return true;
    }

    /**
     * @param alreadyLoadedNPCs sets if Citizen NPCs have been already successfully loaded by the plugin
     */
    public void setAlreadyLoadedNPCs(boolean alreadyLoadedNPCs) {
        this.alreadyLoadedNPCs = alreadyLoadedNPCs;
    }

    public void setAlreadyLoadedGeneral(boolean alreadyLoadedGeneral) {
        this.alreadyLoadedGeneral = alreadyLoadedGeneral;
    }

    public void setAlreadyLoadedQuests(boolean alreadyLoadedQuests) {
        this.alreadyLoadedQuests = alreadyLoadedQuests;
    }


    /**
     * @return the configuration object which contains values from the General.yml
     */
    public final Configuration getConfiguration(){
        return configuration;
    }

    /**
     *
     * (Taken from API, but this method also throws errors)
     *
     * Creates a new {@link YamlConfiguration}, loading from the given file.
     * <p>
     * Any errors loading the Configuration will be logged and then ignored.
     * If the specified input is not a valid config, a blank config will be
     * returned.
     * <p>
     * The encoding used may follow the system dependent default.
     *
     * @param file Input file
     * @return Resulting configuration
     * @throws IllegalArgumentException Thrown if file is null
     */
    public final YamlConfiguration loadYAMLConfiguration(File file) throws IOException, InvalidConfigurationException {
        Validate.notNull(file, "File cannot be null");
        YamlConfiguration config = new YamlConfiguration();
        config.load(file);
        return config;
    }


    public HashMap<Integer, Object> getItemStackCache() {
        return itemStackCache;
    }

    /**
     * Load values of the standard tab-completion lists. This is done in a separate method,
     * because some lists are dependent on the integrations being loaded first.
     */
    public void loadStandardCompletions() {
        numberCompletions.clear();
        numberPositiveCompletions.clear();
        standardEntityTypeCompletions.clear();
        standardEliteMobNamesCompletions.clear();

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
            standardEntityTypeCompletions.add(entityType.toString().toLowerCase(Locale.ROOT));
        }
        //Add extra Mythic Mobs completions, if enabled
        if (main.getIntegrationsManager().isMythicMobsEnabled() && main.getIntegrationsManager().getMythicMobsManager() != null) {
            standardEntityTypeCompletions.addAll(main.getIntegrationsManager().getMythicMobsManager().getMobNames());
        }

        //Add extra EcoBosses completions, if enabled
        if (main.getIntegrationsManager().isEcoBossesEnabled() && main.getIntegrationsManager().getEcoBossesManager() != null) {
            standardEntityTypeCompletions.addAll(main.getIntegrationsManager().getEcoBossesManager().getBossNames());
        }

        //Fill up standardEliteMobNamesCompletions if the EliteMobs integration is enabled
        if (main.getIntegrationsManager().isEliteMobsEnabled()) {
            standardEliteMobNamesCompletions.add("Elite_Blaze");
            standardEliteMobNamesCompletions.add("Elite_Cave_Spider");
            standardEliteMobNamesCompletions.add("Elite_Creeper");
            standardEliteMobNamesCompletions.add("Elite_Drowned");
            standardEliteMobNamesCompletions.add("Elite_Elder_Guardian");
            standardEliteMobNamesCompletions.add("Elite_Enderman");
            standardEliteMobNamesCompletions.add("Elite_Endermite");
            standardEliteMobNamesCompletions.add("Elite_Evoker");
            standardEliteMobNamesCompletions.add("Elite_Ghast");
            standardEliteMobNamesCompletions.add("Elite_Guardian");
            standardEliteMobNamesCompletions.add("Elite_Hoglin");
            standardEliteMobNamesCompletions.add("Elite_Husk");
            standardEliteMobNamesCompletions.add("Elite_Illusioner");
            standardEliteMobNamesCompletions.add("Elite_Iron_Golem");
            standardEliteMobNamesCompletions.add("Elite_Phantom");
            standardEliteMobNamesCompletions.add("Elite_Piglin");
            standardEliteMobNamesCompletions.add("Elite_Piglin_Brute");
            standardEliteMobNamesCompletions.add("Elite_Pillager");
            standardEliteMobNamesCompletions.add("Elite_Polar_Bear");
            standardEliteMobNamesCompletions.add("Elite_Killer_Rabbit");
            standardEliteMobNamesCompletions.add("Elite_Ravager");
            standardEliteMobNamesCompletions.add("Elite_Shulker");
            standardEliteMobNamesCompletions.add("Elite_Silverfish");
            standardEliteMobNamesCompletions.add("Elite_Skeleton");
            standardEliteMobNamesCompletions.add("Elite_Spider");
            standardEliteMobNamesCompletions.add("Elite_Stray");
            standardEliteMobNamesCompletions.add("Elite_Vex");
            standardEliteMobNamesCompletions.add("Elite_Vindicator");
            standardEliteMobNamesCompletions.add("Elite_Witch");
            standardEliteMobNamesCompletions.add("Elite_Wither_Skeleton");
            standardEliteMobNamesCompletions.add("Elite_Wolf");
            standardEliteMobNamesCompletions.add("Elite_Zoglin");
            standardEliteMobNamesCompletions.add("Elite_Zombie");
            standardEliteMobNamesCompletions.add("Elite_Zombified_Piglin");
        }
    }




    /*public void openConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            if(!getConfiguration().isMySQLEnabled()){
                File dataFolder = new File(main.getMain().getDataFolder(), "database_sqlite.db");
                if (!dataFolder.exists()){
                    try {
                        if(!dataFolder.createNewFile()){
                            main.getLogManager().severe("File write error: database_sqlite.db (1)");
                        }
                    } catch (IOException e) {
                        main.getLogManager().severe("File write error: database_sqlite.db (2)");
                    }
                }
                try {
                    if(connection!=null && !connection.isClosed()){
                        return;
                    }
                    Class.forName("org.sqlite.JDBC");
                    connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder);
                } catch (SQLException ex) {
                    main.getLogManager().severe("SQLite exception on initialize");
                    ex.printStackTrace();
                } catch (ClassNotFoundException ex) {
                    main.getLogManager().severe("You need the SQLite JBDC library. Google it. Put it in /lib folder.");
                }
            }else{
                // Class.forName("com.mysql.jdbc.Driver"); - Use this with old version of the Driver
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection("jdbc:mysql://"
                                + configuration.getDatabaseHost() + ":" + configuration.getDatabasePort() + "/" + configuration.getDatabaseName() + "?autoReconnect=true",
                        configuration.getDatabaseUsername(), configuration.getDatabasePassword());
            }

        } catch (SQLException | ClassNotFoundException e) {
            disablePluginAndSaving("Could not connect to MySQL Database. Please check the information you entered in the general.yml. If you're not using MySQL, make sure it's disable in the general.yml. It would then use SQLite instead.", e);
        }
    }*/

    public void openConnection(){
        if(hikariDataSource != null){
            return;
        }
        hikariConfig = new HikariConfig();
        if(!getConfiguration().isMySQLEnabled()){
            File dataFolder = new File(main.getMain().getDataFolder(), "database_sqlite.db");
            if (!dataFolder.exists()){
                try {
                    if(!dataFolder.createNewFile()){
                        main.getLogManager().severe("File write error: database_sqlite.db (1)");
                    }
                } catch (IOException e) {
                    main.getLogManager().severe("File write error: database_sqlite.db (2)");
                }
            }

            hikariConfig.setJdbcUrl("jdbc:sqlite:" +  dataFolder);
        }else{
            hikariConfig.setJdbcUrl("jdbc:mysql://" +  configuration.getDatabaseHost() + ":" + configuration.getDatabasePort() + "/" + configuration.getDatabaseName());
            hikariConfig.setUsername(configuration.getDatabaseUsername());
            hikariConfig.setPassword(configuration.getDatabasePassword());
        }
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.setMaximumPoolSize(20);

        hikariDataSource = new HikariDataSource(hikariConfig);
    }

    public final Connection getConnection() throws SQLException {
        return hikariDataSource.getConnection();
    }

    public void closeDatabaseConnection() {
        main.getLogManager().info("Closing database connection...");
        try{
            hikariDataSource.close();
            //getConnection().close();
        }catch (Exception e){
            main.getLogManager().severe("Error closing database connection:");
            e.printStackTrace();
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
    /*public void refreshDatabaseConnection(final boolean newTask) {
        if (newTask) {
            if (Bukkit.isPrimaryThread()) {
                Bukkit.getScheduler().runTaskAsynchronously(main.getMain(), () -> {
                    openConnection();
                    try {
                        statement = connection.createStatement();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                });
            } else {
                openConnection();
                try {
                    statement = connection.createStatement();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } else {
            openConnection();
            try {
                statement = connection.createStatement();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }*/


    public void sendPluginDisabledMessage(final CommandSender commandSender){
        commandSender.sendMessage(main.parse("<error>Error - NotQuests is disabled. This usually happens when something goes wrong during loading any data from not quests (usually a faulty quest configuration). NotQuests does this to protect itself from data loss. Please report this to the server owner and tell him to check the console for any errors BEFORE the 'notquests has been disabled' message.\n<highlight>Reasons why NotQuests disabled itself (read them & fix them):"));
        if(criticalErrors.size() == 0){
            commandSender.sendMessage(main.parse(
                    "<warn>No critical errors found. Please check your console log for any errors during startup."
            ));
        }else{
            {
                int counter = 0;
                for(final String criticalError : criticalErrors){
                    commandSender.sendMessage(main.parse(
                            "<highlight>" + ++counter + ". <warn>" + criticalError
                    ));
                }
            }
        }
        if(main.getLogManager().getErrorLogs().size() > 0) {
            int counter = 0;

            commandSender.sendMessage(main.parse(
                   "\n<highlight>Other collected (possibly-relevant) error logs:"
            ));
            for(final String error : main.getLogManager().getErrorLogs()){
                commandSender.sendMessage(main.parse(
                        "<highlight>" + ++counter + ". <warn>" + error
                ));
            }
        }

        if(main.getLogManager().getWarnLogs().size() > 0) {
            int counter = 0;

            commandSender.sendMessage(main.parse(
                    "\n<highlight>Other warnings (they are not the cause for this issue, but may still be worth fixing. Some warnings can be ignored):"
            ));
            for(final String warning : main.getLogManager().getWarnLogs()){
                commandSender.sendMessage(main.parse(
                        "<highlight>" + ++counter + ". <warn>" + warning
                ));
            }
        }

        commandSender.sendMessage(main.parse("<unimportant>You could enable the plugin again by using <main>/qa debug enablePluginAndSaving <reason></main>, but it's not recommended. Fix the issues above or in the console log instead - this mechanism is there to protect yourself from faulty quests. If you're an admin of the server and don't know how to fix these issues alone, please contact the me (the plugin developer) and send me this message, together with your full server log and optimally the NotQuests folder - I'll gladly help <3"));
    }

}
