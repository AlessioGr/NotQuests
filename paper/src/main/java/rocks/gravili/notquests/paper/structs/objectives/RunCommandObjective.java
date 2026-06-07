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

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.Command;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.suggestion.Suggestion;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.incendo.cloud.parser.standard.StringParser.quotedStringParser;
import static rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueParser.numberVariableParser;

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
    manager.command(addObjectiveBuilder
            .required("amount", numberVariableParser("amount", null), Description.of("Amount of times the command needs to be run"))
                    .required("Command", quotedStringParser(), Description.of("Command to run"), (context, lastString) -> {
                        main.getUtilManager().sendFancyCommandCompletion(context.sender(), lastString.input().split(" "), "[Enter command (put between \" \" if you want to use spaces)]", "");
                        ArrayList<Suggestion> completions = new ArrayList<>();
                        completions.add(Suggestion.suggestion("<Enter command (put between \" \" if you want to use spaces)>"));
                        return CompletableFuture.completedFuture(completions);
                    })
            .flag(manager.flagBuilder("ignoreCase").withDescription(Description.of("Makes it so it doesn't matter whether the player uses uppercase or lowercase characters")))
            .flag(manager.flagBuilder("cancelCommand").withDescription(Description.of("Makes it so the command will be cancelled (not actually run) when entered while this objective is active")))
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
