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

package rocks.gravili.notquests.Managers;

import cloud.commandframework.Command;
import cloud.commandframework.bukkit.CloudBukkitCapabilities;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.Commands.CommandNotQuests;
import rocks.gravili.notquests.Commands.old.CommandNotQuestsAdmin;
import rocks.gravili.notquests.NotQuests;

import java.util.function.Function;

public class CommandManager {
    private final NotQuests main;
    private final boolean useNewCommands = false;
    private PaperCommandManager<CommandSender> commandManager;
    private MinecraftHelp<CommandSender> minecraftHelp;


    public CommandManager(final NotQuests main) {
        this.main = main;
    }

    public void setupCommands() {


        if (!useNewCommands) {
            final PluginCommand notQuestsAdminCommand = main.getCommand("notquestsadmin");
            if (notQuestsAdminCommand != null) {
                final CommandNotQuestsAdmin commandNotQuestsAdmin = new CommandNotQuestsAdmin(main);
                notQuestsAdminCommand.setTabCompleter(commandNotQuestsAdmin);
                notQuestsAdminCommand.setExecutor(commandNotQuestsAdmin);
            }
            //Register the notquests command & tab completer. This command will be used by Players
            final PluginCommand notQuestsCommand = main.getCommand("notquests");
            if (notQuestsCommand != null) {
                final CommandNotQuests commandNotQuests = new CommandNotQuests(main);
                notQuestsCommand.setExecutor(commandNotQuests);
                notQuestsCommand.setTabCompleter(commandNotQuests);
            }
        } else {
            //Cloud command framework
            try {
                commandManager = new PaperCommandManager<>(
                        /* Owning plugin */ main,
                        /* Coordinator function */ CommandExecutionCoordinator.simpleCoordinator(),
                        /* Command Sender -> C */ Function.identity(),
                        /* C -> Command Sender */ Function.identity()
                );
            } catch (final Exception e) {
                main.getLogManager().severe("There was an error setting up the commands.");
                return;
            }


            //asynchronous completions
            if (commandManager.queryCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
                commandManager.registerAsynchronousCompletions();
            }

            minecraftHelp = new MinecraftHelp<>(
                    "/notquestsadmin help",
                    main.adventure()::sender,
                    commandManager
            );

            constructCommands();
        }


    }



    public void constructCommands() {

        // /ag
        final Command.Builder<CommandSender> agBuilder = commandManager.commandBuilder("notquestsadmin", "qa");
        commandManager.command(agBuilder.meta(CommandMeta.DESCRIPTION, "fwefwe")
                .senderType(Player.class)
                .handler(commandContext -> {
                    minecraftHelp.queryCommands(commandContext.getOrDefault("", ""), commandContext.getSender());
                }));


    }
}
