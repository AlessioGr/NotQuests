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

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import rocks.gravili.notquests.Commands.NotQuestColors;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Actions.Action;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class ActionsManager {
    private final NotQuests main;
    private final HashMap<String, Action> actionsAndIdentifiers;
    /**
     * actions.yml Configuration File
     */
    private File actionsConfigFile = null;
    /**
     * actions.yml Configuration
     */
    private FileConfiguration actionsConfig;


    public ActionsManager(final NotQuests main) {
        this.main = main;
        actionsAndIdentifiers = new HashMap<>();

        setupFiles();
    }

    public void setupFiles() {
        main.getLogManager().info("Loading actions.yml config");
        if (actionsConfigFile == null) {

            main.getDataManager().prepareDataFolder();

            actionsConfigFile = new File(main.getDataFolder(), "actions.yml");

            if (!actionsConfigFile.exists()) {
                main.getLogManager().info("Actions Configuration (actions.yml) does not exist. Creating a new one...");
                try {
                    //Try to create the actions.yml config file, and throw an error if it fails.
                    if (!actionsConfigFile.createNewFile()) {
                        main.getDataManager().disablePluginAndSaving("There was an error creating the actions.yml config file.");
                        return;

                    }
                } catch (IOException ioException) {
                    main.getDataManager().disablePluginAndSaving("There was an error creating the actions.yml config file. (2)", ioException);
                    return;
                }
            }

            actionsConfig = new YamlConfiguration();
            try {
                actionsConfig.load(actionsConfigFile);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }


        } else {
            actionsConfig = YamlConfiguration.loadConfiguration(actionsConfigFile);
        }
    }

    public void loadActions() {
        //First load from actions.yml:

        final ConfigurationSection actionsConfigurationSection = getActionsConfig().getConfigurationSection("actions");
        if (actionsConfigurationSection != null) {
            for (final String actionIdentifier : actionsConfigurationSection.getKeys(false)) {
                if (actionsAndIdentifiers.get(actionIdentifier) != null) {
                    main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an error while loading actions.yml actions data: The action " + actionIdentifier + " already exists.");
                    return;
                }
                Class<? extends Action> actionType = null;
                String actionTypeString = actionsConfigurationSection.getString(actionIdentifier + ".actionType", "");
                if (actionTypeString.isBlank()) {
                    actionTypeString = main.getUpdateManager().convertActionsYMLTypeToActionType(actionsConfigurationSection, actionIdentifier);
                }

                try {
                    actionType = main.getActionManager().getActionClass(actionTypeString);
                } catch (NullPointerException ex) {
                    main.getDataManager().disablePluginAndSaving("Error parsing actions.yml action Type of action with name <AQUA>" + actionIdentifier + "</AQUA>.", ex);
                }

                if (!actionIdentifier.isBlank() && actionType != null) {
                    Action action = null;

                    try {
                        action = actionType.getDeclaredConstructor(NotQuests.class).newInstance(main);
                        action.setActionName(actionIdentifier);
                        action.load(getActionsConfig(), "actions." + actionIdentifier);

                    } catch (Exception ex) {
                        main.getDataManager().disablePluginAndSaving("Error parsing action Type of actions.yml action with name <AQUA>" + actionIdentifier + "</AQUA>.", ex);
                    }

                    if (action != null) {
                        final String actionsDisplayName = actionsConfigurationSection.getString(actionIdentifier + ".displayName", "");
                        if (!actionsDisplayName.isBlank()) {
                            action.setActionName(actionsDisplayName);
                        }
                        actionsAndIdentifiers.put(actionIdentifier, action);
                    } else {
                        main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an error while loading actions.yml actions data.");
                    }
                }

            }
        }

    }

    public void saveActions() {
        try {
            actionsConfig.save(actionsConfigFile);
            main.getLogManager().info("Saved Data to actions.yml");
        } catch (IOException e) {
            main.getLogManager().severe("Error saving actions. Actions were not saved...");

        }

    }

    public final FileConfiguration getActionsConfig() {
        return actionsConfig;
    }

    public final HashMap<String, Action> getActionsAndIdentifiers() {
        return actionsAndIdentifiers;
    }

    public final Action getAction(String actionIdentifer) {
        return actionsAndIdentifiers.get(actionIdentifer);
    }


    public final String addAction(String actionIdentifier, Action action) {
        boolean nameAlreadyExists = getActionsAndIdentifiers().get(actionIdentifier) != null;
        action.setActionName(actionIdentifier);

        if (!nameAlreadyExists) {
            actionsAndIdentifiers.put(actionIdentifier, action);

            getActionsConfig().set("actions." + actionIdentifier + ".actionType", action.getActionType());
            if (!action.getActionName().isBlank()) {
                getActionsConfig().set("actions." + actionIdentifier + ".displayName", action.getActionName());
            }

            action.save(getActionsConfig(), "actions." + actionIdentifier);

            saveActions();

            return (NotQuestColors.successGradient + "Action successfully created!");
        } else {
            return (NotQuestColors.errorGradient + "Action already exists!");
        }
    }


    public String removeAction(String actionToDeleteIdentifier) {
        actionsAndIdentifiers.remove(actionToDeleteIdentifier);
        main.getDataManager().getQuestsConfig().set("actions." + actionToDeleteIdentifier, null);
        return NotQuestColors.successGradient + "Action " + NotQuestColors.highlightGradient + actionToDeleteIdentifier + "</gradient> successfully deleted!";
    }
}
