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

package rocks.gravili.notquests.paper.structs.objectives;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;

import java.util.ArrayList;
import java.util.List;

public class RunCommandObjective extends Objective {

    private String commandToRun;
    private boolean ignoreCase;
    private boolean cancelCommand;

    public RunCommandObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Amount of times the command needs to be run."))
                .argument(StringArgument.<CommandSender>newBuilder("Command").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Enter command (put between \" \" if you want to use spaces)]", "");

                            ArrayList<String> completions = new ArrayList<>();

                            completions.add("<Enter command (put between \" \" if you want to use spaces)>");
                            return completions;
                        }
                ).quoted().build(), ArgumentDescription.of("Command"))
                .flag(
                        manager.flagBuilder("ignoreCase")
                                .withDescription(ArgumentDescription.of("Makes it so it doesn't matter whether the player uses uppercase or lowercase characters"))
                )
                .flag(
                        manager.flagBuilder("cancelCommand")
                                .withDescription(ArgumentDescription.of("Makes it so the command will be cancelled (not actually run) when entered while this objective is active"))
                )
                .meta(CommandMeta.DESCRIPTION, "Adds a new RunCommand Objective to a quest")
                .handler((context) -> {
                    String command = context.get("Command");
                    final int amount = context.get("amount");
                    final boolean ignoreCase = context.flags().isPresent("ignoreCase");
                    final boolean cancelCommand = context.flags().isPresent("cancelCommand");

                    if (!command.startsWith("/")) {
                        command = "/" + command;
                    }

                    RunCommandObjective runCommandObjective = new RunCommandObjective(main);
                    runCommandObjective.setProgressNeeded(amount);
                    runCommandObjective.setCommandToRun(command);
                    runCommandObjective.setIgnoreCase(ignoreCase);
                    runCommandObjective.setCancelCommand(cancelCommand);

                    main.getObjectiveManager().addObjective(runCommandObjective, context);
                }));
    }

    public void setCommandToRun(final String commandToRun) {
        this.commandToRun = commandToRun;
    }

    public void setIgnoreCase(final boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    public void setCancelCommand(final boolean cancelCommand) {
        this.cancelCommand = cancelCommand;
    }

    @Override
    public String getObjectiveTaskDescription(final String eventualColor, final Player player) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.runCommand.base", player)
                .replace("%EVENTUALCOLOR%", eventualColor)
                .replace("%COMMANDTORUN%", getCommandToRun());
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
    public void onObjectiveUnlock(ActiveObjective activeObjective) {

    }

    public final String getCommandToRun() {
        return commandToRun;
    }

    public final long getAmountToRun() {
        return super.getProgressNeeded();
    }

    public final boolean isIgnoreCase() {
        return ignoreCase;
    }

    public final boolean isCancelCommand() {
        return cancelCommand;
    }
}
