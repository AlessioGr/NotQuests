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

package rocks.gravili.notquests.paper.structs.objectives.hooks.projectkorra;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import rocks.gravili.notquests.paper.structs.objectives.Objective;

public class ProjectKorraUseAbilityObjective extends Objective {
  private String abilityName = "";

  public ProjectKorraUseAbilityObjective(NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      NotQuests main,
      PaperCommandManager<CommandSender> manager,
      Command.Builder<CommandSender> addObjectiveBuilder,
      final int level) {
    if (!main.getIntegrationsManager().isProjectKorraEnabled()) {
      return;
    }

    manager.command(
        addObjectiveBuilder
            .argument(
                StringArgument.<CommandSender>newBuilder("Ability")
                    .withSuggestionsProvider(
                        (context, lastString) -> {
                          final List<String> allArgs = context.getRawInput();
                          main.getUtilManager()
                              .sendFancyCommandCompletion(
                                  context.getSender(),
                                  allArgs.toArray(new String[0]),
                                  "[Ability Name]",
                                  "");

                          return main.getIntegrationsManager()
                              .getProjectKorraManager()
                              .getAbilityCompletions();
                        })
                    .single()
                    .build(),
                ArgumentDescription.of("Name of the ability"))
            .argument(
                NumberVariableValueArgument.newBuilder("amount", main, null),
                ArgumentDescription.of("Amount of times to use the ability"))
            .handler(
                (context) -> {
                  String abilityName = context.get("Ability");

                  if (!main.getIntegrationsManager()
                      .getProjectKorraManager()
                      .isAbility(abilityName)) {
                    context
                        .getSender()
                        .sendMessage(
                            main.parse(
                                "<error>Error: The ability <highlight>"
                                    + abilityName
                                    + "</highlight> was not found."));
                    return;
                  }

                  ProjectKorraUseAbilityObjective projectKorraUseAbilityObjective =
                      new ProjectKorraUseAbilityObjective(main);
                  projectKorraUseAbilityObjective.setProgressNeededExpression(
                      context.get("amount"));
                  projectKorraUseAbilityObjective.setAbilityName(abilityName);

                  main.getObjectiveManager().addObjective(projectKorraUseAbilityObjective, context, level);
                }));
  }

  public final String getAbilityName() {
    return abilityName;
  }

  public void setAbilityName(final String abilityName) {
    this.abilityName = abilityName;
  }

  @Override
  public String getTaskDescriptionInternal(
      final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
    return main.getLanguageManager()
        .getString(
            "chat.objectives.taskDescription.ProjectKorraUseAbility.base",
            questPlayer,
            activeObjective,
            Map.of("%ABILITY%", getAbilityName()));
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.ability", getAbilityName());
  }

  @Override
  public void load(FileConfiguration configuration, String initialPath) {
    abilityName = configuration.getString(initialPath + ".specifics.ability");
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
}
