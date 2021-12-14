package rocks.gravili.notquests.Managers;

import org.bukkit.configuration.ConfigurationSection;
import rocks.gravili.notquests.NotQuests;
import rocks.gravili.notquests.Structs.Conditions.ObjectiveCompletedCondition;
import rocks.gravili.notquests.Structs.Objectives.Objective;
import rocks.gravili.notquests.Structs.Quest;

public class UpdateManager {
    final UpdateChecker updateChecker;
    private final NotQuests main;

    public UpdateManager(final NotQuests main) {
        this.main = main;
        this.updateChecker = new UpdateChecker(main, 95872);
    }

    public void checkForPluginUpdates() {
        try {
            if (updateChecker.checkForUpdates()) {
                main.getLogManager().info("<GOLD>The version <Yellow>" + main.getDescription().getVersion()
                        + " <GOLD>is not the latest version (<Green>" + updateChecker.getLatestVersion() + "<GOLD>)! Please update the plugin here: <Aqua>https://www.spigotmc.org/resources/95872/ <DARK_GRAY>(If your version is newer, the spigot API might not be updated yet).");
            } else {
                main.getLogManager().info("NotQuests seems to be up to date! :)");
            }
        } catch (Exception e) {
            e.printStackTrace();
            main.getLogManager().info("Unable to check for updates ('" + e.getMessage() + "').");
        }
    }

    public String convertQuestRequirementTypeToConditionType(final String questName, final String requirementID) {
        main.getLogManager().info("Converting old requirementType to conditionType...");
        String oldRequirementType = main.getDataManager().getQuestsConfig().getString("quests." + questName + ".requirements." + requirementID + ".requirementType", "");
        if(oldRequirementType.isBlank()){
            main.getLogManager().warn("There was an error converting the old requirementType to conditionType: Old requirementType is empty. Skipping conversion...");
            return "";
        }

        main.getDataManager().getQuestsConfig().set("quests." + questName + ".requirements." + requirementID + ".requirementType", null);
        main.getDataManager().getQuestsConfig().set("quests." + questName + ".requirements." + requirementID + ".conditionType", oldRequirementType);
        main.getDataManager().saveQuestsConfig();
        return oldRequirementType;
    }


    public void convertObjectiveDependenciesToNewObjectiveConditions(final Quest quest) {

        for (final Objective objective : quest.getObjectives()) {
            final ConfigurationSection objectiveDependenciesConfigurationSection = main.getDataManager().getQuestsConfig().getConfigurationSection("quests." + quest.getQuestName() + ".objectives." + objective.getObjectiveID() + ".dependantObjectives.");
            if (objectiveDependenciesConfigurationSection != null) {
                main.getLogManager().info("Converting old objective dependencies to objective conditions...");
                for (String objectiveDependencyNumber : objectiveDependenciesConfigurationSection.getKeys(false)) {
                    //Get old stuff
                    int dependantObjectiveID = main.getDataManager().getQuestsConfig().getInt("quests." + quest.getQuestName() + ".objectives." + (objective.getObjectiveID()) + ".dependantObjectives." + objectiveDependencyNumber + ".objectiveID", objective.getObjectiveID());

                    //Delete old stuff
                    main.getDataManager().getQuestsConfig().set("quests." + quest.getQuestName() + ".objectives." + objective.getObjectiveID() + ".dependantObjectives", null);

                    //Create new stuff with old stuff
                    ObjectiveCompletedCondition objectiveCompletedCondition = new ObjectiveCompletedCondition(main, quest, objective);
                    objectiveCompletedCondition.setObjectiveID(dependantObjectiveID);
                    objective.addCondition(objectiveCompletedCondition, true);

                    //Conversion done. Now save conversion
                    main.getDataManager().saveQuestsConfig();
                }
            }
        }

    }
}
