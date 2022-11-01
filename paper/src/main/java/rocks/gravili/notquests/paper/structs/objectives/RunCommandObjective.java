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

package rocks.gravili.notquests.paper.structs.objectives;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class RunCommandObjective extends Objective {

  private String commandToRun;
  private boolean ignoreCase;
  private boolean cancelCommand;

  public RunCommandObjective(NotQuests main) {
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
                NumberVariableValueArgument.newBuilder("amount", main, null),
                ArgumentDescription.of("Amount of times the command needs to be run"))
            .argument(
                StringArgument.<CommandSender>newBuilder("Command")
                    .withSuggestionsProvider(
                        (context, lastString) -> {
                          final List<String> allArgs = context.getRawInput();
                          main.getUtilManager()
                              .sendFancyCommandCompletion(
                                  context.getSender(),
                                  allArgs.toArray(new String[0]),
                                  "[Enter command (put between \" \" if you want to use spaces)]",
                                  "");

                          ArrayList<String> completions = new ArrayList<>();

                          completions.add(
                              "<Enter command (put between \" \" if you want to use spaces)>");
                          return completions;
                        })
                    .quoted()
                    .build(),
                ArgumentDescription.of("Command"))
            .flag(
                manager
                    .flagBuilder("ignoreCase")
                    .withDescription(
                        ArgumentDescription.of(
                            "Makes it so it doesn't matter whether the player uses uppercase or lowercase characters")))
            .flag(
                manager
                    .flagBuilder("cancelCommand")
                    .withDescription(
                        ArgumentDescription.of(
                            "Makes it so the command will be cancelled (not actually run) when entered while this objective is active")))
            .handler(
                (context) -> {
                  String command = context.get("Command");
                  final String amountExpression = context.get("amount");
                  final boolean ignoreCase = context.flags().isPresent("ignoreCase");
                  final boolean cancelCommand = context.flags().isPresent("cancelCommand");

                  if (!command.startsWith("/")) {
                    command = "/" + command;
                  }

                  RunCommandObjective runCommandObjective = new RunCommandObjective(main);
                  runCommandObjective.setProgressNeededExpression(amountExpression);
                  runCommandObjective.setCommandToRun(command);
                  runCommandObjective.setIgnoreCase(ignoreCase);
                  runCommandObjective.setCancelCommand(cancelCommand);

                  main.getObjectiveManager().addObjective(runCommandObjective, context, level);
                }));
  }

  @Override
  public String getTaskDescriptionInternal(
      final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
    return main.getLanguageManager()
        .getString(
            "chat.objectives.taskDescription.runCommand.base",
            questPlayer,
            activeObjective,
            Map.of("%COMMANDTORUN%", getCommandToRun()));
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.commandToRun", getCommandToRun());
    configuration.set(initialPath + ".specifics.ignoreCase", isIgnoreCase());
    configuration.set(initialPath + ".specifics.cancelCommand", isCancelCommand());
  }

  @Override
  public void load(FileConfiguration configuration, String initialPath) {
    commandToRun = configuration.getString(initialPath + ".specifics.commandToRun");
    ignoreCase = configuration.getBoolean(initialPath + ".specifics.ignoreCase", false);
    cancelCommand = configuration.getBoolean(initialPath + ".specifics.cancelCommand", false);
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

  public final String getCommandToRun() {
    return commandToRun;
  }

  public void setCommandToRun(final String commandToRun) {
    this.commandToRun = commandToRun;
  }

  public final boolean isIgnoreCase() {
    return ignoreCase;
  }

  public void setIgnoreCase(final boolean ignoreCase) {
    this.ignoreCase = ignoreCase;
  }

  public final boolean isCancelCommand() {
    return cancelCommand;
  }

  public void setCancelCommand(final boolean cancelCommand) {
    this.cancelCommand = cancelCommand;
  }
}
