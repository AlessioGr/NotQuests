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

package rocks.gravili.notquests.paper.structs.actions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.CommandSelector;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class ConsoleCommandAction extends Action {

  private String consoleCommand = "";

  public ConsoleCommandAction(final NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      NotQuests main,
      PaperCommandManager<CommandSender> manager,
      Command.Builder<CommandSender> builder,
      ActionFor actionFor) {
    manager.command(
        builder
            .argument(
                CommandSelector.<CommandSender>newBuilder("Console Command", main).build(),
                ArgumentDescription.of(
                    "Command which will be executed from the console as a reward. A '/' at the beginning is not required."))
            .handler(
                (context) -> {
                  final String consoleCommand =
                      String.join(" ", (String[]) context.get("Console Command"));

                  ConsoleCommandAction consoleCommandAction = new ConsoleCommandAction(main);
                  consoleCommandAction.setConsoleCommand(consoleCommand);

                  main.getActionManager().addAction(consoleCommandAction, context, actionFor);
                }));
  }

  @Override
  public void executeInternally(final QuestPlayer questPlayer, Object... objects) {
    if (consoleCommand.isBlank()) {
      main.getLogManager()
          .warn("Tried to execute ConsoleCommand action with invalid console command.");
      return;
    }

    final String rewardConsoleCommand =
        main.getUtilManager()
            .applyPlaceholders(
                consoleCommand, questPlayer.getPlayer(), questPlayer, getObjectiveHolder(), objects);

    final ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();

    if (Bukkit.isPrimaryThread()) {
      Bukkit.dispatchCommand(console, rewardConsoleCommand);
    } else {
      Bukkit.getScheduler()
          .runTask(main.getMain(), () -> Bukkit.dispatchCommand(console, rewardConsoleCommand));
    }
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.consoleCommand", getConsoleCommand());
  }

  @Override
  public void load(final FileConfiguration configuration, String initialPath) {
    this.consoleCommand = configuration.getString(initialPath + ".specifics.consoleCommand");
  }

  @Override
  public void deserializeFromSingleLineString(ArrayList<String> arguments) {
    this.consoleCommand = String.join(" ", arguments);
  }

  public final String getConsoleCommand() {
    return consoleCommand;
  }

  public void setConsoleCommand(final String consoleCommand) {
    this.consoleCommand = consoleCommand;
  }

  @Override
  public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
    return "Console Command: " + getConsoleCommand();
  }
}
