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

package rocks.gravili.notquests.paper.structs.objectives.hooks.betonquest;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.betonquest.betonquest.api.config.quest.QuestPackage;
import org.betonquest.betonquest.config.Config;
import org.betonquest.betonquest.exceptions.ObjectNotFoundException;
import org.betonquest.betonquest.id.ObjectiveID;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.Objective;

public class BetonQuestObjectiveStateChangeObjective extends Objective {
  private String packageName = "";
  private String objectiveName = "";
  private ObjectiveID cachedObjectiveID = null;
  private org.betonquest.betonquest.api.Objective.ObjectiveState objectiveState = null;

  public BetonQuestObjectiveStateChangeObjective(NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      NotQuests main,
      PaperCommandManager<CommandSender> manager,
      Command.Builder<CommandSender> addObjectiveBuilder,
      final int level) {
    manager.command(
        addObjectiveBuilder
            .argument(
                StringArgument.<CommandSender>newBuilder("package")
                    .withSuggestionsProvider(
                        (context, lastString) -> {
                          final ArrayList<String> completions =
                              new ArrayList<>(Config.getPackages().keySet());

                          final List<String> allArgs = context.getRawInput();
                          main.getUtilManager()
                              .sendFancyCommandCompletion(
                                  context.getSender(),
                                  allArgs.toArray(new String[0]),
                                  "[Package Name]",
                                  "[Event Name]");

                          return completions;
                        })
                    .build(),
                ArgumentDescription.of("BetonQuest Event Package Name"))
            .argument(
                StringArgument.<CommandSender>newBuilder("objective")
                    .withSuggestionsProvider(
                        (context, lastString) -> {
                          String packageName = context.get("package");
                          final QuestPackage configPack = Config.getPackages().get(packageName);
                          ConfigurationSection objectivesFileConfiguration =
                              configPack.getConfig().getConfigurationSection("objectives");
                          if (objectivesFileConfiguration == null) {
                            return new ArrayList<>();
                          }
                          final ArrayList<String> completions =
                              new ArrayList<>(objectivesFileConfiguration.getKeys(false));

                          final List<String> allArgs = context.getRawInput();
                          main.getUtilManager()
                              .sendFancyCommandCompletion(
                                  context.getSender(),
                                  allArgs.toArray(new String[0]),
                                  "[Objective Name]",
                                  "[...]");

                          return completions;
                        })
                    .build(),
                ArgumentDescription.of("BetonQuest Objective Name"))
            .argument(
                StringArgument.<CommandSender>newBuilder("objectiveState")
                    .withSuggestionsProvider(
                        (context, lastString) -> {
                          final ArrayList<String> completions = new ArrayList<>();
                          for (org.betonquest.betonquest.api.Objective.ObjectiveState
                              objectiveState :
                                  org.betonquest.betonquest.api.Objective.ObjectiveState.values()) {
                            completions.add(objectiveState.name());
                          }

                          final List<String> allArgs = context.getRawInput();
                          main.getUtilManager()
                              .sendFancyCommandCompletion(
                                  context.getSender(),
                                  allArgs.toArray(new String[0]),
                                  "[Objective State]",
                                  "[...]");

                          return completions;
                        })
                    .build(),
                ArgumentDescription.of("BetonQuest Objective State"))
            .handler(
                (context) -> {
                  final String packageName = context.get("package");
                  final String objectiveName = context.get("objective");
                  final String objectiveStateString = context.get("objectiveState");

                  try {
                    final QuestPackage configPack = Config.getPackages().get(packageName);
                    ObjectiveID cachedObjectiveID = new ObjectiveID(configPack, objectiveName);
                  } catch (final ObjectNotFoundException e) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<error>Error: The BetonQuest Objective <highlight>"
                                    + packageName
                                    + "."
                                    + objectiveName
                                    + " does not exist!"));
                    return;
                  }

                  org.betonquest.betonquest.api.Objective.ObjectiveState objectiveState = null;
                  try {
                    objectiveState =
                        org.betonquest.betonquest.api.Objective.ObjectiveState.valueOf(
                            objectiveStateString);
                  } catch (Exception e) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<error>Error: The BetonQuest Objective State <highlight>"
                                    + objectiveName
                                    + " does not exist!"));
                    return;
                  }
                  if (objectiveState == null) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<error>Error: The BetonQuest Objective State <highlight>"
                                    + objectiveName
                                    + " does not exist!"));
                    return;
                  }

                  BetonQuestObjectiveStateChangeObjective betonQuestObjectiveStateChangeObjective =
                      new BetonQuestObjectiveStateChangeObjective(main);
                  betonQuestObjectiveStateChangeObjective.setPackageName(packageName);
                  betonQuestObjectiveStateChangeObjective.setObjectiveName(objectiveName);
                  betonQuestObjectiveStateChangeObjective.setObjectiveState(objectiveState);

                  main.getObjectiveManager()
                      .addObjective(betonQuestObjectiveStateChangeObjective, context, level);
                }));
  }

  public final String getPackageName() {
    return packageName;
  }

  public void setPackageName(final String packageName) {
    this.packageName = packageName;
  }

  public final String getObjectiveName() {
    return objectiveName;
  }

  public void setObjectiveName(final String objectiveName) {
    this.objectiveName = objectiveName;
  }

  public final org.betonquest.betonquest.api.Objective.ObjectiveState getObjectiveState() {
    return objectiveState;
  }

  public void setObjectiveState(
      final org.betonquest.betonquest.api.Objective.ObjectiveState objectiveState) {
    this.objectiveState = objectiveState;
  }

  public final ObjectiveID getBQObjectiveID() {
    if (cachedObjectiveID == null) {
      final QuestPackage configPack = Config.getPackages().get(getPackageName());
      try {
        cachedObjectiveID = new ObjectiveID(configPack, getObjectiveName());
      } catch (final ObjectNotFoundException e) {
        main.getLogManager()
            .warn(
                "Tried to get BetonQuestCompleteObjective Objective Objective, but the BetonQuest objective was not found: "
                    + e.getMessage());
        return null;
      }
    }
    return cachedObjectiveID;
  }

  @Override
  public String getTaskDescriptionInternal(
      final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
    return main.getLanguageManager()
        .getString(
            "chat.objectives.taskDescription.BetonQuestCompleteObjective.base",
            questPlayer,
            activeObjective,
            Map.of("%BETONQUESTOBJECTIVENAME%", getObjectiveName()));
  }

  @Override
  public void onObjectiveUnlock(
      final ActiveObjective activeObjective,
      final boolean unlockedDuringPluginStartupQuestLoadingProcess) {}

  @Override
  public void onObjectiveCompleteOrLock(
      final ActiveObjective activeObjective,
      final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
      final boolean completed) {}

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.packageName", getPackageName());
    configuration.set(initialPath + ".specifics.objectiveName", getObjectiveName());
    configuration.set(initialPath + ".specifics.objectiveState", getObjectiveState().name());
  }

  @Override
  public void load(FileConfiguration configuration, String initialPath) {
    this.packageName = configuration.getString(initialPath + ".specifics.packageName");
    this.objectiveName = configuration.getString(initialPath + ".specifics.objectiveName");
    this.objectiveState =
        org.betonquest.betonquest.api.Objective.ObjectiveState.valueOf(
            configuration.getString(initialPath + ".specifics.objectiveState"));
  }

  public String getObjectiveFullID() {
    return getPackageName() + "." + getObjectiveName();
  }
}
