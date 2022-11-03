/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
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

import com.jeff_media.updatechecker.UpdateCheckSource;
import com.jeff_media.updatechecker.UpdateChecker;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;

public class UpdateManager {
  final UpdateChecker updateChecker;
  private final NotQuests main;
  private String latestVersion = "";

  public UpdateManager(final NotQuests main) {
    this.main = main;
    latestVersion = main.getMain().getDescription().getVersion();

    this.updateChecker =
        new UpdateChecker(
                main.getMain(),
                UpdateCheckSource.CUSTOM_URL,
                "https://www.notquests.com/latest-version.txt")
            .checkEveryXHours(24) // Check every 24 hours
            .setDownloadLink("https://www.notquests.com/update/")
            .onSuccess(
                (commandSenders, latestVersion) -> {
                  this.latestVersion = (String) latestVersion;

                  final String[] split = ((String) latestVersion).split("\\.");

                  final int latestMajor = Integer.parseInt(split[0]);
                  final int latestMinor = Integer.parseInt(split[1]);
                  final int latestPatch = Integer.parseInt(split[2]);

                  final String oldVersion = main.getMain().getDescription().getVersion();

                  final String[] oldSplit = oldVersion.split("\\.");

                  final int oldMajor = Integer.parseInt(oldSplit[0]);
                  final int oldMinor = Integer.parseInt(oldSplit[1]);
                  final int oldPatch = Integer.parseInt(oldSplit[2]);

                  if (latestMajor < oldMajor) {
                    return;
                  } else if (latestMajor == oldMajor) {
                    if (latestMinor < oldMinor) {
                      return;
                    } else if (latestMinor == oldMinor) {
                      if (latestPatch <= oldPatch) {
                        return;
                      }
                    }
                  }

                  for (final CommandSender sender : (CommandSender[]) commandSenders) {
                    sender.sendMessage(
                        main.parse(
                            "<hover:show_text:\"<highlight>Click to update!\"><click:open_url:\"https://www.notquests.com/update/\"><main>[NotQuests]</main> <warn>Your version <red>"
                                + main.getMain().getDescription().getVersion()
                                + "</red> is not the latest version (<green>"
                                + latestVersion
                                + "</green>). <bold>Click this message to update: <underlined>https://www.notquests.com/update/</underlined></bold></click></hover>"));
                  }
                })
            .onFail(
                (commandSenders, exception) -> {
                  for (final CommandSender sender : (CommandSender[]) commandSenders) {
                    sender.sendMessage("Failed to run update check.");
                  }
                })
            .setNotifyRequesters(false)
            .setNotifyOpsOnJoin(false) // Notify OPs on Join when a new version is found (default)
            .checkEveryXHours(8) // Check every 30 minutes
            .setColoredConsoleOutput(true)
            .checkNow(); // And check right now
  }

  public final String getLatestVersion() {
    return latestVersion;
  }

  public void checkForPluginUpdates(final CommandSender commandSender) {
    updateChecker.checkNow(commandSender);
  }

  public void checkForPluginUpdates() {
    try {

      updateChecker.checkNow(main.getMain().getServer().getConsoleSender());
      /*updateChecker.requestUpdateCheck().whenComplete((result, e) -> {
          latestVersion = result.getNewestVersion();

          if (result.requiresUpdate()) {
              main.getLogManager().info("<unimportant>---------------------------------------------------------------------------------</unimportant>");
              main.getLogManager().info("<warn>The version <highlight>" + main.getMain().getDescription().getVersion()
                      + "</highlight> is not the latest version (<Green>" + result.getNewestVersion() + "</green>)!");
              main.getLogManager().info("Please update the plugin here: <highlight2>https://www.notquests.com/update</highlight2>");
              main.getLogManager().info("<unimportant>---------------------------------------------------------------------------------</unimportant>");
              updateAvailable = true;
              return;
          }

          updateAvailable = false;
          UpdateChecker.UpdateReason reason = result.getReason();
          if (reason == UpdateChecker.UpdateReason.UP_TO_DATE) {
              main.getLogManager().info("<success>Your version of NotQuests (<green>" + result.getNewestVersion() + ")</green> is up to date!");
          } else if (reason == UpdateChecker.UpdateReason.UNRELEASED_VERSION) {
              main.getLogManager().info("Your version of NotQuests (<highlight>" + result.getNewestVersion() + ")</highlight> is more recent than the one publicly available. Are you on a development build?");
          } else {
              main.getLogManager().warn("Could not check for a new version of NotQuests. Reason: <highlight>" + reason);
          }
      });*/

    } catch (Exception e) {
      e.printStackTrace();
      main.getLogManager().warn("Unable to check for updates ('" + e.getMessage() + "').");
    }
  }

  /*public String convertQuestRequirementTypeToConditionType(final String questName, final String requirementID) {
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
          final ConfigurationSection objectiveDependenciesConfigurationSection = main.getDataManager().getQuestsConfig().getConfigurationSection("quests." + quest.getIdentifier()  + ".objectives." + objective.getObjectiveID() + ".dependantObjectives.");
          if (objectiveDependenciesConfigurationSection != null) {
              main.getLogManager().info("Converting old objective dependencies to objective conditions...");
              for (String objectiveDependencyNumber : objectiveDependenciesConfigurationSection.getKeys(false)) {
                  //Get old stuff
                  int dependantObjectiveID = main.getDataManager().getQuestsConfig().getInt("quests." + quest.getIdentifier()  + ".objectives." + (objective.getObjectiveID()) + ".dependantObjectives." + objectiveDependencyNumber + ".objectiveID", objective.getObjectiveID());

                  //Delete old stuff
                  main.getDataManager().getQuestsConfig().set("quests." + quest.getIdentifier()  + ".objectives." + objective.getObjectiveID() + ".dependantObjectives", null);

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
                  main.getLogManager().warn("Action has an empty console command. This should NOT be possible! Creating an action with an empty console command... Action name: <highlight>" + actionIdentifier + "</highlight>");
              }

              ConsoleCommandAction consoleCommandAction = new ConsoleCommandAction(main);
              consoleCommandAction.setConsoleCommand(consoleCommand);
              consoleCommandAction.setActionName(actionIdentifier);

              main.getActionsYMLManager().addAction(actionIdentifier, consoleCommandAction);

              main.getLogManager().info("Migrated the following action from quests.yml to actions.yml: <highlight>" + actionIdentifier + "</highlight>");
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
  }*/
}
