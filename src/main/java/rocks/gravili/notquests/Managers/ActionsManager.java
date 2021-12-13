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
import rocks.gravili.notquests.Structs.Triggers.Action;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ActionsManager {
    private final NotQuests main;
    private final ArrayList<Action> actions;
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
        actions = new ArrayList<>();

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
            for (final String actionName : actionsConfigurationSection.getKeys(false)) {
                final String consoleCommand = getActionsConfig().getString("actions." + actionName + ".specifics.consoleCommand", "");
                if (consoleCommand.equalsIgnoreCase("")) {
                    main.getLogManager().warn("Action has an empty console command. This should NOT be possible! Creating an action with an empty console command... Action name: <AQUA>" + actionName + "</AQUA>");
                }
                boolean nameAlreadyExists = false;
                for (final Action action : actions) {
                    if (action.getActionName().equalsIgnoreCase(actionName)) {
                        nameAlreadyExists = true;
                        break;
                    }
                }

                if (!nameAlreadyExists) {
                    final Action newAction = new Action(main, actionName, consoleCommand);
                    actions.add(newAction);
                    getActionsConfig().set("actions." + actionName + ".type", "ConsoleCommand");
                    getActionsConfig().set("actions." + actionName + ".specifics.consoleCommand", consoleCommand);

                } else {
                    main.getDataManager().disablePluginAndSaving("Action already exists. This should NOT be possible! Action name: <AQUA>" + actionName + "</AQUA>");
                    return;
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

    public final ArrayList<Action> getActions() {
        return actions;
    }

    public final Action getAction(String actionName) {
        for (Action action : actions) {
            if (action.getActionName().equalsIgnoreCase(actionName)) {
                return action;
            }
        }
        return null;
    }

    public final String createAction(String actionName, String consoleCommand) {
        boolean nameAlreadyExists = false;
        for (Action action : actions) {
            if (action.getActionName().equalsIgnoreCase(actionName)) {
                nameAlreadyExists = true;
                break;
            }
        }

        if (!nameAlreadyExists) {
            final Action newAction = new Action(main, actionName, consoleCommand);
            actions.add(newAction);
            getActionsConfig().set("actions." + actionName + ".type", "ConsoleCommand");
            getActionsConfig().set("actions." + actionName + ".specifics.consoleCommand", consoleCommand);

            saveActions();

            return (NotQuestColors.successGradient + "Action successfully created!");
        } else {
            return (NotQuestColors.errorGradient + "Action already exists!");
        }
    }

    public String removeAction(Action actionToDelete) {
        actions.remove(actionToDelete);
        main.getDataManager().getQuestsConfig().set("actions." + actionToDelete.getActionName(), null);
        return "§aAction successfully deleted!";

    }
}
