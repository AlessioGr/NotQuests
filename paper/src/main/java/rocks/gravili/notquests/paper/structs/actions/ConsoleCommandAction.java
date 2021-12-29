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

package rocks.gravili.notquests.paper.structs.actions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.CommandSelector;

public class ConsoleCommandAction extends Action {

    private String consoleCommand = "";


    public ConsoleCommandAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ActionFor rewardFor) {
        manager.command(builder.literal("ConsoleCommand")
                .argument(CommandSelector.<CommandSender>newBuilder("Console Command", main).build(), ArgumentDescription.of("Command which will be executed from the console as a reward. A '/' at the beginning is not required."))
                .meta(CommandMeta.DESCRIPTION, "Adds a new ConsoleCommand Reward to a quest")
                .handler((context) -> {
                    final String consoleCommand = String.join(" ", (String[]) context.get("Console Command"));

                    ConsoleCommandAction consoleCommandAction = new ConsoleCommandAction(main);
                    consoleCommandAction.setConsoleCommand(consoleCommand);

                    main.getActionManager().addAction(consoleCommandAction, context);

                }));
    }

    public void setConsoleCommand(final String consoleCommand) {
        this.consoleCommand = consoleCommand;
    }

    @Override
    public void execute(final Player player, Object... objects) {
        if (consoleCommand.isBlank()) {
            main.getLogManager().warn("Tried to execute ConsoleCommand action with invalid console command.");
            return;
        }

        final String rewardConsoleCommand = main.getUtilManager().applyPlaceholders(consoleCommand, player, getQuest(), objects);

        final ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();

        if (Bukkit.isPrimaryThread()) {
            Bukkit.dispatchCommand(console, rewardConsoleCommand);
        } else {
            Bukkit.getScheduler().runTask(main.getMain(), () -> Bukkit.dispatchCommand(console, rewardConsoleCommand));
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


    public final String getConsoleCommand() {
        return consoleCommand;
    }

    @Override
    public String getActionDescription() {
        return "Reward Command: " + getConsoleCommand();
    }
}
