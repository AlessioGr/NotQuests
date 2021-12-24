package rocks.gravili.notquests.paper.managers;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.Quest;
import rocks.gravili.notquests.paper.structs.actions.ConsoleCommandAction;
import rocks.gravili.notquests.paper.structs.conditions.CompletedObjectiveCondition;
import rocks.gravili.notquests.paper.structs.objectives.Objective;

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
                main.getLogManager().info("<GOLD>The version <Yellow>" + main.getMain().getDescription().getVersion()
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
                    CompletedObjectiveCondition completedObjectiveCondition = new CompletedObjectiveCondition(main);
                    completedObjectiveCondition.setQuest(quest);
                    completedObjectiveCondition.setObjective(objective);
                    completedObjectiveCondition.setObjectiveID(dependantObjectiveID);
                    objective.addCondition(completedObjectiveCondition, true);

                    //Conversion done. Now save conversion
                    main.getDataManager().saveQuestsConfig();
                }
            }
        }

    }


    public void convertQuestsYMLActions() {
        //Actions load from quests.yml, so we can migrate them to actions.yml
        final ConfigurationSection oldActionsConfigurationSection = main.getDataManager().getQuestsConfig().getConfigurationSection("actions");
        if (oldActionsConfigurationSection != null) {
            for (final String actionIdentifier : oldActionsConfigurationSection.getKeys(false)) {
                String consoleCommand = main.getDataManager().getQuestsConfig().getString("actions." + actionIdentifier + ".consoleCommand", "");
                if (consoleCommand.isBlank()) {
                    consoleCommand = main.getDataManager().getQuestsConfig().getString("actions." + actionIdentifier + ".specifics.consoleCommand", ""); //TODO: Potentially unneeded and might be better to just use that directly. I think it ALWAYS uses the sepcifics. prefix in old versions.
                }

                if (consoleCommand.isBlank()) {
                    main.getLogManager().warn("Action has an empty console command. This should NOT be possible! Creating an action with an empty console command... Action name: <AQUA>" + actionIdentifier + "</AQUA>");
                }

                ConsoleCommandAction consoleCommandAction = new ConsoleCommandAction(main);
                consoleCommandAction.setConsoleCommand(consoleCommand);
                consoleCommandAction.setActionName(actionIdentifier);

                main.getActionsYMLManager().addAction(actionIdentifier, consoleCommandAction);

                main.getLogManager().info("Migrated the following action from quests.yml to actions.yml: <AQUA>" + actionIdentifier + "</AQUA>");
            }
        }

        //Now that they are loaded, let's delete them from the quests.yml and save the actions.yml
        main.getDataManager().getQuestsConfig().set("actions", null);
        main.getDataManager().saveQuestsConfig();


        //save them to write them to the actions.yml (in case of migration)
        main.getActionsYMLManager().saveActions();
    }

    public void convertActionsYMLBeforeVersion3() { //Pre-3.0
        boolean convertedSomething = false;
        final ConfigurationSection oldActionsConfigurationSection = main.getActionsYMLManager().getActionsConfig().getConfigurationSection("actions");
        if (oldActionsConfigurationSection != null) {
            for (final String actionIdentifier : oldActionsConfigurationSection.getKeys(false)) {
                String oldActionsType = oldActionsConfigurationSection.getString(actionIdentifier + ".type", "");
                if (!oldActionsType.isBlank()) {
                    oldActionsConfigurationSection.set(actionIdentifier + ".type", null);
                    oldActionsConfigurationSection.set(actionIdentifier + ".actionType", oldActionsType);
                    convertedSomething = true;
                }
            }
        }
        if (convertedSomething) {
            //save them to write them to the actions.yml (in case of migration)
            main.getActionsYMLManager().saveActions();
            main.getLogManager().info("Updated old actions.yml!");
        }

    }

    //Converts rewardType => actionType converter for Quest rewards
    public String convertQuestRewardTypeToActionType(final String questName, final String rewardNumber) { //Pre-3.0
        main.getLogManager().info("Converting old Quest rewardType to actionType...");
        String oldRewardType = main.getDataManager().getQuestsConfig().getString("quests." + questName + ".rewards." + rewardNumber + ".rewardType", "");
        if (oldRewardType.isBlank()) {
            main.getLogManager().warn("There was an error converting the old rewardType to actionType: Old rewardType is empty. Skipping conversion...");
            return "";
        }

        //Old reward types to new reward types
        oldRewardType = oldRewardType.replace("QuestPoints", "GiveQuestPoints")
                .replace("Item", "GiveItem")
                .replace("Money", "GiveMoney")
                .replace("Permission", "GrantPermission");

        main.getDataManager().getQuestsConfig().set("quests." + questName + ".rewards." + rewardNumber + ".rewardType", null);
        main.getDataManager().getQuestsConfig().set("quests." + questName + ".rewards." + rewardNumber + ".actionType", oldRewardType);
        main.getDataManager().saveQuestsConfig();
        return oldRewardType;
    }


    //Converts type => actionType converter for action.yml actions
    public String convertActionsYMLTypeToActionType(final ConfigurationSection actionsConfigurationSection, final String actionIdentifier) { //Pre-3.0
        main.getLogManager().info("Converting old Quest action.yml types to actionType...");
        String oldActionType = actionsConfigurationSection.getString(actionIdentifier + ".type", "");
        if (oldActionType.isBlank()) {
            main.getLogManager().warn("There was an error converting the old actions.yml action type to actionType: Old type is empty. Skipping conversion...");
            return "";
        }

        //Old reward types to new reward types
        oldActionType = oldActionType.replace("QuestPoints", "GiveQuestPoints")
                .replace("Item", "GiveItem")
                .replace("Money", "GiveMoney")
                .replace("Permission", "GrantPermission");

        actionsConfigurationSection.set(actionIdentifier + ".type", null);
        actionsConfigurationSection.set(actionIdentifier + ".actionType", oldActionType);

        main.getActionsYMLManager().saveActions();
        return oldActionType;
    }

    //BETA-6 => BETA-7
    public ItemStack convertTakeItemMaterialToItemStack(String questName) {
        if (main.getDataManager().getQuestsConfig().isString("quests." + questName + ".takeItem")) {
            //Convert to ItemStack
            ItemStack newItemStack = new ItemStack(Material.valueOf(main.getDataManager().getQuestsConfig().getString("quests." + questName + ".takeItem", "BOOK")));
            main.getDataManager().getQuestsConfig().set("quests." + questName + ".takeItem", newItemStack);
            main.getDataManager().saveQuestsConfig();
            return newItemStack;
        } else {
            return main.getDataManager().getQuestsConfig().getItemStack("quests." + questName + ".takeItem");
        }
    }

    //3.0BETA-9 => 3.0 Release
    public String convertOldConditionTypesToNewConditionTypes(final String oldConditionType) {
        if (oldConditionType.equals("OtherQuest")) {
            main.getLogManager().info("Converting old OtherQuest Condition Type to new CompletedQuest Condition Type...");
            return "CompletedQuest";
        } else if (oldConditionType.equals("CompleteObjective")) { //TODO: Might be unneeded
            main.getLogManager().info("Converting old CompleteObjective Condition Type to new CompletedObjective Condition Type...");
            return "CompletedObjective";
        } else if (oldConditionType.equals("ObjectiveCompleted")) {
            main.getLogManager().info("Converting old ObjectiveCompleted Condition Type to new CompletedObjective Condition Type...");
            return "CompletedObjective";
        }
        return oldConditionType;
    }
}
