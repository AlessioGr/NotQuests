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

package rocks.gravili.notquests.Commands.newCMDs;

import cloud.commandframework.Command;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.NotQuests;

public class AdminEditCommands {
    private final NotQuests main;
    private final PaperCommandManager<CommandSender> manager;
    private final Command.Builder<CommandSender> builder;

    public AdminEditCommands(final NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        this.main = main;
        this.manager = manager;
        this.builder = builder;
    }
}
