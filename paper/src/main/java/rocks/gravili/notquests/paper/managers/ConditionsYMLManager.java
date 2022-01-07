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

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.conditions.Condition;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static rocks.gravili.notquests.paper.commands.NotQuestColors.*;

public class ConditionsYMLManager {
    private final NotQuests main;
    private final HashMap<String, Condition> conditionsAndIdentifiers;
    /**
     * conditions.yml Configuration File
     */
    private File conditionsConfigFile = null;
    /**
     * conditions.yml Configuration
     */
    private FileConfiguration conditionsConfig;


    public ConditionsYMLManager(final NotQuests main) {
        this.main = main;
        conditionsAndIdentifiers = new HashMap<>();

        setupFiles();
    }

    public void setupFiles() {
        main.getLogManager().info("Loading conditions.yml config");
        if (conditionsConfigFile == null) {

            main.getDataManager().prepareDataFolder();

            conditionsConfigFile = new File(main.getMain().getDataFolder(), "conditions.yml");

            if (!conditionsConfigFile.exists()) {
                main.getLogManager().info("Conditions Configuration (conditions.yml) does not exist. Creating a new one...");
                try {
                    //Try to create the conditions.yml config file, and throw an error if it fails.
                    if (!conditionsConfigFile.createNewFile()) {
                        main.getDataManager().disablePluginAndSaving("There was an error creating the conditions.yml config file.");
                        return;

                    }
                } catch (IOException ioException) {
                    main.getDataManager().disablePluginAndSaving("There was an error creating the conditions.yml config file. (2)", ioException);
                    return;
                }
            }

            conditionsConfig = new YamlConfiguration();
            try {
                conditionsConfig.load(conditionsConfigFile);
            } catch (IOException | InvalidConfigurationException e) {
                e.printStackTrace();
            }


        } else {
            conditionsConfig = YamlConfiguration.loadConfiguration(conditionsConfigFile);
        }
    }

    public void loadConditions() {
        //First load from conditions.yml:

        final ConfigurationSection conditionsConfigurationSection = getConditionsConfig().getConfigurationSection("conditions");
        if (conditionsConfigurationSection != null) {
            for (final String conditionIdentifier : conditionsConfigurationSection.getKeys(false)) {
                if (conditionsAndIdentifiers.get(conditionIdentifier) != null) {
                    main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an error while loading conditions.yml conditions data: The conditions " + conditionIdentifier + " already exists.");
                    return;
                }
                Class<? extends Condition> conditionType = null;
                String conditionTypeString = conditionsConfigurationSection.getString(conditionIdentifier + ".conditionType", "");


                try {
                    conditionType = main.getConditionsManager().getConditionClass(conditionTypeString);
                } catch (NullPointerException ex) {
                    main.getDataManager().disablePluginAndSaving("Error parsing conditions.yml condition Type of condition with name <highlight>" + conditionIdentifier + "</highlight>.", ex);
                }

                if (!conditionIdentifier.isBlank() && conditionType != null) {
                    Condition condition = null;

                    int progressNeeded = getConditionsConfig().getInt("conditions." + conditionIdentifier + ".progressNeeded");

                    try {
                        condition = conditionType.getDeclaredConstructor(NotQuests.class).newInstance(main);
                        condition.setConditionName(conditionIdentifier);
                        condition.setProgressNeeded(progressNeeded);
                        condition.load(getConditionsConfig(), "conditions." + conditionIdentifier);

                    } catch (Exception ex) {
                        main.getDataManager().disablePluginAndSaving("Error parsing condition Type of conditions.yml condition with name <highlight>" + conditionIdentifier + "</highlight>.", ex);
                    }

                    if (condition != null) {
                        /*final String actionsDisplayName = actionsConfigurationSection.getString(actionIdentifier + ".displayName", "");
                        if (!actionsDisplayName.isBlank()) {
                            action.setActionName(actionsDisplayName);
                        }*/
                        conditionsAndIdentifiers.put(conditionIdentifier, condition);
                    } else {
                        main.getDataManager().disablePluginAndSaving("Plugin disabled, because there was an error while loading conditions.yml condition data.");
                    }
                }

            }
        }

    }

    public void saveConditions() {
        try {
            conditionsConfig.save(conditionsConfigFile);
            main.getLogManager().info("Saved Data to conditions.yml");
        } catch (IOException e) {
            main.getLogManager().severe("Error saving condition. Condition were not saved...");

        }

    }

    public final FileConfiguration getConditionsConfig() {
        return conditionsConfig;
    }

    public final HashMap<String, Condition> getConditionsAndIdentifiers() {
        return conditionsAndIdentifiers;
    }

    public final Condition getCondition(String conditionIdentifier) {
        return conditionsAndIdentifiers.get(conditionIdentifier);
    }


    public final String addCondition(String conditionIdentifier, Condition condition) {
        boolean nameAlreadyExists = getConditionsAndIdentifiers().get(conditionIdentifier) != null;
        condition.setConditionName(conditionIdentifier);

        if (!nameAlreadyExists) {
            conditionsAndIdentifiers.put(conditionIdentifier, condition);

            getConditionsConfig().set("conditions." + conditionIdentifier + ".conditionType", condition.getConditionType());
            getConditionsConfig().set("conditions." + conditionIdentifier + ".progressNeeded", condition.getProgressNeeded());


            condition.save(getConditionsConfig(), "conditions." + conditionIdentifier);

            saveConditions();
            return ("<success>Condition <highlight>" + conditionIdentifier + "</highlight> successfully created!");
        } else {
            return ("<error>Condition <highlight>" + conditionIdentifier + "</highlight> already exists!");
        }
    }


    public String removeCondition(String conditionToDeleteIdentifier) {
        conditionsAndIdentifiers.remove(conditionToDeleteIdentifier);
        getConditionsConfig().set("conditions." + conditionToDeleteIdentifier, null);
        return "<success>Condition <highlight>" + conditionToDeleteIdentifier + "</highlight> successfully deleted!";
    }
}
